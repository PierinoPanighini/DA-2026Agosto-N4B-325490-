package uy.edu.ort.battleshipdemo.modelo;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;
import uy.edu.ort.battleshipdemo.modelo.Barco.Orientacion;
import uy.edu.ort.battleshipdemo.modelo.Disparo.ResultadoDisparo;

class PrecargasTest {

    private SistemaBatallas cargar() throws BatallaNavalException {
        SistemaBatallas sistema = new SistemaBatallas();
        Precargas.cargar(sistema);
        return sistema;
    }

    @Test
    void cargaCatalogosYCuatroBatallasConParticipantesIndependientes() throws Exception {
        SistemaBatallas sistema = cargar();
        Map<String, Integer> tipos = new HashMap<>();
        for (TipoBarco tipo : sistema.listarTiposBarco()) {
            tipos.put(tipo.getNombre(), tipo.getTamanio());
        }
        assertEquals(Map.of("Portaaviones", 5, "Acorazado", 4, "Crucero", 3,
                "Submarino", 2, "Destructor", 1), tipos);
        assertEquals(3, sistema.listarTacticas().size());
        Set<Class<?>> tacticas = new HashSet<>();
        for (Tactica tactica : sistema.listarTacticas()) {
            tacticas.add(tactica.getClass());
        }
        assertEquals(Set.of(TacticaSecuencial.class, TacticaEspiral.class, TacticaCazaAtaque.class), tacticas);
        assertEquals(4, sistema.listarBatallas().size());
        Set<Batalla.Estados> estados = EnumSet.noneOf(Batalla.Estados.class);
        Set<Integer> ids = new HashSet<>();
        Set<String> lugares = new HashSet<>();
        Set<Tactica> instancias = new HashSet<>();
        for (Batalla batalla : sistema.listarBatallas()) {
            estados.add(batalla.getEstado());
            assertTrue(ids.add(batalla.getId()));
            assertTrue(lugares.add(batalla.getLugar()));
            assertTrue(instancias.add(batalla.getTactica()));
            assertFalse(sistema.listarTacticas().contains(batalla.getTactica()));
            assertSame(batalla, sistema.buscarBatalla(batalla.getId()));
            assertNotSame(batalla.getFlotaJugador(), batalla.getFlotaMaquina());
            assertNotSame(batalla.getOceanoJugador(), batalla.getOceanoMaquina());
            List<Barco> propios = batalla.getFlotaJugador().getBarcos();
            List<Barco> enemigos = batalla.getFlotaMaquina().getBarcos();
            assertTrue(propios.size() >= 2);
            assertEquals(propios.size(), enemigos.size());
            Set<Integer> tamanios = new HashSet<>();
            for (int i = 0; i < propios.size(); i++) {
                assertNotSame(propios.get(i), enemigos.get(i));
                assertSame(propios.get(i).getTipoBarco(), enemigos.get(i).getTipoBarco());
                tamanios.add(propios.get(i).getTamanio());
            }
            assertTrue(tamanios.size() >= 2);
        }
        assertEquals(EnumSet.allOf(Batalla.Estados.class), estados);
        assertEquals(1, sistema.contarBatallasGanadas());
        assertEquals(1, sistema.contarBatallasPerdidas());
        assertEquals(50.0, sistema.calcularExitoCampania());
    }

    @Test
    void posicionesYEstadosCoincidenConLasReglasDelJuego() throws Exception {
        for (Batalla batalla : cargar().listarBatallas()) {
            boolean preparacion = batalla.getEstado() == Batalla.Estados.EN_PREPARACION;
            verificarFlota(batalla.getFlotaJugador(), batalla.getOceanoJugador(), preparacion);
            verificarFlota(batalla.getFlotaMaquina(), batalla.getOceanoMaquina(), preparacion);
            boolean propiaHundida = batalla.getFlotaJugador().getBarcos().stream().allMatch(Barco::estaHundido);
            boolean enemigaHundida = batalla.getFlotaMaquina().getBarcos().stream().allMatch(Barco::estaHundido);
            if (preparacion) {
                assertTrue(batalla.getRondas().isEmpty());
            } else {
                assertFalse(batalla.getRondas().isEmpty());
            }
            assertEquals(batalla.fuePerdida(), propiaHundida);
            assertEquals(batalla.fueGanada(), enemigaHundida);
        }
    }

    private void verificarFlota(Flota flota, Oceano oceano, boolean preparacion) throws BatallaNavalException {
        assertTrue(oceano.getFilas() >= 6 && oceano.getFilas() <= 12);
        assertTrue(oceano.getColumnas() >= 6 && oceano.getColumnas() <= 12);
        int partes = 0;
        Set<Cuadrante> ocupados = new HashSet<>();
        for (Barco barco : flota.getBarcos()) {
            partes += barco.getTamanio();
            assertEquals(!preparacion, barco.estaUbicado());
            if (preparacion) continue;
            assertEquals(barco.getTamanio(), barco.getCuadrantes().size());
            Cuadrante proa = barco.getProa();
            for (int i = 0; i < barco.getCuadrantes().size(); i++) {
                Cuadrante cuadrante = barco.getCuadrantes().get(i);
                int fila = proa.getFila() + (barco.getOrientacion() == Orientacion.VERTICAL ? i : 0);
                int columna = proa.getColumna() + (barco.getOrientacion() == Orientacion.HORIZONTAL ? i : 0);
                assertSame(oceano.getCuadrante(fila, columna), cuadrante);
                assertSame(barco, cuadrante.getBarco());
                assertTrue(ocupados.add(cuadrante));
                for (int df = -1; df <= 1; df++) {
                    for (int dc = -1; dc <= 1; dc++) {
                        if (oceano.estaDentro(fila + df, columna + dc)) {
                            Barco vecino = oceano.getCuadrante(fila + df, columna + dc).getBarco();
                            assertTrue(vecino == null || vecino == barco, "Los barcos no deben tocarse");
                        }
                    }
                }
            }
        }
        assertTrue(partes * 4 < oceano.getFilas() * oceano.getColumnas());
        for (int f = 0; f < oceano.getFilas(); f++) {
            for (int c = 0; c < oceano.getColumnas(); c++) {
                Cuadrante cuadrante = oceano.getCuadrante(f, c);
                assertEquals(ocupados.contains(cuadrante), cuadrante.estaOcupado());
                if (preparacion) assertFalse(cuadrante.isDisparado());
            }
        }
    }

    @Test
    void disparosRondasYBajasFormanUnHistorialCoherente() throws Exception {
        for (Batalla batalla : cargar().listarBatallas()) {
            Set<Cuadrante> tirosJugador = new HashSet<>();
            Set<Cuadrante> tirosMaquina = new HashSet<>();
            Map<Barco, Integer> impactosEnemigos = new HashMap<>();
            Map<Barco, Integer> impactosPropios = new HashMap<>();
            List<Barco> bajasEnemigas = new ArrayList<>();
            List<Barco> bajasAmigas = new ArrayList<>();
            for (int i = 0; i < batalla.getRondas().size(); i++) {
                Ronda ronda = batalla.getRondas().get(i);
                assertEquals(i + 1, ronda.getNumero());
                verificarDisparo(ronda.getDisparoJugador(), batalla.getOceanoMaquina(), tirosJugador,
                        impactosEnemigos, bajasEnemigas);
                if (ronda.getDisparoMaquina() == null) {
                    assertEquals(Batalla.Estados.TERMINADA, batalla.getEstado());
                    assertTrue(batalla.fueGanada());
                    assertEquals(batalla.getRondas().size() - 1, i);
                    assertTrue(batalla.getFlotaMaquina().getBarcos().stream()
                            .allMatch(b -> impactosEnemigos.getOrDefault(b, 0) == b.getTamanio()));
                } else {
                    verificarDisparo(ronda.getDisparoMaquina(), batalla.getOceanoJugador(), tirosMaquina,
                            impactosPropios, bajasAmigas);
                }
            }
            assertEquals(bajasEnemigas, batalla.getBajasEnemigas());
            assertEquals(bajasAmigas, batalla.getBajasAmigas());
            assertEquals(bajasEnemigas.size(), batalla.getMaquina().contarBajas());
            assertEquals(bajasAmigas.size(), batalla.getJugador().contarBajas());
            verificarMarcas(batalla.getOceanoMaquina(), tirosJugador);
            verificarMarcas(batalla.getOceanoJugador(), tirosMaquina);
            for (Barco barco : batalla.getFlotaJugador().getBarcos()) {
                assertEquals(barco.getTamanio() - impactosPropios.getOrDefault(barco, 0), barco.partesSanas());
            }
            for (Barco barco : batalla.getFlotaMaquina().getBarcos()) {
                assertEquals(barco.getTamanio() - impactosEnemigos.getOrDefault(barco, 0), barco.partesSanas());
            }
        }
    }

    private void verificarDisparo(Disparo disparo, Oceano oceano, Set<Cuadrante> tiros,
            Map<Barco, Integer> impactos, List<Barco> bajas) throws BatallaNavalException {
        assertNotNull(disparo);
        Cuadrante cuadrante = disparo.getCuadrante();
        assertSame(oceano.getCuadrante(cuadrante.getFila(), cuadrante.getColumna()), cuadrante);
        assertTrue(tiros.add(cuadrante), "No debe haber disparos repetidos");
        ResultadoDisparo esperado = ResultadoDisparo.AGUA;
        if (cuadrante.estaOcupado()) {
            Barco barco = cuadrante.getBarco();
            int cantidad = impactos.getOrDefault(barco, 0) + 1;
            impactos.put(barco, cantidad);
            esperado = cantidad == barco.getTamanio() ? ResultadoDisparo.HUNDIDO : ResultadoDisparo.IMPACTO;
            if (esperado == ResultadoDisparo.HUNDIDO) bajas.add(barco);
        }
        assertEquals(esperado, disparo.getResultado());
    }

    private void verificarMarcas(Oceano oceano, Set<Cuadrante> tiros) throws BatallaNavalException {
        for (int f = 0; f < oceano.getFilas(); f++) {
            for (int c = 0; c < oceano.getColumnas(); c++) {
                Cuadrante cuadrante = oceano.getCuadrante(f, c);
                assertEquals(tiros.contains(cuadrante), cuadrante.isDisparado());
            }
        }
    }

    @Test
    void consultaDeOcupacionCoincideConLosBarcosPosicionadosYDevuelveUnaListaNueva() throws Exception {
        for (Batalla batalla : cargar().listarBatallas()) {
            verificarConsultaDeOcupacion(batalla.getFlotaJugador(), batalla.getOceanoJugador());
            verificarConsultaDeOcupacion(batalla.getFlotaMaquina(), batalla.getOceanoMaquina());
        }
    }

    private void verificarConsultaDeOcupacion(Flota flota, Oceano oceano) throws BatallaNavalException {
        Set<Cuadrante> esperados = new HashSet<>();
        for (Barco barco : flota.getBarcos()) {
            esperados.addAll(barco.getCuadrantes());
        }
        List<Cuadrante> ocupados = oceano.listarCuadrantesOcupados();
        assertEquals(esperados.size(), ocupados.size());
        assertEquals(esperados, new HashSet<>(ocupados));
        for (Cuadrante cuadrante : ocupados) {
            assertSame(oceano.getCuadrante(cuadrante.getFila(), cuadrante.getColumna()), cuadrante);
        }
        ocupados.clear();
        assertEquals(esperados.size(), oceano.listarCuadrantesOcupados().size());
    }

    @Test
    void constructorDeFlotaCopiaLaListaYLaEquivalenteNoComparteBarcos() throws Exception {
        assertThrows(BatallaNavalException.class, () -> new Flota(List.of()));
        assertThrows(BatallaNavalException.class, () -> new Flota(null));
        List<Barco> barcosNulos = new ArrayList<>();
        barcosNulos.add(null);
        assertThrows(BatallaNavalException.class, () -> new Flota(barcosNulos));
        Barco barco = new Barco(new TipoBarco("Submarino", 2));
        List<Barco> entrada = new ArrayList<>(List.of(barco));
        Flota original = new Flota(entrada);
        assertThrows(BatallaNavalException.class, () -> original.agregarBarco(null));
        entrada.clear();
        assertEquals(1, original.getBarcos().size());
        Oceano oceano = new Oceano(6, 6);
        barco.ubicar(List.of(oceano.getCuadrante(0, 0), oceano.getCuadrante(0, 1)), Orientacion.HORIZONTAL);
        oceano.getCuadrante(0, 0).recibirDisparo();
        Flota equivalente = original.crearFlotaEquivalente();
        Barco nuevo = equivalente.getBarcos().get(0);
        assertNotSame(original, equivalente);
        assertNotSame(barco, nuevo);
        assertSame(barco.getTipoBarco(), nuevo.getTipoBarco());
        assertFalse(nuevo.estaUbicado());
        assertEquals(2, nuevo.partesSanas());
        assertEquals(1, barco.partesSanas());
    }

    @Test
    void idsSonUnicosAunqueSeCreenBatallasSimultaneamente() throws Exception {
        var ejecutor = Executors.newFixedThreadPool(4);
        try {
            List<Callable<Integer>> tareas = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                tareas.add(() -> {
                    Flota propia = new Flota(List.of(new Barco(new TipoBarco("Destructor", 1))));
                    Jugador jugador = new Jugador(propia, new Oceano(6, 6));
                    Maquina maquina = new Maquina(propia.crearFlotaEquivalente(), new Oceano(6, 6),
                            new TacticaSecuencial());
                    return new Batalla("Prueba", jugador, maquina).getId();
                });
            }
            Set<Integer> ids = new HashSet<>();
            for (var resultado : ejecutor.invokeAll(tareas)) {
                assertTrue(ids.add(resultado.get()));
            }
            assertEquals(100, ids.size());
        } finally {
            ejecutor.shutdownNow();
        }
    }
}
