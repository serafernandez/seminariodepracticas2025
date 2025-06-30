# SIGCR - Sistema Integral de Gestion para Clinicas de Rehabilitacion

## Descripcion del proyecto

Prototipo del SIGCR para centralizar y gestionar informacion clinica, terapeutica y administrativa de pacientes en clinicas de rehabilitacion. Implementado con Java SE, JavaFX para la interfaz grafica, y MySQL para la base de datos.

## Requerimientos
```
- Java SE 17 (o superior)
- JavaFX SDK
- MySQL 8.0+
- MySQL Connector/J (incluido en lib/mysql-connector-j-8.4.0.jar)
- IDE sugerido: IntelliJ IDEA o Eclipse
```
## Instalacion y configuracion inicial

1. Configuracion del proyecto JavaFX

**Compilacion:**
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
2. Puedes usar Docker para ejecutar una base de datos MySQL rapidamente:
```
  docker run -d -p 3306:3306 --name sigcr-db \
    -e MYSQL_ROOT_PASSWORD=password \
    -e MYSQL_DATABASE=sigcr \
    mysql:8.0

  docker cp init.db.sql sigcr-db:/init.db.sql

  docker exec -it sigcr-db bash

  mysql -u root -p < init.db.sql

  exit
```
3. Ejecucion del proyecto

**Ejecucion:**
```bash
java --module-path $PATH_TO_FX \
    --add-modules javafx.controls \
    -cp "out:lib/mysql-connector-j-8.4.0.jar" \
    com.sigcr.Main
```