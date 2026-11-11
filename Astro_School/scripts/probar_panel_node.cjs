/* Smoke tests for the Spanish school panel. No external packages required. */
const fs = require('node:fs');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const path = require('node:path');
const app = {innerHTML:''};
const toast = {textContent:'',className:'toast',hidden:true};
const switchDialog = {innerHTML:'',open:false,showModal(){this.open=true;},close(){this.open=false;}};
const store = new Map();
const requests = [];
const copied = [];
const cuentas = [
  {identificacion:'9001001',rol:'PROFESOR',nombre:'Ana Docente (demo)',materiaOGrupo:'Matematicas',numeroLista:null},
  {identificacion:'9001002',rol:'PROFESOR',nombre:'Carlos Docente (demo)',materiaOGrupo:'Ciencias',numeroLista:null},
  {identificacion:'9002001',rol:'ESTUDIANTE',nombre:'Laura Estudiante (demo)',materiaOGrupo:'11-2',numeroLista:101},
  {identificacion:'9002002',rol:'ESTUDIANTE',nombre:'Mateo Estudiante (demo)',materiaOGrupo:'11-2',numeroLista:102}
];
const estudiantes = [
  {id:7,identificacion:'9002001',nombre:'Laura Estudiante (demo)',grupo:'11-2',numeroLista:101},
  {id:8,identificacion:'9002002',nombre:'Mateo Estudiante (demo)',grupo:'11-2',numeroLista:102}
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
    return response({token:'TOKEN_'+body.identificacion,rol:cuenta.rol,usuarioId:1,nombre:cuenta.nombre,
      expiraEn:'2099-01-01T00:00:00Z',mensaje:'Sesion de demostracion iniciada.'});
  }
  if(route==='/api/auth/login') {
    const body=JSON.parse(opts.body);
    assert.equal(body.usuario,'9001002');
    currentRol='PROFESOR';
    return response({token:'TOKEN_PROFESOR',rol:'PROFESOR',usuarioId:2,nombre:'Carlos Docente (demo)',
      expiraEn:'2099-01-01T00:00:00Z',mensaje:'Sesion iniciada correctamente como profesor.'});
  }
  if(route==='/api/profesores/yo')return response({nombre:'Ana Docente (demo)',materia:'Matematicas',identificacion:'9001001'});
  if(route==='/api/estudiantes/yo')return response({nombre:'Laura Estudiante (demo)',grupo:'11-2',identificacion:'9002001',numeroLista:101});
  if(route.startsWith('/api/profesores/estudiantes/numero-lista/')){
    const number=Number(route.slice(route.lastIndexOf('/')+1));
    const student=estudiantes.find(x=>x.numeroLista===number);
    assert.ok(student);
    return response(student);
  }
  if(route.startsWith('/api/profesores/estudiantes?'))return response(estudiantes);
  if(route==='/api/notas'&&opts.method==='POST') {
    const body=JSON.parse(opts.body);
    assert.equal(body.numeroLista,101);
    assert.equal('estudianteId' in body,false);
    return response({id:17,numeroLista:101,valor:4.5,mensaje:'Nota agregada correctamente.'});
  }
  if(route.startsWith('/api/notas/estudiante/'))return response([]);
  if(route==='/api/estudiantes/qr')return response({contenido:'QR_DE_PRUEBA',imagenBase64:'iVBORw0KGgo=',mimeType:'image/png',expiraEn:'2099-01-01T00:00:00Z'});
  if(route==='/api/estudiantes/notas' || route==='/api/estudiantes/notas/resumen')return response([]);
  throw new Error(`Ruta no simulada: ${route}`);
}
const FakeForm = class {
  constructor(id,fields){this.id=id;this.fields=fields;this.isConnected=true;this.submitButton={dataset:{},disabled:false,textContent:'Enviar'};}
  reportValidity(){return true;}
  querySelector(){return this.submitButton;}
};
const documentMock = {
  getElementById:id=>id==='app'?app:id==='toast'?toast:id==='account-switch-dialog'?switchDialog:null,
  addEventListener:()=>{},
};
const ctx = vm.createContext({
  document:documentMock,
  sessionStorage:{getItem:k=>store.get(k)||null,setItem:(k,v)=>store.set(k,v),removeItem:k=>store.delete(k)},
  fetch:fetchApi,console,setTimeout,clearTimeout,Date,URL,window:{},
  navigator:{clipboard:{writeText:async text=>copied.push(text)}},
  HTMLFormElement:FakeForm,
  FormData:class {constructor(form){this.fields=form.fields;}get(k){return this.fields[k]??null;}},
});
const script = fs.readFileSync(path.join(__dirname,'../src/main/resources/static/app.js'),'utf8');
const click = (name, prop, value) => ({target:{closest:()=>({
  dataset:{[prop]:value},hasAttribute:key=>key===name,disabled:false
})}});
(async()=>{
  vm.runInContext(script,ctx);
  await new Promise(resolve=>setImmediate(resolve));
  assert.match(app.innerHTML,/Probar sin registrarme/);
  assert.match(app.innerHTML,/9001001/);
  assert.match(app.innerHTML,/Correo o n/u);
  assert.match(app.innerHTML,/login-role/);
  assert.match(app.innerHTML,/register-list|auth-tabs/);

  const handleClick = vm.runInContext('handleClick',ctx);
  const submitForm = vm.runInContext('submitForm',ctx);
  await handleClick(click('data-demo-id','demoId','9001001'));
  assert.match(app.innerHTML,/Token activo: Profesor/);
  assert.match(app.innerHTML,/data-switch-role="ESTUDIANTE"/);
  assert.match(app.innerHTML,/Copiar token/);

  await handleClick(click('data-copy-token','copyToken',''));
  assert.equal(copied.at(-1),'TOKEN_9001001');
  await vm.runInContext('navigate("notas")',ctx);
  assert.match(app.innerHTML,/N.&ordm;|mero de lista/u);
  const formGrade = new FakeForm('add-grade-form',{numeroLista:'101',valor:'4.5',observacion:'Prueba'});
  await submitForm({target:formGrade,preventDefault:()=>{}});
  assert.ok(requests.some(([url,opt])=>url==='/api/notas'&&JSON.parse(opt.body||'{}').numeroLista===101));
  assert.match(app.innerHTML,/Nota agregada correctamente/);

  await handleClick(click('data-switch-role','switchRole','ESTUDIANTE'));
  assert.equal(switchDialog.open,true);
  assert.match(switchDialog.innerHTML,/Elegir estudiante/);
  assert.match(switchDialog.innerHTML,/Laura Estudiante/);
  assert.match(switchDialog.innerHTML,/102/);
  await handleClick(click('data-demo-id','demoId','9002001'));
  assert.match(app.innerHTML,/Token activo: Estudiante/);
  assert.match(app.innerHTML,/Laura Estudiante/);
  await vm.runInContext('navigate("qr")',ctx);
  assert.match(app.innerHTML,/data:image\/png;base64/);

  await handleClick(click('data-switch-role','switchRole','PROFESOR'));
  assert.match(switchDialog.innerHTML,/Elegir profesor/);
  await handleClick(click('data-login-as','loginAs','PROFESOR'));
  assert.match(app.innerHTML,/login-form/);
  assert.match(app.innerHTML,/option value="PROFESOR" selected/);
  assert.match(app.innerHTML,/Iniciar sesi/u);
  const formLogin = new FakeForm('login-form',{usuario:'9001002',password:'Demo2026!',rol:'PROFESOR'});
  await submitForm({target:formLogin,preventDefault:()=>{}});
  assert.match(app.innerHTML,/Token activo: Profesor/);
  assert.match(app.innerHTML,/Sesion iniciada correctamente/);
  console.log('OK: login, role switch, demo tokens, grade by unique list number and QR view.');
  clearTimeout(vm.runInContext('toastTimer',ctx));
})().catch(err=>{console.error(err);process.exitCode=1;});
