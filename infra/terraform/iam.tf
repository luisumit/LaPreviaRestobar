locals {
  phase2_required_apis = toset([
    "artifactregistry.googleapis.com",
    "cloudbuild.googleapis.com",
    "iam.googleapis.com",
    "run.googleapis.com",
    "secretmanager.googleapis.com"
  ])

  backend_runtime_roles = toset([
    "roles/firebasedatabase.admin"
  ])

  backend_cloud_build_roles = toset([
    "roles/artifactregistry.writer",
    "roles/run.developer"
  ])

  cloud_run_service_agent = "service-${data.google_project.current.number}@serverless-robot-prod.iam.gserviceaccount.com"
}

resource "google_project_service" "phase2_required" {
  provider = google-beta.no_user_project_override
  for_each = local.phase2_required_apis

  project = local.project_id
  service = each.key

  disable_on_destroy = false

  depends_on = [
    google_project_service.required
  ]
}

data "google_project" "current" {
  project_id = local.project_id

  depends_on = [
    google_project_service.phase2_required
  ]
}

resource "google_service_account" "backend_runtime" {
  account_id   = "laprevia-backend-run"
  display_name = "La Previa backend Cloud Run runtime"
  description  = "Service account used by the Express backend running on Cloud Run."
  project      = local.project_id

  depends_on = [
    google_project_service.phase2_required
  ]
}

resource "google_service_account" "backend_cloud_build" {
  account_id   = "laprevia-backend-build"
  display_name = "La Previa backend Cloud Build deployer"
  description  = "Service account used to build and deploy the backend container."
  project      = local.project_id

  depends_on = [
    google_project_service.phase2_required
  ]
}

resource "google_project_iam_member" "backend_runtime" {
  for_each = local.backend_runtime_roles

  project = local.project_id
  role    = each.key
  member  = "serviceAccount:${google_service_account.backend_runtime.email}"
}

resource "google_project_iam_member" "backend_cloud_build" {
  for_each = local.backend_cloud_build_roles

  project = local.project_id
  role    = each.key
  member  = "serviceAccount:${google_service_account.backend_cloud_build.email}"
}

resource "google_service_account_iam_member" "cloud_build_can_use_backend_runtime" {
  service_account_id = google_service_account.backend_runtime.name
  role               = "roles/iam.serviceAccountUser"
  member             = "serviceAccount:${google_service_account.backend_cloud_build.email}"
}

resource "google_project_iam_member" "cloud_run_service_agent_artifact_reader" {
  project = local.project_id
  role    = "roles/artifactregistry.reader"
  member  = "serviceAccount:${local.cloud_run_service_agent}"

  depends_on = [
    google_project_service.phase2_required
  ]
}
