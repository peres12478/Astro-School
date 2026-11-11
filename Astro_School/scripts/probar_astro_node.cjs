/* Test de interacciones frontend Astro School sin dependencias externas. */
'use strict';
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const app = {innerHTML:''};
const toast = {hidden:true,textContent:'',className:''};
const listeners = {};
const requests = [];
const storage = new Map();
let currentEnrollment = null;
const courses = [
  {codigo:'ROBOTICA',nombre:'Robotica',descripcion:'Robots y sensores',inscritos:0},
  {codigo:'FISICA',nombre:'Fisica',descripcion:'Experimentos',inscritos:1},
  {codigo:'ASTRONOMIA',nombre:'Astronomia',descripcion:'Estrellas',inscritos:2}
];
const stories = [{id:4,titulo:'Feria de ciencias',descripcion:'Presentaciones del campus',
  autor:'Comunidad',creadaEn:'2026-11-10T15:00:00Z',nombreArchivo:'boletin.pdf',
  urlArchivo:'/api/noticias/4/archivo'}];
function response(body,status=200){
  return {ok:status>=200&&status<300,status,headers:{get:()=> 'application/json'},
    json:async()=>body,text:async()=>JSON.stringify(body)};
}
async function fetchMock(url,opts={}) {
  requests.push([url,opts]);
  if(url==='/api/demo/cuentas')return response([]);
  if(url==='/api/noticias')return response(stories);
  if(url==='/api/cursos')return response(courses);
  if(url==='/api/cursos/mi-inscripcion' && (!opts.method || opts.method==='GET'))
    return response(currentEnrollment,currentEnrollment ? 200 : 204);
  if(url==='/api/cursos/inscripciones' && opts.method==='POST') {
    if(currentEnrollment)return response({mensaje:'Solo puedes tener un curso'},409);
    const course = JSON.parse(opts.body).curso;
    assert.ok(['ROBOTICA','FISICA','ASTRONOMIA'].includes(course));
    currentEnrollment = {curso:course,nombre:courses.find(x=>x.codigo===course).nombre};
    return response(currentEnrollment,201);
  }
  if(url==='/api/cursos/mi-inscripcion' && opts.method==='DELETE') {
    currentEnrollment=null;return response(null,204);
  }
  throw new Error('Unexpected route: '+url);
}
const context = vm.createContext({
  document:{getElementById:id=>id==='app'?app:id==='toast'?toast:null,
    addEventListener:(name,cb)=>{listeners[name]=cb;},querySelectorAll:()=>[]},
  window:{},setTimeout:()=>1,clearTimeout:()=>{},
  sessionStorage:{getItem:key=>storage.get(key)||null,setItem:(key,val)=>storage.set(key,val),removeItem:key=>storage.delete(key)},
  fetch:fetchMock,confirm:()=>true,
  URL,FormData,Date,Number,String,Math,JSON,Object,Array,
  navigator:{clipboard:{writeText:async()=>{}}},
  HTMLFormElement:class {},console
});
const source = fs.readFileSync(path.join(__dirname,'../src/main/resources/static/app.js'),'utf8');
vm.runInContext(source,context);
async function main(){
  await new Promise(resolve=>setImmediate(resolve));
  vm.runInContext("state.publicPage='noticias'; render()",context);
  assert.match(app.innerHTML,/Lo que pasa en nuestra órbita/);
  assert.match(app.innerHTML,/Feria de ciencias/);
  assert.match(app.innerHTML,/boletin.pdf/);
  assert.match(app.innerHTML,/Swagger \/ API/);

  vm.runInContext("state.session={rol:'ESTUDIANTE',token:'DEMO',nombre:'Estudiante Astro',expiraEn:'2099-01-01T00:00:00Z'};state.page='cursos'",context);
  await vm.runInContext('loadPage()',context);
  assert.match(app.innerHTML,/Elige tu próxima misión/);
  assert.match(app.innerHTML,/ROBOTICA|Robotica/);
  assert.match(app.innerHTML,/Física|Fisica/);
  assert.match(app.innerHTML,/Inscribirme/);
  function click(attribute,value) {
    const attrs = new Set([attribute]);
    const dataset = attribute==='data-join-course' ? {joinCourse:value} : {};
    return listeners.click({target:{closest:()=>({dataset,hasAttribute:key=>attrs.has(key)})}});
  }
  await click('data-join-course','ROBOTICA');
  assert.equal(currentEnrollment.curso,'ROBOTICA');
  assert.match(app.innerHTML,/Ya tienes un curso/);
  assert.match(app.innerHTML,/Cancelar mi inscripción/);
  await click('data-drop-course');
  assert.equal(currentEnrollment,null);
  assert.match(app.innerHTML,/Inscribirme/);
  assert.ok(requests.some(([url])=>url==='/api/noticias'));
  assert.ok(requests.some(([url,o])=>url==='/api/cursos/inscripciones' && o.method==='POST'));
  console.log('OK Astro School: noticias publicas, Swagger, catalogo, inscripcion unica y cancelacion (UI simulada).');
}
main().catch(err=>{console.error(err);process.exitCode=1;});
