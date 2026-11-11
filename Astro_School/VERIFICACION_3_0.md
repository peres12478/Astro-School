# Astro School 3.0 — verificación de entrega

## Comprobado en el entorno de preparación

- `node --check src/main/resources/static/app.js`: **correcto**.
- `node scripts/probar_panel_node.cjs`: **correcto** (sesión, perfiles, notas y QR).
- `node scripts/probar_astro_node.cjs`: **correcto** (noticiero, cursos, inscripción y cancelación).
- `node scripts/probar_laboratorios_node.cjs`: **correcto** (galería pública, filtros, escape de HTML, talleres de Robótica/Física/Astronomía y editor de código, mediante respuestas API simuladas).
- `python scripts/comprobar_estructura.py`: **correcto** (XML del POM, componentes, 72 rutas REST identificadas, sin duplicados evidentes de rutas).
- `unzip -t`: comprobar tras empaquetar.

## Preparado, pero no ejecutado aquí

- `src/test/java/com/ejemplo/colegio/LaboratoriosIntegrationTest.java`: pruebas de Spring Boot y MockMvc para permisos, carga y descarga de programas, inscripción, publicación en galería y Swagger.
- **No se pudo ejecutar `mvn clean verify`**, porque Maven no está instalado en el entorno de preparación. Ejecutar en el equipo destino antes de dar el backend por validado.

## Nota de seguridad

Solo se consultan proyectos publicados públicamente. Los programas almacenados en el historial de robots requieren acceso del estudiante dueño y su inscripción en Robótica. El código se almacena, pero nunca se ejecuta ni se compila automáticamente. Esta versión es apta como prototipo escolar local; requiere endurecimiento antes de Internet público.
