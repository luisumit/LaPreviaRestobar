# Guion para presentar la arquitectura al profesor

App: **LaPrevia Restobar** — POS (punto de venta) para restobar.
Arquitectura: **Clean Architecture + DDD pragmático + Hexagonal (Ports & Adapters)**.
Duración sugerida: ~5 minutos.

---

## 1. Apertura (30 s)

> "Nuestra app es un sistema de punto de venta para un restobar: toma pedidos en tiempo
> real, los envía a cocina, cobra, imprime tickets y genera reportes. La construimos con
> **Clean Architecture**, y le aplicamos **Domain-Driven Design** y **Arquitectura Hexagonal**
> de forma **pragmática** — sin sobre-ingeniería."

---

## 2. La idea central (1 min)

> "La clave de nuestra arquitectura es que **el dominio del negocio va al centro, aislado
> de los frameworks**. La lógica del restobar (pedidos, cobros, descuentos) no depende de
> Android ni de Firebase. Todo lo externo se conecta por **puertos** (interfaces) que
> implementan **adaptadores**."

**Frase clave:** *"El dominio no conoce a nadie; los adaptadores dependen del dominio."*

---

## 3. Domain-Driven Design — con ejemplos (1.5 min)

> "Aplicamos DDD con estos bloques:"

- **Lenguaje ubicuo:** el código usa las palabras del negocio → *comanda, ticket, cobro,
  vuelto, cierre de caja.*
- **Value Objects:** en vez de `Double` sueltos usamos objetos con reglas:
  - `Money` → encapsula el dinero (no permite negativos, hace porcentajes).
  - `Percentage` → un descuento válido (se acota a 0–100).
  > *"`Percentage(20).of(Money(50))` devuelve `Money(10)` — dos value objects colaborando."*
- **Aggregate Root:** `Order` (el pedido) **contiene su comportamiento y sus invariantes**:
  - No se puede cobrar un pedido sin ítems.
  - Un descuento nunca deja el total negativo.
  - El vuelto solo aplica a efectivo.
  > *"Antes `Order` era un data class 'anémico'; ahora es un agregado rico: cobrar pasa por
  > `order.applyDiscount(...).payWith(...)`."*
- **Domain Services:** lógica pura → `Billing`, `SplitBill`, `SalesCalculator`.

**Frase clave:** *"Modelamos el negocio con objetos ricos, no con primitivos sueltos."*

---

## 4. Arquitectura Hexagonal — Ports & Adapters (1 min)

> "El dominio se comunica con el exterior por **puertos**:"

- **Puerto de salida:** la interfaz `OrderRepository` (está en el dominio).
- **Adaptadores de salida:** `FirebaseOrderRepositoryImpl` (nube) y Room (local) la implementan.
- **Adaptador de entrada:** la UI (Compose) + los ViewModels.

> *"El ViewModel **no sabe** si por debajo hay Firebase o Room — solo conoce el puerto.
> Si mañana cambiamos Firebase por otro backend, **el dominio no se entera**."*

**Frase clave:** *"Cambiar la base de datos = cambiar un adaptador, no el dominio."*

---

## 5. Demostración de un flujo (1 min)

> "Veamos cómo se **cobra un pedido**:"

```
WaiterViewModel                         ← adaptador de entrada (UI)
  └→ Order.applyDiscount(Money)         ← dominio: aggregate + value object
       .payWith(método, Money)          ← invariantes (vuelto solo efectivo)
  └→ OrderRepository.updateOrder(...)   ← puerto de salida
        └── FirebaseOrderRepositoryImpl ← adaptador (Firebase)
        └── Room (OrderDao)             ← adaptador (local, offline)
```

> "Las reglas de dinero viven en el dominio; la UI y la base de datos son solo adaptadores."

---

## 6. Evidencia (30 s)

- **94 pruebas unitarias** que corren **sin Android ni Firebase** → prueba de que el dominio
  está aislado.
- Herramientas de calidad: **Detekt** (análisis estático), **JaCoCo** (cobertura), **SonarCloud**, **CI**.
- Todo documentado: `arquitectura-ddd.md`, `arquitectura-hexagonal.md`.

**Cierre:** *"En resumen: dominio puro y probado, protegido de los frameworks — DDD y
Hexagonal aplicados de forma pragmática."*

---

## 🎤 Preguntas probables del profe (y cómo responder)

**¿Toda la app usa DDD?**
> "No. DDD se aplica al **núcleo del negocio** (el dominio). La UI y la base de datos son
> adaptadores. Aplicar DDD a todo sería sobre-ingeniería — hasta Eric Evans lo dice."

**¿Qué es un Value Object y por qué usarlo?**
> "Un objeto inmutable que se compara por valor y **encapsula reglas**. Ej: `Money` evita
> que sumemos dinero mal o quede negativo. Reemplaza `Double` suelto."

**¿Qué es un Aggregate Root?**
> "La entidad principal de un grupo que **garantiza las reglas** (invariantes). En nuestro
> caso `Order`: toda modificación pasa por él, así el estado siempre es válido."

**¿Cuál es la ventaja de la arquitectura hexagonal?**
> "El dominio queda **aislado e intercambiable**: podemos cambiar Firebase, la UI o la base
> de datos sin tocar la lógica de negocio. Y se puede **testear sin frameworks**."

**¿Cuántos módulos tiene el proyecto?**
> "Un módulo Gradle (`:app`), pero varios **módulos funcionales** (Pedidos, Cocina, Caja,
> Inventario, Impresión). Para explicar arquitectura usamos los funcionales."

**¿Por qué Clean + DDD + Hexagonal juntos?**
> "Son complementarios: DDD dice *qué* modelar, Hexagonal *cómo* aislar el dominio, Clean
> *cómo* organizar las capas. Los tres protegen el dominio de los frameworks."
