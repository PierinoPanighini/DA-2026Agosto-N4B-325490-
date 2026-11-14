package uy.edu.ort.battleshipdemo.modelo;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import uy.edu.ort.battleshipdemo.modelo.Barco.Orientacion;
import uy.edu.ort.battleshipdemo.modelo.Disparo.ResultadoDisparo;

class BatallaEstadosTest {

    @Test
    void constructorRechazaParticipantesInvalidosConExcepcionDelModelo() throws BatallaNavalException {
        Batalla valida = crearBatalla();
        Jugador jugador = valida.getJugador();
        Maquina maquina = valida.getMaquina();
        assertThrows(BatallaNavalException.class, () -> new Batalla("Prueba", null, maquina));
        assertThrows(BatallaNavalException.class, () -> new Batalla("Prueba", jugador, null));
        Maquina flotaCompartida = new Maquina(jugador.getFlota(), new Oceano(6, 6), new TacticaSecuencial());
        assertThrows(BatallaNavalException.class, () -> new Batalla("Prueba", jugador, flotaCompartida));
        Maquina oceanoCompartido = new Maquina(jugador.getFlota().crearFlotaEquivalente(),
                jugador.getOceano(), new TacticaSecuencial());
        assertThrows(BatallaNavalException.class, () -> new Batalla("Prueba", jugador, oceanoCompartido));
    }

    @Test
    void preparacionRechazaDisparosSinAlterarElTableroNiElHistorial() throws BatallaNavalException {
        Batalla batalla = crearBatalla();
        assertEquals(Batalla.Estados.EN_PREPARACION, batalla.getEstado());
        assertFalse(batalla.fueGanada());
        assertFalse(batalla.fuePerdida());
        assertThrows(BatallaNavalException.class, () -> batalla.generarDisparo(1, 1));
        assertTrue(batalla.getRondas().isEmpty());
        assertEquals(0, contarDisparados(batalla.getOceanoMaquina()));
        assertEquals(0, contarDisparados(batalla.getOceanoJugador()));
        assertEquals(1, batalla.getNumeroProximaRonda());
    }

    @Test
    void unaRondaRegistraAmbosDisparosYAvanzaElNumero() throws Exception {
        Batalla batalla = crearBatallaEnCombate(5);
        batalla.generarDisparo(1, 1);
        assertEquals(Batalla.Estados.EN_COMBATE, batalla.getEstado());
        assertEquals(1, batalla.getRondas().size());
        Ronda ronda = batalla.getRondas().get(0);
        assertEquals(1, ronda.getNumero());
        assertSame(batalla.getOceanoMaquina().getCuadrante(1, 1), ronda.getDisparoJugador().getCuadrante());
        assertSame(batalla.getOceanoJugador().getCuadrante(0, 0), ronda.getDisparoMaquina().getCuadrante());
        assertEquals(ResultadoDisparo.AGUA, ronda.getDisparoJugador().getResultado());
        assertEquals(ResultadoDisparo.AGUA, ronda.getDisparoMaquina().getResultado());
        assertEquals(2, batalla.getNumeroProximaRonda());
    }

    @Test
    void victoriaFinalizaSinRespuestaYRechazaMasDisparos() throws Exception {
        Batalla batalla = crearBatallaEnCombate(5);
        batalla.generarDisparo(2, 0);
        assertEquals(1, batalla.getBajasEnemigas().size());
        assertFalse(batalla.fueGanada());
        assertFalse(batalla.fuePerdida());

        batalla.generarDisparo(2, 2);
        assertEquals(Batalla.Estados.TERMINADA, batalla.getEstado());
        assertTrue(batalla.fueGanada());
        assertFalse(batalla.fuePerdida());
        BatallaNavalException errorInicio = assertThrows(BatallaNavalException.class, batalla::iniciarCombate);
        assertTrue(errorInicio.getMessage().contains("finalizo"));
        assertEquals(2, batalla.getBajasEnemigas().size());
        assertTrue(batalla.getBajasAmigas().isEmpty());
        assertNull(batalla.getRondas().get(1).getDisparoMaquina());
        assertEquals(1, contarDisparados(batalla.getOceanoJugador()));
        assertThrows(BatallaNavalException.class, () -> batalla.generarDisparo(4, 4));
        assertFalse(batalla.getOceanoMaquina().getCuadrante(4, 4).isDisparado());
        assertEquals(2, batalla.getRondas().size());
    }

    @Test
    void derrotaObtieneLasBajasAmigasSinDuplicarlas() throws Exception {
        Batalla batalla = crearBatallaEnCombate(0);
        batalla.generarDisparo(5, 5);
        assertEquals(List.of(batalla.getFlotaJugador().getBarcos().get(0)), batalla.getBajasAmigas());
        assertFalse(batalla.fuePerdida());
        assertFalse(batalla.fueGanada());

        batalla.generarDisparo(2, 0);
        assertEquals(1, batalla.getBajasAmigas().size());
        assertFalse(batalla.fuePerdida());
        assertFalse(batalla.fueGanada());

        batalla.generarDisparo(5, 4);
        assertEquals(Batalla.Estados.TERMINADA, batalla.getEstado());
        assertTrue(batalla.fuePerdida());
        assertFalse(batalla.fueGanada());
        assertEquals(batalla.getFlotaJugador().getBarcos(), batalla.getBajasAmigas());
        assertEquals(1, batalla.getBajasEnemigas().size());
        assertEquals(ResultadoDisparo.HUNDIDO, batalla.getRondas().get(2).getDisparoMaquina().getResultado());
    }

    @Test
    void bajasSeRegistranEnOrdenCronologicoSinDuplicarseYDevuelvenUnaCopia() throws Exception {
        Batalla batalla = crearBatallaEnCombate(5);
        Barco primero = batalla.getFlotaMaquina().getBarcos().get(0);
        Barco segundo = batalla.getFlotaMaquina().getBarcos().get(1);
        assertTrue(batalla.getMaquina().listarBajas().isEmpty());
        assertEquals(0, batalla.getMaquina().contarBajas());
        assertEquals(0, batalla.getJugador().contarBajas());

        batalla.generarDisparo(2, 2);
        assertEquals(List.of(segundo), batalla.getFlotaMaquina().listarBajas());
        assertEquals(List.of(segundo), batalla.getBajasEnemigas());
        assertEquals(1, batalla.getMaquina().contarBajas());
        assertEquals(1, batalla.getFlotaMaquina().contarBajas());
        batalla.getMaquina().registrarBaja(segundo);
        assertEquals(1, batalla.getMaquina().contarBajas());
        batalla.getBajasEnemigas().clear();
        assertEquals(List.of(segundo), batalla.getBajasEnemigas());

        batalla.generarDisparo(2, 0);
        assertEquals(List.of(segundo, primero), batalla.getBajasEnemigas());
        assertEquals(2, batalla.getMaquina().contarBajas());
        assertTrue(batalla.fueGanada());
        assertNull(batalla.getRondas().get(1).getDisparoMaquina());
    }

    @Test
    void flotaSoloRegistraBarcosPropiosHundidos() throws Exception {
        Batalla batalla = crearBatallaEnCombate(5);
        Flota flota = batalla.getFlotaMaquina();
        Barco propioSano = flota.getBarcos().get(0);
        Barco ajeno = batalla.getFlotaJugador().getBarcos().get(0);
        assertThrows(BatallaNavalException.class, () -> flota.registrarBaja(propioSano));
        assertThrows(BatallaNavalException.class, () -> flota.registrarBaja(ajeno));
        assertEquals(0, flota.contarBajas());
    }

    @Test
    void disparoRepetidoNoConsumeLaRondaNiGeneraRespuestaDeLaMaquina() throws Exception {
        Batalla batalla = crearBatallaEnCombate(5);
        batalla.generarDisparo(1, 1);
        assertThrows(BatallaNavalException.class, () -> batalla.generarDisparo(1, 1));
        assertEquals(1, batalla.getRondas().size());
        assertEquals(2, batalla.getNumeroProximaRonda());
        assertEquals(1, contarDisparados(batalla.getOceanoJugador()));
        assertEquals(1, contarDisparados(batalla.getOceanoMaquina()));
        batalla.generarDisparo(1, 2);
        assertEquals(2, batalla.getRondas().size());
    }

    @Test
    void estadisticasExcluyenBatallasEnPreparacionYCombate() throws Exception {
        SistemaBatallas sistema = new SistemaBatallas();
        assertEquals(0, sistema.contarBatallasGanadas());
        assertEquals(0, sistema.contarBatallasPerdidas());
        assertEquals(0.0, sistema.calcularExitoCampania());
        sistema.registrarBatalla(crearBatalla());
        sistema.registrarBatalla(crearBatallaEnCombate(5));
        assertEquals(0.0, sistema.calcularExitoCampania());
        Precargas.cargar(sistema);
        assertEquals(6, sistema.listarBatallas().size());
        assertEquals(1, sistema.contarBatallasGanadas());
        assertEquals(1, sistema.contarBatallasPerdidas());
        assertEquals(50.0, sistema.calcularExitoCampania());
    }

    @Test
    void noIniciaHastaCompletarLaFlotaHumana() throws Exception {
        Batalla batalla = crearBatalla();
        assertThrows(BatallaNavalException.class, batalla::iniciarCombate);
        assertEquals(Batalla.Estados.EN_PREPARACION, batalla.getEstado());
        assertTrue(batalla.getOceanoMaquina().listarCuadrantesOcupados().isEmpty());

        Barco primero = batalla.getFlotaJugador().getBarcos().get(0);
        batalla.colocarBarco(primero, batalla.getOceanoJugador().getCuadrante(5, 0), Orientacion.HORIZONTAL);
        assertThrows(BatallaNavalException.class, batalla::iniciarCombate);
        assertEquals(Batalla.Estados.EN_PREPARACION, batalla.getEstado());
        assertSame(primero, batalla.getOceanoJugador().getCuadrante(5, 0).getBarco());
        assertTrue(batalla.getOceanoMaquina().listarCuadrantesOcupados().isEmpty());
        assertTrue(batalla.getRondas().isEmpty());
    }

    @Test
    void iniciarCombateDespliegaLaMaquinaYPermiteJugarLaPrimeraRonda() throws Exception {
        Batalla batalla = crearBatalla();
        ubicarFlotaHumana(batalla);
        List<Cuadrante> propios = batalla.getOceanoJugador().listarCuadrantesOcupados();
        batalla.iniciarCombate();
        assertEquals(Batalla.Estados.EN_COMBATE, batalla.getEstado());
        assertTrue(batalla.getFlotaJugador().estaCompletamenteUbicada());
        assertTrue(batalla.getFlotaMaquina().estaCompletamenteUbicada());
        assertEquals(propios, batalla.getOceanoJugador().listarCuadrantesOcupados());
        assertTrue(batalla.getRondas().isEmpty());
        assertEquals(1, batalla.getNumeroProximaRonda());
        assertEquals(0, contarDisparados(batalla.getOceanoMaquina()));
        assertEquals(0, contarDisparados(batalla.getOceanoJugador()));

        Cuadrante agua = buscarAgua(batalla.getOceanoMaquina());
        batalla.generarDisparo(agua.getFila(), agua.getColumna());
        assertEquals(1, batalla.getRondas().size());
        assertEquals(ResultadoDisparo.AGUA, batalla.getRondas().get(0).getDisparoJugador().getResultado());
        assertNotNull(batalla.getRondas().get(0).getDisparoMaquina());
        assertEquals(2, batalla.getNumeroProximaRonda());
    }

    @Test
    void inicioConFlotaMaquinaYaColocadaConservaLasPosicionesYRechazaReiniciar() throws Exception {
        Batalla batalla = crearBatalla();
        ubicarFlotaHumana(batalla);
        for (int i = 0; i < 2; i++) {
            batalla.getOceanoMaquina().colocarBarco(batalla.getFlotaMaquina().getBarcos().get(i),
                    batalla.getOceanoMaquina().getCuadrante(2, i * 2), Orientacion.HORIZONTAL);
        }
        List<Cuadrante> posiciones = batalla.getOceanoMaquina().listarCuadrantesOcupados();
        batalla.iniciarCombate();
        assertEquals(Batalla.Estados.EN_COMBATE, batalla.getEstado());
        assertEquals(posiciones, batalla.getOceanoMaquina().listarCuadrantesOcupados());
        BatallaNavalException error = assertThrows(BatallaNavalException.class, batalla::iniciarCombate);
        assertTrue(error.getMessage().contains("ya esta en combate"));
        assertEquals(posiciones, batalla.getOceanoMaquina().listarCuadrantesOcupados());
        assertTrue(batalla.getRondas().isEmpty());
    }

    @Test
    void falloDeDespliegueConservaPreparacionYLosBarcosYaColocados() throws Exception {
        Batalla batalla = crearBatalla();
        ubicarFlotaHumana(batalla);
        Barco primero = batalla.getFlotaMaquina().getBarcos().get(0);
        batalla.getOceanoMaquina().colocarBarco(primero,
                batalla.getOceanoMaquina().getCuadrante(2, 0), Orientacion.HORIZONTAL);
        List<Cuadrante> propios = batalla.getOceanoJugador().listarCuadrantesOcupados();
        List<Cuadrante> enemigos = batalla.getOceanoMaquina().listarCuadrantesOcupados();
        assertThrows(BatallaNavalException.class, batalla::iniciarCombate);
        assertEquals(Batalla.Estados.EN_PREPARACION, batalla.getEstado());
        assertEquals(propios, batalla.getOceanoJugador().listarCuadrantesOcupados());
        assertEquals(enemigos, batalla.getOceanoMaquina().listarCuadrantesOcupados());
        assertSame(primero, batalla.getOceanoMaquina().getCuadrante(2, 0).getBarco());
        assertFalse(batalla.getFlotaMaquina().getBarcos().get(1).estaUbicado());
        assertTrue(batalla.getRondas().isEmpty());
    }

    @Test
    void dosSolicitudesSimultaneasDeInicioSoloInicianUnaVez() throws Exception {
        Batalla batalla = crearBatalla();
        ubicarFlotaHumana(batalla);
        CountDownLatch inicio = new CountDownLatch(1);
        var ejecutor = Executors.newFixedThreadPool(2);
        try {
            var primero = ejecutor.submit(() -> iniciarAlMismoTiempo(batalla, inicio));
            var segundo = ejecutor.submit(() -> iniciarAlMismoTiempo(batalla, inicio));
            inicio.countDown();
            assertNotEquals(primero.get(5, TimeUnit.SECONDS), segundo.get(5, TimeUnit.SECONDS));
            assertEquals(Batalla.Estados.EN_COMBATE, batalla.getEstado());
            assertTrue(batalla.getFlotaJugador().estaCompletamenteUbicada());
            assertTrue(batalla.getFlotaMaquina().estaCompletamenteUbicada());
            assertEquals(2, batalla.getOceanoMaquina().listarCuadrantesOcupados().size());
            assertTrue(batalla.getRondas().isEmpty());
        } finally {
            inicio.countDown();
            ejecutor.shutdownNow();
        }
    }

    @Test
    void combateIniciadoSeNotificaUnaVezConLasFlotasYaColocadas() throws Exception {
        Batalla batalla = crearBatalla();
        List<Batalla.Estados> estadosAlAvisar = new ArrayList<>();
        List<Boolean> flotasListasAlAvisar = new ArrayList<>();
        batalla.agregarObservador((evento, origen) -> {
            if (evento == Batalla.Eventos.COMBATE_INICIADO) {
                estadosAlAvisar.add(batalla.getEstado());
                flotasListasAlAvisar.add(batalla.getFlotaJugador().estaCompletamenteUbicada()
                        && batalla.getFlotaMaquina().estaCompletamenteUbicada());
            }
        });
        assertThrows(BatallaNavalException.class, batalla::iniciarCombate);
        assertTrue(estadosAlAvisar.isEmpty());
        ubicarFlotaHumana(batalla);
        batalla.iniciarCombate();
        assertEquals(List.of(Batalla.Estados.EN_COMBATE), estadosAlAvisar);
        assertEquals(List.of(true), flotasListasAlAvisar);
        assertThrows(BatallaNavalException.class, batalla::iniciarCombate);
        assertEquals(1, estadosAlAvisar.size());
    }

    @Test
    void terminarAvisaConLaRondaFinalGuardadaYNoVuelveAAnunciarElInicio() throws Exception {
        Batalla batalla = crearBatallaEnCombate(5);
        List<Object> eventos = new ArrayList<>();
        List<Integer> rondasAlTerminar = new ArrayList<>();
        batalla.agregarObservador((evento, origen) -> {
            eventos.add(evento);
            if (evento == Batalla.Eventos.BATALLA_TERMINADA) {
                rondasAlTerminar.add(batalla.getRondas().size());
            }
        });
        batalla.generarDisparo(2, 0);
        batalla.generarDisparo(2, 2);
        assertEquals(List.of(Batalla.Eventos.RONDA_COMPLETADA, Batalla.Eventos.RONDA_COMPLETADA,
                Batalla.Eventos.BATALLA_TERMINADA), eventos);
        assertEquals(List.of(2), rondasAlTerminar);
        assertTrue(batalla.fueGanada());
    }

    private boolean iniciarAlMismoTiempo(Batalla batalla, CountDownLatch inicio) throws InterruptedException {
        inicio.await();
        try {
            batalla.iniciarCombate();
            return true;
        } catch (BatallaNavalException error) {
            assertTrue(error.getMessage().contains("ya esta en combate"));
            return false;
        }
    }

    private void ubicarFlotaHumana(Batalla batalla) throws BatallaNavalException {
        for (int i = 0; i < 2; i++) {
            batalla.colocarBarco(batalla.getFlotaJugador().getBarcos().get(i),
                    batalla.getOceanoJugador().getCuadrante(5, i * 2), Orientacion.HORIZONTAL);
        }
    }

    private Cuadrante buscarAgua(Oceano oceano) throws BatallaNavalException {
        for (int fila = 0; fila < oceano.getFilas(); fila++) {
            for (int columna = 0; columna < oceano.getColumnas(); columna++) {
                Cuadrante cuadrante = oceano.getCuadrante(fila, columna);
                if (!cuadrante.estaOcupado()) {
                    return cuadrante;
                }
            }
        }
        throw new BatallaNavalException("El escenario de prueba debe tener agua.");
    }

    private Batalla crearBatalla() throws BatallaNavalException {
        TipoBarco tipo = new TipoBarco("Destructor", 1);
        Flota propia = new Flota(List.of(new Barco(tipo), new Barco(tipo)));
        Jugador jugador = new Jugador(propia, new Oceano(6, 6));
        Maquina maquina = new Maquina(propia.crearFlotaEquivalente(), new Oceano(6, 6),
                new TacticaSecuencial());
        return new Batalla("Prueba", jugador, maquina);
    }

    private Batalla crearBatallaEnCombate(int filaJugador) throws BatallaNavalException {
        Batalla batalla = crearBatalla();
        for (int i = 0; i < 2; i++) {
            batalla.colocarBarco(batalla.getFlotaJugador().getBarcos().get(i),
                    batalla.getOceanoJugador().getCuadrante(filaJugador, i * 2), Orientacion.HORIZONTAL);
            batalla.getOceanoMaquina().colocarBarco(batalla.getFlotaMaquina().getBarcos().get(i),
                    batalla.getOceanoMaquina().getCuadrante(2, i * 2), Orientacion.HORIZONTAL);
        }
        batalla.iniciarCombate();
        return batalla;
    }

    private int contarDisparados(Oceano oceano) throws BatallaNavalException {
        int cantidad = 0;
        for (int fila = 0; fila < oceano.getFilas(); fila++) {
            for (int columna = 0; columna < oceano.getColumnas(); columna++) {
                if (oceano.getCuadrante(fila, columna).isDisparado()) {
                    cantidad++;
                }
            }
        }
        return cantidad;
    }
}
