# Flujo BPMN para Bizagi: Pedido del cliente hasta liberar mesa

## Nombre del proceso

**Atencion de pedido, cobro y liberacion de mesa - La Previa Restobar**

## Objetivo

Representar el flujo completo desde que el cliente realiza un pedido hasta que el pedido queda cobrado o cancelado y la mesa vuelve a estar libre.

## Pool y carriles para Bizagi

Pool:

- **La Previa Restobar**

Carriles:

- **Cliente**
- **Mesero**
- **Cocina / Chef**
- **Caja / App de escritorio**
- **Sistema La Previa**
- **Firebase / Base de datos**

## Flujo principal

| Orden | Carril | Tipo BPMN | Texto dentro del elemento |
|---:|---|---|---|
| 1 | Cliente | Evento de inicio | Cliente solicita pedido |
| 2 | Cliente | Tarea | Revisar carta QR o pedir al mesero |
| 3 | Mesero | Tarea | Tomar pedido en la app |
| 4 | Sistema La Previa | Tarea | Validar productos y mesa |
| 5 | Sistema La Previa | Compuerta exclusiva | Hay stock disponible? |
| 6A | Mesero | Tarea | Informar producto no disponible |
| 6B | Cliente | Tarea | Elegir otro producto |
| 7 | Sistema La Previa | Tarea | Registrar pedido y ocupar mesa |
| 8 | Firebase / Base de datos | Tarea | Guardar pedido y estado de mesa |
| 9 | Cocina / Chef | Tarea | Recibir pedido |
| 10 | Cocina / Chef | Tarea | Aceptar pedido |
| 11 | Cocina / Chef | Tarea | Preparar pedido |
| 12 | Cocina / Chef | Tarea | Marcar pedido como listo |
| 13 | Mesero | Tarea | Entregar pedido al cliente |
| 14 | Cliente | Compuerta exclusiva | Desea agregar mas productos? |
| 15A | Mesero | Tarea | Agregar productos al pedido |
| 15B | Cliente | Tarea | Solicitar cuenta |
| 16 | Mesero / Caja | Compuerta exclusiva | Donde se realiza el cobro? |
| 17A | Mesero | Tarea | Cobrar desde app movil |
| 17B | Caja / App de escritorio | Tarea | Buscar pedido o mesa |
| 18B | Caja / App de escritorio | Tarea | Cobrar desde app de escritorio |
| 19 | Sistema La Previa | Tarea | Registrar metodo de pago |
| 20 | Sistema La Previa | Tarea | Generar ticket |
| 21 | Firebase / Base de datos | Tarea | Actualizar pedido como cobrado |
| 22 | Sistema La Previa | Tarea | Liberar mesa |
| 23 | Firebase / Base de datos | Tarea | Guardar mesa como libre |
| 24 | Cliente | Evento de fin | Mesa liberada y venta registrada |

## Flujo alternativo: sin stock

Desde la compuerta **Hay stock disponible?**:

- Si la respuesta es **No**, conectar a **Informar producto no disponible**.
- Luego conectar a **Elegir otro producto**.
- Volver a **Tomar pedido en la app**.

## Flujo alternativo: pedido cancelado

Agregar una compuerta despues de **Registrar pedido y ocupar mesa**:

| Orden | Carril | Tipo BPMN | Texto dentro del elemento |
|---:|---|---|---|
| A1 | Mesero | Compuerta exclusiva | Pedido cancelado? |
| A2 | Sistema La Previa | Tarea | Cambiar pedido a cancelado |
| A3 | Firebase / Base de datos | Tarea | Guardar cancelacion |
| A4 | Sistema La Previa | Tarea | Liberar mesa |
| A5 | Firebase / Base de datos | Tarea | Guardar mesa como libre |
| A6 | Cliente | Evento de fin | Pedido cancelado y mesa liberada |

Si la respuesta es **No**, continuar con **Recibir pedido** en Cocina / Chef.

## Flujo alternativo: pago en caja de escritorio

Desde la compuerta **Donde se realiza el cobro?**:

- Si el cobro lo hace el mesero, conectar a **Cobrar desde app movil**.
- Si el cliente va a caja, conectar a **Buscar pedido o mesa**.
- Luego conectar a **Cobrar desde app de escritorio**.
- Ambas rutas se unen antes de **Registrar metodo de pago**.

## Metodos de pago

En la tarea **Registrar metodo de pago**, colocar una nota o anotacion BPMN con:

- Efectivo
- Yape / Plin
- Tarjeta

## Estados importantes del sistema

| Momento | Pedido | Mesa |
|---|---|---|
| Antes del pedido | Sin pedido activo | Libre |
| Pedido registrado | Pendiente / Enviado a cocina | Ocupada |
| Chef acepta | Aceptado | Ocupada |
| Chef prepara | En preparacion | Ocupada |
| Chef marca listo | Listo | Ocupada |
| Mesero entrega | Entregado | Ocupada |
| Pedido cobrado | Cobrado / Completado | Libre |
| Pedido cancelado | Cancelado | Libre |

## Explicacion corta para exposicion

Este flujo muestra como La Previa Restobar controla el ciclo completo de atencion. Primero el mesero registra el pedido desde la app movil y el sistema ocupa la mesa para evitar duplicidad. Luego el chef recibe el pedido, lo acepta, lo prepara y lo marca como listo. Cuando el cliente termina, el cobro puede hacerlo el mesero desde el celular o el cajero desde la app de escritorio. Al registrar el pago, el sistema genera el ticket, actualiza el pedido como cobrado en Firebase y libera automaticamente la mesa. Si el pedido se cancela, tambien se registra la cancelacion y la mesa vuelve a quedar libre.

## Como dibujarlo rapido en Bizagi

1. Crear un nuevo diagrama BPMN.
2. Insertar un Pool llamado **La Previa Restobar**.
3. Agregar los carriles: Cliente, Mesero, Cocina / Chef, Caja / App de escritorio, Sistema La Previa y Firebase / Base de datos.
4. Colocar el evento de inicio en Cliente.
5. Agregar las tareas en el orden de la tabla del flujo principal.
6. Usar compuertas exclusivas para: stock disponible, pedido cancelado, agregar mas productos y lugar de cobro.
7. Conectar las rutas alternativas con flechas de retorno o salida segun corresponda.
8. Finalizar con el evento **Mesa liberada y venta registrada**.
