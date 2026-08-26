# Plan de migración a Kotlin Multiplatform (KMP)

Migración de **LaPrevia Restobar** de Android nativo a **Kotlin Multiplatform + Compose Multiplatform**, con targets **Android + Desktop + iOS**.

> Estado actual: app Android (Kotlin + Jetpack Compose + Hilt + Room + Firebase + WorkManager).
> La lógica de negocio crítica ya está extraída a objetos puros (`Billing`, `SalesCalculator`, `SplitBill`, `ReceiptFormatter`, `EscPosEncoder`) y cubierta con 66 tests — lista para compartirse.

---

## ⚙️ Prerrequisitos

- **Mac + Xcode** → obligatorio para compilar iOS. Sin Mac: solo Android + Desktop.
- Plugin **Kotlin Multiplatform** + **Compose Multiplatform** en Android Studio.
- Git limpio, con **una rama por fase** (mantener Android funcionando siempre).

---

## 🏗️ Estructura objetivo

```
proyecto/
├─ shared/           ← código compartido
│  ├─ commonMain/    ← lógica + modelos + UI Compose MP (lo máximo posible)
│  ├─ androidMain/   ← implementaciones Android (Bluetooth, PDF…)
│  ├─ iosMain/       ← implementaciones iOS
│  └─ desktopMain/   ← implementaciones Desktop
├─ androidApp/       ← app Android (entry point)
├─ iosApp/           ← app iOS (Xcode)
└─ desktopApp/       ← app Escritorio
```

---

## 🔄 Qué se reutiliza vs qué se reemplaza

### Se reutiliza casi directo
- `Billing`, `SalesCalculator`, `SplitBill`, `ReceiptFormatter`, `EscPosEncoder`
- Modelos (`Order`, `Product`, `Table`, `Inventory`…)
- Los 66 tests unitarios (son puros → van a `commonTest`)
- Pantallas Compose (con adaptación a Compose Multiplatform)

### Se reemplaza (librerías solo-Android)

| Ahora (Android) | Para KMP |
|---|---|
| Hilt (DI) | **Koin** |
| Room | **SQLDelight** (o Room-KMP) |
| Jetpack Compose (androidx) | **Compose Multiplatform** (jetbrains) |
| Firebase SDK | **GitLive Firebase KMP** |
| WorkManager, notificaciones, Context, PDF, Bluetooth | código `expect/actual` por plataforma |

---

## 📋 Fases

### 🟢 Fase 1 — Lógica pura al `commonMain` *(quick win, esfuerzo bajo)*
**Meta:** mover lo que ya es 100% Kotlin puro.
- Mover: `Billing`, `SalesCalculator`, `SplitBill`, `ReceiptFormatter`, `EscPosEncoder`, modelos.
- Agregar **kotlinx.serialization** a los modelos.
- Mover los 66 tests → `commonTest`.
- **Ganancia inmediata:** esa lógica ya corre en las 3 plataformas.

### 🟡 Fase 2 — Reemplazar librerías solo-Android *(esfuerzo alto)*
**Meta:** quitar lo no-multiplataforma (app sigue siendo Android, verificando en cada paso).
- **Hilt → Koin**
- **Room → SQLDelight**
- Es el trabajo estructural más pesado. Android debe seguir funcionando en cada commit.

### 🟡 Fase 3 — Firebase multiplataforma *(esfuerzo medio-alto)*
**Meta:** Firebase fuera de Android.
- **Firebase SDK → GitLive Firebase KMP** (Realtime DB + Auth).
- Verificar que Android siga igual.

### 🟡 Fase 4 — UI a Compose Multiplatform *(esfuerzo medio)*
**Meta:** compartir las pantallas.
- **`androidx.compose` → `org.jetbrains.compose`**.
- Pantallas compartibles → `commonMain`; lo específico de Android → `androidMain`.

### 🟢 Fase 5 — Target Desktop *(la fácil, esfuerzo bajo-medio)*
**Meta:** correr en PC.
- Agregar `desktopApp` (JVM + Compose MP).
- Resolver específico de escritorio (rutas de archivos, notificaciones).
- ⭐ **Hacer ANTES que iOS** — valida toda la arquitectura compartida con poco esfuerzo.

### 🔴 Fase 6 — Target iOS *(la difícil, esfuerzo alto, requiere Mac)*
**Meta:** correr en iPhone.
- Agregar `iosApp` (Xcode) + `iosMain`.
- Firebase KMP en iOS, ajustes de UI.

### 🔴 Fase 7 — Integraciones nativas (`expect/actual`) *(esfuerzo alto)*
**Meta:** lo que toca hardware/SO: interfaz común, implementación por plataforma.
- **Impresión Bluetooth** ← ⚠️ lo más difícil (iOS usa BLE/MFi, hay que rehacerla).
- **PDF**, **notificaciones**, **conectividad**, **almacenamiento**.

### 🟢 Fase 8 — Testing + release por plataforma
- Tests puros ya en `commonTest`.
- Firmar/empaquetar: Android APK, iOS IPA, Desktop .exe/.dmg.

---

## 🗺️ Secuencia recomendada

```
Fase 1 (lógica) → 2 (Koin/SQLDelight) → 3 (Firebase) → 4 (Compose MP)
   → 5 (DESKTOP ✅ primer logro) → 6 (iOS) → 7 (nativo) → 8 (release)
```

---

## ⚠️ Riesgos clave

1. **Impresión Bluetooth en iOS** — el punto más caro. iOS no soporta Bluetooth clásico (SPP); las térmicas van por BLE o MFi. Planearlo aparte.
2. **iOS necesita Mac.** Sin Mac, quedarse en Android + Desktop.
3. Es una migración de **semanas**, no días — por eso va por fases, con Android siempre funcionando.

---

## 💡 Consejo práctico

Arrancar por **Fase 1** (mover lógica + tests, ganancia inmediata, cero riesgo) y llegar hasta **Fase 5 (Desktop)**. Con eso ya se tendría **Android + PC** compartiendo código. iOS (Fases 6-7) después, con Mac y tiempo disponibles.
