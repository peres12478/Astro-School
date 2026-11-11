/* Pruebas del frontend Astro School 3.0 sin dependencias externas. */
'use strict';
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const app = {innerHTML: ''};
const toast = {textContent: '', hidden: true, className: ''};
const listeners = {};
const history = [];
let enrolled = 'ROBOTICA';
const creations = [
  {id:1,curso:'ROBOTICA',cursoNombre:'Robótica',titulo:'Seguidor de línea',descripcion:'Con Arduino',
    autor:'Laura',creadaEn:'2026-11-10T15:00:00Z',nombreArchivo:'informe.pdf',urlArchivo:'/api/creaciones/1/archivo'},
  {id:2,curso:'FISICA',cursoNombre:'Física',titulo:'<script>no</script>',descripcion:'Leyes del movimiento',
    autor:'Mateo',creadaEn:'2026-11-10T16:00:00Z',urlArchivo:null},
  {id:3,curso:'ASTRONOMIA',cursoNombre:'Astronomía',titulo:'La Luna',descripcion:'Fotografías',
    autor:'Astrónoma',creadaEn:'2026-11-10T17:00:00Z',urlArchivo:null}
];
function answer(body, status=200) {
  return {ok:status>=200&&status<300,status,headers:{get:()=> 'application/json'},
    json:async()=>body,text:async()=>JSON.stringify(body)};
}
async function fetchMock(url, opts={}) {
  history.push([url,opts]);
  if(url==='/api/demo/cuentas')return answer([]);
  if(url==='/api/noticias')return answer([]);
  if(url==='/api/creaciones')return answer(creations);
  if(url.startsWith('/api/creaciones?curso='))return answer(creations.filter(c => c.curso===url.split('=')[1]));
  if(url==='/api/cursos/mi-inscripcion')return enrolled ? answer({curso:enrolled,nombre:enrolled}) : answer(null,204);
  if(url==='/api/robotica/mi-espacio')return answer({curso:'ROBOTICA',mensaje:'Construye prototipos',misProyectos:[creations[0]]});
  if(url==='/api/fisica/mi-espacio')return answer({curso:'FISICA',mensaje:'Experimentos',misProyectos:[creations[1]]});
  if(url==='/api/astronomia/mi-espacio')return answer({curso:'ASTRONOMIA',mensaje:'Astronomía',misProyectos:[creations[2]]});
  if(url==='/api/robotica/robots')return answer([{id:4,nombre:'AstroBot',plataforma:'Arduino Uno',descripcion:'Sensores'}]);
  if(url==='/api/robotica/robots/4/codigos')return answer([{id:8,nombre:'parpadeo.ino',lenguaje:'ARDUINO',contenido:'void setup() {}',creadoEn:'2026-11-11T00:00:00Z'}]);
  throw Error('Ruta no esperada: '+url);
}
const storage = new Map();
const context = vm.createContext({
  document:{getElementById:id=>id==='app'?app:id==='toast'?toast:null,
    addEventListener:(name,listener)=>listeners[name]=listener,querySelectorAll:()=>[]},
  sessionStorage:{getItem:k=>storage.get(k)||null,setItem:(k,v)=>storage.set(k,v),removeItem:k=>storage.delete(k)},
  fetch:fetchMock, URL, FormData, Date, Number, String, JSON, Object, Array, Math,
  HTMLFormElement:class {},setTimeout:()=>1,clearTimeout:()=>{},console,
  navigator:{clipboard:{writeText:async()=>{}}},confirm:()=>true
});
vm.runInContext(fs.readFileSync(path.join(__dirname,'../src/main/resources/static/app.js'),'utf8'),context);
function check(regex) {assert.match(app.innerHTML,regex);}
async function run() {
  await new Promise(resolve=>setImmediate(resolve));
  vm.runInContext("state.publicPage='creaciones';render()",context);
  check(/Nuestras creaciones/);check(/Seguidor de línea/);check(/La Luna/);check(/Filtrar proyectos por curso/);
  assert.ok(!app.innerHTML.includes('<script>no</script>'), 'No se deben interpretar etiquetas del visitante');
  check(/&lt;script&gt;/);
  await listeners.click({target:{closest:()=>({dataset:{filterCreaciones:'ROBOTICA'},hasAttribute:k=>k==='data-filter-creaciones'})}});
  check(/Seguidor de línea/);
  assert.ok(!app.innerHTML.includes('La Luna'));
  const filtered = history.find(([url])=>url==='/api/creaciones?curso=ROBOTICA');
  assert.ok(filtered);
  vm.runInContext("state.session={rol:'ESTUDIANTE',token:'TOKEN_FICTICIO',nombre:'Laura',expiraEn:'2099-01-01T00:00:00Z'};state.page='mi-curso'",context);
  await vm.runInContext('loadPage()',context);
  check(/Mi espacio de Robótica/);check(/AstroBot/);check(/Estudio de programación/);
  check(/code-form/);check(/code-file-form/);check(/data-code-download="8"/);check(/data-code-open="8"/);
  check(/project-form/);
  assert.ok(history.some(([url,opts])=>url==='/api/robotica/robots'&&String(opts.headers?.Authorization).startsWith('Bearer')));
  enrolled='FISICA';
  await vm.runInContext('loadPage()',context);
  check(/Mi espacio de Física/);check(/project-form/);
  assert.ok(!app.innerHTML.includes('code-form'), 'Física no debe mostrar el editor de Robótica');
  enrolled='ASTRONOMIA';
  await vm.runInContext('loadPage()',context);
  check(/Mi espacio de Astronomía/);
  enrolled=null;
  await vm.runInContext('loadPage()',context);
  check(/Tu laboratorio te espera/);
  console.log('OK Astro School 3: galeria publica, filtros, sanitizacion, 3 espacios, editor y subida de codigo (UI simulada).');
}
run().catch(err=>{console.error(err);process.exitCode=1;});
