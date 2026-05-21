# Manual de Usuario – Cocinero/a

## Índice

1. [Descripción general del rol](#descripción-general-del-rol)
2. [Objetivo del rol](#objetivo-del-rol)
3. [Permisos y restricciones](#permisos-y-restricciones)
4. [Requisitos previos](#requisitos-previos)
5. [Acceso e inicio de sesión](#acceso-e-inicio-de-sesión)
6. [Pantalla principal](#pantalla-principal)
7. [Visualización de pedidos entrantes](#visualización-de-pedidos-entrantes)
8. [Flujo de preparación de pedidos](#flujo-de-preparación-de-pedidos)
9. [Control de inventario en cocina](#control-de-inventario-en-cocina)
10. [Cancelación de pedidos](#cancelación-de-pedidos)
11. [Pedidos entregados por el mesero](#pedidos-entregados-por-el-mesero)
12. [Notificaciones](#notificaciones)
13. [Funcionamiento sin conexión](#funcionamiento-sin-conexión)
14. [Mensajes y estados frecuentes](#mensajes-y-estados-frecuentes)
15. [Flujo completo de trabajo](#flujo-completo-de-trabajo)
16. [Buenas prácticas](#buenas-prácticas)
17. [Solución de problemas frecuentes](#solución-de-problemas-frecuentes)
18. [Recomendaciones finales](#recomendaciones-finales)

---

## Descripción general del rol

El rol de **Cocinero/a** es el responsable de recibir, preparar y despachar los pedidos que envían los meseros desde el salón. La aplicación te muestra en tiempo real todos los pedidos entrantes, te permite actualizar el estado de preparación y te ayuda a controlar el inventario de ingredientes automáticamente.

La interfaz está diseñada para que puedas concentrarte en lo importante: **cocinar**, sin perder de vista los pedidos pendientes ni el estado de cada preparación.

---

## Objetivo del rol

Tu objetivo principal es:

- Visualizar **en tiempo real** todos los pedidos que llegan desde el salón.
- Aceptar los pedidos entrantes para confirmar que los recibiste.
- Cambiar el estado de cada pedido a medida que avanzas en su preparación.
- Marcar los pedidos como **listos** para que el mesero los retire.
- Controlar que el inventario de ingredientes se descuente correctamente.
- Mantener el flujo de trabajo organizado y eficiente.

---

## Permisos y restricciones

### Lo que PUEDES hacer

| Acción | Descripción |
|--------|-------------|
| Ver pedidos activos | Visualizar todos los pedidos no completados ni cancelados |
| Aceptar pedidos | Confirmar la recepción de un pedido enviado por el mesero |
| Iniciar preparación | Marcar un pedido como "En preparación" |
| Marcar como listo | Indicar que el pedido está listo para ser entregado |
| Completar pedido | Finalizar el pedido después de la entrega |
| Cancelar pedido | Anular un pedido si es necesario |
| Ver inventario | Consultar el stock de productos |
| Recibir notificaciones | Ver alertas de nuevos pedidos, cambios y entregas |

### Lo que NO puedes hacer

| Restricción | Descripción |
|-------------|-------------|
| Crear o editar productos | Solo el administrador puede gestionar el catálogo |
| Modificar inventario manualmente | El inventario se descuenta automáticamente al aceptar pedidos |
| Marcar como Entregado | Solo el mesero confirma la entrega al cliente |
| Gestionar mesas | Las mesas son responsabilidad exclusiva del mesero |
| Tomar pedidos | No puedes crear pedidos, solo recibirlos |
| Ver estadísticas | El panel de administración está bloqueado para tu rol |

---

## Requisitos previos

- Tener una cuenta de usuario con rol **COCINERO** registrada en el sistema.
- Tener la aplicación instalada en un dispositivo Android (idealmente una tablet en cocina).
- Conexión a internet (recomendada para funcionamiento completo, pero no obligatoria).

---

## Acceso e inicio de sesión

### Paso 1: Abrir la aplicación

Al abrir la aplicación verás la pantalla de inicio con tres roles disponibles:

- 🍽️ **Mesero**
- 👨‍🍳 **Cocinero**
- ⚙️ **Administrador**

### Paso 2: Seleccionar tu rol

Toca el botón **Cocinero**. La aplicación te dirigirá a la pantalla de inicio de sesión donde deberás ingresar:

- **Correo electrónico** registrado
- **Contraseña**

Toca **Iniciar Sesión**. Si las credenciales son correctas y tu usuario tiene rol de cocinero, accederás a la pantalla principal.

> **Nota:** Si el sistema está configurado en modo rápido (sin autenticación), al tocar "Cocinero" ingresarás directamente sin necesidad de credenciales.

---

## Pantalla principal

Una vez dentro del sistema, verás la **Pantalla Principal del Cocinero** con los siguientes elementos:

### Barra superior

- **Título:** "LA PREVIA RESTOBAR"
- **Subtítulo:** "SISTEMA COCINERO"
- **Indicador de conexión:** Muestra el estado de la conexión:
  - 🟢 **Conectado a Firebase** – Recibiendo pedidos en tiempo real
  - 🔴 **Sin conexión – Modo offline** – Sin comunicación con el salón
- **Ícono de campana** 🔔 – Panel de notificaciones con contador de alertas
- **Botón rojo de salida** – Cierra tu sesión

### Pestañas de navegación

| Pestaña | Función |
|---------|---------|
| **Pedidos** | Lista de todos los pedidos activos para preparar |
| **Inventario** | Consulta del stock de productos |

### Panel de notificaciones

Al tocar el ícono de campana 🔔 se despliega un panel con las notificaciones recientes:

- **Nuevos pedidos** enviados por meseros
- **Pedidos cancelados** por meseros
- **Entregas confirmadas** (comida entregada al cliente)
- **Cambios de inventario** por descuento automático

---

## Visualización de pedidos entrantes

La pestaña **Pedidos** es tu centro de trabajo principal.

### Lista de pedidos

Cada pedido se muestra como una **tarjeta** que contiene:

- **Número de mesa** de donde proviene el pedido
- **Estado actual** del pedido con código de colores
- **Lista de productos** solicitados con cantidades
- **Notas** del mesero (si las hay)
- **Hora** de creación del pedido

### Códigos de colores por estado

| Estado | Color | Significado |
|--------|-------|-------------|
| **Enviado** | 🟡 Amarillo | ¡NUEVO! El mesero acaba de enviar el pedido |
| **Aceptado** | 🔵 Azul | Ya confirmaste la recepción |
| **En preparación** | 🟠 Naranja | Estás preparando el pedido |
| **Listo** | 🟢 Verde | Pedido terminado, esperando que el mesero lo recoja |
| **Entregado** | ⚪ Gris | El mesero ya entregó al cliente |
| **Completado** | ⚪ Gris oscuro | Pedido finalizado (se oculta de la lista) |
| **Cancelado** | 🔴 Rojo | Pedido anulado (se oculta de la lista) |

### Filtros por estado

En la parte superior de la lista puedes filtrar los pedidos por estado para enfocarte en lo que necesitas:

- **Todos** – Muestra todos los pedidos activos
- **Nuevos** – Solo pedidos recién enviados (ENVIADO)
- **En preparación** – Solo los que estás preparando
- **Listos** – Solo los terminados

---

## Flujo de preparación de pedidos

Este es el ciclo de vida de un pedido en cocina y las acciones que debes realizar.

### 1. Recibir un nuevo pedido

Cuando un mesero envía un pedido:

1. El pedido aparece en tu lista en estado **Enviado** (color 🟡 amarillo).
2. Recibes una **notificación** sonora/visual: "🎯 ¡NUEVA ORDEN DEL MESERO!"
3. El contador de notificaciones aumenta.

### 2. Aceptar el pedido

1. Toca el pedido entrante (color amarillo).
2. Revisa los productos solicitados y cantidades.
3. Toca el botón **"Aceptar Pedido"**.
4. El estado cambia a **Aceptado** (color 🔵 azul).
5. El mesero recibe una notificación de que aceptaste el pedido.
6. **Automáticamente**, el sistema descuenta los ingredientes del inventario para los productos que tienen control de stock.

> **Importante:** Al aceptar, el inventario se actualiza automáticamente. Si un producto no tiene stock suficiente, verás una alerta: "⚠️ Stock insuficiente: [Producto]".

### 3. Iniciar preparación

1. Con el pedido en estado **Aceptado**, toca **"Iniciar Preparación"**.
2. El estado cambia a **En preparación** (color 🟠 naranja).
3. Verás el mensaje: "👨‍🍳 Orden en preparación - Mesa [N]".
4. El mesero puede ver que estás preparando el pedido.

### 4. Marcar como listo

1. Cuando termines la preparación, toca **"Marcar como Listo"**.
2. El estado cambia a **Listo** (color 🟢 verde).
3. Verás el mensaje: "🎉 ¡Orden lista! - Mesa [N]".
4. El mesero recibe una **notificación urgente** de que el pedido está listo para entregar.
5. El pedido permanece en tu lista para que sepas que está pendiente de recogida.

### 5. Completar el pedido

Después de que el mesero entregue la comida al cliente:

1. El mesero marca el pedido como **Entregado**.
2. Recibes una notificación: "🍽️ Comida Entregada - Mesa [N]".
3. Puedes tocar **"Completar Pedido"** para finalizarlo.
4. El pedido pasa a estado **Completado** y se oculta de la lista activa.

### Resumen del flujo de estados

```
ENVIADO ──► ACEPTADO ──► EN PREPARACIÓN ──► LISTO ──► ENTREGADO ──► COMPLETADO
  (mesero)   (tú)         (tú)             (tú)     (mesero)      (tú)
                                                          │
                                              CANCELLED ←─┘ (tú o mesero)
```

---

## Control de inventario en cocina

La pestaña **Inventario** te muestra el stock actual de productos.

### Lo que ves en inventario

- **Nombre del producto**
- **Stock actual**
- **Stock mínimo** configurado por el administrador
- **Unidad de medida**
- **Categoría**

### Filtros

Puedes filtrar por **categoría** para ver solo los productos que te interesan (bebidas, comidas, postres, etc.).

### Alertas de stock bajo

Los productos con **stock bajo** (cantidad menor o igual al mínimo configurado) aparecen resaltados como alerta. Esto te permite:

- Anticiparte a posibles faltantes.
- Avisar al administrador para que reponga inventario.
- Informar al mesero si un plato no se puede preparar.

### Descuento automático de inventario

Cuando aceptas un pedido, el sistema **automáticamente** descuenta del inventario los productos marcados con control de stock. Verás una notificación como:

> "📦 Inventario Actualizado: [Producto]: 10 → 8"

Esto significa que se usaron 2 unidades de ese producto y quedan 8.

> **Importante:** No necesitas hacer nada manualmente. El descuento es automático y solo aplica a productos que el administrador marcó con "Control de inventario".

---

## Cancelación de pedidos

Puedes cancelar un pedido si es necesario (por ejemplo, si un cliente cambia de opinión o hay un error).

### Cómo cancelar

1. Toca el pedido que deseas cancelar.
2. Toca el botón **"Cancelar Pedido"**.
3. El estado cambia a **Cancelado** (color 🔴 rojo).
4. Verás el mensaje: "❌ Orden cancelada - Mesa [N]".
5. El pedido se oculta de la lista activa.

### Cuándo puede cancelar el mesero

El mesero también puede cancelar pedidos. Si esto ocurre:

1. Recibirás una notificación: "❌ Pedido Cancelado - Mesa [N]".
2. El mensaje dirá: "El mesero canceló el pedido de la mesa".
3. El pedido desaparecerá automáticamente de tu lista activa.
4. No necesitas hacer nada.

### Protección contra cancelaciones duplicadas

El sistema está diseñado para evitar que una misma cancelación se procese dos veces. Si el mesero cancela y tú intentas cancelar el mismo pedido, no ocurrirá nada porque la orden ya fue procesada.

---

## Pedidos entregados por el mesero

Cuando el mesero recoge un pedido listo y lo entrega al cliente:

1. El mesero marca el pedido como **Entregado** en su aplicación.
2. Tú recibes una notificación: "🍽️ Comida Entregada - Mesa [N]".
3. El mensaje dirá: "El mesero entregó la comida al cliente".
4. El pedido permanece en estado **Entregado** en tu lista (color gris).
5. Puedes tocar **"Completar Pedido"** para finalizarlo definitivamente.

---

## Notificaciones

El sistema te mantiene informado mediante notificaciones en tiempo real.

### Tipos de notificaciones

| Tipo | Notificación | Cuándo aparece |
|------|-------------|----------------|
| 🆕 **Nuevo pedido** | "NUEVA ORDEN DEL MESERO - Mesa [N]" | Mesero envía un pedido |
| 🔄 **Cambio de estado** | Cambio en el estado del pedido | Cualquier modificación |
| 📦 **Inventario** | "Inventario Actualizado: [Producto]" | Al aceptar un pedido |
| ❌ **Cancelación** | "Pedido Cancelado - Mesa [N]" | Mesero cancela un pedido |
| 🍽️ **Entrega** | "Comida Entregada - Mesa [N]" | Mesero entrega al cliente |
| 🔴 **Sin conexión** | "SIN INTERNET - Modo offline" | Se pierde la conexión |

### Acceso a notificaciones

Toca el ícono de **campana** 🔔 en la barra superior. Se abrirá un panel lateral con las notificaciones más recientes.

El contador rojo sobre la campana indica cuántas notificaciones no leídas tienes.

---

## Funcionamiento sin conexión

La aplicación está diseñada para funcionar **sin internet** en modo offline.

### Sin conexión a internet

- 🔴 Verás el mensaje: **"📱 SIN INTERNET - Modo offline"**
- Puedes seguir viendo los pedidos que ya estaban cargados.
- Puedes cambiar el estado de los pedidos (aceptar, preparar, listo).
- Los cambios se guardan en el dispositivo.
- **No recibirás nuevos pedidos** hasta que vuelva la conexión.

### Al recuperar la conexión

- 🟢 Verás el mensaje: **"🟢 Internet disponible - Sincronizando..."**
- Todos tus cambios de estado se enviarán automáticamente.
- Los nuevos pedidos que llegaron mientras estabas offline aparecerán.
- Verás el mensaje: **"✅ Sincronización completada"**.

### Sincronización manual

Si necesitas forzar la sincronización, puedes tocar el botón de sincronización en el banner de estado de conexión. Esto ejecutará una sincronización completa.

---

## Mensajes y estados frecuentes

### Mensajes de éxito

| Mensaje | Significado |
|---------|-------------|
| ✅ Orden aceptada - Mesa [N] | Recepción confirmada |
| 👨‍🍳 Orden en preparación - Mesa [N] | Preparación iniciada |
| 🎉 ¡Orden lista! - Mesa [N] | Pedido terminado |
| ✅ Orden completada - Mesa [N] | Pedido finalizado |
| ✅ Sincronización completada | Datos actualizados con el servidor |

### Mensajes de advertencia

| Mensaje | Significado |
|---------|-------------|
| ⚠️ Stock insuficiente: [Producto] | No hay inventario para ese producto |
| 📱 SIN INTERNET - Cambio guardado localmente | Sin conexión, cambios en dispositivo |
| 📱 SIN INTERNET - Modo offline | Trabajando sin conexión |
| Pedido de mesa [N] fue cancelado | El mesero canceló la orden |

### Mensajes de error

| Mensaje | Significado | Solución |
|---------|-------------|----------|
| ❌ Sin conexión a internet | No hay red | Verificar WiFi/datos |
| ❌ Error actualizando orden | Fallo al cambiar estado | Reintentar, verificar conexión |
| Error de conexión | No se puede contactar Firebase | Esperar o forzar sincronización |

### Estados de carga

- **Indicador giratorio:** Cargando pedidos desde el servidor
- **"Conectando..."** en la barra: Estableciendo comunicación con Firebase

---

## Flujo completo de trabajo

### Ejemplo: Preparar un pedido completo

1. **Recibes un nuevo pedido** → Notificación: "🎯 NUEVA ORDEN DEL MESERO - Mesa 5".
2. **Revisas el pedido** → Mesa 5: 2 Pisco Sour, 1 Tabla de Quesos.
3. **Aceptas el pedido** → Toca "Aceptar Pedido".
   - Estado cambia a **Aceptado** (🔵).
   - Inventario descuenta automáticamente los ingredientes.
   - Mensaje: "✅ Orden aceptada - Mesa 5".
4. **Comienzas a preparar** → Toca "Iniciar Preparación".
   - Estado cambia a **En preparación** (🟠).
   - Mensaje: "👨‍🍳 Orden en preparación - Mesa 5".
5. **Terminas la preparación** → Toca "Marcar como Listo".
   - Estado cambia a **Listo** (🟢).
   - El mesero recibe notificación inmediata.
   - Mensaje: "🎉 ¡Orden lista! - Mesa 5".
6. **Mesero recoge y entrega** → Recibes notificación: "🍽️ Comida Entregada - Mesa 5".
   - Estado cambia a **Entregado** (⚪).
7. **Finalizas el pedido** → Toca "Completar Pedido".
   - Pedido desaparece de la lista activa.

### Interacción entre roles

```
MESERO                        COCINA (TÚ)                 ADMIN
  │                              │                          │
  ├─ Crea pedido ───────────────►│                          │
  │                              ├─ Recibes notificación    │
  │                              ├─ Aceptas pedido ─────────┼──► Descuenta inventario
  │   ◄─ Notificación aceptado ──┤                          │
  │                              ├─ Inicias preparación     │
  │                              ├─ Preparas...             │
  │                              ├─ ¡Marcas LISTO!          │
  │   ◄─ Notificación LISTO ────┤                          │
  ├─ Recoges y entregas          │                          │
  ├─ Marcas Entregado ──────────►│                          │
  │                              ├─ Notificación entrega    │
  │                              ├─ Completas pedido        │
```

---

## Buenas prácticas

1. **Acepta los pedidos inmediatamente** al recibirlos. Esto confirma al mesero que estás al tanto y dispara el descuento de inventario.

2. **Revisa el inventario al inicio de tu turno** para saber qué ingredientes están bajos y evitar sorpresas durante la preparación.

3. **Procesa los pedidos en orden**, respetando la secuencia de llegada para mantener un servicio justo y organizado.

4. **Si un producto tiene stock insuficiente**, notifica al mesero de inmediato para que pueda ofrecer alternativas al cliente.

5. **No dejes pedidos en estado "Aceptado"** por mucho tiempo sin avanzar a "En preparación". Esto genera incertidumbre en el mesero.

6. **Marca como listo apenas termines**. Cuanto antes reciba la notificación el mesero, más rápido llegará la comida al cliente.

7. **En modo sin conexión**, tus cambios de estado se guardan localmente. Apenas vuelva internet, se sincronizarán. Sigue trabajando normalmente.

8. **Revisa el panel de notificaciones** regularmente para no perder cancelaciones o cambios importantes.

---

## Solución de problemas frecuentes

### Problema: No aparecen pedidos nuevos

- **Causa:** Sin conexión a internet.
- **Solución:** Verifica el indicador de conexión en la barra superior. Si estás offline, los pedidos nuevos no llegarán hasta que vuelva internet. Toca "Sincronizar" cuando se recupere.

### Problema: El inventario no se descuenta al aceptar un pedido

- **Causa:** Sin conexión a internet o el producto no tiene control de inventario activado.
- **Solución:** Verifica tu conexión. Solo los productos con "Control de inventario" activado por el administrador descuentan stock automáticamente.

### Problema: No puedo cambiar el estado de un pedido

- **Causa:** El pedido está en un estado que no permite la transición solicitada.
- **Solución:** Verifica el estado actual. No todos los cambios son posibles desde cualquier estado (por ejemplo, no puedes marcar como listo un pedido cancelado).

### Problema: Recibí una notificación pero el pedido ya no aparece

- **Causa:** El mesero canceló el pedido.
- **Solución:** Los pedidos cancelados desaparecen automáticamente de la lista. Revisa el panel de notificaciones para ver el historial.

### Problema: El pedido sigue apareciendo como Entregado y no desaparece

- **Causa:** No has completado el pedido.
- **Solución:** Toca el pedido en estado "Entregado" y luego "Completar Pedido" para finalizarlo y ocultarlo de la lista.

---

## Recomendaciones finales

- **Mantén la aplicación visible** en todo momento durante el servicio. Los pedidos llegan en tiempo real y cada segundo cuenta.
- **Usa una tablet o pantalla grande** si es posible. La interfaz está optimizada para mostrar varios pedidos simultáneamente.
- **Comunícate directamente con el mesero** si hay dudas sobre un pedido. La aplicación no reemplaza la comunicación verbal en cocina.
- **Reporta productos agotados** al administrador para que actualice el inventario o reponga stock.
- **No acumules pedidos sin procesar.** Acepta cada pedido ni bien llegue para mantener el flujo ordenado.

---

*Documentación generada a partir del análisis del código fuente del proyecto La Previa Restobar. Última actualización: Mayo 2026.*
