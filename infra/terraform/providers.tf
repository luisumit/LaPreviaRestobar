provider "google" {
  project = var.project_id
  region  = var.region
}

provider "google-beta" {
  project               = var.project_id
  region                = var.region
  user_project_override = true
}

provider "google-beta" {
  alias                 = "no_user_project_override"
  project               = var.project_id
  region                = var.region
  user_project_override = false
}
