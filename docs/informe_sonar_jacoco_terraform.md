# Informe: SonarCloud, JaCoCo y Terraform en La Previa Restobar

## 1. Objetivo

En el proyecto La Previa Restobar se agregaron herramientas de calidad, metricas y automatizacion de infraestructura para mejorar el proceso DevOps.

Las herramientas principales son:

- **SonarCloud**: analiza la calidad del codigo.
- **JaCoCo**: genera el porcentaje de cobertura de pruebas unitarias.
- **Terraform**: define infraestructura cloud como codigo.

Estas herramientas no cambian directamente la funcionalidad de la app Android. Su objetivo es mejorar la calidad, seguridad, mantenibilidad y preparacion para despliegue.

## 2. SonarCloud

SonarCloud se agrego para realizar analisis estatico del codigo. Esto permite detectar problemas antes de que lleguen a produccion.

En el proyecto se configura desde el archivo:

```text
build.gradle.kts
```

Configuracion principal:

```kotlin
id("org.sonarqube") version "6.3.1.5724"

sonar {
    properties {
        property("sonar.projectKey", "luisumit_LaPreviaRestobar")
        property("sonar.organization", "luisumit")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.androidLint.reportPaths", "app/build/reports/lint-results-debug.xml")
        property("sonar.coverage.jacoco.xmlReportPaths", "app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml")
    }
}
```

### Que mide SonarCloud

SonarCloud muestra metricas como:

- **Bugs**: posibles errores en el codigo.
- **Vulnerabilidades**: riesgos de seguridad.
- **Security Hotspots**: partes que deben revisarse manualmente.
- **Code Smells**: codigo dificil de mantener.
- **Duplicaciones**: codigo repetido.
- **Coverage**: porcentaje de codigo cubierto por pruebas.

### Como se ejecuta

SonarCloud se ejecuta automaticamente en GitHub Actions con:

```bash
./gradlew sonar -Dsonar.token=$SONAR_TOKEN
```

El token se guarda en GitHub como secreto:

```text
SONAR_TOKEN
```

## 3. JaCoCo

JaCoCo se agrego para medir la cobertura de pruebas unitarias. La cobertura indica que porcentaje del codigo fue ejecutado durante los tests.

La configuracion principal esta en:

```text
app/build.gradle.kts
```

Se agrego el plugin:

```kotlin
id("jacoco")
```

Y la version:

```kotlin
jacoco {
    toolVersion = "0.8.12"
}
```

Tambien se creo la tarea:

```kotlin
tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")
}
```

### Para que sirve

JaCoCo genera reportes en formato XML y HTML:

- XML: lo lee SonarCloud para mostrar el porcentaje de cobertura.
- HTML: sirve para revisar localmente que clases tienen mas o menos cobertura.

Ruta del reporte usado por SonarCloud:

```text
app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml
```

### Como se ejecuta en GitHub Actions

En el workflow:

```text
.github/workflows/android-ci.yml
```

se ejecuta:

```bash
./gradlew :app:cleanTestDebugUnitTest :app:testDebugUnitTest :app:jacocoTestReport --rerun-tasks
```

Esto hace tres cosas:

1. Limpia resultados anteriores de tests.
2. Ejecuta pruebas unitarias.
3. Genera el reporte de cobertura JaCoCo.

Luego el workflow verifica que el reporte exista antes de enviar los datos a SonarCloud.

## 4. Relacion entre SonarCloud y JaCoCo

SonarCloud no calcula la cobertura por si solo. SonarCloud necesita que JaCoCo genere primero el archivo XML.

Flujo:

```text
Tests unitarios -> JaCoCo genera XML -> SonarCloud lee XML -> SonarCloud muestra Coverage
```

Si el XML no existe, SonarCloud puede mostrar:

```text
Coverage 0.0%
```

Por eso se agrego una validacion en GitHub Actions para confirmar que el reporte de JaCoCo se genere correctamente.

## 5. Terraform

Terraform se agrego como herramienta de Infrastructure as Code (IaC). Esto significa que la infraestructura cloud se puede definir mediante archivos de codigo, en lugar de crear recursos manualmente desde la consola.

Los archivos estan en:

```text
infra/terraform/
```

### Objetivo de Terraform en el proyecto

Terraform prepara la infraestructura de Google Cloud y Firebase para una fase de despliegue del backend.

Permite definir:

- Proyecto Firebase.
- Apps Android en Firebase.
- Realtime Database.
- Secret Manager.
- Artifact Registry.
- Cloud Run.
- Service accounts.
- Permisos IAM.

## 6. Terraform: IAM y permisos

El archivo principal para permisos es:

```text
infra/terraform/iam.tf
```

Este archivo aplica el principio de minimo privilegio. Eso significa que cada servicio recibe solo los permisos necesarios.

### APIs habilitadas

Terraform habilita APIs necesarias para la fase de backend:

- **Artifact Registry**: almacena imagenes Docker.
- **Cloud Build**: construye y despliega contenedores.
- **IAM**: administra identidades y permisos.
- **Cloud Run**: ejecuta el backend.
- **Secret Manager**: guarda credenciales de forma segura.

### Service accounts creadas

Se crean dos identidades:

```text
laprevia-backend-run
laprevia-backend-build
```

Uso de cada una:

- **laprevia-backend-run**: identidad usada por el backend cuando corre en Cloud Run.
- **laprevia-backend-build**: identidad usada para construir y desplegar la imagen del backend.

### Permisos asignados

Terraform asigna permisos como:

- Acceso a Firebase Realtime Database para el backend.
- Permiso para escribir imagenes Docker en Artifact Registry.
- Permiso para desplegar en Cloud Run.
- Permiso para que Cloud Run lea imagenes desde Artifact Registry.

Esto evita usar cuentas personales o archivos JSON con permisos demasiado amplios.

## 7. Terraform: Cloud Run y backend

El archivo:

```text
infra/terraform/cloud_run.tf
```

define recursos para ejecutar el backend Express en Cloud Run.

Incluye:

- Repositorio Docker en Artifact Registry.
- Servicio Cloud Run para el backend.
- Variables de entorno como `NODE_ENV`.
- URL de Firebase Realtime Database.
- Configuracion de CPU, memoria e instancias.

Tambien puede conectar secretos desde Secret Manager para que las credenciales no queden escritas directamente en el codigo.

## 8. Terraform: Firebase

El archivo:

```text
infra/terraform/firebase.tf
```

define apps Android dentro de Firebase.

Incluye:

- App principal Android.
- App staging opcional.
- Package name del proyecto.
- SHA-256 de la app cuando sea necesario.

Esto permite registrar la app Android en Firebase de forma repetible.

## 9. GitHub Actions

El archivo:

```text
.github/workflows/android-ci.yml
```

automatiza el proceso CI/CD.

Pasos principales:

1. Descarga el codigo.
2. Configura Java 17.
3. Crea `google-services.json` desde secretos.
4. Ejecuta Detekt.
5. Ejecuta Android Lint.
6. Ejecuta tests unitarios con JaCoCo.
7. Verifica el reporte de cobertura.
8. Ejecuta SonarCloud.
9. Compila el APK debug.
10. Sube reportes y APK como artefactos.

## 10. Conclusion

Con SonarCloud, JaCoCo y Terraform, el proyecto mejora en tres aspectos:

- **Calidad de codigo**: SonarCloud detecta bugs, vulnerabilidades y malas practicas.
- **Metricas de pruebas**: JaCoCo permite medir la cobertura de tests unitarios.
- **DevOps e IaC**: Terraform prepara la infraestructura cloud de forma automatizada y versionada.

Estas herramientas ayudan a que La Previa Restobar sea un proyecto mas profesional, medible y preparado para despliegues futuros.
