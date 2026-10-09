# Transecto Map

Aplicación Java Swing para dibujar áreas, cuadrantes y transectos sobre un mapa satelital interactivo usando JMapViewer, GeoTools y JTS.

## Requisitos
- Windows 11 64 bits
- JDK 21 LTS 64 bits
- Apache NetBeans 20 o superior
- Conexión a internet para cargar los tiles del mapa

## Abrir en NetBeans
1. Abrir NetBeans.
2. File > Open Project...
3. Seleccionar la carpeta `app`.
4. Esperar a que Maven resuelva dependencias.
5. Ejecutar con Run > Run Project.

## Ejecutar desde línea de comandos
```bash
java -jar target/transecto-map-1.0.0-shaded.jar
```

## Empaquetar en JAR ejecutable
Desde NetBeans:
- Run > Clean and Build
- o Run > Create JAR File

El artefacto final se genera en la carpeta `target/`.

## Nota
La aplicación usa Bing Aerial y OSM como origen de tiles. La carga del mapa necesita acceso a internet.
