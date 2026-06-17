resource "time_sleep" "wait_for_realtime_database_api" {
  create_duration = "60s"

  depends_on = [
    google_project_service.required
  ]
}

resource "google_firebase_database_instance" "default" {
  provider = google-beta

  project       = google_firebase_project.default.project
  region        = var.realtime_database_region
  instance_id   = var.realtime_database_instance_id
  type          = "DEFAULT_DATABASE"
  desired_state = "ACTIVE"

  depends_on = [
    time_sleep.wait_for_realtime_database_api
  ]
}
