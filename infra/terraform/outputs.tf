output "project_id" {
  description = "Proyecto GCP/Firebase administrado por esta configuracion."
  value       = local.project_id
}

output "firebase_project_number" {
  description = "Numero del proyecto Firebase."
  value       = google_firebase_project.default.project_number
}

output "android_app_id" {
  description = "ID de la app Android principal en Firebase."
  value       = google_firebase_android_app.main.app_id
}

output "staging_android_app_id" {
  description = "ID de la app Android staging en Firebase."
  value       = try(google_firebase_android_app.staging[0].app_id, null)
}

output "realtime_database_url" {
  description = "URL de Firebase Realtime Database."
  value       = google_firebase_database_instance.default.database_url
}

output "realtime_database_instance_name" {
  description = "Nombre completo de la instancia Realtime Database."
  value       = google_firebase_database_instance.default.name
}
