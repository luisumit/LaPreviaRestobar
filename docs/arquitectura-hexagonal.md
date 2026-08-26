# Arquitectura Hexagonal (Ports & Adapters) — LaPrevia Restobar

Combina **Arquitectura Hexagonal** + **DDD pragmático** sobre la base de Clean Architecture.
La idea central: **el dominio va al centro, aislado de los frameworks**, y todo lo externo
(UI, Firebase, Room, Bluetooth) se conecta por **puertos (interfaces)** implementados por **adaptadores**.

> Ver también: [`arquitectura-ddd.md`](arquitectura-ddd.md) (modelo de dominio) y
> [`plan-migracion-kmp.md`](plan-migracion-kmp.md) (un dominio puro = base para multiplataforma).

---

## 🔷 El hexágono

```
        ADAPTADORES DE ENTRADA (driving)
        ┌───────────────────────────────┐
        │   Compose (UI) + ViewModels   │
        └───────────────┬───────────────┘
                        │ PUERTO DE ENTRADA
                        │  (Use Cases)
                        ▼
        ╔═══════════════════════════════╗
        ║           DOMINIO             ║
        ║  (el hexágono — PURO)         ║
        ║                               ║
        ║  • Value Objects: Money,      ║
        ║    Percentage                 ║
        ║  • Aggregate Root: Order      ║
        ║  • Domain Services: Billing,  ║
        ║    SplitBill, SalesCalculator ║
        ╚═══════════════┬═══════════════╝
                        │ PUERTO DE SALIDA
                        │  (Repository interfaces)
                        ▼
        ┌───────────────────────────────┐
        │   Firebase · Room · Bluetooth │
        └───────────────────────────────┘
        ADAPTADORES DE SALIDA (driven)
```

**Regla de dependencia:** las flechas apuntan **hacia el dominio**. El dominio no conoce
a nadie; los adaptadores dependen de los puertos, no al revés.

---

## 🔌 Puertos y adaptadores en el código

| Elemento hexagonal | Qué es | En el proyecto |
|---|---|---|
| **Dominio (hexágono)** | reglas del negocio, puro | `domain/` — `Money`, `Percentage`, `Order` (aggregate), `Billing`, `SplitBill`, `SalesCalculator` |
| **Puerto de entrada** | cómo se le pide algo al dominio | Use Cases: `CreateOrderUseCase`, `UpdateOrderStatusUseCase`… (`domain/usecase`) |
| **Adaptador de entrada** | quien maneja al usuario | Compose (`presentation/screens`) + ViewModels |
| **Puerto de salida** | qué necesita el dominio del exterior | interfaces `OrderRepository`, `ProductRepository`, `TableRepository`, `InventoryRepository` (`domain/repository`) |
| **Adaptador de salida** | implementación concreta | `FirebaseOrderRepositoryImpl`, Room DAOs, `BluetoothPrinterManager` (`data/`) |

### Ejemplo concreto: cobrar un pedido
```
WaiterViewModel (adaptador de entrada)
   → Order.applyDiscount(Money).payWith(método, Money)   [dominio: aggregate + value objects]
   → OrderRepository.updateOrder(order)                  [puerto de salida]
        └── FirebaseOrderRepositoryImpl                   [adaptador de salida: Firebase]
        └── Room (OrderDao)                               [adaptador de salida: local]
```

El ViewModel **no sabe** que por debajo hay Firebase o Room — solo conoce el **puerto**
(`OrderRepository`). Mañana podríamos cambiar Firebase por Ktor y **el dominio no se entera**.

---

## ✅ Beneficios (por qué vale la pena)

1. **Dominio testeable sin frameworks** → 94 tests unitarios corren sin Android ni Firebase.
2. **Intercambiable:** cambiar Firebase por otro backend = cambiar un adaptador, no el dominio.
3. **Listo para KMP:** un dominio puro es exactamente lo que necesita `commonMain`.
4. **Reglas protegidas:** las invariantes viven en el dominio (`Order`), no dispersas en la UI.

---

## 🧹 Purificación aplicada

Como parte de esta arquitectura se movió el domain service **`SalesCalculator`** (y sus
modelos `DailySalesPoint`, `ProductSalesPoint`) desde `presentation/viewmodel` hacia
`domain/service` y `domain/model` — donde corresponde al hexágono. Antes era un "leak":
lógica de dominio viviendo en la capa de presentación.

---

## ⚖️ Nota pragmática (sin sobre-ingeniería)

Para una app de este tamaño **no** se hace hexagonal "estricto" (módulos separados por
adaptador, mapeos DTO por todos lados). Basta con:
- Dominio puro y aislado ✅
- Puertos = interfaces en `domain` ✅
- Adaptadores = implementaciones en `data` / `presentation` ✅
- Dependencias apuntando hacia el dominio ✅

Eso **es** arquitectura hexagonal bien hecha, sin exagerar.

---

## 📊 Estado

| Elemento | Estado |
|---|---|
| Dominio puro (VOs + aggregate + services) | ✅ |
| Puertos de salida (repository interfaces) | ✅ |
| Puertos de entrada (use cases) | ✅ |
| Adaptadores (Firebase, Room, Compose, Bluetooth) | ✅ |
| `SalesCalculator` movido al dominio | ✅ |
| Pendiente opcional: mover entidades (`Order`) de `data.model` a `domain.model` | ⏳ (churn alto; no crítico para un bar chico) |

> **94 tests unitarios en verde** respaldan que el dominio funciona aislado.
