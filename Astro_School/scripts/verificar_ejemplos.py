"""Comprobacion estructural offline de ejemplos, imagenes y permisos del proyecto.
No sustituye las pruebas de integracion con Maven/Spring Boot."""
from pathlib import Path
from zipfile import ZipFile
import re
ROOT = Path(__file__).resolve().parents[1]
java = (ROOT/'src/main/java/com/ejemplo/colegio/config/DemoContenidoConfig.java').read_text()
front = (ROOT/'src/main/resources/static/app.js').read_text()
security = (ROOT/'src/main/java/com/ejemplo/colegio/config/SecurityConfig.java').read_text()
readme = (ROOT/'README.md').read_text()
for s in ['ROBOTICA', 'FISICA', 'ASTRONOMIA', 'new Nota(', 'new Trabajo(', 'new ClaseVirtual(', 'new Noticia(', 'new Creacion(', 'new Robot(', 'new CodigoRobot(']:
    assert s in java, f'Falta contenido ejemplo: {s}'
for a in ['robotica-demo.png','fisica-demo.png','astronomia-demo.png']:
    img = ROOT/'src/main/resources/demo-images'/a
    assert img.stat().st_size > 1000 and img.read_bytes()[:8] == b'\x89PNG\r\n\x1a\n', f'Imagen ausente: {a}'
assert 'const imagen = archivo' in front and 'class="creation-photo"' in front
assert 'const demo = String(p.titulo || \'\').startsWith(\'[DEMO]\')' in front
assert 'crearCuentasDePrueba' in (ROOT/'src/main/java/com/ejemplo/colegio/config/DemoDataConfig.java').read_text()
assert '@Profile("h2")' in java and '@ConditionalOnProperty' in java
assert 'requestMatchers(HttpMethod.GET, "/api/creaciones", "/api/creaciones/**").permitAll()' in security
assert 'Astro School 4' in readme
print('OK: contenido de ejemplo en profesores, estudiantes, notas, tarea, clase, noticia, cursos, robot, codigo y 3 proyectos.')
print('OK: 3 imagenes locales, galeria con vista previa, modo demo H2 y galeria publica.')
