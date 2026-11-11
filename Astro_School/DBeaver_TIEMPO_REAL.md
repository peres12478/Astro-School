# Configuración H2 local

Esta versión no incluye ni inicia el servidor TCP de H2 para conectar DBeaver simultáneamente con Spring Boot. El registro se guarda en `data/colegio.mv.db` y puede consultarse desde las funciones del panel o de la API.

Si quieres inspeccionar el archivo directamente con una herramienta externa, detén primero Spring Boot y guarda una copia de seguridad. No abras la base en modo Embedded simultáneamente con la aplicación.
