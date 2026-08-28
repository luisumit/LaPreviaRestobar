# Arquitectura DDD — LaPrevia Restobar

Diseño del dominio siguiendo **Domain-Driven Design (DDD)**, aplicado de forma **pragmática** (light DDD) sobre la base de Clean Architecture que ya tiene el proyecto.

> Objetivo: poner el **dominio del negocio** (el restobar) al centro, con un modelo rico, independiente de frameworks (Android/Firebase/Room). Esto además deja el dominio **listo para compartir** en una futura migración a Kotlin Multiplatform.

---

## 1. Lenguaje ubicuo (Ubiquitous Language)

Vocabulario compartido entre el negocio y el código. Se usan **los mismos nombres** en ambos:

| Término | Significado |
|---|---|
| **Pedido / Orden** | Lo que pide una mesa (uno o varios productos). |
| **Comanda** | Documento para la **cocina** (qué preparar, sin precios). |
| **Ticket** | Documento para el **cliente** (con precios y total). |
| **Mesa** | Ubicación física; puede estar libre u ocupada. |
| **Producto** | Ítem del menú (con precio de venta y costo). |
| **Cobro** | Cerrar el pedido eligiendo método de pago. |
| **Vuelto** | Diferencia entre lo recibido y el total (solo efectivo). |
| **Descuento / Promoción** | Rebaja aplicada al pedido (ej. Happy Hour). |
| **Cierre de caja** | Resumen del turno (ventas, métodos de pago). |
| **Mozo / Cocina / Admin** | Roles del sistema. |

---

## 2. Bounded Contexts (contextos delimitados)

Áreas del negocio con responsabilidad propia:

| Bounded Context | Responsabilidad | En el código |
|---|---|---|
| **Pedidos** | crear, enviar, preparar, entregar y cobrar órdenes | `Order`, `OrderRepository`, `CreateOrderUseCase`, `WaiterViewModel`, `ChefViewModel` |
| **Inventario** | stock de productos, alertas de stock bajo | `Product`, `Inventory`, `InventoryRepository`, `ProductManager` |
| **Caja y Reportes** | cobro, descuentos, vuelto, cierre de caja, reportes | `Billing`, `SplitBill`, `SalesCalculator`, `CashClosure` |
| **Impresión** | armar comanda y ticket (ESC/POS) | `ReceiptFormatter`, `EscPosEncoder`, `AutoPrintManager` |
| **Identidad y Acceso** | login por rol | `LoginViewModel`, `UserRole`, Firebase Auth |

---

## 3. Building blocks (bloques tácticos de DDD)

### 🧱 Entities (entidades — tienen identidad propia)
- **`Order`** — identidad = `id`. Es el **Aggregate Root** (ver abajo).
- **`Product`** — identidad = `id`.
- **`Table`** — identidad = número de mesa.
- **`Inventory`** — stock de un producto.

### 💎 Value Objects (sin identidad; se comparan por valor, inmutables)
- **`Money`** — un monto en soles (reemplaza `Double` suelto). ← primer VO implementado.
- **`Percentage`** — un porcentaje de descuento.
- **`PaymentMethod`** — efectivo / Yape-Plin / tarjeta (enum).
- **`OrderStatus`** — pendiente / enviado / en preparación / listo / entregado / completado / cancelado (enum).

### 📦 Aggregate Root
- **`Order`** es la raíz del agregado. Contiene sus **`OrderItem`** y garantiza **invariantes**:
  - El total debe ser coherente con la suma de sus ítems.
  - No se puede cobrar un pedido sin ítems.
  - Un descuento no puede dejar el total negativo.
  - El vuelto solo aplica a pagos en efectivo.
  > Toda modificación a los ítems pasa por el agregado, nunca directamente.

### ⚙️ Domain Services (lógica de negocio pura, sin estado)
- **`Billing`** — descuentos, total neto, vuelto.
- **`SplitBill`** — división de cuenta (igual / por comensal).
- **`SalesCalculator`** — totales, métodos de pago, ganancia, top productos, ventas por hora.
- **`ReceiptFormatter`** — arma comanda y ticket.

### 🗄️ Repositories (abstracción de persistencia, definida en el dominio)
- Interfaces en `domain/repository`: `OrderRepository`, `ProductRepository`, `TableRepository`, `InventoryRepository`.
- Implementadas en `data/repository` (Firebase + Room). **El dominio no conoce Firebase ni Room.**

### 🎬 Application Services / Use Cases
- `CreateOrderUseCase`, `UpdateOrderStatusUseCase`, `CreateProductUseCase`, etc. — orquestan el dominio.

---

## 4. Regla de oro (dependencia hacia el dominio)

```
  Presentation (Compose + ViewModels)
        ↓ depende de
  Domain (Entities, Value Objects, Domain Services, Repository interfaces)  ← PURO
        ↑ implementado por
  Data / Infrastructure (Firebase, Room, Bluetooth)
```

- El **dominio no depende de nadie** (ni de Android, ni de Firebase).
- Los primitivos (`Double`, `String`) viven en la **infraestructura**; en el dominio usamos **Value Objects** (`Money`).
- La conversión primitivo ↔ Value Object se hace en los **bordes** (mappers).

---

## 5. Estado de la implementación

| Bloque DDD | Estado |
|---|---|
| Lenguaje ubicuo | ✅ documentado |
| Domain Services (`Billing`, `SplitBill`, `SalesCalculator`) | ✅ implementados y probados |
| Repositorios como interfaces de dominio | ✅ existen |
| Value Object `Money` | ✅ implementado + 10 tests |
| Value Object `Percentage` | ✅ implementado + 7 tests (colabora con `Money`; usado por `Billing`) |
| `Order` como Aggregate Root con invariantes | ✅ implementado + 11 tests (usa `Money`; el cobro pasa por el agregado) |

> **Cobertura total: 94 tests unitarios en verde.**

---

## 6. Próximos pasos
1. `Money` value object usado por los domain services.
2. Convertir `Order` en Aggregate Root (agregar métodos con invariantes).
3. Value Objects restantes (`Percentage`).
4. Mantener `Double` solo en Room/Firebase, mapeando en los bordes.
