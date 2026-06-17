locals {
  project_id = var.create_project ? google_project.firebase[0].project_id : var.project_id

  required_apis = toset([
    "cloudresourcemanager.googleapis.com",
    "firebase.googleapis.com",
    "firebasedatabase.googleapis.com",
    "identitytoolkit.googleapis.com",
    "serviceusage.googleapis.com"
  ])
}

resource "google_project" "firebase" {
  count    = var.create_project ? 1 : 0
  provider = google-beta.no_user_project_override

  project_id      = var.project_id
  name            = var.project_name
  billing_account = var.billing_account
  org_id          = var.folder_id == null ? var.org_id : null
  folder_id       = var.folder_id

  labels = {
    app       = "laprevia-restobar"
    managedby = "terraform"
  }

  deletion_policy = "PREVENT"
}

resource "google_project_service" "required" {
  provider = google-beta.no_user_project_override
  for_each = local.required_apis

  project = local.project_id
  service = each.key

  disable_on_destroy = false

  depends_on = [
    google_project.firebase
  ]
}

resource "google_firebase_project" "default" {
  provider = google-beta
  project  = local.project_id

  depends_on = [
    google_project_service.required
  ]
}
