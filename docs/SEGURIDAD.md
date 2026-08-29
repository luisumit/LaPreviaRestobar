# Guía de seguridad — La Previa Restobar

Fecha: 2026-08-28. Cubre el endurecimiento de la autenticación y la base de datos
(Firebase Realtime Database) del POS.

---

## Resumen: ¿qué tan seguro era el login?

El **login en sí es sólido**: Firebase Authentication maneja las contraseñas
(hashing, throttling de intentos, HTTPS). Tu app nunca guarda ni escribe la
contraseña en logs, y usa el SDK oficial. Que la `apiKey` esté embebida en el
APK **no es una falla** — es un identificador público, no un secreto.

El riesgo real **no estaba en el login sino en la autorización detrás de él**:

| # | Hallazgo | Severidad | Estado |
|---|----------|-----------|--------|
| 1 | Las reglas solo verificaban `auth != null`, no el rol: cualquier cuenta logueada podía leer/escribir TODOS los pedidos y mesas. | 🔴 Crítico | Se cierra con Paso 2 (queda riesgo insider) |
| 2 | Registro público probablemente abierto: cualquiera con la apiKey podía crear cuenta y entrar. | 🔴 Crítico | Requiere Paso 2 (consola) |
| 3 | `products` con lectura pública exponía `costPrice` (tus márgenes) en el menú web. | 🟠 Alto | Documentado (ver "Menú público") |
| 4 | El nodo `users` no tenía regla → los roles no se persistían y quedaban en MESERO. | 🟠 Alto | Corregido en reglas |
| 5 | App Check instalado pero sin *enforcement*; el desktop no tiene App Check. | 🟡 Medio | Paso 3 |
| 6 | Token de sesión del desktop en texto plano en `~/.laprevia-desktop-auth.properties`. | 🟡 Medio | Aceptable en PC del dueño |

---

## PASO 0 — OBLIGATORIO antes de desplegar las reglas

Las reglas nuevas hacen que **editar productos** y **cerrar caja** sean solo para
cuentas con rol `ADMIN`. Como hasta ahora los roles no se guardaban bien, primero
debes marcar tu cuenta de dueño como ADMIN **a mano en la consola**, o te
bloquearás de esas funciones.

1. Entra a <https://console.firebase.google.com> → proyecto **laprevia-restobar**.
2. Busca tu UID: **Authentication → Users** → copia el *User UID* de tu cuenta
   de dueño (la que usas en la caja/desktop).
3. Ve a **Realtime Database → Data** y crea/edita este dato:
   ```
   users
     └── <TU_UID>
           └── role: "ADMIN"
   ```
   (Botón `+`, clave `role`, valor `ADMIN` en mayúsculas.)
4. Verifica que quedó `users/<TU_UID>/role = "ADMIN"`.

> Si tienes cuentas para mesero y cocina, ponles `role: "MESERO"` o
> `role: "COCINERO"` igual. Solo el ADMIN edita menú y cierra caja.

---

## PASO 1 — Desplegar las reglas endurecidas

> **Nota:** las reglas que corren HOY en tu proyecto pueden no coincidir con este
> archivo del repo (si el cierre de caja te funcionaba antes, es que lo desplegado
> era más permisivo). Desplegar **reemplaza** lo actual por esto.

El archivo nuevo ya está en `database.rules.json`. Qué hace cada nodo:

- **`products`**: lectura pública (para el menú web con QR), **escritura solo ADMIN**.
- **`tables` / `orders` / `inventory`**: lectura y escritura solo con sesión iniciada.
- **`cash_closures`**: lectura y escritura **solo ADMIN** (son datos financieros).
- **`users/<uid>`**: cada quien lee/escribe **solo su propio registro**; un usuario
  **no puede cambiarse el rol a sí mismo** (regla `.validate` en `role`). Solo un
  ADMIN cambia roles.
- **Raíz**: todo lo demás queda denegado por defecto (`.read/.write: false`).

Desplegar (necesitas Firebase CLI y estar logueado):

```bash
firebase deploy --only database
```

O pega el contenido de `database.rules.json` en la consola:
**Realtime Database → Rules → Publicar**.

> Tras publicar, prueba: entra al panel, edita un producto (debe funcionar como
> ADMIN), cierra caja (debe guardar). Si algo falla con "permission denied",
> revisa el Paso 0.

---

## PASO 2 — Cerrar el registro público (el fix más importante)

Aunque las reglas ya limitan qué hace cada cuenta, mientras cualquiera pueda
**crear** una cuenta, tendrá una sesión válida y acceso a pedidos/mesas. Como es
un restobar con pocas cuentas fijas, cierra el registro:

1. Consola → **Authentication → Sign-in method**.
2. En **Email/Password**, mantenlo habilitado para iniciar sesión, pero
   **crea tú mismo las cuentas** desde **Authentication → Users → Add user**
   (dueño, mesero, cocina). No expongas ningún botón de "Registrarse" en la app.
3. Si en algún momento agregaste un flujo de auto-registro, quítalo. (Hoy la app
   no muestra registro en la UI, así que solo falta no volver a activarlo.)

Con esto, "cualquier cuenta autenticada" pasa a ser "solo tus 2-3 cuentas".
(Queda un riesgo *insider*: un mesero con sesión sigue pudiendo tocar pedidos y
mesas, porque el flujo normal lo necesita. Para un local chico es aceptable; si
más adelante quieres separar eso por rol, se puede afinar `orders`/`tables`.)

---

## PASO 3 — App Check (opcional pero recomendado)

App Check asegura que solo TU app (no scripts externos con la apiKey) hable con
Firebase. Ya está instalado en Android (Play Integrity en release). Para activarlo:

1. Consola → **App Check** → registra la app Android con **Play Integrity**.
2. Activa *enforcement* para **Realtime Database** SOLO cuando confirmes que
   llegan tokens válidos (la consola te muestra el % de tráfico verificado).

> ⚠️ **Cuidado con el panel de escritorio:** el desktop (JVM) no puede generar
> tokens de Play Integrity. Si activas *enforcement* estricto, el panel dejaría
> de conectar. Opciones: (a) dejar App Check en modo monitor/no-enforce mientras
> uses el desktop, o (b) registrar un token de depuración para la PC de la caja
> (App Check → Apps → Debug tokens). Prueba en un horario sin clientes.

---

## Nota: menú público y el precio de costo

El menú web (`public-menu/app.js`) lee todo el nodo `products`, así que descarga
también `costPrice` aunque solo muestre el precio de venta. Cualquiera puede verlo
con las herramientas del navegador. Mantuvimos `products` con lectura pública para
**no romper el menú con QR**, pero si quieres cerrar esa fuga tienes dos caminos:

- **Simple:** no cargues el precio de costo en los productos (déjalo vacío en el
  CRUD). Sin costo guardado, no hay nada que filtrar.
- **Estricto:** crea un nodo aparte `menu_publico` con solo
  `{name, description, category, salePrice, imageUrl, isActive}`, apunta
  `public-menu/app.js` a ese nodo, y cambia la regla de `products` a
  `".read": "auth != null"`. Esto requiere un cambio de código (avísame y lo hago).

---

## Lo que ya estaba bien (no se toca)

- Contraseñas 100% gestionadas por Firebase Auth; nunca se guardan ni loguean.
- Comunicación por HTTPS; anti-fuerza-bruta propio de Firebase.
- SDK oficial, sin superficie de inyección SQL.
- La apiKey pública embebida es normal y esperado.
