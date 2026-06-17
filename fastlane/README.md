# Fastlane

Fastlane se agrego como automatizacion basica para el flujo Android de La Previa Restobar.

No modifica el funcionamiento de la app y no se ejecuta automaticamente. Solo corre cuando se llama manualmente desde la terminal.

## Lanes disponibles

```powershell
bundle exec fastlane android validate
bundle exec fastlane android build_debug
bundle exec fastlane android clean
```

## Que hace

- `validate`: compila Kotlin en modo debug para validar el codigo.
- `build_debug`: genera un APK debug local.
- `clean`: limpia archivos generados por Gradle.

## Instalacion futura

Si Ruby y Bundler estan instalados:

```powershell
bundle install
```

Luego se pueden ejecutar los comandos de Fastlane.
