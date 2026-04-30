# Configuración de GitHub Secrets para CI/CD

Para que el flujo de integración continua (GitHub Actions) funcione correctamente, debes configurar los siguientes secretos en tu repositorio de GitHub:

## Pasos para configurar los Secretos:
1. Ve a tu repositorio en GitHub.
2. Haz clic en **Settings** (Configuración).
3. En el menú de la izquierda, selecciona **Secrets and variables** > **Actions**.
4. Haz clic en **New repository secret**.

## Secretos Requeridos:

### 1. `GOOGLE_SERVICES_JSON`
- **Descripción**: El contenido completo del archivo `google-services.json`.
- **Valor**: Abre tu archivo `app/google-services.json`, copia todo el contenido JSON y pégalo aquí.

### 2. `FIREBASE_API_KEY`
- **Descripción**: La llave de API de Firebase.
- **Valor**: La misma llave que pusiste en tu `local.properties` (puedes encontrarla dentro del `google-services.json` en el campo `api_key` -> `current_key`).

---

## Nota sobre Seguridad
Estos secretos permiten que GitHub Actions compile la aplicación sin necesidad de subir archivos sensibles al repositorio. Nunca compartas estos valores en texto plano fuera de GitHub Secrets o archivos ignorados por Git.
