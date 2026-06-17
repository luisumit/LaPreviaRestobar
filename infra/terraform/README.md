# Terraform - La Previa Restobar

Esta carpeta contiene la Fase 1 de infraestructura como codigo para la app Android de La Previa Restobar.

La configuracion cubre:

- Proyecto GCP/Firebase, usando uno existente por defecto.
- APIs necesarias para Firebase.
- Registro de apps Android Firebase para `com.laprevia.restobar` y `com.laprevia.restobar.staging`.
- Firebase Realtime Database.
- Firebase Auth con email/password.
- Outputs utiles para conectar la configuracion con Android y CI/CD.

## Prerrequisitos

1. Instalar Terraform.
2. Instalar Google Cloud CLI.
3. Autenticarse con una cuenta con permisos sobre el proyecto:

```powershell
gcloud auth application-default login
gcloud config set project laprevia-restobar
```

4. Copiar la plantilla de variables:

```powershell
Copy-Item terraform.tfvars.example terraform.tfvars
```

5. Revisar `terraform.tfvars` y ajustar valores como `project_id`, `authorized_domains` y certificados SHA-256 si corresponde.

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

## Notas para el curso

- No se crean datos de negocio como mesas, productos, inventario u ordenes. Esos datos pertenecen a la aplicacion y actualmente se inicializan desde Kotlin.
- Las reglas de Realtime Database no estan incluidas en esta Fase 1. Pueden gestionarse luego con Firebase CLI y un archivo `database.rules.json`.
- Firebase Auth mediante `google_identity_platform_config` puede requerir que el proyecto tenga facturacion habilitada.
- Los archivos `terraform.tfvars` y cualquier credencial local no deben subirse al repositorio si contienen valores sensibles.
