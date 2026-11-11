"""Revision estatica auxiliar de los componentes y rutas REST.

NO reemplaza `mvn clean verify`: detecta duplicados visibles en anotaciones,
comprueba el XML de Maven y la existencia de archivos de configuracion.
Se ejecuta con Python 3 sin dependencias externas.
"""
from pathlib import Path
from collections import defaultdict
import re
import sys
import xml.etree.ElementTree as ET

raiz = Path(__file__).resolve().parent.parent
codigo = raiz / 'src' / 'main' / 'java'
archivos = list(codigo.rglob('*.java'))
errores = []

ET.parse(raiz / 'pom.xml')
print('pom.xml: XML valido')

componentes = defaultdict(list)
rutas = defaultdict(list)
for archivo in archivos:
    texto = archivo.read_text(encoding='utf-8')
    es_rest = bool(re.search(r'@RestController\b', texto))
    if re.search(r'@(RestController|RestControllerAdvice|Service|Component|Configuration)\b', texto):
        nombre = re.search(r'\bpublic\s+(?:final\s+)?class\s+(\w+)', texto)
        if nombre:
            componentes[nombre.group(1)[0].lower()+nombre.group(1)[1:]].append(str(archivo.relative_to(codigo)))
    if not es_rest:
        continue
    base_match = re.search(r'@RequestMapping\(\s*"([^"]+)"', texto)
    base = base_match.group(1) if base_match else ''
    for metodo, argumentos in re.findall(r'@(Get|Post|Put|Patch|Delete)Mapping\b(?:\s*\((.*?)\))?', texto, re.DOTALL):
        partes = re.findall(r'"(/[^"]*)"', argumentos)
        if not partes:
            partes = ['']
        for parte in partes:
            ruta = (base.rstrip('/') + ('/' + parte.lstrip('/') if parte else '')) if base else parte
            if not ruta.startswith('/'):
                ruta = '/' + ruta
            rutas[(metodo.upper(), ruta)].append(str(archivo.relative_to(codigo)))

for nombre, src in componentes.items():
    if len(src)>1:
        errores.append(f'Componentes duplicados con nombre {nombre}: {src}')
for (metodo, ruta), src in rutas.items():
    if len(src)>1:
        errores.append(f'Ruta REST duplicada {metodo} {ruta}: {src}')

esenciales = [
    ('POST', '/api/auth/login'),
    ('POST', '/api/auth/registro/profesor'),
    ('POST', '/api/auth/registro/profesores'),
    ('POST', '/api/notas'),
    ('POST', '/api/profesores/notas'),
    ('POST', '/api/profesores/qr/escanear'),
    ('POST', '/api/profesores/qr/calificar'),
    ('GET', '/api/estudiantes/qr'),
    ('GET', '/api/estudiantes/me/qr'),
    ('GET', '/api/estudiantes/me/notas'),
    ('GET', '/api/estudiantes/notas/resumen'),
    ('GET', '/api/estudiantes/clases/activas'),
    ('GET', '/api/profesores/grupos/{grupo}/estudiantes'),
]
for esencial in esenciales:
    if esencial not in rutas:
        errores.append(f'Ruta requerida ausente: {esencial}')

print(f'Archivos Java: {len(archivos)}')
print(f'Componentes con anotaciones detectados: {len(componentes)}')
print(f'Rutas REST detectadas: {len(rutas)}')
if errores:
    for error in errores:
        print('ERROR:', error)
    sys.exit(1)
print('Sin duplicados de nombres de componente ni rutas REST en el codigo fuente.')
print('Para validar arranque real, ejecutar: mvn clean verify')
