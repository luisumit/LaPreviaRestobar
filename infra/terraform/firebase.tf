resource "google_firebase_android_app" "main" {
  provider = google-beta

  project       = google_firebase_project.default.project
  display_name  = var.android_display_name
  package_name  = var.android_package_name
  sha256_hashes = var.android_sha256_hashes

  depends_on = [
    google_firebase_project.default
  ]
}

resource "google_firebase_android_app" "staging" {
  count    = var.create_staging_android_app ? 1 : 0
  provider = google-beta

  project       = google_firebase_project.default.project
  display_name  = var.staging_android_display_name
  package_name  = var.staging_android_package_name
  sha256_hashes = var.staging_android_sha256_hashes

  depends_on = [
    google_firebase_project.default
  ]
}
