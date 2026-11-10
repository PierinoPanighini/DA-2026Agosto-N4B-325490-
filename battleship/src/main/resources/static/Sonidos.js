//-------------------------------EXTRAS SONIDOS----------------------------------//
import CommandDispatcher from './CommandDispatcher.js';

// Efectos originales sintetizados con Web Audio. No descarga audios externos.
class Sonidos {
  constructor() {
    this.contexto = null;
    this.salida = null;
    this.ruido = null;
    this.fuentes = new Set();
    this.silenciado = this.leerPreferencia('sonidos.silenciado', 'false') === 'true';
    const volumenGuardado = Number(this.leerPreferencia('sonidos.volumen', '35'));
    this.volumen = Number.isFinite(volumenGuardado) ? Math.max(0, Math.min(100, volumenGuardado)) : 35;

    // El audio se habilita dentro de una interacción, antes de esperar al servidor.
    document.addEventListener('pointerdown', () => this.activar(), { capture: true });
    document.addEventListener('keydown', () => this.activar(), { capture: true });
  }

  leerPreferencia(clave, valorInicial) {
    try {
      return localStorage.getItem(clave) ?? valorInicial;
    } catch {
      return valorInicial;
    }
  }

  guardarPreferencia(clave, valor) {
    try {
      localStorage.setItem(clave, String(valor));
    } catch {
      // El juego también funciona si el navegador no permite guardar preferencias.
    }
  }

  activar() {
    const AudioContext = window.AudioContext || window.webkitAudioContext;
    if (!AudioContext) return;
    try {
      if (!this.contexto) {
        this.contexto = new AudioContext();
        this.salida = this.contexto.createGain();
        this.salida.connect(this.contexto.destination);
        this.actualizarVolumen();
        this.ruido = this.contexto.createBuffer(1, this.contexto.sampleRate * 2, this.contexto.sampleRate);
        const muestras = this.ruido.getChannelData(0);
        for (let i = 0; i < muestras.length; i++) {
          muestras[i] = Math.random() * 2 - 1;
        }
      }
      if (this.contexto.state === 'suspended') {
        this.contexto.resume().catch(() => {});
      }
    } catch {
      // Si el dispositivo no ofrece audio, las acciones del juego siguen funcionando.
    }
  }

  instalarEnVista() {
    const contenedor = document.createElement('div');
    contenedor.id = 'controlesSonido';
    contenedor.className = 'controles-sonido';
    contenedor.setAttribute('role', 'group');
    contenedor.setAttribute('aria-label', 'Efectos de sonido');
    document.querySelector('main').prepend(contenedor);
    this.instalarControles(contenedor);
    CommandDispatcher.mapFunction('reproducirSonidos', efectos => this.reproducirSecuencia(efectos));

    if (document.body.classList.contains('fondo-combate')) {
      // El segundo tablero del componente es el enemigo. Se corta antes de enviar el disparo.
      document.addEventListener('click', evento => {
        const celda = evento.target.closest('[role="gridcell"]');
        const enemigo = document.querySelectorAll('#tablero [role="grid"]')[1];
        if (celda && enemigo && enemigo.contains(celda)) this.detener();
      }, { capture: true });
    }
    window.addEventListener('pagehide', () => this.detener());
  }

  instalarControles(contenedor) {
    const boton = document.createElement('button');
    boton.type = 'button';
    boton.className = 'boton boton-secundario';
    const actualizarBoton = () => {
      boton.textContent = this.silenciado ? 'Activar sonido' : 'Silenciar';
      boton.setAttribute('aria-pressed', String(this.silenciado));
      boton.setAttribute('aria-label', 'Silenciar los efectos de sonido');
    };
    actualizarBoton();
    boton.addEventListener('click', () => {
      this.silenciado = !this.silenciado;
      this.guardarPreferencia('sonidos.silenciado', this.silenciado);
      this.actualizarVolumen();
      if (this.silenciado) this.detener();
      actualizarBoton();
    });

    const etiqueta = document.createElement('label');
    etiqueta.textContent = 'Volumen';
    const volumen = document.createElement('input');
    volumen.type = 'range';
    volumen.min = '0';
    volumen.max = '100';
    volumen.value = String(this.volumen);
    volumen.setAttribute('aria-label', 'Volumen de los efectos de sonido');
    volumen.addEventListener('input', () => {
      this.volumen = Number(volumen.value);
      this.guardarPreferencia('sonidos.volumen', this.volumen);
      this.actualizarVolumen();
    });
    etiqueta.appendChild(volumen);
    contenedor.append(boton, etiqueta);
  }

  actualizarVolumen() {
    if (this.salida) {
      this.salida.gain.setValueAtTime(this.silenciado ? 0 : this.volumen / 100, this.contexto.currentTime);
    }
  }

  detener() {
    for (const fuente of this.fuentes) fuente.stop();
    this.fuentes.clear();
  }

  reproducirSecuencia(efectos) {
    // Cada ronda reemplaza el audio anterior, incluso los sonidos que aun no empezaron.
    this.detener();
    if (this.silenciado || this.volumen === 0 || this.contexto?.state !== 'running') return;
    let inicio = this.contexto.currentTime + 0.015;
    for (const efecto of efectos) {
      inicio += this.reproducirEfecto(efecto, inicio) + 0.06;
    }
  }

  conectarFuente(fuente, destino, inicio, duracion) {
    fuente.connect(destino);
    this.fuentes.add(fuente);
    fuente.onended = () => {
      this.fuentes.delete(fuente);
      fuente.disconnect();
      destino.disconnect();
    };
    fuente.start(inicio);
    fuente.stop(inicio + duracion);
  }

  tono(inicio, duracion, frecuencia, frecuenciaFinal, amplitud, tipo = 'sine') {
    const tono = this.contexto.createOscillator();
    tono.type = tipo;
    tono.frequency.setValueAtTime(frecuencia, inicio);
    tono.frequency.exponentialRampToValueAtTime(frecuenciaFinal, inicio + duracion);
    const envolvente = this.contexto.createGain();
    envolvente.gain.setValueAtTime(0, inicio);
    envolvente.gain.linearRampToValueAtTime(amplitud, inicio + 0.008);
    envolvente.gain.exponentialRampToValueAtTime(0.001, inicio + duracion);
    envolvente.connect(this.salida);
    this.conectarFuente(tono, envolvente, inicio, duracion);
  }

  soplido(inicio, duracion, frecuencia, frecuenciaFinal, amplitud, tipo) {
    const fuente = this.contexto.createBufferSource();
    fuente.buffer = this.ruido;
    const filtro = this.contexto.createBiquadFilter();
    filtro.type = tipo;
    filtro.frequency.setValueAtTime(frecuencia, inicio);
    filtro.frequency.exponentialRampToValueAtTime(frecuenciaFinal, inicio + duracion);
    const envolvente = this.contexto.createGain();
    envolvente.gain.setValueAtTime(0, inicio);
    envolvente.gain.linearRampToValueAtTime(amplitud, inicio + 0.008);
    envolvente.gain.exponentialRampToValueAtTime(0.001, inicio + duracion);
    filtro.connect(envolvente);
    envolvente.connect(this.salida);
    this.conectarFuente(fuente, filtro, inicio, duracion);
    fuente.onended = () => {
      this.fuentes.delete(fuente);
      fuente.disconnect();
      filtro.disconnect();
      envolvente.disconnect();
    };
  }

  reproducirEfecto(efecto, inicio) {
    switch (efecto) {
      case 'colocar':
        this.tono(inicio, 0.16, 420, 170, 0.25, 'triangle');
        this.tono(inicio + 0.025, 0.18, 840, 340, 0.08);
        return 0.21;
      case 'agua':
        this.soplido(inicio, 0.18, 1800, 350, 0.42, 'bandpass');
        this.tono(inicio + 0.025, 0.12, 410, 130, 0.1);
        return 0.18;
      case 'impacto':
        this.soplido(inicio, 0.16, 2800, 140, 0.5, 'lowpass');
        this.tono(inicio, 0.14, 145, 38, 0.34, 'triangle');
        return 0.16;
      case 'hundido':
        this.soplido(inicio, 0.3, 2600, 65, 0.55, 'lowpass');
        this.tono(inicio, 0.26, 110, 28, 0.4, 'triangle');
        this.soplido(inicio + 0.1, 0.17, 1000, 100, 0.22, 'lowpass');
        return 0.3;
      case 'victoria':
        [392, 494, 587, 784].forEach((nota, i) => {
          this.tono(inicio + i * 0.16, 0.42, nota, nota, 0.24, 'triangle');
        });
        return 0.9;
      case 'derrota':
        [330, 294, 247, 165].forEach((nota, i) => {
          this.tono(inicio + i * 0.22, 0.45, nota, nota, 0.22, 'triangle');
        });
        return 1.11;
      default:
        return 0;
    }
  }
}

export default new Sonidos();
