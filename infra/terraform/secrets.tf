variable "backend_secret_ids" {
  description = "Secretos que Secret Manager debe crear para el backend. No se crean versiones ni valores sensibles desde Terraform."
  type        = set(string)
  default = [
    "laprevia-backend-firebase-service-account-json"
  ]
}

resource "google_secret_manager_secret" "backend" {
  for_each = var.backend_secret_ids

  project   = local.project_id
  secret_id = each.key

  replication {
    auto {}
  }

  labels = {
    app       = "laprevia-restobar"
    component = "backend"
    managedby = "terraform"
  }

  depends_on = [
    google_project_service.phase2_required
  ]
}

resource "google_secret_manager_secret_iam_member" "backend_runtime_accessor" {
  for_each = google_secret_manager_secret.backend

  project   = local.project_id
  secret_id = each.value.secret_id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.backend_runtime.email}"
}
