# SIGCR - Sistema Integral de Gestión para Clínicas de Rehabilitación

## Descripción del proyecto

Prototipo del SIGCR para centralizar y gestionar información clínica, terapéutica y administrativa de pacientes en clínicas de rehabilitación. Implementado con Java SE, JavaFX para la interfaz gráfica, y MySQL para la base de datos.

## Requerimientos
```
- Java SE 17 (o superior)
- JavaFX SDK
- MySQL 8.0+
- MySQL Connector/J (incluido en lib/mysql-connector-j-8.4.0.jar)
- IDE sugerido: IntelliJ IDEA o Eclipse
```
## Instalación y configuración inicial

1. Configuración del proyecto JavaFX

**Compilación:**
```bash
export PATH_TO_FX=path/to/javafx/lib

javac --module-path $PATH_TO_FX \
    --add-modules javafx.controls,javafx.fxml \
    -cp "lib/mysql-connector-j-8.4.0.jar:src/main/java" \
    -d out \
    src/main/java/com/sigcr/models/*.java \
    src/main/java/com/sigcr/dao/*.java \
    src/main/java/com/sigcr/controllers/*.java \
    src/main/java/com/sigcr/components/*.java \
    src/main/java/com/sigcr/views/*.java \
    src/main/java/com/sigcr/services/*.java \
    src/main/java/com/sigcr/repositories/*.java \
    src/main/java/com/sigcr/Main.java
```
2. Puedes usar Docker para ejecutar una base de datos MySQL rápidamente:
```
  docker run -d -p 3306:3306 --name sigcr-db \
    -e MYSQL_ROOT_PASSWORD=password \
    -e MYSQL_DATABASE=sigcr \
    mysql:8.0

  docker cp init.db.sql sigcr-db:/init.db.sql

  docker exec -it sigcr-db bash

  mysql -u root -p password < init.db.sql

  exit
```
3. Ejecución del proyecto

**Ejecución:**
```bash
java --module-path $PATH_TO_FX \
    --add-modules javafx.controls \
    -cp "out:lib/mysql-connector-j-8.4.0.jar" \
    com.sigcr.Main
```