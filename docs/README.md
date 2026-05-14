# LaPrevia Restobar 🍷🍽️

**LaPrevia Restobar** es una solución integral de gestión para restaurantes y bares, diseñada para optimizar la toma de pedidos, el control de inventario y la comunicación en tiempo real entre el personal de salón, cocina y administración.

## 🚀 Características Principales

- **Gestión de Órdenes en Tiempo Real:** Seguimiento instantáneo del estado de los pedidos (Pendiente, Preparando, Listo, Entregado).
- **Control de Inventario Inteligente:** Descuento automático de stock y alertas de niveles bajos.
- **Arquitectura Offline-First:** Capacidad de trabajar sin conexión con sincronización automática al recuperar el internet mediante **WorkManager**.
- **Multi-rol:** Interfaces personalizadas para Meseros, Chefs y Administradores.
- **Seguridad y Privacidad:** Implementación de ofuscación de código y logs protegidos con **Timber**.

## 🛠️ Stack Tecnológico

- **Lenguaje:** [Kotlin](https://kotlinlang.org/)
- **Interfaz de Usuario:** [Jetpack Compose](https://developer.android.com/jetpack/compose) (Modern Declarative UI)
- **Inyección de Dependencias:** [Hilt](https://developer.android.com/training/dependency-injection/hilt-android)
- **Base de Datos Local:** [Room](https://developer.android.com/training/data-storage/room)
- **Base de Datos Remota:** [Firebase Realtime Database](https://firebase.google.com/docs/database)
- **Sincronización de Fondo:** [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)
- **Redes:** [Retrofit](https://square.github.io/retrofit/) & [OkHttp](https://square.github.io/okhttp/)
- **Registro de Logs:** [Timber](https://github.com/JakeWharton/timber)

## 📦 Instalación y Configuración

Sigue estos pasos para ejecutar el proyecto localmente:

1. **Clonar el repositorio:**
   ```bash
   git clone https://github.com/luisumit/LaPreviaRestobar.git
   ```

2. **Configurar Firebase:**
    - Crea un proyecto en [Firebase Console](https://console.firebase.google.com/).
    - Descarga el archivo `google-services.json` y colócalo en la carpeta `app/`.
    - Habilita **Realtime Database** y **Authentication**.

3. **Configurar llaves de API:**
   Crea o edita el archivo `local.properties` en la raíz del proyecto y añade tu clave:
   ```properties
   firebase.api.key=TU_API_KEY_AQUI
   ```

4. **Compilar y Ejecutar:**
   Abre el proyecto en **Android Studio Jellyfish** (o superior) y presiona `Run`.

## 🔧 Notas Técnicas Recientes

- **Compatibilidad Android 15 (16KB Page Alignment):** El proyecto incluye configuraciones en `build.gradle.kts` y `AndroidManifest.xml` para asegurar la compatibilidad con dispositivos de nueva generación que requieren alineación de página de 16KB en librerías nativas.
- **Sincronización Optimizada:** Se implementó una lógica de comparación de *timestamps* (`updatedAt`) para evitar sobreescrituras innecesarias en la base de datos local, reduciendo el consumo de batería y datos.

## 🛡️ Seguridad y Privacidad

- **Ofuscación:** Se utiliza R8 en el modo `release` para proteger la lógica de negocio.
- **Logs Limpios:** Se ha migrado de `println` a `Timber`, asegurando que no se filtre información sensible en versiones de producción.
- **Git Hygiene:** Archivos sensibles como `google-services.json` y `serviceAccountKey.json` están incluidos en el `.gitignore`.

## 🤝 Contribución

Si deseas contribuir:
1. Crea un **Fork** del proyecto.
2. Crea una nueva rama para tu funcionalidad (`git checkout -b feature/NuevaMejora`).
3. Haz un **Commit** de tus cambios (`git commit -m 'Añade nueva funcionalidad'`).
4. Haz **Push** a la rama (`git push origin feature/NuevaMejora`).
5. Abre un **Pull Request**.

---
*Desarrollado por [Luis Umit](https://github.com/luisumit) - 2024*
