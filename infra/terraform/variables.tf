variable "project_id" {
  description = "ID del proyecto GCP/Firebase. Para este proyecto existente: laprevia-restobar."
  type        = string
  default     = "laprevia-restobar"
}

variable "project_name" {
  description = "Nombre visible del proyecto si Terraform debe crearlo."
  type        = string
  default     = "La Previa Restobar"
}

variable "create_project" {
  description = "Si es true, Terraform crea el proyecto GCP. Si es false, usa un proyecto ya existente."
  type        = bool
  default     = false
}

variable "billing_account" {
  description = "Cuenta de facturacion GCP para proyectos nuevos o servicios que requieren Blaze. Ejemplo: 000000-000000-000000."
  type        = string
  default     = null
}

variable "org_id" {
  description = "ID de la organizacion GCP para crear el proyecto. Usar solo si create_project = true y no se usa folder_id."
  type        = string
  default     = null
}

variable "folder_id" {
  description = "ID de carpeta GCP para crear el proyecto. Usar solo si create_project = true y no se usa org_id."
  type        = string
  default     = null
}

variable "region" {
  description = "Region principal de GCP para recursos regionales."
  type        = string
  default     = "us-central1"
}

variable "realtime_database_region" {
  description = "Region de Firebase Realtime Database."
  type        = string
  default     = "us-central1"
}

variable "realtime_database_instance_id" {
  description = "ID global de la instancia Realtime Database. La default actual del proyecto es laprevia-restobar-default-rtdb."
  type        = string
  default     = "laprevia-restobar-default-rtdb"
}

variable "android_package_name" {
  description = "Package name de la app Android principal."
  type        = string
  default     = "com.laprevia.restobar"
}

variable "android_display_name" {
  description = "Nombre visible de la app Android principal en Firebase."
  type        = string
  default     = "La Previa Restobar Android"
}

variable "create_staging_android_app" {
  description = "Crea tambien la app Android de staging."
  type        = bool
  default     = true
}

variable "staging_android_package_name" {
  description = "Package name de la variante staging."
  type        = string
  default     = "com.laprevia.restobar.staging"
}

variable "staging_android_display_name" {
  description = "Nombre visible de la app Android staging en Firebase."
  type        = string
  default     = "La Previa Restobar Android Staging"
}

variable "android_sha256_hashes" {
  description = "Certificados SHA-256 para la app Android principal. Se puede dejar vacio en Fase 1."
  type        = list(string)
  default     = []
}

variable "staging_android_sha256_hashes" {
  description = "Certificados SHA-256 para la app Android staging. Se puede dejar vacio en Fase 1."
  type        = list(string)
  default     = []
}

variable "authorized_domains" {
  description = "Dominios autorizados para Firebase Auth."
  type        = list(string)
  default     = ["localhost"]
}
