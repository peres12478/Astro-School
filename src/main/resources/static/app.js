/* Panel escolar en español. Usa exclusivamente los endpoints de la API existente. */
'use strict';

const STORAGE_KEY = 'colegio_panel_sesion';
const app = document.getElementById('app');
const toast = document.getElementById('toast');
const teacherNav = [
  ['inicio', '⌂', 'Inicio'], ['estudiantes', '♙', 'Estudiantes'],
  ['notas', '✎', 'Calificaciones'], ['trabajos', '▤', 'Tareas'],
  ['clases', '▣', 'Clases virtuales'], ['qr', '▦', 'Código QR']
];
const studentNav = [
  ['inicio', '⌂', 'Inicio'], ['notas', '✎', 'Mis notas'],
  ['trabajos', '▤', 'Tareas'], ['clases', '▣', 'Clases virtuales'], ['qr', '▦', 'Mi QR']
];
const state = {
  session: readSession(), authTab: 'login', page: 'inicio', loading: false,
  group: '11-2', students: [], studentId: null, grades: [],
  teacherWorks: [], studentWorks: [], teacherClasses: [], studentClasses: [], recordings: [],
  studentGrades: [], averages: [], qr: null, scanned: null, profile: null,
  notice: '', noticeIsError: false, demoAccounts: [], demoMode: false
};
let toastTimer;

function readSession() {
  try {
    const parsed = JSON.parse(sessionStorage.getItem(STORAGE_KEY) || 'null');
    if (parsed && typeof parsed.token === 'string' && ['PROFESOR', 'ESTUDIANTE'].includes(parsed.rol)) {
      if (parsed.expiraEn && Date.parse(parsed.expiraEn) <= Date.now()) return null;
      return parsed;
    }
  } catch (_) { /* sesión inválida */ }
  return null;
}
function escapeHtml(value) {
  return String(value ?? '').replace(/[&<>"']/g, ch => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
  }[ch]));
}
function text(value, fallback = '—') { return value === null || value === undefined || value === '' ? fallback : escapeHtml(value); }
function formatDate(iso) {
  if (!iso) return 'Sin fecha';
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return 'Fecha no disponible';
  return date.toLocaleString('es-CO', {dateStyle: 'medium', timeStyle: 'short'});
}
function grade(value) { return Number(value).toLocaleString('es-CO', {minimumFractionDigits: 1, maximumFractionDigits: 2}); }
function id(value) { return String(Number(value)); }
function safeUrl(value) {
  try {
    const url = new URL(String(value));
    return url.protocol === 'https:' ? escapeHtml(url.href) : null;
  } catch (_) { return null; }
}
function statusPill(value, kind = 'class') {
  const classes = {
    'ACTIVA': ['Activa', 'good'], 'FINALIZADA': ['Finalizada', 'pending'],
    'ENVIADA': ['Enviado al servidor de correo', 'good'], 'PARCIAL': ['Envío parcial', 'pending'],
    'FALLIDA': ['Envío fallido', 'bad'], 'PENDIENTE': ['Envío pendiente', 'pending'],
    'SIN_DESTINATARIOS': ['Sin destinatarios', 'pending']
  };
  const [label, type] = classes[value] || [value || (kind === 'mail' ? 'Sin información' : 'Sin estado'), 'pending'];
  return `<span class="pill pill-${type}">${escapeHtml(label)}</span>`;
}
function showToast(message, error = false) {
  if (toastTimer) clearTimeout(toastTimer);
  toast.textContent = (error ? 'Atención: ' : '✓ ') + message;
  toast.className = 'toast' + (error ? ' error' : '');
  toast.hidden = false;
  toastTimer = setTimeout(() => { toast.hidden = true; }, 6500);
}
function notify(message, error = false) {
  state.notice = message;
  state.noticeIsError = error;
  showToast(message, error);
}
function formValue(form, name) { return String(new FormData(form).get(name) ?? '').trim(); }
function makeBody(obj) { return JSON.stringify(obj); }
async function api(path, {method = 'GET', body, headers = {}} = {}) {
  let response;
  try {
    response = await fetch(path, {
      method,
      headers: {
        ...(body === undefined ? {} : {'Content-Type': 'application/json'}),
        ...(state.session?.token ? {'Authorization': `Bearer ${state.session.token}`} : {}),
        ...headers
      },
      ...(body === undefined ? {} : {body: makeBody(body)})
    });
  } catch (_) {
    throw new Error('No se pudo conectar con el servidor. Verifica que Spring Boot siga abierto.');
  }
  const contentType = response.headers.get('content-type') || '';
  const content = contentType.includes('json') ? await response.json().catch(() => null) : await response.text().catch(() => '');
  if (!response.ok) {
    if (response.status === 401 && !path.startsWith('/api/auth/')) {
      clearSession();
      render();
      loadDemoAccounts();
      showToast('Tu sesión venció. Puedes iniciar sesión o entrar con una cuenta de prueba.', true);
      throw new Error('Tu sesión venció. Inicia sesión de nuevo.');
    }
    const generic = response.status === 403 ? 'No tienes permiso para realizar esta acción.' :
      response.status === 404 ? 'No se encontró el registro solicitado.' :
      `La operación no se pudo completar (código ${response.status}).`;
    throw new Error(typeof content === 'object' && content?.mensaje ? content.mensaje : generic);
  }
  return content;
}
function saveSession(auth) {
  state.session = {
    token: auth.token, rol: auth.rol, usuarioId: auth.usuarioId,
    nombre: auth.nombre, expiraEn: auth.expiraEn, demo: state.demoAccounts.some(c => c.identificacion === state.pendingDemoId)
  };
  sessionStorage.setItem(STORAGE_KEY, JSON.stringify(state.session));
  state.page = 'inicio';
  state.profile = null;
  state.students = [];
  state.studentId = null;
  state.notice = '';
  state.pendingDemoId = null;
}
function clearSession() {
  sessionStorage.removeItem(STORAGE_KEY);
  state.session = null;
  state.page = 'inicio';
  state.profile = null;
  state.students = [];
  state.grades = [];
  state.studentId = null;
  state.qr = null;
  state.scanned = null;
  state.scannedContent = null;
  state.notice = '';
}
function title(kicker, heading, description, right = '') {
  return `<header class="pagehead"><div><div class="eyebrow">${escapeHtml(kicker)}</div><h1>${escapeHtml(heading)}</h1><p>${escapeHtml(description)}</p></div>${right}</header>`;
}
function messageBanner() {
  if (!state.notice) return '';
  return `<div class="success-banner" style="${state.noticeIsError ? 'background:#fff1ef;border-color:#f5c1bd;color:#933d38' : ''}">
    <span aria-hidden="true">${state.noticeIsError ? '!' : '✓'}</span> ${escapeHtml(state.notice)}</div>`;
}
function emptyState(textContent) { return `<div class="empty">${escapeHtml(textContent)}</div>`; }
function navButton(page, textContent, cls = 'btn-primary') {
  return `<button class="btn ${cls}" data-nav="${page}" type="button">${escapeHtml(textContent)}</button>`;
}
function renderDemoAccounts() {
  if (!state.demoAccounts.length) return '';
  return `<section class="demo-access" aria-label="Cuentas de prueba">
    <div class="demo-heading"><span class="demo-dot"></span><strong>Probar sin registrarme</strong></div>
    <p>Elige una cuenta ficticia. La aplicación iniciará una sesión de prueba automáticamente y podrás usar todas las herramientas de ese rol.</p>
    <div class="demo-grid">${state.demoAccounts.map(c => `
      <button class="demo-account" type="button" data-demo-id="${escapeHtml(c.identificacion)}">
        <span class="demo-role">${c.rol === 'PROFESOR' ? 'Profesor/a' : 'Estudiante'} · ${escapeHtml(c.materiaOGrupo)}</span>
        <strong>${escapeHtml(c.nombre)}</strong><span class="demo-id">Documento de prueba: ${escapeHtml(c.identificacion)}</span>
        <span class="demo-link">Entrar a esta cuenta →</span>
      </button>`).join('')}</div>
    <small>Datos ficticios de demostración. No utilices cuentas de prueba con información escolar real.</small>
  </section>`;
}
async function loadDemoAccounts() {
  try {
    const result = await api('/api/demo/cuentas');
    state.demoAccounts = Array.isArray(result) ? result : [];
  } catch (_) {
    state.demoAccounts = []; // Cuando el modo de prueba está apagado, solo hay acceso normal.
  }
  if (!state.session) render();
}
function renderAuth() {
  const tab = state.authTab;
  const tabs = [ ['login', 'Iniciar sesión'], ['estudiante', 'Registrar estudiante'], ['profesor', 'Registrar docente'] ];
  const descriptions = {
    login: ['Bienvenido al colegio', 'Entra con tu correo o número de documento y tu contraseña, o prueba con una cuenta de ejemplo.'],
    estudiante: ['Crear cuenta estudiantil', 'Regístrate con tu información personal y tu grupo.'],
    profesor: ['Crear cuenta docente', 'Necesitas la clave especial de registro docente.'],
    olvide: ['Recuperar contraseña', 'Te enviaremos instrucciones si el correo está registrado y el envío está configurado.'],
    restablecer: ['Establecer nueva contraseña', 'Pega el token de recuperación recibido por correo.']
  };
  const [heading, subtitle] = descriptions[tab] || descriptions.login;
  let form;
  if (tab === 'login') {
    form = `<form id="login-form">
      <div class="field"><label for="login-identifier">Correo o número de identificación</label><input id="login-identifier" type="text" autocomplete="username" name="usuario" placeholder="Ej.: 9001001 o nombre@colegio.edu" required><small>Puedes escribir la cédula, tarjeta de identidad o correo registrado. No necesitas el ID interno de la tabla.</small></div>
      <div class="field"><label for="login-pass">Contraseña</label><input id="login-pass" type="password" autocomplete="current-password" name="password" required></div>
      <button class="btn btn-primary btn-full" type="submit">Iniciar sesión</button>
      <p style="text-align:center"><button class="link-button" type="button" data-auth="olvide">¿Olvidaste la contraseña?</button></p>
      ${renderDemoAccounts()}
    </form>`;
  } else if (tab === 'profesor' || tab === 'estudiante') {
    const teacher = tab === 'profesor';
    form = `<form id="register-${tab}-form">
      <div class="field"><label for="register-name">Nombre completo</label><input id="register-name" name="nombre" required maxlength="120" autocomplete="name" placeholder="Tu nombre completo"></div>
      <div class="fields-row" style="margin-bottom:16px">
        <div class="field"><label for="register-id">Número de documento</label><input id="register-id" name="identificacion" required maxlength="30" placeholder="Cédula o tarjeta"></div>
        <div class="field"><label for="register-group">${teacher ? 'Materia que enseñas' : 'Salón o curso'}</label><input id="register-group" name="${teacher ? 'materia' : 'grupo'}" required maxlength="${teacher ? '100' : '30'}" placeholder="${teacher ? 'Matemáticas' : '11-2'}"></div>
      </div>
      <div class="field"><label for="register-email">Correo electrónico</label><input id="register-email" name="correo" type="email" autocomplete="email" required maxlength="180" placeholder="nombre@colegio.edu"></div>
      <div class="field"><label for="register-pass">Contraseña personal</label><input id="register-pass" name="password" type="password" autocomplete="new-password" minlength="8" maxlength="72" required><small>Al menos 8 caracteres.</small></div>
      ${teacher ? `<div class="field"><label for="teacher-key">Clave de alta de docentes</label><input id="teacher-key" name="claveDocente" type="password" autocomplete="off" required><small>La entrega el administrador; no es tu contraseña personal.</small></div>` : ''}
      <button class="btn btn-primary btn-full" type="submit">Crear mi cuenta de ${teacher ? 'profesor' : 'estudiante'}</button>
    </form>`;
  } else if (tab === 'olvide') {
    form = `<form id="recover-form"><div class="field"><label for="recover-email">Correo de tu cuenta</label><input id="recover-email" name="correo" type="email" required placeholder="nombre@colegio.edu"></div>
      <button class="btn btn-primary btn-full" type="submit">Solicitar recuperación</button></form>
      <div class="note warning">El correo solo llegará si el servidor SMTP está configurado. La confirmación de la solicitud no significa que se entregó el mensaje.</div>
      <p><button type="button" class="link-button" data-auth="restablecer">Ya tengo un token de recuperación</button></p>`;
  } else {
    form = `<form id="reset-form"><div class="field"><label for="recover-token">Token recibido por correo</label><input id="recover-token" name="token" required autocomplete="off"></div>
      <div class="field"><label for="recover-new">Contraseña nueva</label><input id="recover-new" name="nuevaPassword" type="password" minlength="8" maxlength="72" autocomplete="new-password" required></div>
      <button class="btn btn-primary btn-full" type="submit">Cambiar contraseña</button></form>`;
  }
  app.innerHTML = `<div class="auth-layout">
    <section class="auth-brand"><div class="brand-mark"><span class="brand-symbol">C</span><span>COLEGIO <span style="color:#53ceb9">.</span></span></div>
      <div><div class="eyebrow" style="color:#67ddc7">Un espacio para aprender</div>
        <h1>Tu colegio, <span>más sencillo.</span></h1>
        <p>Consulta calificaciones, crea tareas y organiza las clases desde un lugar pensado para profesores y estudiantes.</p></div>
      <div class="brand-foot">Sistema escolar · Versión para uso local</div></section>
    <section class="auth-form-wrap"><div class="eyebrow">Acceso escolar</div><h2>${escapeHtml(heading)}</h2><p class="subtext">${escapeHtml(subtitle)}</p>
      <div class="auth-tabs" role="group" aria-label="Elegir el tipo de acceso">${tabs.map(([v, name]) =>
        `<button type="button" class="${tab === v ? 'active' : ''}" data-auth="${v}" aria-pressed="${tab === v}">${escapeHtml(name)}</button>`).join('')}</div>
      ${messageBanner()}${form}<div class="auth-foot">${['olvide', 'restablecer'].includes(tab) ? '<button class="link-button" type="button" data-auth="login">Volver a iniciar sesión</button>' : 'Tus datos se guardan en la base de datos del colegio.'}</div>
    </section></div>`;
}
function render() {
  if (!state.session) return renderAuth();
  const teacher = state.session.rol === 'PROFESOR';
  const nav = teacher ? teacherNav : studentNav;
  const name = state.session.nombre || (teacher ? 'Docente' : 'Estudiante');
  const pageInfo = nav.find(item => item[0] === state.page) || nav[0];
  app.innerHTML = `<div class="layout">
    <aside class="sidebar"><div class="brand-mark"><span class="brand-symbol">C</span> COLEGIO <span style="color:#57d7c3">.</span></div>
      <nav aria-label="Secciones del colegio">${nav.map(([key, symbol, label]) =>
        `<button class="nav-item ${state.page === key ? 'active' : ''}" data-nav="${key}" type="button" aria-current="${state.page === key ? 'page' : 'false'}"><span class="nav-icon" aria-hidden="true">${symbol}</span>${escapeHtml(label)}</button>`).join('')}</nav>
      <div class="sidebar-foot">¿Necesitas revisar los detalles técnicos?<br><a href="/swagger-ui.html" target="_blank" rel="noopener noreferrer">Abrir Swagger (avanzado)</a></div>
    </aside><div class="main"><header class="topbar"><div class="top-label">${escapeHtml(pageInfo[2])}</div>
      <div class="user-info"><span class="session-pill"><span class="session-dot"></span>${state.session.demo ? 'Modo de prueba activo' : 'Sesión iniciada'}</span><span class="user-avatar">${escapeHtml(name.slice(0,1).toUpperCase())}</span><div><div class="user-name">${escapeHtml(name)}</div><div class="user-role">${teacher ? 'Profesor/a' : 'Estudiante'}</div></div>
      <button class="btn btn-outline btn-small" type="button" data-logout>Salir</button></div></header>
      <main class="content">${messageBanner()}${state.loading ? '<div class="panel">Cargando información...</div>' : buildPage(teacher)}</main></div></div>
    <dialog class="dialog" id="edit-note-dialog" aria-labelledby="edit-note-heading"></dialog>
    <dialog class="dialog" id="recording-dialog" aria-labelledby="recording-heading"></dialog>
    <dialog class="dialog qr-camera-dialog" id="camera-dialog" aria-labelledby="camera-heading"></dialog>`;
}
function buildPage(teacher) {
  if (state.page === 'inicio') return renderHome(teacher);
  if (state.page === 'estudiantes' && teacher) return renderStudents();
  if (state.page === 'notas') return teacher ? renderTeacherNotes() : renderStudentNotes();
  if (state.page === 'trabajos') return renderWorks(teacher);
  if (state.page === 'clases') return renderClasses(teacher);
  if (state.page === 'qr') return renderQR(teacher);
  return '<div class="panel">Sección no disponible.</div>';
}
function renderHome(teacher) {
  const isTeacher = teacher;
  const profile = state.profile;
  return `${title('Panel principal', `Hola, ${state.session.nombre?.split(' ')[0] || 'bienvenido'}`, 'Tu sesión está iniciada. Elige una de las acciones de abajo para empezar.')}
    <div class="dashboard-intro"><div><div class="eyebrow">${isTeacher ? 'Espacio docente' : 'Espacio estudiantil'}</div>
      <h2>${isTeacher ? 'Enseñar y calificar, sin complicaciones.' : 'Todo lo que necesitas para tus clases.'}</h2>
      <p>${isTeacher ? 'Busca estudiantes por salón, registra sus notas y lleva el control de tareas y reuniones.' : 'Revisa tus notas, tareas, clases virtuales y tu código QR desde aquí.'}</p></div><span class="large-emoji" aria-hidden="true">${isTeacher ? '✎' : '▤'}</span></div>
    <div class="quick-grid">
      <button class="quick-card" type="button" data-nav="notas"><span class="qicon">✎</span><strong>${isTeacher ? 'Agregar o editar notas' : 'Mis calificaciones'}</strong><span>${isTeacher ? 'Sin escribir los ID de las notas manualmente.' : 'Promedio por materia y estado de aprobación.'}</span></button>
      <button class="quick-card" type="button" data-nav="trabajos"><span class="qicon">▤</span><strong>${isTeacher ? 'Publicar tareas' : 'Ver mis tareas'}</strong><span>${isTeacher ? 'Comparte actividades por grupo.' : 'Trabajos asignados a tu salón.'}</span></button>
      <button class="quick-card" type="button" data-nav="clases"><span class="qicon">▣</span><strong>${isTeacher ? 'Crear una clase' : 'Entrar a mis clases'}</strong><span>${isTeacher ? 'Consulta el estado de los correos de invitación.' : 'Accede a las reuniones y grabaciones disponibles.'}</span></button>
    </div>
    <div class="panel" style="margin-top:20px"><div class="panel-title"><div><h2>Mi información</h2><p>Datos de tu cuenta</p></div></div>
      ${profile ? `<div class="metric-grid">
        <div class="metric"><div class="label">Nombre</div><div style="margin-top:8px;font-weight:800">${text(profile.nombre)}</div></div>
        <div class="metric"><div class="label">${isTeacher ? 'Materia' : 'Grupo'}</div><div style="margin-top:8px;font-weight:800">${text(isTeacher ? profile.materia : profile.grupo)}</div></div>
        <div class="metric"><div class="label">Documento</div><div style="margin-top:8px;font-weight:800">${text(profile.identificacion)}</div></div></div>` : emptyState('Todavía no se han cargado tus datos.')}
    </div>`;
}
function groupSearchForm(formId) {
  return `<form id="${formId}" class="toolbar"><div class="field"><label for="group-${formId}">Salón o curso</label>
    <input id="group-${formId}" name="grupo" required maxlength="30" placeholder="Ej.: 11-2" value="${escapeHtml(state.group)}">
    <small>Escribe el grupo del cual quieres consultar estudiantes.</small></div>
    <button class="btn btn-primary" type="submit">Buscar estudiantes</button></form>`;
}
function studentsTable() {
  if (!state.students.length) return emptyState('No hay estudiantes cargados de ese grupo. Busca un salón para verlos.');
  return `<div class="field" style="max-width:410px"><label for="buscar-estudiante">Filtrar por nombre o documento</label><input id="buscar-estudiante" type="search" placeholder="Escribe parte del nombre..."></div>
    <div class="table-scroll"><table class="data-table"><thead><tr><th>Estudiante</th><th>Documento</th><th>Salón</th><th>ID interno</th><th>Acciones</th></tr></thead><tbody>
      ${state.students.map(s => `<tr class="student-row"><td><strong>${text(s.nombre)}</strong></td><td>${text(s.identificacion)}</td><td>${text(s.grupo)}</td>
      <td><span class="code-id">#${id(s.id)}</span></td><td><div class="actions">
        <button type="button" class="btn btn-secondary btn-small" data-student-grade="${id(s.id)}">Agregar nota</button>
        <button type="button" class="btn btn-outline btn-small" data-student-view="${id(s.id)}">Ver notas</button>
      </div></td></tr>`).join('')}
      </tbody></table></div>`;
}
function renderStudents() {
  return `${title('Gestión de grupo','Estudiantes','Busca un salón y selecciona al estudiante con el que quieres trabajar.', navButton('notas','Ir a calificaciones','btn-outline'))}
    <section class="panel"><div class="panel-title"><div><h2>Buscar por grupo</h2><p>Escribe el número de salón.</p></div></div>
    ${groupSearchForm('students-search')}</section>
    <section class="panel"><div class="panel-title"><div><h2>Estudiantes ${state.students.length ? `(${state.students.length})` : ''}</h2><p>Puedes seleccionar uno para asignarle o revisar notas.</p></div></div>
    ${studentsTable()}</section><div class="note">El <strong>ID interno</strong> identifica al alumno dentro de la base de datos. No es su número de cédula ni de tarjeta de identidad. Aquí puedes seleccionar al estudiante sin copiar ese número.</div>`;
}
function renderTeacherNotes() {
  const student = state.students.find(s => Number(s.id) === Number(state.studentId));
  const options = state.students.map(s => `<option value="${id(s.id)}" ${Number(s.id) === Number(state.studentId) ? 'selected' : ''}>${text(s.nombre)} · ${text(s.grupo)}</option>`).join('');
  return `${title('Calificaciones','Asignar y modificar notas','Elige al estudiante, escribe la nota y guarda los cambios.', navButton('estudiantes','Buscar estudiantes','btn-outline'))}
    <div class="split"><section class="panel"><h2 class="form-title">Nueva calificación</h2>
      ${state.students.length ? `<form id="add-grade-form">
        <div class="field"><label for="grade-student">Estudiante</label><select id="grade-student" name="estudianteId" required>${options}</select><small>El ID del estudiante se toma automáticamente de esta selección.</small></div>
        <div class="field"><label for="grade-value">Nota (0 a 5)</label><input id="grade-value" type="number" min="0" max="5" step="0.01" name="valor" placeholder="4.5" required></div>
        <div class="field"><label for="grade-obs">Observación (opcional)</label><textarea id="grade-obs" maxlength="500" name="observacion" placeholder="Ej.: Excelente trabajo"></textarea></div>
        <button class="btn btn-primary btn-full" type="submit">Guardar nota</button></form>
      ` : `<p class="panel-sub">Primero carga los estudiantes de un grupo.</p>${groupSearchForm('grade-search')}`}
      <div class="note">La materia de la nota se asigna automáticamente según tu cuenta de profesor.</div>
    </section><section class="panel"><div class="panel-title"><div><h2>Notas registradas</h2><p>${student ? `Estudiante: ${text(student.nombre)}` : 'Elige un estudiante para consultar sus notas.'}</p></div></div>
      ${student && state.grades.length ? `<div class="table-scroll"><table class="data-table"><thead><tr><th>Nota</th><th>Calificación</th><th>Observación</th><th>Acción</th></tr></thead><tbody>
        ${state.grades.map(n => `<tr><td><span class="code-id">#${id(n.id)}</span></td><td><strong>${grade(n.valor)}</strong></td>
          <td>${text(n.observacion)}</td><td><button class="btn btn-outline btn-small" type="button" data-edit-note="${id(n.id)}">Editar esta nota</button></td></tr>`).join('')}
        </tbody></table></div>` : emptyState(student ? 'Este estudiante aún no tiene notas de tu materia. Registra la primera nota con el formulario de la izquierda.' : 'Selecciona un estudiante para consultar sus notas.')}
      <div class="note"><strong>Importante:</strong> cada nota tiene un ID propio. Al pulsar «Editar esta nota», el sistema identifica automáticamente la nota correcta; no necesitas escribir ese ID.</div>
    </section></div>`;
}
function renderStudentNotes() {
  return `${title('Mi rendimiento', 'Mis calificaciones', 'Consulta tus notas, promedios y materias.')}
    <div class="metric-grid" style="margin-bottom:19px">
      <div class="metric"><div class="label">Calificaciones recibidas</div><div class="value">${state.studentGrades.length}</div></div>
      <div class="metric"><div class="label">Materias registradas</div><div class="value">${state.averages.length}</div></div>
      <div class="metric"><div class="label">Materias aprobadas</div><div class="value">${state.averages.filter(x => x.aprobado).length}</div></div>
    </div>
    <section class="panel"><div class="panel-title"><div><h2>Promedios por materia</h2><p>Se calcula el promedio aritmético; apruebas desde 3.0.</p></div></div>
      ${state.averages.length ? `<div class="table-scroll"><table class="data-table"><thead><tr><th>Materia</th><th>Promedio</th><th>N.º notas</th><th>Resultado</th></tr></thead><tbody>
      ${state.averages.map(x => `<tr><td>${text(x.materia)}</td><td class="number">${grade(x.promedio)}</td><td>${id(x.cantidadNotas)}</td><td>${x.aprobado ? '<span class="pill pill-good">Aprobada</span>' : '<span class="pill pill-bad">Por mejorar</span>'}</td></tr>`).join('')}</tbody></table></div>` : emptyState('Todavía no tienes promedios disponibles.')}</section>
    <section class="panel"><div class="panel-title"><div><h2>Todas mis notas</h2><p>Organizadas de la más reciente a la más antigua.</p></div></div>
      ${state.studentGrades.length ? `<div class="table-scroll"><table class="data-table"><thead><tr><th>Materia</th><th>Nota</th><th>Observación</th><th>Registrada</th></tr></thead><tbody>
      ${state.studentGrades.map(n => `<tr><td>${text(n.materia)}</td><td class="number">${grade(n.valor)}</td><td>${text(n.observacion)}</td><td>${formatDate(n.creadaEn)}</td></tr>`).join('')}</tbody></table></div>` : emptyState('Aún no te han asignado notas.')}</section>`;
}
function renderWorks(teacher) {
  const works = teacher ? state.teacherWorks : state.studentWorks;
  return `${title('Actividades', teacher ? 'Trabajos y tareas' : 'Tareas de mi grupo', teacher ? 'Publica actividades para un salón y consulta las que has creado.' : 'Aquí encontrarás las tareas asignadas a tu salón.')}
    ${teacher ? `<section class="panel"><div class="panel-title"><div><h2>Publicar nueva tarea</h2><p>Los estudiantes del grupo indicado podrán consultarla.</p></div></div>
      <form id="work-form"><div class="fields-row"><div class="field"><label for="work-group">Salón o curso</label><input id="work-group" name="grupo" required maxlength="30" value="${escapeHtml(state.group)}" placeholder="11-2"></div>
      <div class="field"><label for="work-name">Título de la tarea</label><input id="work-name" name="titulo" maxlength="160" required placeholder="Ej.: Taller de ecuaciones"></div></div>
      <div class="field" style="margin-top:16px"><label for="work-desc">Instrucciones</label><textarea id="work-desc" name="descripcion" maxlength="4000" required placeholder="Explica qué deben entregar los estudiantes..."></textarea></div>
      <div class="field"><label for="work-date">Fecha límite (opcional)</label><input id="work-date" type="datetime-local" name="fechaEntrega"><small>Deja este campo vacío si la actividad no tiene fecha límite.</small></div>
      <button type="submit" class="btn btn-primary">Publicar tarea</button></form></section>` : ''}
    <section class="panel"><div class="panel-title"><div><h2>${teacher ? 'Mis tareas publicadas' : 'Tareas asignadas'}</h2><p>${works.length} tareas encontradas.</p></div></div>
      ${works.length ? `<div class="list-stack">${works.map(w => `<article class="item-card"><header><div><h3>${text(w.titulo)}</h3>
        <span class="muted" style="font-size:12px">Grupo ${text(w.grupo)} · ${text(w.materia)}</span></div><span class="code-id">#${id(w.id)}</span></header>
        <p>${text(w.descripcion)}</p><div class="mini-meta"><span>${w.fechaEntrega ? `Entrega: ${formatDate(w.fechaEntrega)}` : 'Sin fecha límite'}</span><span>Profesor: ${text(w.profesor)}</span></div></article>`).join('')}</div>` : emptyState(teacher ? 'Todavía no has publicado tareas.' : 'Aún no hay tareas asignadas a tu grupo.')}</section>`;
}
function renderClasses(teacher) {
  const active = teacher ? state.teacherClasses : state.studentClasses;
  const recordings = teacher ? [] : state.recordings;
  return `${title('Clases virtuales', teacher ? 'Mis clases virtuales' : 'Clases y grabaciones', teacher ? 'Crea reuniones y revisa el estado de las invitaciones por correo.' : 'Encuentra los enlaces para conectarte a tus clases.')}
    ${teacher ? `<section class="panel"><div class="panel-title"><div><h2>Crear clase virtual</h2><p>Se genera un enlace de Jitsi y se intenta avisar a los estudiantes por correo.</p></div></div>
      <form id="class-form" class="toolbar"><div class="field"><label for="class-group">Grupo</label><input id="class-group" name="grupo" required maxlength="30" value="${escapeHtml(state.group)}" placeholder="11-2"></div>
      <div class="field"><label for="class-name">Nombre de la clase</label><input id="class-name" name="titulo" required maxlength="160" placeholder="Ej.: Clase de trigonometría"></div>
      <button class="btn btn-primary" type="submit">Crear clase</button></form>
      <div class="note warning">Los correos solo llegarán fuera del computador si configuraste un servidor SMTP real. «Enviada» indica aceptación por el servidor de correo, no lectura por el alumno.</div></section>` : ''}
    <section class="panel"><div class="panel-title"><div><h2>${teacher ? 'Clases creadas' : 'Clases activas'}</h2>${teacher ? '<button class="btn btn-outline btn-small" type="button" data-refresh-classes>Actualizar estado de correos</button>' : ''}</div></div>
      ${active.length ? `<div class="list-stack">${active.map(c => renderClassCard(c, teacher)).join('')}</div>` : emptyState(teacher ? 'Todavía no has creado ninguna clase.' : 'No hay clases activas en este momento.')}</section>
    ${!teacher ? `<section class="panel"><div class="panel-title"><div><h2>Grabaciones disponibles</h2><p>Enlaces compartidos por los profesores.</p></div></div>
      ${recordings.length ? `<div class="list-stack">${recordings.map(c => renderClassCard(c, false)).join('')}</div>` : emptyState('Todavía no hay grabaciones disponibles.')}</section>` : ''}`;
}
function renderClassCard(c, teacher) {
  const link = safeUrl(c.enlace), recordLink = safeUrl(c.urlGrabacion);
  return `<article class="item-card"><header><div><h3>${text(c.titulo)}</h3>
    <div class="mini-meta"><span>Grupo ${text(c.grupo)}</span><span>Creada: ${formatDate(c.iniciadaEn)}</span></div></div><span class="code-id">#${id(c.id)}</span></header>
    <div style="display:flex;gap:6px;flex-wrap:wrap">${statusPill(c.estado)}${teacher ? statusPill(c.estadoNotificacion, 'mail') : ''}</div>
    ${teacher ? `<p>Correos aceptados: <strong>${Number(c.correosEnviados) || 0}</strong> · Fallidos: <strong>${Number(c.correosFallidos) || 0}</strong></p>` : ''}
    <footer>${link ? `<a href="${link}" class="btn btn-secondary btn-small" target="_blank" rel="noopener noreferrer">Abrir reunión</a>` : ''}
      ${recordLink ? `<a href="${recordLink}" class="btn btn-outline btn-small" target="_blank" rel="noopener noreferrer">Ver grabación</a>` : ''}
      ${teacher && c.estado === 'ACTIVA' ? `<button class="btn btn-outline btn-small" type="button" data-class-end="${id(c.id)}">Finalizar clase</button>` : ''}
      ${teacher && c.estado === 'FINALIZADA' ? `<button class="btn btn-outline btn-small" type="button" data-class-record="${id(c.id)}">${c.urlGrabacion ? 'Cambiar grabación' : 'Añadir grabación'}</button>` : ''}
    </footer></article>`;
}
function renderQR(teacher) {
  if (teacher) {
    return `${title('Código QR', 'Identificar y calificar con QR', 'Escanea con la cámara (si está disponible) o pega el código que te entregue el estudiante.')}
      <div class="split"><section class="panel"><h2 class="form-title">Consultar estudiante por QR</h2>
        <form id="scan-qr-form"><div class="field"><label for="qr-content">Texto completo del código QR</label>
          <textarea id="qr-content" name="contenido" required placeholder="Pega aquí el contenido leído del QR..."></textarea>
          <small>El estudiante puede abrir «Mi QR» y pulsar «Copiar contenido». También se aceptan códigos antiguos en JSON.</small></div>
          <div class="qr-actions"><button class="btn btn-primary" type="submit">Comprobar código</button>
          <button class="btn btn-outline" type="button" data-scan-camera>Escanear con cámara</button></div></form>
          <div class="note">Para probar ahora: entra como estudiante de ejemplo, abre «Mi QR», copia el contenido, vuelve como profesor de ejemplo y pégalo aquí.</div>
        </section>
      <section class="panel"><h2 class="form-title">Resultado de la lectura</h2>
        ${state.scanned ? `<p><strong>${text(state.scanned.nombre)}</strong> · Grupo ${text(state.scanned.grupo)}</p>
          <p class="panel-sub">Materia: ${text(state.scanned.materiaProfesor)}</p>
          <form id="qr-grade-form"><div class="field"><label for="qr-grade">Nota (0 a 5)</label>
            <input id="qr-grade" name="valor" type="number" step="0.01" min="0" max="5" required placeholder="4.5"></div>
            <div class="field"><label for="qr-obs">Observación (opcional)</label><textarea id="qr-obs" name="observacion" maxlength="500"></textarea></div>
            <button class="btn btn-primary" type="submit">Guardar nota por QR</button></form>`
        : emptyState('Verifica un QR para identificar a un estudiante y calificarlo.')}</section></div>`;
  }
  return `${title('Mi identificación', 'Mi código QR', 'Muéstraselo al profesor para que pueda identificarte.')}
    <div class="panel"><div class="qr-wrap">
      ${state.qr?.imagenBase64 ? `<img class="qr-img" src="data:image/png;base64,${escapeHtml(state.qr.imagenBase64)}" alt="Código QR personal del estudiante, listo para mostrar al profesor">` : '<div class="empty">Todavía no hay un QR. Pulsa «Generar mi QR» para volver a intentarlo.</div>'}
      <div class="grow"><h2>Tu código personal</h2><p class="subtext">Ya puedes mostrar este código al profesor o descargarlo en PNG. Si no tienes lector, utiliza «Copiar contenido». El código caduca y está firmado.</p>
        ${state.qr ? `<p class="panel-sub">Vence: ${formatDate(state.qr.expiraEn)}</p><div style="display:flex;gap:10px;flex-wrap:wrap">
          <button class="btn btn-primary" type="button" data-download-qr>Descargar QR</button>
          <button class="btn btn-outline" type="button" data-copy-qr>Copiar contenido</button>
          <button class="btn btn-outline" type="button" data-refresh-qr>Generar otro QR</button></div>` : `<button class="btn btn-primary" type="button" data-refresh-qr>Generar mi QR</button>`}
      </div></div></div>`;
}

async function loadPage() {
  if (!state.session) return;
  const teacher = state.session.rol === 'PROFESOR';
  state.loading = true;
  render();
  try {
    if (state.page === 'inicio') {
      state.profile = await api(teacher ? '/api/profesores/yo' : '/api/estudiantes/yo');
    } else if (state.page === 'estudiantes' && teacher) {
      await loadStudents(state.group);
    } else if (state.page === 'notas') {
      if (teacher) {
        if (!state.students.length) await loadStudents(state.group);
        if (state.studentId !== null) await loadStudentGrades();
      }
      if (!teacher) {
        [state.studentGrades, state.averages] = await Promise.all([
          api('/api/estudiantes/notas'), api('/api/estudiantes/notas/resumen')
        ]);
      }
    } else if (state.page === 'trabajos') {
      if (teacher) state.teacherWorks = await api('/api/trabajos/mis');
      else state.studentWorks = await api('/api/estudiantes/trabajos');
    } else if (state.page === 'clases') {
      if (teacher) state.teacherClasses = await api('/api/clases/mis');
      else [state.studentClasses, state.recordings] = await Promise.all([
        api('/api/estudiantes/clases/activas'), api('/api/estudiantes/clases/grabaciones')
      ]);
    } else if (state.page === 'qr' && !teacher) {
      state.qr = await api('/api/estudiantes/qr');
    }
  } catch (err) {
    if (state.session) notify(err.message, true);
  } finally {
    state.loading = false;
    render();
  }
}
async function navigate(page) {
  stopQrCamera();
  if (!state.session) return;
  const allowed = state.session.rol === 'PROFESOR' ? teacherNav : studentNav;
  if (!allowed.some(item => item[0] === page)) return;
  state.page = page;
  state.notice = '';
  await loadPage();
}
async function loadStudents(group) {
  state.group = group.toUpperCase();
  state.students = await api(`/api/profesores/estudiantes?grupo=${encodeURIComponent(state.group)}`);
  if (!state.students.some(s => Number(s.id) === Number(state.studentId))) {
    // La primera persona aparece seleccionada y sus notas se cargan automáticamente.
    state.studentId = state.students.length ? Number(state.students[0].id) : null;
    state.grades = [];
  }
  render();
  if (state.students.length === 0) notify(`No hay estudiantes registrados en el grupo ${state.group}.`);
}
async function loadStudentGrades() {
  if (state.studentId === null) { state.grades = []; return; }
  state.grades = await api(`/api/notas/estudiante/${encodeURIComponent(state.studentId)}`);
}
async function selectedStudent(studentId, mode) {
  const student = state.students.find(s => Number(s.id) === Number(studentId));
  if (!student) return notify('No encontramos ese estudiante en el grupo cargado.', true);
  state.studentId = Number(student.id);
  state.page = 'notas';
  state.notice = '';
  await loadPage();
  if (mode === 'new') document.getElementById('grade-value')?.focus();
}
function openNoteEditor(noteId) {
  const note = state.grades.find(n => Number(n.id) === Number(noteId));
  if (!note) return notify('No se encontró la nota seleccionada.', true);
  const dialog = document.getElementById('edit-note-dialog');
  dialog.innerHTML = `<h2 id="edit-note-heading">Modificar calificación</h2><p class="subtext">Vas a modificar la <strong>nota #${id(note.id)}</strong> de <strong>${text(note.estudiante)}</strong>. Este ID es de la nota, no del estudiante.</p>
    <form id="edit-note-form"><input type="hidden" name="notaId" value="${id(note.id)}">
      <div class="field"><label for="edit-value">Nuevo valor (0 a 5)</label><input id="edit-value" name="valor" type="number" step="0.01" min="0" max="5" value="${Number(note.valor)}" required></div>
      <div class="field"><label for="edit-obs">Observación</label><textarea id="edit-obs" name="observacion" maxlength="500">${text(note.observacion, '')}</textarea></div>
      <div class="dialog-actions"><button class="btn btn-outline" type="button" data-close-dialog>Cancelar</button><button type="submit" class="btn btn-primary">Guardar cambios</button></div></form>`;
  dialog.showModal();
}
function openRecordingEditor(classId) {
  const clazz = state.teacherClasses.find(c => Number(c.id) === Number(classId));
  if (!clazz) return notify('No se encontró esa clase.', true);
  const dialog = document.getElementById('recording-dialog');
  dialog.innerHTML = `<h2 id="recording-heading">Añadir grabación</h2><p class="subtext">Clase: <strong>${text(clazz.titulo)}</strong>. Pega el enlace HTTPS de una grabación que ya exista.</p>
    <form id="recording-form"><input type="hidden" name="claseId" value="${id(clazz.id)}">
      <div class="field"><label for="recording-url">URL de la grabación</label>
        <input id="recording-url" type="url" name="url" required pattern="https://.*" placeholder="https://..." maxlength="2048" value="${escapeHtml(clazz.urlGrabacion || '')}">
        <small>El sistema no graba la reunión automáticamente.</small></div>
      <div class="dialog-actions"><button class="btn btn-outline" type="button" data-close-dialog>Cancelar</button><button class="btn btn-primary" type="submit">Guardar grabación</button></div></form>`;
  dialog.showModal();
}
function setSubmitting(form, working) {
  const button = form.querySelector('button[type="submit"]');
  if (button) {
    button.disabled = working;
    button.dataset.initialText ||= button.textContent;
    const verb = form.id === 'login-form' ? 'Iniciando sesión...' :
      form.id.startsWith('register-') ? 'Creando cuenta...' :
      form.id.includes('search') ? 'Buscando...' :
      form.id.includes('qr') ? 'Comprobando...' : 'Guardando...';
    button.textContent = working ? verb : button.dataset.initialText;
  }
}
async function submitForm(e) {
  const form = e.target;
  if (!(form instanceof HTMLFormElement)) return;
  e.preventDefault();
  if (!form.reportValidity()) return;
  setSubmitting(form, true);
  try {
    if (form.id === 'login-form') {
      const result = await api('/api/auth/login', {method: 'POST', body: {usuario: formValue(form,'usuario'), password: formValue(form,'password')}});
      saveSession(result); notify(result.mensaje || 'Sesión iniciada correctamente.'); await loadPage();
    } else if (form.id === 'register-profesor-form' || form.id === 'register-estudiante-form') {
      const teacher = form.id === 'register-profesor-form';
      const body = {identificacion: formValue(form, 'identificacion'), nombre: formValue(form, 'nombre'),
        correo: formValue(form, 'correo'), password: formValue(form, 'password')};
      if (teacher) body.materia = formValue(form, 'materia');
      else body.grupo = formValue(form, 'grupo');
      const result = await api(`/api/auth/registro/${teacher ? 'profesores' : 'estudiantes'}`, {
        method:'POST', body, headers: teacher ? {'X-Clave-Docente': formValue(form, 'claveDocente')} : {}
      });
      saveSession(result); notify(result.mensaje || 'Cuenta creada correctamente.'); await loadPage();
    } else if (form.id === 'recover-form') {
      const result = await api('/api/auth/olvide-password', {method:'POST', body:{correo:formValue(form,'correo')}});
      notify(result.mensaje || 'Solicitud recibida. Comprueba tu correo si el envío está configurado.');
      state.authTab = 'restablecer'; render();
    } else if (form.id === 'reset-form') {
      const result = await api('/api/auth/restablecer-password', {method:'POST', body:{token:formValue(form,'token'),nuevaPassword:formValue(form,'nuevaPassword')}});
      notify(result.mensaje || 'Contraseña actualizada correctamente.'); state.authTab = 'login'; render();
    } else if (form.id === 'students-search' || form.id === 'grade-search') {
      await loadStudents(formValue(form,'grupo'));
    } else if (form.id === 'add-grade-form') {
      const studentId = Number(formValue(form,'estudianteId'));
      const res = await api('/api/notas', {method:'POST', body:{
        estudianteId, valor:Number(formValue(form,'valor')), observacion:formValue(form,'observacion')
      }});
      state.studentId = studentId;
      await loadStudentGrades();
      notify(res.mensaje || 'Nota agregada correctamente.'); render();
    } else if (form.id === 'edit-note-form') {
      const notaId = Number(formValue(form,'notaId'));
      const res = await api(`/api/notas/${notaId}`, {method:'PUT', body:{valor:Number(formValue(form,'valor')),observacion:formValue(form,'observacion')}});
      form.closest('dialog')?.close();
      await loadStudentGrades();
      notify(res.mensaje || 'Nota modificada correctamente.'); render();
    } else if (form.id === 'work-form') {
      const deadline = formValue(form,'fechaEntrega');
      const body = {grupo: formValue(form,'grupo'), titulo: formValue(form,'titulo'), descripcion: formValue(form,'descripcion')};
      if (deadline) body.fechaEntrega = new Date(deadline).toISOString();
      await api('/api/trabajos', {method:'POST', body});
      state.teacherWorks = await api('/api/trabajos/mis');
      notify('Tarea publicada correctamente.'); render();
    } else if (form.id === 'class-form') {
      await api('/api/clases', {method:'POST',body:{grupo:formValue(form,'grupo'),titulo:formValue(form,'titulo')}});
      state.teacherClasses = await api('/api/clases/mis');
      notify('Clase creada correctamente. Las invitaciones por correo están en proceso.'); render();
    } else if (form.id === 'recording-form') {
      const classId = Number(formValue(form,'claseId'));
      await api(`/api/clases/${classId}/grabacion`, {method:'PUT',body:{url:formValue(form,'url')}});
      form.closest('dialog')?.close();
      state.teacherClasses = await api('/api/clases/mis');
      notify('Enlace de grabación guardado correctamente.'); render();
    } else if (form.id === 'scan-qr-form') {
      const content = formValue(form,'contenido');
      state.scanned = await api('/api/profesores/qr/escanear', {method:'POST',body:{contenido:content}});
      state.scannedContent = content;
      notify('Código QR verificado. Estudiante identificado.'); render();
    } else if (form.id === 'qr-grade-form') {
      const res = await api('/api/profesores/qr/calificar', {method:'POST',body:{
        contenido:state.scannedContent, valor:Number(formValue(form,'valor')), observacion:formValue(form,'observacion')
      }});
      notify(res.mensaje || 'Nota agregada desde el código QR.');
      state.scanned = null; state.scannedContent = null; render();
    }
  } catch (err) {
    notify(err.message, true);
    // En la pantalla de acceso muestra un aviso persistente, además del mensaje flotante.
    if (!state.session) render();
  } finally {
    if (form.isConnected) setSubmitting(form, false);
  }
}
let cameraStream = null;
let cameraTimer = null;
let cameraActive = false;
function stopQrCamera() {
  cameraActive = false;
  if (cameraTimer) { clearTimeout(cameraTimer); cameraTimer = null; }
  if (cameraStream) { cameraStream.getTracks().forEach(t => t.stop()); cameraStream = null; }
  const dialog = document.getElementById('camera-dialog');
  if (dialog?.open) dialog.close();
}
async function openQrCamera() {
  if (!('BarcodeDetector' in window) || !navigator.mediaDevices?.getUserMedia) {
    throw new Error('Este navegador no permite leer QR desde la cámara. Puedes copiar el contenido del estudiante y pegarlo en el cuadro. Chrome o Edge recientes pueden admitir el lector.');
  }
  const dialog = document.getElementById('camera-dialog');
  if (!dialog) throw new Error('Abre la sección de códigos QR para activar la cámara.');
  dialog.innerHTML = `<h2 id="camera-heading">Escanear código QR</h2><p class="subtext">Acerca el QR del estudiante a la cámara. Si no funciona, utiliza el texto del código.</p>
    <video class="scan-video" autoplay muted playsinline aria-label="Vista de la cámara"></video>
    <div class="dialog-actions"><button type="button" class="btn btn-outline" data-stop-camera>Cerrar cámara</button></div>`;
  dialog.showModal();
  dialog.addEventListener('close', stopQrCamera, {once:true});
  try {
    cameraStream = await navigator.mediaDevices.getUserMedia({video:{facingMode:{ideal:'environment'}},audio:false});
    const video = dialog.querySelector('video');
    video.srcObject = cameraStream;
    await video.play();
    cameraActive = true;
    const detector = new BarcodeDetector({formats:['qr_code']});
    async function scan() {
      if (!cameraActive) return;
      try {
        const codes = await detector.detect(video);
        const contenido = codes.find(c => c.rawValue)?.rawValue;
        if (contenido) {
          stopQrCamera();
          try {
            state.scanned = await api('/api/profesores/qr/escanear', {method:'POST',body:{contenido}});
            state.scannedContent = contenido;
            notify(`QR verificado. Estudiante identificado: ${state.scanned.nombre}.`);
          } catch (error) {
            notify(error.message, true);
          }
          render();
          return;
        }
      } catch (_) { /* Algunos fotogramas aún no están listos. */ }
      if (cameraActive) cameraTimer = setTimeout(scan, 350);
    }
    await scan();
  } catch (err) {
    stopQrCamera();
    throw new Error(`No se pudo usar la cámara: ${err.message}. También puedes pegar el texto del QR.`);
  }
}
async function handleClick(e) {
  const trigger = e.target.closest('button[data-nav], button[data-auth], button[data-logout], button[data-student-grade], button[data-student-view], button[data-edit-note], button[data-close-dialog], button[data-class-end], button[data-class-record], button[data-refresh-classes], button[data-refresh-qr], button[data-copy-qr], button[data-download-qr], button[data-demo-id], button[data-scan-camera], button[data-stop-camera]');
  if (!trigger) return;
  try {
    if (trigger.hasAttribute('data-nav')) return await navigate(trigger.dataset.nav);
    if (trigger.hasAttribute('data-auth')) { state.authTab = trigger.dataset.auth; state.notice = ''; return render(); }
    if (trigger.hasAttribute('data-demo-id')) {
      trigger.disabled = true;
      state.pendingDemoId = trigger.dataset.demoId;
      try {
        const result = await api('/api/demo/entrar', {method:'POST',body:{identificacion:state.pendingDemoId}});
        saveSession(result);
        notify(result.mensaje || 'Sesión de prueba iniciada correctamente.');
        return await loadPage();
      } finally { state.pendingDemoId = null; }
    }
    if (trigger.hasAttribute('data-stop-camera')) return stopQrCamera();
    if (trigger.hasAttribute('data-scan-camera')) return await openQrCamera();
    if (trigger.hasAttribute('data-logout')) { stopQrCamera(); clearSession(); render(); loadDemoAccounts(); return showToast('Sesión cerrada. Puedes entrar con otra cuenta.'); }
    if (trigger.hasAttribute('data-student-grade')) return await selectedStudent(trigger.dataset.studentGrade, 'new');
    if (trigger.hasAttribute('data-student-view')) return await selectedStudent(trigger.dataset.studentView, 'view');
    if (trigger.hasAttribute('data-edit-note')) return openNoteEditor(trigger.dataset.editNote);
    if (trigger.hasAttribute('data-close-dialog')) return trigger.closest('dialog')?.close();
    if (trigger.hasAttribute('data-class-end')) {
      const classId = Number(trigger.dataset.classEnd);
      if (!confirm('¿Quieres finalizar esta clase? Los estudiantes dejarán de verla como activa.')) return;
      await api(`/api/clases/${classId}/finalizar`, {method:'PATCH'});
      state.teacherClasses = await api('/api/clases/mis');
      notify('Clase finalizada correctamente.'); render(); return;
    }
    if (trigger.hasAttribute('data-class-record')) return openRecordingEditor(trigger.dataset.classRecord);
    if (trigger.hasAttribute('data-refresh-classes')) { state.teacherClasses = await api('/api/clases/mis'); notify('Estado de las clases actualizado.'); return render(); }
    if (trigger.hasAttribute('data-refresh-qr')) { state.qr = await api('/api/estudiantes/qr'); notify('Código QR actualizado.'); return render(); }
    if (trigger.hasAttribute('data-copy-qr')) {
      if (!state.qr?.contenido) throw new Error('No hay código QR para copiar.');
      await navigator.clipboard.writeText(state.qr.contenido);
      return notify('Contenido del QR copiado.');
    }
    if (trigger.hasAttribute('data-download-qr')) {
      if (!state.qr?.imagenBase64) throw new Error('No hay código QR para descargar.');
      const a = document.createElement('a'); a.href = `data:image/png;base64,${state.qr.imagenBase64}`;
      a.download = 'mi-codigo-qr.png'; document.body.appendChild(a); a.click(); a.remove();
      return notify('Descarga del QR iniciada.');
    }
  } catch (err) { notify(err.message, true); if (!state.session) render(); }
}

document.addEventListener('submit', submitForm);
document.addEventListener('click', handleClick);
document.addEventListener('change', async e => {
  if (e.target?.id !== 'grade-student') return;
  const selected = Number(e.target.value);
  if (!state.students.some(s => Number(s.id) === selected)) return;
  state.studentId = selected;
  try {
    await loadStudentGrades();
    // Actualiza el listado de notas de la persona seleccionada.
    render();
  } catch (err) { notify(err.message, true); }
});
document.addEventListener('input', e => {
  if (e.target?.id !== 'buscar-estudiante') return;
  const query = e.target.value.toLocaleLowerCase('es');
  document.querySelectorAll('.student-row').forEach(row => {
    row.hidden = !row.textContent.toLocaleLowerCase('es').includes(query);
  });
});
render();
if (state.session) loadPage();
else loadDemoAccounts();
