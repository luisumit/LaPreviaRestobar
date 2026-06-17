resource "google_identity_platform_config" "default" {
  provider = google-beta
  project  = google_firebase_project.default.project

  autodelete_anonymous_users = true
  authorized_domains         = var.authorized_domains

  sign_in {
    allow_duplicate_emails = false

    email {
      enabled           = true
      password_required = true
    }

    anonymous {
      enabled = false
    }
  }

  depends_on = [
    google_project_service.required,
    google_firebase_project.default
  ]
}
