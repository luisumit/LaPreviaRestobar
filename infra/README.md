# Infraestructura como codigo

Esta carpeta contiene una base simple de IaC para La Previa Restobar.

No forma parte del flujo principal de la app Android. Su objetivo es dejar preparada
la estructura para automatizar infraestructura y tareas de despliegue en el futuro.

## Herramientas incluidas

- `terraform/`: base para administrar recursos de Google Cloud/Firebase.
- `ansible/`: base para verificar o preparar herramientas de despliegue en un entorno local o servidor.

## Uso futuro sugerido

1. Terraform puede encargarse de definir recursos cloud.
2. Ansible puede preparar servidores o entornos de despliegue.
3. Firebase CLI sigue publicando Hosting y reglas con `firebase deploy`.
