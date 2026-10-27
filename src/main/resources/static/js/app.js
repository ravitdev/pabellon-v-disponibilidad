document.getElementById('carga')?.addEventListener('submit', function () {
  document.getElementById('progreso').textContent = 'Validando archivo y actualizando información…';
  this.querySelector('button[type="submit"]').disabled = true;
});
