variable "backend_artifact_registry_repository_id" {
  description = "Repositorio Docker de Artifact Registry para la imagen del backend Express."
  type        = string
  default     = "laprevia-backend"
}

variable "backend_artifact_registry_location" {
  description = "Ubicacion del repositorio Docker de Artifact Registry."
  type        = string
  default     = "us-central1"
}

variable "backend_image_name" {
  description = "Nombre de la imagen Docker del backend."
  type        = string
  default     = "api-firebase"
}

variable "backend_image_tag" {
  description = "Tag de la imagen Docker del backend."
  type        = string
  default     = "latest"
}

variable "backend_container_image" {
  description = "Imagen completa para Cloud Run. Si se deja null, se usa Artifact Registry con repository/name:tag."
  type        = string
  default     = null
}

variable "backend_cloud_run_service_name" {
  description = "Nombre del servicio Cloud Run que despliega el backend Express."
  type        = string
  default     = "laprevia-backend"
}

variable "backend_cloud_run_allow_public_invoker" {
  description = "Permite acceso publico al backend Cloud Run. Para una demo puede ser true; en produccion conviene protegerlo."
  type        = bool
  default     = true
}

variable "backend_cloud_run_min_instances" {
  description = "Instancias minimas de Cloud Run."
  type        = number
  default     = 0
}

variable "backend_cloud_run_max_instances" {
  description = "Instancias maximas de Cloud Run."
  type        = number
  default     = 2
}

variable "backend_cloud_run_cpu" {
  description = "CPU asignada al contenedor del backend."
  type        = string
  default     = "1"
}

variable "backend_cloud_run_memory" {
  description = "Memoria asignada al contenedor del backend."
  type        = string
  default     = "512Mi"
}

variable "backend_service_account_json_secret_id" {
  description = "Secret Manager secret ID que contiene el JSON de Firebase Admin para compatibilidad con el backend actual."
  type        = string
  default     = "laprevia-backend-firebase-service-account-json"
}

variable "backend_service_account_json_secret_version" {
  description = "Version del secreto JSON de Firebase Admin. Usar null para no montarlo; usar latest cuando ya exista una version cargada manualmente."
  type        = string
  default     = null
}

locals {
  backend_container_image = coalesce(
    var.backend_container_image,
    "${var.backend_artifact_registry_location}-docker.pkg.dev/${local.project_id}/${var.backend_artifact_registry_repository_id}/${var.backend_image_name}:${var.backend_image_tag}"
  )

  backend_uses_service_account_json_secret = var.backend_service_account_json_secret_version != null
}

resource "google_artifact_registry_repository" "backend" {
  provider = google-beta

  project       = local.project_id
  location      = var.backend_artifact_registry_location
  repository_id = var.backend_artifact_registry_repository_id
  description   = "Docker images for the La Previa Restobar Express backend."
  format        = "DOCKER"

  labels = {
    app       = "laprevia-restobar"
    component = "backend"
    managedby = "terraform"
  }

  depends_on = [
    google_project_service.phase2_required
  ]
}

resource "google_artifact_registry_repository_iam_member" "cloud_build_writer" {
  project    = local.project_id
  location   = google_artifact_registry_repository.backend.location
  repository = google_artifact_registry_repository.backend.repository_id
  role       = "roles/artifactregistry.writer"
  member     = "serviceAccount:${google_service_account.backend_cloud_build.email}"
}

resource "google_artifact_registry_repository_iam_member" "cloud_run_reader" {
  project    = local.project_id
  location   = google_artifact_registry_repository.backend.location
  repository = google_artifact_registry_repository.backend.repository_id
  role       = "roles/artifactregistry.reader"
  member     = "serviceAccount:${local.cloud_run_service_agent}"
}

resource "google_cloud_run_v2_service" "backend" {
  provider = google-beta

  project  = local.project_id
  name     = var.backend_cloud_run_service_name
  location = var.region

  ingress = "INGRESS_TRAFFIC_ALL"

  template {
    service_account = google_service_account.backend_runtime.email

    scaling {
      min_instance_count = var.backend_cloud_run_min_instances
      max_instance_count = var.backend_cloud_run_max_instances
    }

    containers {
      image = local.backend_container_image

      command = local.backend_uses_service_account_json_secret ? ["/bin/sh"] : null
      args    = local.backend_uses_service_account_json_secret ? ["-c", "printf '%s' \"$FIREBASE_SERVICE_ACCOUNT_JSON\" > serviceAccountKey.json && node server.js"] : null

      ports {
        container_port = 3000
      }

      env {
        name  = "NODE_ENV"
        value = "production"
      }

      env {
        name  = "FIREBASE_DATABASE_URL"
        value = google_firebase_database_instance.default.database_url
      }

      dynamic "env" {
        for_each = local.backend_uses_service_account_json_secret ? [1] : []

        content {
          name = "FIREBASE_SERVICE_ACCOUNT_JSON"

          value_source {
            secret_key_ref {
              secret  = google_secret_manager_secret.backend[var.backend_service_account_json_secret_id].secret_id
              version = var.backend_service_account_json_secret_version
            }
          }
        }
      }

      resources {
        limits = {
          cpu    = var.backend_cloud_run_cpu
          memory = var.backend_cloud_run_memory
        }
      }
    }
  }

  traffic {
    type    = "TRAFFIC_TARGET_ALLOCATION_TYPE_LATEST"
    percent = 100
  }

  depends_on = [
    google_artifact_registry_repository.backend,
    google_project_iam_member.backend_runtime
  ]
}

resource "google_cloud_run_v2_service_iam_member" "public_invoker" {
  count = var.backend_cloud_run_allow_public_invoker ? 1 : 0

  project  = local.project_id
  location = google_cloud_run_v2_service.backend.location
  name     = google_cloud_run_v2_service.backend.name
  role     = "roles/run.invoker"
  member   = "allUsers"
}

output "backend_artifact_registry_repository" {
  description = "Repositorio Docker para construir y publicar la imagen desde backend/."
  value       = google_artifact_registry_repository.backend.name
}

output "backend_container_image" {
  description = "Imagen configurada en Cloud Run para el backend Express."
  value       = local.backend_container_image
}

output "backend_cloud_run_url" {
  description = "URL del servicio Cloud Run del backend Express."
  value       = google_cloud_run_v2_service.backend.uri
}
