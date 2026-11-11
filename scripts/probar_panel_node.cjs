/* Prueba de humo del panel sin dependencias, ejecutable con Node.js 20+. */
const fs = require('node:fs');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const path = require('node:path');
const app = {innerHTML:''};
const toast = {textContent:'',className:'toast',hidden:true};
const store = new Map();
const requests = [];
const cuentas = [
  {identificacion:'9001001',rol:'PROFESOR',nombre:'Ana Docente (demo)',materiaOGrupo:'Matemáticas'},
  {identificacion:'9001002',rol:'PROFESOR',nombre:'Carlos Docente (demo)',materiaOGrupo:'Ciencias'},
  {identificacion:'9002001',rol:'ESTUDIANTE',nombre:'Laura Estudiante (demo)',materiaOGrupo:'11-2'},
  {identificacion:'9002002',rol:'ESTUDIANTE',nombre:'Mateo Estudiante (demo)',materiaOGrupo:'11-2'}
];
let currentRol = null;
function response(body) {
  return {ok:true,status:200,headers:{get:()=> 'application/json'},json:async()=>body,text:async()=>JSON.stringify(body)};
}
async function fetchApi(route,opts={}) {
  requests.push([route,opts]);
  if(route==='/api/demo/cuentas')return response(cuentas);
  if(route==='/api/demo/entrar') {
    const body=JSON.parse(opts.body);
    const cuenta=cuentas.find(x=>x.identificacion===body.identificacion);
    assert.ok(cuenta);
    currentRol=cuenta.rol;
    return response({token:'TOKEN_FICTICIO',rol:cuenta.rol,usuarioId:1,nombre:cuenta.nombre,
      expiraEn:'2099-01-01T00:00:00Z',mensaje:'Sesión de demostración iniciada.'});
  }
  if(route==='/api/auth/login'){
    const body=JSON.parse(opts.body);
    assert.equal(body.usuario,'9001002');
    currentRol='PROFESOR';
    return response({token:'TOKEN_FICTICIO_2',rol:'PROFESOR',usuarioId:2,nombre:'Carlos Docente (demo)',
      expiraEn:'2099-01-01T00:00:00Z',mensaje:'Sesión iniciada correctamente como profesor.'});
  }
  if(route==='/api/profesores/yo')return response({nombre:'Ana Docente (demo)',materia:'Matemáticas',identificacion:'9001001'});
  if(route==='/api/estudiantes/yo')return response({nombre:'Laura Estudiante (demo)',grupo:'11-2',identificacion:'9002001'});
  if(route.startsWith('/api/profesores/estudiantes'))return response([{id:7,identificacion:'9002001',nombre:'Laura Estudiante (demo)',grupo:'11-2'}]);
  if(route==='/api/estudiantes/qr')return response({contenido:'PRUEBA',imagenBase64:'iVBORw0KGgo=',mimeType:'image/png',expiraEn:'2099-01-01T00:00:00Z'});
  if(route==='/api/notas/estudiante/7')return response([]);
  throw new Error(`Ruta no simulada: ${route}`);
}
const FakeForm = class {
  constructor(id,fields){this.id=id;this.fields=fields;this.isConnected=true;this.submitButton={dataset:{},disabled:false,textContent:'Iniciar sesión'};}
  reportValidity(){return true;}
  querySelector(){return this.submitButton;}
};
const ctx = vm.createContext({
  document:{getElementById:id=>id==='app'?app:id==='toast'?toast:null,addEventListener:()=>{}},
  sessionStorage:{getItem:k=>store.get(k)||null,setItem:(k,v)=>store.set(k,v),removeItem:k=>store.delete(k)},
  fetch:fetchApi,console,setTimeout,clearTimeout,Date,URL,window:{},navigator:{},
  HTMLFormElement:FakeForm,FormData:class {constructor(form){this.fields=form.fields;}get(k){return this.fields[k]??null;}},
});
const script = fs.readFileSync(path.join(__dirname,'../src/main/resources/static/app.js'),'utf8');
(async()=>{
  vm.runInContext(script,ctx);
  await new Promise(resolve=>setImmediate(resolve));
  assert.match(app.innerHTML,/Probar sin registrarme/);
  assert.match(app.innerHTML,/9001001/);
  assert.match(app.innerHTML,/Correo o número de identificación/);

  const click = vm.runInContext('handleClick',ctx);
  await click({target:{closest:()=>({dataset:{demoId:'9001001'},hasAttribute:k=>k==='data-demo-id',disabled:false})}});
  assert.match(app.innerHTML,/Modo de prueba activo/);
  assert.match(app.innerHTML,/Sesión de demostración iniciada/);

  await vm.runInContext('navigate("notas")',ctx);
  assert.ok(requests.some(([url])=>url.includes('/api/profesores/estudiantes')),'el profesor debe ver sus alumnos automáticamente');

  await vm.runInContext('clearSession(); loadDemoAccounts()',ctx);
  const form = new FakeForm('login-form',{usuario:'9001002',password:'Demo2026!'});
  await vm.runInContext('submitForm',ctx)({target:form,preventDefault:()=>{}});
  assert.match(app.innerHTML,/Sesión iniciada correctamente como profesor/);
  assert.match(app.innerHTML,/Sesión iniciada/);
  console.log('OK: cuenta demo, acceso con documento, avisos y carga de estudiantes.');
})().catch(err=>{console.error(err);process.exitCode=1;});
