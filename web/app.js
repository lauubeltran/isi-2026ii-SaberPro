const form = document.querySelector('#registration-form');
const fileInput = document.querySelector('#student-file');
const dropzone = document.querySelector('#dropzone');
const dropTitle = document.querySelector('#drop-title');
const feedback = document.querySelector('#feedback');
const submitButton = document.querySelector('#submit-button');
const recordsBody = document.querySelector('#records-body');
const searchInput = document.querySelector('#search-input');
const periodSelect = document.querySelector('#period-select');
let records = [];

function showFeedback(message, errors = []) {
  feedback.replaceChildren();
  feedback.hidden = false;
  feedback.classList.toggle('is-error', errors.length > 0);
  const summary = document.createElement('div');
  summary.textContent = message;
  feedback.append(summary);
  if (errors.length) {
    const list = document.createElement('ul');
    for (const error of errors) {
      const item = document.createElement('li');
      item.textContent = error;
      list.append(item);
    }
    feedback.append(list);
  }
}

function updateFileName(file) {
  dropTitle.textContent = file ? file.name : 'Arrastra el archivo aquí';
}

function renderRecords() {
  const query = searchInput.value.trim().toLocaleLowerCase('es');
  const visibleRecords = records.filter((record) =>
    [record.name, record.code, record.email, record.period].some((value) => value.toLocaleLowerCase('es').includes(query))
  );
  recordsBody.replaceChildren();
  if (!visibleRecords.length) {
    const row = document.createElement('tr');
    row.className = 'empty-row';
    const cell = document.createElement('td');
    cell.colSpan = 5;
    cell.textContent = records.length ? 'No hay coincidencias para la búsqueda.' : 'Aún no hay estudiantes registrados.';
    row.append(cell);
    recordsBody.append(row);
  } else {
    for (const record of visibleRecords) {
      const row = document.createElement('tr');
      for (const value of [record.name, record.code, record.email, record.period]) {
        const cell = document.createElement('td');
        cell.textContent = value;
        row.append(cell);
      }
      const statusCell = document.createElement('td');
      const status = document.createElement('span');
      status.className = 'status-pill';
      status.textContent = record.status;
      statusCell.append(status);
      row.append(statusCell);
      recordsBody.append(row);
    }
  }
  document.querySelector('#total-count').textContent = records.length.toLocaleString('es');
  document.querySelector('#table-count').textContent = `${records.length} ${records.length === 1 ? 'estudiante' : 'estudiantes'}`;
}

async function loadRecords() {
  try {
    const response = await fetch('/api/records');
    if (!response.ok) throw new Error('No se pudieron consultar los registros.');
    const data = await response.json();
    records = data.records;
    renderRecords();
  } catch (error) {
    showFeedback('No fue posible conectar con el servidor local. Reinicia la aplicación Java.', [error.message]);
  }
}

fileInput.addEventListener('change', () => updateFileName(fileInput.files[0]));
searchInput.addEventListener('input', renderRecords);
periodSelect.addEventListener('change', () => {
  document.querySelector('#summary-period').textContent = periodSelect.value;
});

dropzone.addEventListener('click', (event) => {
  if (event.target !== fileInput) fileInput.click();
});
dropzone.addEventListener('keydown', (event) => {
  if (event.key === 'Enter' || event.key === ' ') {
    event.preventDefault();
    fileInput.click();
  }
});
for (const eventName of ['dragenter', 'dragover']) {
  dropzone.addEventListener(eventName, (event) => {
    event.preventDefault();
    dropzone.classList.add('is-over');
  });
}
for (const eventName of ['dragleave', 'drop']) {
  dropzone.addEventListener(eventName, (event) => {
    event.preventDefault();
    dropzone.classList.remove('is-over');
  });
}
dropzone.addEventListener('drop', (event) => {
  const [file] = event.dataTransfer.files;
  if (file) {
    const transfer = new DataTransfer();
    transfer.items.add(file);
    fileInput.files = transfer.files;
    updateFileName(file);
  }
});

form.addEventListener('submit', async (event) => {
  event.preventDefault();
  const file = fileInput.files[0];
  if (!file) {
    showFeedback('Selecciona un archivo .txt para continuar.', ['No se ha seleccionado ningún archivo.']);
    return;
  }
  if (!file.name.toLocaleLowerCase().endsWith('.txt')) {
    showFeedback('El formato del archivo no es válido.', ['Selecciona un archivo con extensión .txt.']);
    return;
  }
  submitButton.disabled = true;
  submitButton.querySelector('span').textContent = 'Validando archivo...';
  feedback.hidden = true;
  try {
    const fileContent = await file.text();
    const response = await fetch('/api/register', {
      method: 'POST',
      headers: {
        'Content-Type': 'text/plain; charset=utf-8',
        'X-Program': form.elements.program.value,
        'X-Period': form.elements.period.value,
      },
      body: fileContent,
    });
    const result = await response.json();
    if (!response.ok) {
      showFeedback(result.message || 'No se pudo completar el registro.', result.errors || []);
      return;
    }
    showFeedback(`${result.registered} estudiantes registrados con estado pendiente de evaluación.`);
    form.reset();
    updateFileName(null);
    document.querySelector('#summary-period').textContent = periodSelect.value;
    await loadRecords();
  } catch (error) {
    showFeedback('Ocurrió un error al procesar el archivo.', [error.message]);
  } finally {
    submitButton.disabled = false;
    submitButton.querySelector('span').textContent = 'Validar y registrar';
  }
});

loadRecords();