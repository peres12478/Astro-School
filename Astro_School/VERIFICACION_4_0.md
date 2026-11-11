# Verificación — Astro School 4.0

Resultados revisados en el entorno de construcción:

- `node --check src/main/resources/static/app.js`: OK.
- `node scripts/probar_panel_node.cjs`: OK (login, roles, notas y QR; simulación).
- `node scripts/probar_astro_node.cjs`: OK (noticias, Swagger, cursos; simulación).
- `node scripts/probar_laboratorios_node.cjs`: OK (galería, filtros, 3 laboratorios, editor de código; simulación).
- `python scripts/verificar_ejemplos.py`: OK (semillas y tres imágenes locales).
- `python scripts/comprobar_estructura.py`: OK (análisis estructural de rutas).

Límite: este entorno **no tiene Maven**, por lo que no se ha compilado ni arrancado Spring Boot. Antes de la exposición, ejecutar `mvn clean verify` con Java 17 o superior y comprobar los ejemplos desde navegador/Swagger. La verificación estructural no sustituye una prueba de integración real.

El contenido [DEMO] es ficticio, y los archivos PNG son **ilustraciones locales**, no fotografías reales.
