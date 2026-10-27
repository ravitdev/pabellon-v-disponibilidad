// Lee los horarios que Thymeleaf ya dibuja en la tabla y los presenta
// como una cuadrícula semanal. No cambia los datos ni realiza peticiones.
(() => {
    'use strict';

    const contenedorTabla = document.getElementById('tabla-horarios');
    const tabla = contenedorTabla?.querySelector('table');
    const calendario = document.getElementById('calendario-semanal');
    const boton = document.getElementById('cambiar-vista');
    if (!tabla || !calendario || !boton) return;

    const DIAS = ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo'];
    const ALTO_POR_HORA = 64; // píxeles por cada hora representada

    function normalizar(texto) {
        return texto.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();
    }

    function aMinutos(hora) {
        if (!/^([01]\d|2[0-3]):[0-5]\d$/.test(hora)) return null;
        const [h, m] = hora.split(':').map(Number);
        return h * 60 + m;
    }

    const actividades = [...tabla.querySelectorAll('tbody tr')].map((fila) => {
        const celdas = [...fila.querySelectorAll('td')];
        if (celdas.length !== 3) return null;
        const diaTexto = celdas[0].textContent.trim();
        const inicio = celdas[1].textContent.trim();
        const fin = celdas[2].textContent.trim();
        const dia = DIAS.findIndex((nombre) => normalizar(nombre) === normalizar(diaTexto));
        const inicioMin = aMinutos(inicio);
        const finMin = aMinutos(fin);
        if (dia < 0 || inicioMin === null || finMin === null || inicioMin >= finMin) return null;
        return { dia, inicio, fin, inicioMin, finMin };
    });

    // Si los datos no pueden interpretarse, dejamos visible la tabla original.
    if (!actividades.length || actividades.some((a) => a === null)) return;

    const horaInicial = Math.max(0, Math.floor(Math.min(...actividades.map(a => a.inicioMin)) / 60) - 1);
    const horaFinal = Math.min(24, Math.ceil(Math.max(...actividades.map(a => a.finMin)) / 60) + 1);
    const altoTotal = (horaFinal - horaInicial) * ALTO_POR_HORA;

    const cabecera = document.createElement('div');
    cabecera.className = 'calendario-cabecera';
    for (const dia of ['Hora', ...DIAS]) {
        const celda = document.createElement('span');
        celda.textContent = dia;
        cabecera.append(celda);
    }

    const cuerpo = document.createElement('div');
    cuerpo.className = 'calendario-cuerpo';
    cuerpo.style.height = `${altoTotal}px`;

    const eje = document.createElement('div');
    eje.className = 'calendario-eje';
    for (let hora = horaInicial; hora < horaFinal; hora++) {
        const etiqueta = document.createElement('span');
        etiqueta.className = 'calendario-etiqueta-hora';
        etiqueta.style.top = `${(hora - horaInicial) * ALTO_POR_HORA + 5}px`;
        etiqueta.textContent = `${String(hora).padStart(2, '0')}:00`;
        eje.append(etiqueta);
    }
    cuerpo.append(eje);

    for (let indice = 0; indice < DIAS.length; indice++) {
        const columna = document.createElement('div');
        columna.className = 'calendario-columna';
        columna.setAttribute('aria-label', DIAS[indice]);

        const delDia = actividades.filter(a => a.dia === indice)
            .sort((a, b) => a.inicioMin - b.inicioMin || a.finMin - b.finMin);

        // Colocar actividades simultáneas en carriles diferentes.
        const grupos = [];
        let grupo = [];
        let maxFinGrupo = -1;
        for (const actividad of delDia) {
            if (grupo.length && actividad.inicioMin >= maxFinGrupo) {
                grupos.push(grupo);
                grupo = [];
                maxFinGrupo = -1;
            }
            grupo.push(actividad);
            maxFinGrupo = Math.max(maxFinGrupo, actividad.finMin);
        }
        if (grupo.length) grupos.push(grupo);

        for (const conjunto of grupos) {
            const finesCarriles = [];
            for (const actividad of conjunto) {
                let carril = finesCarriles.findIndex(fin => fin <= actividad.inicioMin);
                if (carril === -1) {
                    carril = finesCarriles.length;
                    finesCarriles.push(actividad.finMin);
                } else {
                    finesCarriles[carril] = actividad.finMin;
                }
                actividad.carril = carril;
            }
            const porcentaje = 100 / finesCarriles.length;
            for (const actividad of conjunto) {
                const bloque = document.createElement('div');
                bloque.className = 'calendario-evento';
                bloque.style.top = `${(actividad.inicioMin - horaInicial * 60) / 60 * ALTO_POR_HORA + 2}px`;
                bloque.style.height = `${Math.max(12, (actividad.finMin - actividad.inicioMin) / 60 * ALTO_POR_HORA - 4)}px`;
                bloque.style.left = `calc(${actividad.carril * porcentaje}% + 4px)`;
                bloque.style.width = `calc(${porcentaje}% - 8px)`;
                bloque.textContent = `${actividad.inicio}–${actividad.fin}`;
                bloque.title = `${DIAS[indice]}: ${actividad.inicio} a ${actividad.fin}`;
                bloque.setAttribute('aria-label', `Actividad registrada el ${DIAS[indice]} de ${actividad.inicio} a ${actividad.fin}`);
                bloque.setAttribute('tabindex', '0');
                columna.append(bloque);
            }
        }
        cuerpo.append(columna);
    }

    calendario.append(cabecera, cuerpo);
    contenedorTabla.hidden = true;
    calendario.hidden = false;
    boton.hidden = false;
    boton.addEventListener('click', () => {
        const verTabla = contenedorTabla.hidden;
        contenedorTabla.hidden = !verTabla;
        calendario.hidden = verTabla;
        boton.textContent = verTabla ? 'Ver calendario' : 'Ver tabla';
    });
})();
