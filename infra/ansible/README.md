# Ansible

Playbook base para verificar herramientas necesarias del entorno.

No modifica la app Android ni despliega nada automaticamente. Esta pensado como
punto de partida para automatizar tareas futuras.

## Comando futuro

```powershell
ansible-playbook -i inventory.ini playbook.yml
```

Este playbook verifica:

- Node.js
- Firebase CLI
- Gradle Wrapper del proyecto

## Desplegar carta QR

El playbook `deploy-menu.yml` automatiza el despliegue de la carta digital
que usan los codigos QR.

```powershell
ansible-playbook -i inventory.ini deploy-menu.yml
```

Este comando verifica que existan `firebase.json` y `public-menu/index.html`,
y luego ejecuta `firebase deploy --only hosting`.
