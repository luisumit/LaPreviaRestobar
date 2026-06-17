terraform {
  required_version = ">= 1.6.0"

  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 5.0"
    }
  }
}

provider "google" {
  project = var.project_id
  region  = var.region
}

# Base preparada para una futura gestion de recursos en Google Cloud/Firebase.
# No crea recursos todavia para evitar cambios reales en la infraestructura actual.
