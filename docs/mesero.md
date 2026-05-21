# Manual de Usuario – Mesero/a

## Índice

1. [Descripción general del rol](#descripción-general-del-rol)
2. [Objetivo del rol](#objetivo-del-rol)
3. [Permisos y restricciones](#permisos-y-restricciones)
4. [Requisitos previos](#requisitos-previos)
5. [Acceso e inicio de sesión](#acceso-e-inicio-de-sesión)
6. [Pantalla principal](#pantalla-principal)
7. [Gestión de mesas](#gestión-de-mesas)
8. [Toma de pedidos](#toma-de-pedidos)
9. [Consulta de pedidos activos](#consulta-de-pedidos-activos)
10. [Entrega de pedidos listos](#entrega-de-pedidos-listos)
11. [Consulta de inventario](#consulta-de-inventario)
12. [Notificaciones](#notificaciones)
13. [Funcionamiento sin conexión](#funcionamiento-sin-conexión)
14. [Mensajes y estados frecuentes](#mensajes-y-estados-frecuentes)
15. [Flujo completo de trabajo](#flujo-completo-de-trabajo)
16. [Buenas prácticas](#buenas-prácticas)
17. [Solución de problemas frecuentes](#solución-de-problemas-frecuentes)
18. [Recomendaciones finales](#recomendaciones-finales)

---

## Descripción general del rol

El rol de **Mesero/a** es el encargado de atender a los clientes en el salón. Desde la aplicación puedes visualizar el estado de las mesas, tomar pedidos, consultar el menú de productos disponibles, seguir el estado de preparación de cada pedido en tiempo real y entregarlos cuando la cocina los marque como listos.

La aplicación está diseñada para funcionar incluso sin conexión a internet, guardando los pedidos localmente y sincronizándolos automáticamente cuando se recupere la conexión.

---

## Objetivo del rol

Tu objetivo principal es:

- Gestionar las **8 mesas** del restaurante.
- Registrar los pedidos de los clientes.
- Enviar los pedidos a cocina.
- Recibir notificaciones en tiempo real cuando un pedido está listo.
- Entregar los pedidos a los clientes.
- Mantener el control visual del estado de cada mesa.

---

## Permisos y restricciones

### Lo que PUEDES hacer

| Acción | Descripción |
|--------|-------------|
| Ver mesas | Visualizar las 8 mesas con su estado (Libre, Ocupada, Reservada) |
| Seleccionar mesa | Elegir una mesa para tomar un pedido |
| Ver productos | Consultar el catálogo de productos disponibles para la venta |
| Crear pedidos | Agregar productos a un pedido, modificar cantidades y enviarlo a cocina |
| Ver pedidos activos | Consultar todos los pedidos en curso con su estado actual |
| Marcar pedido como entregado | Confirmar que el pedido fue entregado al cliente |
| Ver inventario | Consultar el stock disponible de productos (solo lectura) |
| Recibir notificaciones | Ver alertas de cambios de estado en los pedidos |

### Lo que NO puedes hacer

| Restricción | Descripción |
|-------------|-------------|
| Crear o editar productos | Solo el administrador puede gestionar el catálogo |
| Modificar inventario | No puedes cambiar el stock de productos |
| Cambiar estados de cocina | Solo el cocinero puede aceptar, preparar o marcar listo un pedido |
| Cancelar pedidos aceptados | Solo puedes cancelar pedidos antes de que cocina los acepte |
| Acceder al panel de administración | La pantalla de administrador está bloqueada para tu rol |

---

## Requisitos previos

- Tener una cuenta de usuario con rol **MESERO** registrada en el sistema.
- Tener la aplicación instalada en un dispositivo Android.
- Conexión a internet (recomendada para funcionamiento completo, pero no obligatoria).

---

## Acceso e inicio de sesión

### Paso 1: Abrir la aplicación

Al abrir la aplicación verás la pantalla de inicio con tres roles disponibles:

- 🍽️ **Mesero**
- 👨‍🍳 **Cocinero**
- ⚙️ **Administrador**

### Paso 2: Seleccionar tu rol

Toca el botón **Mesero**. La aplicación te dirigirá a la pantalla de inicio de sesión donde deberás ingresar:

- **Correo electrónico** registrado
- **Contraseña**

Toca **Iniciar Sesión**. Si las credenciales son correctas y tu usuario tiene rol de mesero, accederás a la pantalla principal.

> **Nota:** Si el sistema está configurado en modo rápido (sin autenticación), al tocar "Mesero" ingresarás directamente sin necesidad de credenciales.

---

## Pantalla principal

Una vez dentro del sistema, verás la **Pantalla Principal del Mesero** con los siguientes elementos:

### Barra superior

- **Título:** "LA PREVIA RESTOBAR"
- **Indicador de conexión:** Muestra el estado de la conexión con cocina:
  - 🟢 **Conectado a cocina** – Todo funciona correctamente
  - 🔴 **Sin conexión** – Los pedidos se guardarán localmente
- **Ícono de campana** 🔔 – Acceso al panel de notificaciones (muestra un contador si hay notificaciones nuevas)
- **Botón rojo de salida** – Cierra tu sesión y vuelve a la pantalla de inicio

### Pestañas de navegación

En la parte inferior verás 4 pestañas:

| Pestaña | Ícono | Función |
|---------|-------|---------|
| **Mesas** | 🪑 | Ver y gestionar las 8 mesas del restaurante |
| **Pedidos** | 📋 | Ver todos los pedidos activos y su estado |
| **Menú** | 📖 | Ver el catálogo de productos disponibles |
| **Inventario** | 📦 | Consultar el stock de productos |

### Panel de notificaciones

Al tocar el ícono de campana 🔔 se despliega un panel lateral con las notificaciones recientes sobre cambios de estado en tus pedidos.

---

## Gestión de mesas

La pestaña **Mesas** es tu punto de partida para atender a los clientes.

### Visualización de mesas

Verás una cuadrícula con **8 mesas**. Cada mesa muestra:

- **Número de mesa** (1 al 8)
- **Estado actual** con código de colores:
  - 🟢 **Libre** – Mesa disponible para nuevos clientes
  - 🔴 **Ocupada** – Mesa con un pedido activo
  - 🟡 **Reservada** – Mesa reservada (no disponible)

### Seleccionar una mesa

1. Toca la mesa que deseas atender.
2. La aplicación te dirigirá a la pantalla de **Detalle de Mesa**.
3. Si la mesa está libre, podrás comenzar un nuevo pedido.
4. Si la mesa ya está ocupada, podrás ver su pedido activo y agregar más productos.

### Estados de mesa

| Estado | Significado | Acción posible |
|--------|-------------|----------------|
| **Libre** | Mesa sin clientes | Iniciar un nuevo pedido |
| **Ocupada** | Mesa con pedido activo | Ver pedido, agregar productos |
| **Reservada** | Mesa apartada | No disponible para nuevos pedidos |

> **Importante:** El estado de la mesa cambia automáticamente a **Ocupada** cuando registras un pedido en ella.

---

## Toma de pedidos

### Pantalla de detalle de mesa

Al seleccionar una mesa, verás:

- **Número de mesa** en la parte superior
- **Lista de productos del menú** con nombre, descripción y precio
- **Carrito de pedido actual** con los productos agregados
- **Total del pedido** calculado automáticamente
- **Botón "Enviar a cocina"** para confirmar el pedido

### Agregar productos al pedido

1. Navega por la lista de productos.
2. Toca un producto para **agregarlo al pedido** (cantidad inicial: 1).
3. Si el producto ya está en el carrito, al tocarlo nuevamente **aumenta la cantidad en 1**.
4. Verás un mensaje de confirmación: "✅ [Producto] agregado".

### Modificar cantidades

- Usa los botones **+** y **–** junto a cada producto en el carrito para ajustar la cantidad.
- Si reduces la cantidad a **0**, el producto se elimina del carrito automáticamente.

### Enviar pedido a cocina

Cuando el cliente haya decidido su pedido:

1. Revisa el carrito con todos los productos y cantidades.
2. Verifica el **total** calculado.
3. Toca el botón **"Enviar a cocina"**.
4. El pedido se registra y se envía a la cocina en tiempo real.
5. La mesa cambia automáticamente a estado **Ocupada**.
6. Verás un mensaje de confirmación.

### Estados del pedido

Una vez enviado, el pedido pasa por los siguientes estados:

| Estado | Significado | Quién lo cambia |
|--------|-------------|-----------------|
| **Pendiente** | Recién creado, esperando envío | Sistema (automático) |
| **Enviado** | Llegó a la cocina | Sistema (automático) |
| **Aceptado** | Cocinero confirmó recepción | Cocinero |
| **En preparación** | Se está preparando | Cocinero |
| **Listo** | ¡Listo para entregar! | Cocinero |
| **Entregado** | Entregado al cliente | Tú (Mesero) |
| **Completado** | Pedido finalizado | Sistema/Cocinero |
| **Cancelado** | Pedido anulado | Tú o Cocinero |

---

## Consulta de pedidos activos

La pestaña **Pedidos** muestra todos los pedidos activos en el restaurante.

### Información visible

Cada pedido muestra:

- **Número de mesa**
- **Estado actual** del pedido (con código de colores)
- **Lista de productos** con cantidades
- **Total** del pedido
- **Hora** de creación

### Colores por estado

| Estado | Color |
|--------|-------|
| Pendiente / Enviado | 🟡 Amarillo |
| Aceptado | 🔵 Azul |
| En preparación | 🟠 Naranja |
| Listo | 🟢 Verde |
| Entregado | ⚪ Gris |
| Cancelado | 🔴 Rojo |

---

## Entrega de pedidos listos

Cuando el cocinero marca un pedido como **Listo**:

1. Recibirás una **notificación** en el panel de notificaciones.
2. Verás el mensaje: **"🎉 ¡Orden LISTA! - Mesa [número]"**.
3. El pedido aparecerá en color **verde** en la lista de pedidos.

### Marcar como entregado

1. Ve a la pestaña **Pedidos**.
2. Localiza el pedido en estado **Listo** (verde).
3. Toca el pedido para ver el detalle.
4. Toca el botón **"Marcar como Entregado"**.
5. El pedido pasará a estado **Entregado**.
6. La cocina recibirá una notificación de que la comida fue entregada.

> **Importante:** Solo puedes marcar como entregado un pedido que esté en estado **Listo**.

---

## Consulta de inventario

La pestaña **Inventario** te permite consultar el stock de productos. Esta pantalla es **solo de lectura**.

### Información visible

- **Nombre del producto**
- **Stock actual** (cantidad disponible)
- **Unidad de medida** (unidades)
- **Categoría** del producto

### Filtros

Puedes filtrar los productos por **categoría** usando el menú desplegable en la parte superior.

### Alertas de stock

Si un producto tiene stock bajo o agotado, lo verás reflejado en esta pantalla. Esto te ayuda a informar al cliente si algún producto no está disponible antes de agregarlo al pedido.

> **Importante:** El inventario se actualiza automáticamente cuando el cocinero acepta un pedido y descuenta los ingredientes correspondientes. Mantente atento a los cambios para no ofrecer productos agotados.

---

## Notificaciones

El sistema te mantiene informado mediante notificaciones en tiempo real.

### Tipos de notificaciones

| Notificación | Cuándo aparece |
|--------------|----------------|
| **Cambio de estado** | Cuando cocina modifica el estado de un pedido |
| **Pedido listo** | Cuando cocina marca tu pedido como listo |
| **Pedido cancelado** | Cuando se cancela un pedido |
| **Cambio de conexión** | Cuando se pierde o recupera internet |

### Acceso a notificaciones

Toca el ícono de **campana** 🔔 en la barra superior. Se abrirá un panel lateral con las notificaciones más recientes.

El contador sobre la campana indica cuántas notificaciones no leídas tienes.

---

## Funcionamiento sin conexión

La aplicación está diseñada para funcionar **sin internet**. Esto es lo que sucede:

### Sin conexión a internet

- 🔴 Verás el mensaje: **"📱 SIN INTERNET - Los pedidos se guardarán localmente"**
- Puedes seguir tomando pedidos normalmente.
- Los pedidos se guardan en el dispositivo.
- No recibirás actualizaciones en tiempo real de cocina.
- Las notificaciones de cambio de estado no estarán disponibles.

### Al recuperar la conexión

- 🟢 Verás el mensaje: **"🟢 Internet disponible - Sincronizando..."**
- Todos los pedidos guardados se enviarán automáticamente a la cocina.
- Verás el mensaje: **"✅ [N] pedido(s) sincronizados"**
- Recibirás las actualizaciones de estado pendientes.

> **Importante:** Si estabas sin conexión y la cocina cambió el estado de un pedido, al reconectarte verás el estado actualizado automáticamente.

---

## Mensajes y estados frecuentes

### Mensajes de éxito

| Mensaje | Significado |
|---------|-------------|
| ✅ Mesa [N] seleccionada | Mesa elegida correctamente |
| ✅ [Producto] agregado | Producto añadido al carrito |
| ✅ [N] pedido(s) sincronizados | Pedidos enviados a cocina tras recuperar conexión |
| 🎉 ¡Orden LISTA! - Mesa [N] | La cocina terminó el pedido |

### Mensajes de error

| Mensaje | Significado | Solución |
|---------|-------------|----------|
| Mesa no encontrada | La mesa seleccionada no existe | Intentar de nuevo |
| Error: ID de mesa inválido | ID fuera del rango 1-8 | Seleccionar mesa válida |
| Error cargando datos | Fallo al obtener información | Verificar conexión, reintentar |
| Error de conexión | No se puede contactar con el servidor | Esperar a recuperar internet |

### Estados de carga

- **Indicador giratorio:** La aplicación está cargando datos iniciales
- **"Conectando..."** en la barra: Estableciendo conexión con el servidor

---

## Flujo completo de trabajo

### Ejemplo: Atender una mesa desde cero

1. **Cliente llega** → Abres la pestaña **Mesas**.
2. **Identificas mesa libre** → Mesa 3 está 🟢 Libre.
3. **Seleccionas la mesa** → Toca Mesa 3.
4. **Tomas el pedido:**
   - Cliente pide 2 Pisco Sour → Buscas "Pisco Sour", tocas 2 veces.
   - Cliente pide 1 Tabla de Quesos → Buscas "Tabla de Quesos", tocas 1 vez.
   - Verificas el carrito: 2x Pisco Sour + 1x Tabla de Quesos.
5. **Envías a cocina** → Toca "Enviar a cocina".
6. **Confirmación** → Mensaje de éxito. Mesa 3 pasa a 🔴 Ocupada.
7. **Seguimiento** → En pestaña **Pedidos** ves el pedido en estado "Enviado".
8. **Cocina acepta** → El estado cambia a "Aceptado" → "En preparación".
9. **¡Pedido listo!** → Recibes notificación. El pedido aparece en 🟢 Listo.
10. **Entregas al cliente** → Toca el pedido, luego "Marcar como Entregado".
11. **Pedido completado** → El pedido pasa a estado "Entregado".

### Interacción entre roles

```
MESERO                    COCINA                    ADMIN
  │                         │                         │
  ├─ Selecciona mesa        │                         │
  ├─ Crea pedido ──────────►│                         │
  │                         ├─ Recibe pedido          │
  │                         ├─ Acepta pedido          │
  │   ◄─ Notificación ──────┤                         │
  │                         ├─ En preparación         │
  │                         ├─ ¡Listo!                │
  │   ◄─ Notificación ──────┤                         │
  ├─ Entrega al cliente     │                         │
  ├─ Marca Entregado ──────►│                         │
  │                         ├─ Recibe confirmación    │
  │                         │                         ├─ Gestiona productos
  │                         │                         ├─ Actualiza stock
  │   Productos disponibles ◄─────────────────────────┤
```

---

## Buenas prácticas

1. **Verifica la conexión** antes de empezar tu turno. Una conexión estable garantiza comunicación en tiempo real con cocina.

2. **Revisa el inventario** periódicamente para saber qué productos están disponibles y no ofrecer productos agotados.

3. **Confirma cada pedido** con el cliente antes de enviarlo a cocina. Revisa el carrito y el total.

4. **Atiende las notificaciones** de inmediato. Cuando un pedido está listo, entrégalo lo antes posible para mantener la calidad del servicio.

5. **No cierres sesión** mientras tengas pedidos activos. Si necesitas cambiar de turno, asegúrate de que todos tus pedidos estén en estado Entregado o Completado.

6. **En modo sin conexión:** Los pedidos se guardan localmente. En cuanto vuelva internet, se sincronizarán automáticamente. No es necesario que hagas nada adicional.

7. **Si la cocina no recibe un pedido**, verifica tu estado de conexión. Si estás sin internet, el pedido se enviará automáticamente al reconectarte.

---

## Solución de problemas frecuentes

### Problema: No veo las mesas

- **Causa:** La aplicación está cargando datos o no hay conexión.
- **Solución:** Espera unos segundos. Si el indicador de carga no desaparece, verifica tu conexión a internet.

### Problema: La mesa aparece Libre pero sé que está ocupada

- **Causa:** El estado no se ha sincronizado correctamente.
- **Solución:** Espera a que la aplicación se sincronice. Si el problema persiste, notifica al administrador.

### Problema: No puedo agregar productos a un pedido

- **Causa:** No has seleccionado una mesa primero.
- **Solución:** Ve a la pestaña Mesas, selecciona una mesa y luego agrega productos.

### Problema: El pedido no aparece en la lista de la cocina

- **Causa:** Falta de conexión a internet.
- **Solución:** Verifica el indicador de conexión. Si estás sin internet, el pedido se enviará automáticamente al reconectarte.

### Problema: El botón "Marcar como Entregado" no aparece

- **Causa:** El pedido no está en estado "Listo".
- **Solución:** Solo puedes marcar como entregado pedidos que estén en estado Listo (color verde). Espera a que cocina termine la preparación.

---

## Recomendaciones finales

- **Mantén la aplicación abierta** durante todo tu turno para recibir notificaciones en tiempo real.
- **Revisa el panel de notificaciones** frecuentemente para no perder ninguna actualización.
- **Comunícate con cocina** si un pedido tarda más de lo esperado. La aplicación es una herramienta de apoyo, no reemplaza la comunicación directa.
- **Reporta cualquier error** al administrador del sistema para que pueda solucionarlo.

---

*Documentación generada a partir del análisis del código fuente del proyecto La Previa Restobar. Última actualización: Mayo 2026.*
