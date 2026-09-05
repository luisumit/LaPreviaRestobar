# La Previa Restobar — Guía para el equipo

POS (punto de venta) para un restobar en Perú. Multiplataforma con **Kotlin
Multiplatform**: corre en **Android** (celular) y en **Escritorio** (Windows).

---

## 📁 Cómo está estructurado

| Carpeta | Qué es |
|---|---|
| `app/` | App **Android** (Kotlin + Jetpack Compose). Es lo que corre en el celular. |
| `shared/` | Módulo **compartido** (KMP): dominio, modelos, repositorios de Firebase (GitLive), base local (Room), motor de impresión. Lo usan Android y Escritorio. |
| `desktopApp/` | Panel de **Escritorio** (Compose for Desktop). Caja, cocina (KDS), reportes. |
| `public-menu/` | Menú web para clientes (se lee por QR). |
| `docs/` | Documentación: plan de migración KMP, arquitectura DDD, **SEGURIDAD.md**. |
| `database.rules.json` | Reglas de seguridad de Firebase Realtime Database. |

**Arquitectura:** Clean Architecture + DDD (Money, Order como aggregate) +
Hexagonal. Inyección de dependencias con **Koin**. Backend: **Firebase**
(Auth + Realtime Database). ~94 tests unitarios en `app/src/test/`.

---

## ✅ Requisitos

- **Android Studio** (versión reciente, Ladybug o superior). Trae su propio JDK.
- No hace falta instalar Gradle: el proyecto usa el *wrapper* (`gradlew`).
- El archivo `app/google-services.json` **ya viene incluido** (config de Firebase).

---

## ▶️ Opción rápida: probar en el celular SIN compilar

Dentro de este ZIP está el archivo **`LaPreviaRestobar-RAPIDO.apk`**.

1. Pásalo a tu celular Android (por cable, WhatsApp, Drive, etc.).
2. Ábrelo en el cel → permite "instalar de fuentes desconocidas" si lo pide.
3. Instálalo y ábrelo. Entra con una de las cuentas de prueba (abajo).

> Es una app interna (no está en Play Store), por eso Android pide confirmación
> al instalar. Es normal.

---

## 🛠️ Opción completa: abrir el código y correr desde Android Studio

1. Descomprime el ZIP.
2. Android Studio → **Open** → elige la carpeta `LaPreviaRestobar`.
3. Espera a que sincronice Gradle (la primera vez baja dependencias, tarda).
4. Conecta tu celular por USB con **Depuración USB** activada
   (Ajustes → Opciones de desarrollador → Depuración por USB).
5. Arriba, elige tu dispositivo y el módulo **`app`** → botón **Run ▶**.

Para generar un APK a mano:
```
gradlew :app:assembleDebug
```
El APK queda en `app/build/outputs/apk/debug/`.

### ⚠️ Si compilas por consola (no desde Android Studio)
Usa el JDK que trae Android Studio, o Gradle falla:
```
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
gradlew :app:assembleDebug
```
(Dentro de Android Studio esto es automático, no hace falta.)

### Panel de escritorio (opcional)
```
gradlew :desktopApp:run
```

---

## 👤 Cuentas de prueba

| Correo | Rol | Qué ve |
|---|---|---|
| `nuevoadmin@restobar.com` | Admin | Todo: caja, reportes, productos |
| `lolera@restobar.com` | Mesero | Toma pedidos por mesa |
| `nuevococinero@restobar.com` | Cocinero | Pantalla de cocina (comandas) |

> La contraseña de cada cuenta la tiene el dueño. Pídesela, o que te resetee una
> desde la consola de Firebase (Authentication → Usuarios → Restablecer contraseña).

---

## 🔒 Importante

- La app se conecta a la **base de datos real** del restobar (Firebase). Lo que
  hagas probando (pedidos, cobros) **se escribe en los datos reales**. Usa mesas
  de prueba y avisa antes de trastear.
- El **registro está cerrado**: no se pueden crear cuentas nuevas desde la app;
  las crea el dueño en la consola de Firebase.
- No subas `google-services.json` a un repo público ni lo compartas fuera del
  equipo.
