variable "project_id" {
  type        = string
  description = "ID del proyecto Firebase/Google Cloud."
  default     = "laprevia-restobar"
}

variable "region" {
  type        = string
  description = "Region por defecto para futuros recursos cloud."
  default     = "us-central1"
}
