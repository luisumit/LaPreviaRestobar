# Terraform - La Previa Restobar

Esta carpeta contiene la infraestructura como codigo para la app Android y el backend de La Previa Restobar.

La configuracion esta organizada en dos fases:

- Fase 1: base Firebase/GCP para la app Android.
- Fase 2: backend Express desplegable en Cloud Run con IAM, Secret Manager y Artifact Registry.

## Que cubre esta infraestructura

### Fase 1 - Firebase para Android

- Proyecto GCP/Firebase, usando uno existente por defecto.
- APIs necesarias para Firebase.
- Registro de apps Android Firebase para `com.laprevia.restobar` y `com.laprevia.restobar.staging`.
- Firebase Realtime Database.
- Firebase Auth con email/password.
- Outputs utiles para conectar la configuracion con Android y CI/CD.

### Fase 2 - Backend Express en GCP

- APIs necesarias para backend y despliegue: Cloud Run, Artifact Registry, Cloud Build, IAM y Secret Manager.
- Service accounts separadas para runtime y despliegue.
- Permisos minimos para que el backend acceda a Realtime Database y secretos.
- Secret Manager para credenciales del backend sin guardarlas en Git ni en Terraform.
- Repositorio Docker en Artifact Registry.
- Servicio Cloud Run para ejecutar el backend Express ubicado en `backend/`.

## Prerrequisitos

1. Instalar Terraform.
2. Instalar Google Cloud CLI.
3. Instalar Docker si se va a construir y publicar la imagen del backend.
4. Autenticarse con una cuenta con permisos sobre el proyecto:

```powershell
gcloud auth application-default login
gcloud config set project laprevia-restobar
```

5. La cuenta usada para ejecutar Terraform debe poder administrar, al menos:

- APIs del proyecto.
- Firebase.
- Firebase Realtime Database.
- Identity Platform / Firebase Auth.
- IAM.
- Service Accounts.
- Secret Manager.
- Artifact Registry.
- Cloud Run.

6. Copiar la plantilla de variables:

```powershell
Copy-Item terraform.tfvars.example terraform.tfvars
```

7. Revisar `terraform.tfvars` y ajustar valores como `project_id`, `authorized_domains` y certificados SHA-256 si corresponde.

## Inicializar

```powershell
terraform init
```

## Revisar cambios

```powershell
terraform plan
```

Si el proyecto Firebase ya existe, importa los recursos antes de aplicar para evitar intentar recrearlos. Ejemplos:

```powershell
terraform import google_firebase_project.default laprevia-restobar
terraform import google_firebase_database_instance.default projects/laprevia-restobar/locations/us-central1/instances/laprevia-restobar-default-rtdb
```

Las apps Android existentes tambien deben importarse con su ID de Firebase App si ya fueron creadas desde la consola.

## Aplicar

```powershell
terraform apply
```

Terraform mostrara el plan y pedira confirmacion antes de crear o cambiar infraestructura.

## Archivos de la Fase 1

### `versions.tf`

Declara la version minima de Terraform y los providers usados: `google`, `google-beta` y `time`.

### `providers.tf`

Configura los providers de GCP apuntando al proyecto definido por `project_id`.

### `variables.tf`

Centraliza variables para el proyecto Firebase, regiones, apps Android, Realtime Database y dominios autorizados de Auth.

### `terraform.tfvars.example`

Plantilla para crear un `terraform.tfvars` local. No debe contener secretos reales.

### `project.tf`

Habilita APIs base, permite crear o usar un proyecto existente y activa Firebase en el proyecto.

### `firebase.tf`

Registra las apps Android Firebase para la app principal y la variante staging.

### `realtime_database.tf`

Crea o administra la instancia de Firebase Realtime Database usada por la app para `orders`, `tables`, `products`, `inventory` y `users`.

### `auth.tf`

Configura Firebase Auth / Identity Platform con email/password y deshabilita el acceso anonimo.

### `outputs.tf`

Expone datos utiles como IDs de apps Android, URL de Realtime Database y numero del proyecto Firebase.

## Archivos de la Fase 2

### `iam.tf`

Habilita APIs adicionales para backend y crea identidades separadas:

- `laprevia-backend-run`: service account que ejecuta el backend en Cloud Run.
- `laprevia-backend-build`: service account pensada para construir y desplegar la imagen.

Tambien asigna permisos minimos para:

- acceder a Firebase Realtime Database desde el backend;
- escribir imagenes en Artifact Registry;
- desplegar en Cloud Run;
- permitir que Cloud Run lea imagenes Docker;
- permitir que la service account de build use la service account de runtime.

### `secrets.tf`

Crea secretos en Secret Manager sin guardar valores sensibles en Terraform.

Por defecto crea el secreto:

```text
laprevia-backend-firebase-service-account-json
```

Este secreto esta pensado para guardar el JSON de Firebase Admin que el backend actual espera como `serviceAccountKey.json`.

Terraform crea el contenedor del secreto y permisos de lectura para Cloud Run, pero la version con el valor real se debe cargar manualmente o desde un pipeline seguro.

Ejemplo para cargar una version desde un archivo local:

```powershell
gcloud secrets versions add laprevia-backend-firebase-service-account-json --data-file=backend/serviceAccountKey.json
```

No subas `backend/serviceAccountKey.json` al repositorio.

### `cloud_run.tf`

Define la infraestructura para desplegar el backend Express de `backend/`:

- repositorio Docker en Artifact Registry;
- permisos de lectura/escritura sobre el repositorio;
- servicio Cloud Run v2;
- variables de CPU, memoria, min/max instancias y acceso publico;
- outputs con la imagen configurada y la URL del backend.

La imagen esperada por defecto es:

```text
us-central1-docker.pkg.dev/laprevia-restobar/laprevia-backend/api-firebase:latest
```

El backend actual usa `serviceAccountKey.json`. Para mantener compatibilidad sin subir ese archivo a Git, `cloud_run.tf` permite inyectar el JSON desde Secret Manager si configuras:

```hcl
backend_service_account_json_secret_version = "latest"
```

Si ese valor queda en `null`, Cloud Run no monta el secreto.

## Construir y publicar la imagen del backend

Primero aplica Terraform para crear Artifact Registry:

```powershell
terraform apply
```

Luego configura Docker para autenticarse con Artifact Registry:

```powershell
gcloud auth configure-docker us-central1-docker.pkg.dev
```

Construye la imagen desde la raiz del proyecto:

```powershell
docker build -t us-central1-docker.pkg.dev/laprevia-restobar/laprevia-backend/api-firebase:latest backend/
```

Publica la imagen:

```powershell
docker push us-central1-docker.pkg.dev/laprevia-restobar/laprevia-backend/api-firebase:latest
```

Despues puedes ejecutar nuevamente:

```powershell
terraform apply
```

para que Cloud Run use la imagen publicada.

## Notas para el curso

- No se crean datos de negocio como mesas, productos, inventario u ordenes. Esos datos pertenecen a la aplicacion y actualmente se inicializan desde Kotlin.
- Las reglas de Realtime Database no estan incluidas en esta Fase 1. Pueden gestionarse luego con Firebase CLI y un archivo `database.rules.json`.
- Firebase Auth mediante `google_identity_platform_config` puede requerir que el proyecto tenga facturacion habilitada.
- Los archivos `terraform.tfvars` y cualquier credencial local no deben subirse al repositorio si contienen valores sensibles.
- La Fase 2 prepara el despliegue del backend, pero no construye la imagen Docker por si sola. Esa parte puede ejecutarse manualmente o agregarse luego a CI/CD con GitHub Actions o Cloud Build.
- En produccion conviene revisar si `backend_cloud_run_allow_public_invoker` debe ser `false` y proteger el backend con autenticacion.
