# Manual de Usuario – Administrador/a

## Índice

1. [Descripción general del rol](#descripción-general-del-rol)
2. [Objetivo del rol](#objetivo-del-rol)
3. [Permisos y restricciones](#permisos-y-restricciones)
4. [Requisitos previos](#requisitos-previos)
5. [Acceso e inicio de sesión](#acceso-e-inicio-de-sesión)
6. [Pantalla principal del panel administrativo](#pantalla-principal-del-panel-administrativo)
7. [Gestión de productos](#gestión-de-productos)
8. [Crear un nuevo producto](#crear-un-nuevo-producto)
9. [Editar un producto existente](#editar-un-producto-existente)
10. [Eliminar un producto](#eliminar-un-producto)
11. [Control de inventario y stock](#control-de-inventario-y-stock)
12. [Categorías de productos](#categorías-de-productos)
13. [Alertas de stock bajo](#alertas-de-stock-bajo)
14. [Sincronización de datos](#sincronización-de-datos)
15. [Notificaciones del sistema](#notificaciones-del-sistema)
16. [Funcionamiento sin conexión](#funcionamiento-sin-conexión)
17. [Mensajes y estados frecuentes](#mensajes-y-estados-frecuentes)
18. [Flujo completo de trabajo](#flujo-completo-de-trabajo)
19. [Buenas prácticas](#buenas-prácticas)
20. [Solución de problemas frecuentes](#solución-de-problemas-frecuentes)
21. [Recomendaciones finales](#recomendaciones-finales)

---

## Descripción general del rol

El rol de **Administrador/a** es el encargado de gestionar el catálogo de productos, precios, inventario y la configuración general del sistema. Eres la persona responsable de que el menú esté actualizado, los precios sean correctos y el stock de productos esté al día para que meseros y cocineros puedan trabajar sin inconvenientes.

Desde el panel administrativo puedes crear, modificar y eliminar productos, controlar el stock, gestionar categorías y supervisar el estado de sincronización de los datos con la nube.

---

## Objetivo del rol

Tu objetivo principal es:

- **Gestionar el catálogo de productos** que meseros y cocineros utilizan.
- **Configurar precios** de venta y costo de cada producto.
- **Controlar el inventario** definiendo stock inicial, stock mínimo y seguimiento automático.
- **Organizar categorías** para una navegación eficiente del menú.
- **Supervisar la sincronización** de datos entre dispositivos locales y la nube.
- **Recibir alertas** de productos con stock bajo o agotado.

---

## Permisos y restricciones

### Lo que PUEDES hacer

| Acción | Descripción |
|--------|-------------|
| Crear productos | Agregar nuevos productos al catálogo con todos sus atributos |
| Editar productos | Modificar nombre, descripción, precio, categoría, stock e imagen |
| Eliminar productos | Quitar productos del catálogo |
| Configurar precios | Establecer precio de venta y precio de costo |
| Gestionar stock | Definir stock actual, stock mínimo y activar/desactivar control de inventario |
| Gestionar categorías | Las categorías se generan automáticamente a partir de los productos creados |
| Sincronizar datos | Forzar la sincronización manual entre el dispositivo y Firebase |
| Ver estado de conexión | Monitorear conectividad y productos pendientes de sincronizar |

### Lo que NO puedes hacer

| Restricción | Descripción |
|-------------|-------------|
| Tomar pedidos | No puedes crear pedidos para los clientes |
| Gestionar mesas | Las mesas son gestionadas automáticamente por el sistema |
| Cambiar estados de pedidos | Solo cocina y meseros pueden modificar el estado de los pedidos |
| Ver estadísticas de ventas | Esta funcionalidad no está implementada actualmente |
| Gestionar usuarios | El panel de gestión de usuarios no está disponible desde la app |
| Gestionar reportes | Los reportes no están implementados en esta versión |

### Lo que está PARCIALMENTE implementado

| Funcionalidad | Estado actual |
|---------------|---------------|
| Gestión de usuarios | Solo registro e inicio de sesión. No hay panel para administrar usuarios desde la app |
| Reportes de ventas | No implementado |
| Estadísticas | No implementado |
| Reservas de mesas | Las mesas tienen estado "Reservada" pero no se gestionan desde el panel admin |

---

## Requisitos previos

- Tener una cuenta de usuario con rol **ADMIN** registrada en el sistema.
- Tener la aplicación instalada en un dispositivo Android.
- Conexión a internet (recomendada para sincronización, pero no obligatoria).

---

## Acceso e inicio de sesión

### Paso 1: Abrir la aplicación

Al abrir la aplicación verás la pantalla de inicio con tres roles disponibles:

- 🍽️ **Mesero**
- 👨‍🍳 **Cocinero**
- ⚙️ **Administrador**

### Paso 2: Seleccionar tu rol

Toca el botón **Administrador**. La aplicación te dirigirá a la pantalla de inicio de sesión donde deberás ingresar:

- **Correo electrónico** registrado
- **Contraseña**

Toca **Iniciar Sesión**. Si las credenciales son correctas y tu usuario tiene rol de administrador, accederás al panel administrativo.

> **Nota:** Si el sistema está configurado en modo rápido (sin autenticación), al tocar "Administrador" ingresarás directamente sin necesidad de credenciales.

---

## Pantalla principal del panel administrativo

Una vez dentro, verás el **Panel Administrativo** con los siguientes elementos:

### Barra superior

- **Título:** "LA PREVIA RESTOBAR"
- **Subtítulo:** "Panel Administrativo"
- **Indicador de conexión y sincronización:** Muestra:
  - 🟢 Estado de internet y cantidad de productos sincronizados
  - 📱 Productos pendientes de sincronizar
- **Ícono de notificación** 🔔 – Para probar alertas de stock
- **Botón rojo de salida** – Cierra tu sesión

### Banner de estado de conexión

Debajo de la barra superior verás un banner que indica:

| Estado | Mensaje |
|--------|---------|
| 🟢 Conectado | "Conectado - [N] pendiente(s)" si hay productos sin sincronizar |
| 📱 Sin conexión | "SIN INTERNET - Modo offline" con indicador de productos pendientes |

En el banner hay un **botón "Sincronizar"** para forzar la sincronización manual.

### Área principal

Muestra la **lista de productos** con:

- **Tarjeta de bienvenida** con resumen del catálogo
- **Lista de productos** con nombre, categoría, precio y stock
- **Indicadores visuales** de stock bajo o agotado

### Botón flotante (FAB)

En la esquina inferior derecha hay un **botón rojo con el ícono "+"**. Tócalo para **crear un nuevo producto**.

---

## Gestión de productos

### Vista de lista de productos

Cada producto se muestra en una **tarjeta** que contiene:

| Información | Descripción |
|-------------|-------------|
| **Nombre** | Nombre del producto |
| **Categoría** | Categoría a la que pertenece |
| **Precio de venta** | Precio que ve el mesero al tomar pedidos |
| **Precio de costo** | Costo interno del producto (no visible para meseros) |
| **Stock actual** | Cantidad disponible |
| **Stock mínimo** | Umbral para alertas de stock bajo |
| **Control de inventario** | Indicador de si el producto descuenta stock automáticamente |

### Estados visuales del stock

| Estado | Indicador visual |
|--------|-----------------|
| Stock normal | Sin indicador especial |
| Stock bajo (≤ mínimo) | ⚠️ Advertencia amarilla |
| Stock agotado (0) | ❌ Alerta roja |

### Acciones sobre un producto

Al tocar una tarjeta de producto se despliegan las opciones:

- **Editar** – Abre el formulario de edición
- **Eliminar** – Muestra un diálogo de confirmación para eliminar

---

## Crear un nuevo producto

### Paso 1: Abrir el formulario

Toca el **botón flotante "+"** en la esquina inferior derecha.

### Paso 2: Completar el formulario

Se abrirá el diálogo **"Nuevo Producto"** con los siguientes campos:

| Campo | Tipo | Descripción | Obligatorio |
|-------|------|-------------|-------------|
| **Nombre** | Texto | Nombre del producto tal como aparecerá en el menú | ✅ Sí |
| **Descripción** | Texto multilínea | Detalle del producto (ingredientes, presentación, etc.) | No |
| **Categoría** | Texto | Ej: Bebidas, Comidas, Postres, Cócteles | No |
| **Precio de venta** | Número decimal | Precio que pagará el cliente | No |
| **Precio de costo** | Número decimal | Costo del producto para el negocio | No |
| **Stock** | Número decimal | Cantidad actual en inventario | No |
| **Stock mínimo** | Número decimal | Umbral para activar alertas de stock bajo | No |
| **Control de inventario** | Interruptor (Sí/No) | Si está activado, el stock se descuenta automáticamente al aceptar pedidos | No |
| **Producto activo** | Interruptor (Sí/No) | Si está desactivado, no aparece en el menú del mesero | No |

### Paso 3: Guardar

1. Completa los campos necesarios (mínimo el nombre).
2. Toca **"Guardar"** o **"Crear"**.
3. Si tienes internet, verás: **"✅ Producto '[nombre]' creado y sincronizado"**.
4. Si estás sin internet, verás: **"📱 Producto guardado LOCALMENTE. Se sincronizará después"**.
5. El producto aparece inmediatamente en la lista.

### Validaciones al crear

| Validación | Mensaje de error |
|------------|-----------------|
| Producto duplicado (mismo ID) | "❌ Ya existe un producto con ese ID" |
| Error de conexión | "📱 SIN INTERNET - Producto guardado LOCALMENTE" |
| Error general | "Error al crear producto: [detalle]" |

### Estados durante la creación

- **Indicador de carga:** Aparece mientras se guarda el producto.
- **Mensaje de éxito:** Desaparece automáticamente después de 3 segundos.
- **Mensaje de advertencia:** Si se guarda localmente sin conexión.

---

## Editar un producto existente

### Paso 1: Seleccionar el producto

En la lista de productos, toca el producto que deseas modificar y selecciona **"Editar"**.

### Paso 2: Modificar los campos

Se abre el mismo formulario que en la creación, pero con los datos actuales del producto precargados. Puedes modificar cualquier campo:

- Cambiar el **nombre** si hubo un error tipográfico.
- Ajustar el **precio de venta** si cambió el menú.
- Actualizar el **stock** después de recibir un nuevo cargamento.
- Cambiar la **categoría** para reorganizar el menú.
- Modificar el **stock mínimo** para ajustar las alertas.
- Activar o desactivar el **control de inventario**.

### Paso 3: Guardar cambios

1. Realiza las modificaciones necesarias.
2. Toca **"Guardar"** o **"Actualizar"**.
3. Si tienes internet: **"✅ Producto '[nombre]' actualizado y sincronizado"**.
4. Si estás sin internet: **"📱 Producto actualizado LOCALMENTE. Se sincronizará después"**.

### Efectos inmediatos de la edición

| Cambio | Efecto en el sistema |
|--------|---------------------|
| Nombre o descripción | Se actualiza inmediatamente en el menú del mesero |
| Precio de venta | El nuevo precio aplica a partir del siguiente pedido |
| Stock | La cocina verá el nuevo stock disponible |
| Control de inventario | Si lo activas, la cocina empezará a descontar stock; si lo desactivas, dejará de hacerlo |
| Producto activo | Si lo desactivas, el producto desaparece del menú del mesero |

---

## Eliminar un producto

### Paso 1: Seleccionar el producto

En la lista, toca el producto a eliminar y selecciona **"Eliminar"**.

### Paso 2: Confirmar eliminación

Aparecerá un **diálogo de confirmación** preguntando si estás seguro de eliminar el producto.

- Toca **"Cancelar"** si cambiaste de opinión.
- Toca **"Eliminar"** para confirmar.

### Paso 3: Resultado

- Si tienes internet: **"✅ Producto '[nombre]' eliminado de la nube"**.
- Si estás sin internet: **"📱 Producto eliminado LOCALMENTE. Se eliminará de la nube después"**.
- El producto desaparece de la lista.

> **Precaución:** La eliminación es **permanente**. Si eliminas un producto por error, deberás volver a crearlo manualmente.

---

## Control de inventario y stock

### ¿Cómo funciona el control de inventario?

El sistema tiene dos modos para cada producto:

| Control de inventario | Comportamiento |
|----------------------|----------------|
| **Activado** ✅ | Al aceptar un pedido, la cocina descuenta automáticamente el stock. El mesero ve el stock actualizado. Las alertas de stock bajo se activan. |
| **Desactivado** ❌ | El producto siempre está disponible. No se descuenta stock ni se generan alertas. Útil para productos que se preparan al momento sin límite de inventario. |

### Actualizar stock manualmente

1. Toca el producto en la lista.
2. Selecciona **"Editar"**.
3. Modifica el campo **Stock** con la nueva cantidad.
4. Toca **"Guardar"**.

Esto es útil cuando:
- Recibiste un nuevo cargamento de ingredientes.
- Hiciste un inventario físico y necesitas corregir cantidades.
- Detectaste una diferencia entre el stock del sistema y el stock real.

### Stock mínimo

El **stock mínimo** es el umbral que define cuándo un producto está por agotarse. Cuando el stock actual es menor o igual al stock mínimo, el sistema genera una alerta.

**Ejemplo:**
- Stock actual: 5 unidades
- Stock mínimo: 10 unidades
- Resultado: ⚠️ Alerta de stock bajo (5 ≤ 10)

---

## Categorías de productos

Las categorías se generan **automáticamente** a partir de los productos que creas. No hay una pantalla separada para gestionar categorías.

### Cómo organizar por categorías

Al crear o editar un producto, escribe el nombre de la categoría en el campo **Categoría**. Ejemplos:

- `Bebidas`
- `Cócteles`
- `Entradas`
- `Platos de Fondo`
- `Postres`
- `Vinos`
- `Cervezas`

### Visualización para meseros y cocina

- Los productos se agrupan automáticamente por categoría en las pantallas de menú.
- Los filtros por categoría permiten navegar más rápido.

> **Consejo:** Sé consistente con los nombres de categorías. "Bebidas" y "Bebida" se tratarán como categorías diferentes.

---

## Alertas de stock bajo

### ¿Cuándo se activan?

El sistema verifica automáticamente el stock de todos los productos con control de inventario activado y genera alertas en dos casos:

| Caso | Mensaje de alerta |
|------|------------------|
| Stock agotado (0) | "❌ [N] producto(s) AGOTADOS" |
| Stock bajo (≤ mínimo) | "⚠️ [N] producto(s) con stock bajo" |
| Ambos casos | "❌ [N] agotados \| ⚠️ [N] stock bajo" |

### Verificación en segundo plano

La aplicación programa verificaciones periódicas de stock mediante un **trabajador en segundo plano**. Esto significa que puedes recibir alertas incluso si no tienes la aplicación abierta (dependiendo de la configuración de notificaciones del dispositivo).

### Probar notificaciones

En la barra superior hay un **botón naranja con ícono de notificación** 🔔. Al tocarlo, se ejecuta una verificación inmediata de stock. Es útil para probar que el sistema de notificaciones funciona correctamente.

---

## Sincronización de datos

### ¿Qué es la sincronización?

La aplicación guarda los datos en dos lugares simultáneamente:

1. **Dispositivo local** (base de datos Room) – Funciona sin internet.
2. **Nube Firebase** – Sincroniza todos los dispositivos.

La sincronización asegura que los cambios que hagas se reflejen en los dispositivos de meseros y cocineros.

### Sincronización automática

- Cuando hay internet, los cambios se sincronizan **inmediatamente**.
- Los cambios aparecen con estado **SYNCED** (sincronizado).
- Los meseros y cocineros ven tus cambios en tiempo real.

### Sincronización manual

Si necesitas forzar la sincronización:

1. Revisa el banner de estado de conexión.
2. Si hay productos pendientes, verás un número.
3. Toca el botón **"Sincronizar"** en el banner.
4. Verás: **"Sincronizando productos pendientes..."**.
5. Al finalizar: **"✅ ¡Sincronización completada!"**.

### Productos pendientes

Si creaste o editaste productos sin conexión, aparecerán con un indicador de **pendiente**. La cantidad de pendientes se muestra en el banner superior.

Cuando vuelva internet, la sincronización se ejecuta automáticamente y los productos pendientes se envían a la nube.

---

## Notificaciones del sistema

### Notificaciones de stock

Cuando un producto llega a su stock mínimo o se agota, el sistema puede enviar notificaciones a tu dispositivo.

Estas notificaciones son gestionadas por:

- **AdminStockScheduler:** Programa verificaciones periódicas.
- **AdminStockWorker:** Ejecuta la verificación y envía la notificación.

> **Nota:** Las notificaciones en segundo plano dependen de los permisos del sistema operativo Android. Asegúrate de tener los permisos de notificación habilitados para la aplicación.

---

## Funcionamiento sin conexión

### ¿Qué puedes hacer sin internet?

- ✅ Ver todos los productos existentes (cargados previamente).
- ✅ Crear nuevos productos.
- ✅ Editar productos existentes.
- ✅ Eliminar productos.
- ✅ Ver el estado de stock actual.

### ¿Qué NO funciona sin internet?

- ❌ Sincronización con otros dispositivos.
- ❌ Cambios visibles para meseros y cocineros hasta que vuelva internet.
- ❌ Carga de nuevos productos desde la nube.

### Comportamiento al reconectarse

- Los productos creados/ editados/eliminados localmente se sincronizan automáticamente.
- El banner de estado cambia a 🟢.
- El contador de pendientes baja a 0.
- Verás: **"✅ ¡Sincronización completada! Todos los productos están en la nube"**.

---

## Mensajes y estados frecuentes

### Mensajes de éxito

| Mensaje | Significado |
|---------|-------------|
| ✅ Producto '[nombre]' creado y sincronizado | Producto nuevo guardado en dispositivo y nube |
| ✅ Producto '[nombre]' actualizado y sincronizado | Cambios guardados correctamente |
| ✅ Producto '[nombre]' eliminado de la nube | Producto eliminado correctamente |
| ✅ ¡Sincronización completada! | Todos los datos están al día |
| ✅ ¡Sincronización completada! Todos los productos están en la nube | Sin productos pendientes |

### Mensajes de advertencia

| Mensaje | Significado |
|---------|-------------|
| 📱 Producto guardado LOCALMENTE. Se sincronizará después | Creado sin internet |
| 📱 Producto actualizado LOCALMENTE. Se sincronizará después | Modificado sin internet |
| 📱 SIN INTERNET - Los cambios se guardarán localmente | Modo offline activo |
| ⚠️ [N] producto(s) con stock bajo | Productos bajo el mínimo |
| ❌ [N] producto(s) AGOTADOS | Productos con stock en 0 |
| 📱 [N] producto(s) pendiente(s) de sincronizar | Cambios sin enviar a la nube |

### Mensajes de error

| Mensaje | Significado | Solución |
|---------|-------------|----------|
| ❌ Ya existe un producto con ese ID | ID duplicado | El sistema genera IDs automáticos; si ves este error, es un caso excepcional |
| No se seleccionó ningún producto para eliminar | Intentaste eliminar sin seleccionar | Toca un producto primero |
| Error al crear producto: [detalle] | Fallo en la operación | Verificar conexión, reintentar |
| Error al eliminar producto: [detalle] | Fallo en la operación | Verificar conexión, reintentar |
| Error de conexión: [detalle] | Sin comunicación con Firebase | Esperar o verificar internet |

---

## Flujo completo de trabajo

### Ejemplo: Agregar un nuevo producto al menú

1. **Inicias sesión** como Administrador.
2. **Verificas la conexión** en el banner superior (debe decir 🟢 Conectado).
3. **Tocas el botón "+"** abajo a la derecha.
4. **Completas el formulario:**
   - Nombre: "Pisco Sour"
   - Descripción: "Cóctel peruano con pisco, limón y clara de huevo"
   - Categoría: "Cócteles"
   - Precio de venta: 8500
   - Precio de costo: 2500
   - Stock: 40
   - Stock mínimo: 10
   - Control de inventario: ✅ Activado
   - Producto activo: ✅ Activado
5. **Tocas "Guardar"**.
6. **Confirmación:** "✅ Producto 'Pisco Sour' creado y sincronizado".
7. **Verificación:** El mesero ya puede ver "Pisco Sour" en su menú.
8. **La cocina** podrá descontar stock cuando acepte pedidos.

### Ejemplo: Actualizar stock después de recibir mercadería

1. **Inicias sesión** como Administrador.
2. **Buscas el producto** en la lista (ej: "Pisco Sour").
3. **Tocas el producto** y seleccionas "Editar".
4. **Modificas el campo Stock:** de 5 a 45 (recibiste 40 unidades).
5. **Tocas "Guardar"**.
6. **Confirmación:** "✅ Producto 'Pisco Sour' actualizado y sincronizado".
7. **La alerta de stock bajo** desaparece porque ahora tienes 45 > 10 (mínimo).

### Ejemplo: Desactivar un producto temporalmente

1. Buscas el producto en la lista.
2. Tocas "Editar".
3. Desactivas el interruptor **"Producto activo"**.
4. Tocas "Guardar".
5. El producto **desaparece del menú del mesero** inmediatamente.
6. Cuando quieras reactivarlo, edítalo y vuelve a activar el interruptor.

---

## Buenas prácticas

1. **Mantén el catálogo actualizado.** Si un producto sale del menú, desactívalo en lugar de eliminarlo. Así conservas el historial y puedes reactivarlo después.

2. **Configura el stock mínimo con criterio.** Define un umbral que te dé tiempo para reponer antes de agotar. Por ejemplo: si vendes 10 unidades por día, configura el mínimo en 15 o 20.

3. **Activa el control de inventario solo en productos que lo necesiten.** Para productos como "Cubiertos" o "Servilletas" que no se descuentan por pedido, déjalo desactivado.

4. **Sincroniza manualmente después de hacer muchos cambios.** Aunque la sincronización es automática, forzarla te da la tranquilidad de que todo está al día.

5. **Revisa las alertas de stock regularmente.** No dependas solo de las notificaciones. Al iniciar tu turno, revisa el panel para ver si hay productos agotados.

6. **Sé consistente con los nombres de categorías.** Usa siempre la misma ortografía y formato (ej: "Bebidas" no "bebidas" ni "BEBIDAS").

7. **Verifica los precios periódicamente.** Asegúrate de que los precios de venta reflejen los valores correctos del menú físico.

8. **No elimines productos que tengan pedidos activos.** Si un producto está en medio de un pedido, puede causar inconsistencias.

---

## Solución de problemas frecuentes

### Problema: El producto que creé no aparece en el menú del mesero

- **Causa 1:** El campo "Producto activo" está desactivado.
  - **Solución:** Edita el producto y activa el interruptor "Producto activo".
- **Causa 2:** Sin conexión a internet y el mesero no ha sincronizado.
  - **Solución:** Espera a que ambos dispositivos tengan internet y se sincronicen.
- **Causa 3:** El mesero necesita refrescar su aplicación.
  - **Solución:** Pide al mesero que cierre y vuelva a abrir la aplicación.

### Problema: Hay productos pendientes de sincronizar y no bajan

- **Causa:** Sin conexión a internet.
- **Solución:** Verifica el WiFi o datos móviles. Toca "Sincronizar" manualmente. Si el problema persiste, cierra sesión y vuelve a ingresar.

### Problema: Eliminé un producto por error

- **Causa:** No hay papelera de reciclaje.
- **Solución:** Debes crear el producto nuevamente desde cero con todos sus datos.

### Problema: El stock se descuenta más de lo esperado

- **Causa:** Cada vez que la cocina acepta un pedido, se descuenta la cantidad de cada producto según lo solicitado.
- **Solución:** Es el comportamiento normal. Si detectas una inconsistencia, haz un inventario físico y actualiza el stock manualmente.

### Problema: No recibo notificaciones de stock bajo

- **Causa:** Permisos de notificación desactivados en Android.
- **Solución:** Ve a Ajustes → Aplicaciones → La Previa Restobar → Notificaciones y actívalas.

### Problema: La aplicación se cierra inesperadamente

- **Causa:** Error interno o falta de memoria.
- **Solución:** Cierra la aplicación completamente y vuelve a abrirla. Tus datos están a salvo en el dispositivo y en la nube.

---

## Recomendaciones finales

- **Dedica tiempo a configurar bien cada producto** al crearlo. Un catálogo bien organizado facilita el trabajo de meseros y cocineros.
- **Revisa el panel al inicio de cada jornada** para asegurarte de que no haya productos agotados.
- **Mantén comunicación con cocina** para saber qué ingredientes están por agotarse y necesitan reposición urgente.
- **Configura el stock mínimo** de forma realista según el volumen de ventas de cada producto.
- **No compartas tu cuenta.** El rol de administrador tiene acceso total al catálogo y precios.
- **Ante cualquier duda técnica**, contacta al equipo de desarrollo o soporte del sistema.

---

*Documentación generada a partir del análisis del código fuente del proyecto La Previa Restobar. Última actualización: Mayo 2026.*
