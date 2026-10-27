
(() => {
    'use strict';

    const tablaContenedor = document.getElementById('tabla-horarios');
    const tabla = tablaContenedor?.querySelector('table');
    const calendario = document.getElementById('calendario-semanal');
    const alternar = document.getElementById('cambiar-vista');
    const controles = document.getElementById('semana-controles');
    const error = document.getElementById('error-calendario');
    const filasAdicionales = document.getElementById('filas-adicionales');

    if (!tabla || !calendario || !calendario.dataset.codigo) return;

    const DIAS = [
        'Lunes', 'Martes', 'Miércoles', 'Jueves',
        'Viernes', 'Sábado', 'Domingo'
    ];

    const PX_POR_HORA = 64;

    const normalizar = s => s.normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '').toLowerCase();

    const minutos = s => {
        if (!/^([01]\d|2[0-3]):[0-5]\d(?::[0-5]\d)?$/.test(s))
            return null;
        const p = s.split(':').map(Number);
        return p[0] * 60 + p[1];
    };

    const fechaUtc = s => {
        if (!/^\d{4}-\d{2}-\d{2}$/.test(s || '')) return null;
        const d = new Date(`${s}T12:00:00Z`);
        return Number.isNaN(d.getTime()) ||
            d.toISOString().slice(0, 10) !== s ? null : d;
    };

    const iso = d => d.toISOString().slice(0, 10);

    const sumarDias = (d, dias) => {
        const x = new Date(d.getTime());
        x.setUTCDate(x.getUTCDate() + dias);
        return x;
    };

    const inicioSemana = d =>
        sumarDias(d, -((d.getUTCDay() + 6) % 7));

    const label = d =>
        `${String(d.getUTCDate()).padStart(2, '0')}/` +
        `${String(d.getUTCMonth() + 1).padStart(2, '0')}`;

    const hoyLima = new Intl.DateTimeFormat('sv-SE', {
        timeZone: 'America/Lima',
        year: 'numeric',
        month: '2-digit',
        day: '2-digit'
    }).format(new Date());

    const fechaInicial = fechaUtc(
        new URLSearchParams(location.search).get('semana')
    ) || fechaUtc(hoyLima);

    let lunes = inicioSemana(fechaInicial);
    let vistaTabla = false;

    // Programación recurrente procedente del CSV.
    const horariosCsv = [
        ...tabla.querySelectorAll('tbody:first-of-type tr')
    ].map(fila => {
        const c = [...fila.querySelectorAll('td')];
        if (c.length !== 3) return null;

        const dia = DIAS.findIndex(d =>
            normalizar(d) === normalizar(c[0].textContent.trim())
        );
        const inicio = c[1].textContent.trim();
        const fin = c[2].textContent.trim();

        return {
            dia, inicio, fin,
            inicioMin: minutos(inicio),
            finMin: minutos(fin),
            titulo: 'Clase programada',
            tipo: 'CSV'
        };
    });

    if (horariosCsv.some(x =>
        !x || x.dia < 0 ||
        x.inicioMin === null || x.finMin === null
    )) return;

    async function cargarSemana() {
        const semana = iso(lunes);
        const codigo = encodeURIComponent(calendario.dataset.codigo);
        const url =
            `/api/ambientes/${codigo}/actividades?semana=${semana}`;

        try {
            const respuesta = await fetch(url, {
                headers: { 'Accept': 'application/json' }
            });

            if (!respuesta.ok)
                throw new Error(`HTTP ${respuesta.status}`);

            const datos = await respuesta.json();

            const extras = datos.map(x => ({
                dia: Number(x.dia) - 1,
                inicio: x.inicio,
                fin: x.fin,
                inicioMin: minutos(x.inicio),
                finMin: minutos(x.fin),
                titulo: x.titulo,
                tipo: x.tipo,
                fecha: x.fecha
            }));

            if (extras.some(x =>
                x.dia < 0 || x.dia > 6 ||
                x.inicioMin === null || x.finMin === null
            )) throw new Error('Horario inválido.');

            error.hidden = true;

            document.getElementById('rango-semana').textContent =
                `${label(lunes)} – ${label(sumarDias(lunes, 6))} · ` +
                lunes.getUTCFullYear();

            rellenarTabla(extras);
            dibujar([...horariosCsv, ...extras]);

            controles.hidden = false;
            alternar.hidden = false;
            actualizarVista();

        } catch (e) {
            error.textContent =
                'No se pudieron cargar las actividades adicionales.';
            error.hidden = false;
            controles.hidden = true;
            calendario.hidden = true;
            tablaContenedor.hidden = false;
            alternar.hidden = true;
        }
    }

    function rellenarTabla(extras) {
        filasAdicionales.replaceChildren();

        for (const a of extras) {
            const fila = document.createElement('tr');

            const valores = [
                `${DIAS[a.dia]} · ${a.titulo} ` +
                `(${a.tipo === 'PUNTUAL' ? a.fecha : 'semanal'})`,
                a.inicio,
                a.fin
            ];

            for (const valor of valores) {
                const celda = document.createElement('td');
                celda.textContent = valor;
                fila.append(celda);
            }

            filasAdicionales.append(fila);
        }
    }

    function actualizarVista() {
        tablaContenedor.hidden = !vistaTabla;
        calendario.hidden = vistaTabla;
        alternar.textContent = vistaTabla
            ? 'Ver calendario' : 'Ver tabla';
    }

    function dibujar(actividades) {
        calendario.replaceChildren();

        const validas = actividades.filter(a =>
            a.inicioMin < a.finMin
        );

        const primera = validas.length
            ? Math.min(...validas.map(a => a.inicioMin))
            : 8 * 60;

        const ultima = validas.length
            ? Math.max(...validas.map(a => a.finMin))
            : 18 * 60;

        const minHora = Math.max(0, Math.floor(primera / 60) - 1);
        const maxHora = Math.min(24, Math.ceil(ultima / 60) + 1);

        const cabecera = document.createElement('div');
        cabecera.className = 'calendario-cabecera';

        ['Hora', ...DIAS.map((d, i) =>
            `${d} ${label(sumarDias(lunes, i))}`
        )].forEach(nombre => {
            const celda = document.createElement('span');
            celda.textContent = nombre;
            cabecera.append(celda);
        });

        const cuerpo = document.createElement('div');
        cuerpo.className = 'calendario-cuerpo';
        cuerpo.style.height =
            `${(maxHora - minHora) * PX_POR_HORA}px`;

        const eje = document.createElement('div');
        eje.className = 'calendario-eje';

        for (let h = minHora; h < maxHora; h++) {
            const marca = document.createElement('span');
            marca.className = 'calendario-etiqueta-hora';
            marca.style.top =
                `${(h - minHora) * PX_POR_HORA + 5}px`;
            marca.textContent =
                `${String(h).padStart(2, '0')}:00`;
            eje.append(marca);
        }

        cuerpo.append(eje);

        for (let dia = 0; dia < 7; dia++) {
            const columna = document.createElement('div');
            columna.className = 'calendario-columna';
            columna.setAttribute('aria-label', DIAS[dia]);

            const orden = validas.filter(a => a.dia === dia)
                .sort((a, b) =>
                    a.inicioMin - b.inicioMin ||
                    a.finMin - b.finMin
                );

            // Organizar actividades superpuestas en carriles.
            const grupos = [];
            let grupo = [];
            let finGrupo = -1;

            for (const a of orden) {
                if (grupo.length && a.inicioMin >= finGrupo) {
                    grupos.push(grupo);
                    grupo = [];
                    finGrupo = -1;
                }
                grupo.push(a);
                finGrupo = Math.max(finGrupo, a.finMin);
            }

            if (grupo.length) grupos.push(grupo);

            for (const g of grupos) {
                const fines = [];

                for (const a of g) {
                    let carril = fines.findIndex(f =>
                        f <= a.inicioMin
                    );

                    if (carril < 0) {
                        carril = fines.length;
                        fines.push(a.finMin);
                    } else {
                        fines[carril] = a.finMin;
                    }
                    a.carril = carril;
                }

                const ancho = 100 / fines.length;

                for (const a of g) {
                    const bloque = document.createElement('div');
                    bloque.className = 'calendario-evento' +
                        (a.tipo !== 'CSV' ? ' evento-extra' : '');

                    bloque.style.top =
                        `${((a.inicioMin - minHora * 60) / 60) *
                          PX_POR_HORA + 2}px`;

                    bloque.style.height =
                        `${Math.max(14,
                          ((a.finMin - a.inicioMin) / 60) *
                          PX_POR_HORA - 4)}px`;

                    bloque.style.left =
                        `calc(${a.carril * ancho}% + 4px)`;
                    bloque.style.width =
                        `calc(${ancho}% - 8px)`;

                    bloque.textContent =
                        `${a.inicio}–${a.fin} ` +
                        (a.tipo === 'CSV' ? '' : a.titulo);

                    bloque.title =
                        `${a.titulo}: ${a.inicio} a ${a.fin}`;
                    bloque.tabIndex = 0;

                    columna.append(bloque);
                }
            }

            cuerpo.append(columna);
        }

        calendario.append(cabecera, cuerpo);
    }

    alternar.addEventListener('click', () => {
        vistaTabla = !vistaTabla;
        actualizarVista();
    });

    document.getElementById('semana-anterior')
        .addEventListener('click', () => {
            lunes = sumarDias(lunes, -7);
            cargarSemana();
        });

    document.getElementById('semana-siguiente')
        .addEventListener('click', () => {
            lunes = sumarDias(lunes, 7);
            cargarSemana();
        });

    cargarSemana();
})();
