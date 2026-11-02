package uy.edu.ort.battleshipdemo.modelo;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

class DespliegueMaquinaTest {

    @Test
    void consultaDeFlotaSoloEsVerdaderaCuandoTodosSusBarcosEstanUbicados() throws Exception {
        Flota flota = crearFlota(3, 2);
        Oceano oceano = new Oceano(6, 6);
        assertFalse(flota.estaCompletamenteUbicada());
        oceano.colocarBarco(flota.getBarcos().get(0), oceano.getCuadrante(0, 0), Orientacion.HORIZONTAL);
        assertFalse(flota.estaCompletamenteUbicada());
        oceano.colocarBarco(flota.getBarcos().get(1), oceano.getCuadrante(3, 0), Orientacion.VERTICAL);
        assertTrue(flota.estaCompletamenteUbicada());
    }

    @Test
    void validacionInformaQueFlotaEstaPendienteYNoIniciaElCombate() throws Exception {
        Batalla batalla = crearBatalla();
        BatallaNavalException faltaJugador = assertThrows(BatallaNavalException.class,
                batalla::validarFlotasColocadas);
        assertTrue(faltaJugador.getMessage().contains("jugador"));
        ubicarFlotaHumana(batalla);
        BatallaNavalException faltaMaquina = assertThrows(BatallaNavalException.class,
                batalla::validarFlotasColocadas);
        assertTrue(faltaMaquina.getMessage().contains("maquina"));
        batalla.colocarFlotaMaquina();
        assertDoesNotThrow(batalla::validarFlotasColocadas);
        assertEquals(Batalla.Estados.EN_PREPARACION, batalla.getEstado());
        assertTrue(batalla.getRondas().isEmpty());
        assertTrue(batalla.getBajasAmigas().isEmpty());
        assertTrue(batalla.getBajasEnemigas().isEmpty());
    }

    @Test
    void despliegueConservaLosObjetosRealesYNoModificaLaFlotaHumana() throws Exception {
        Batalla batalla = crearBatalla();
        Maquina maquina = batalla.getMaquina();
        Flota flotaOriginal = maquina.getFlota();
        Oceano oceanoOriginal = maquina.getOceano();
        List<Barco> barcosOriginales = flotaOriginal.getBarcos();
        List<Cuadrante> cuadrantesOriginales = todosLosCuadrantes(oceanoOriginal);
        ubicarFlotaHumana(batalla);
        List<Cuadrante> propios = batalla.getOceanoJugador().listarCuadrantesOcupados();

        batalla.colocarFlotaMaquina();

        assertSame(flotaOriginal, maquina.getFlota());
        assertSame(oceanoOriginal, maquina.getOceano());
        assertEquals(barcosOriginales, maquina.getFlota().getBarcos());
        assertEquals(cuadrantesOriginales, todosLosCuadrantes(oceanoOriginal));
        assertEquals(propios, batalla.getOceanoJugador().listarCuadrantesOcupados());
        verificarDistribucion(maquina);
        assertEquals(Batalla.Estados.EN_PREPARACION, batalla.getEstado());
    }

    @Test
    void despliegaEnTodasLasDimensionesPermitidasIncluidosTablerosRectangulares() throws Exception {
        for (int filas = 6; filas <= 12; filas++) {
            for (int columnas = 6; columnas <= 12; columnas++) {
                Maquina maquina = new Maquina(crearFlota(3, 2, 1), new Oceano(filas, columnas),
                        new TacticaSecuencial());
                maquina.colocarFlota();
                verificarDistribucion(maquina);
            }
        }
    }

    @Test
    void despliegaBarcosDeTodosLosTamaniosSinCambiarElOrdenDeLaFlota() throws Exception {
        Maquina maquina = new Maquina(crearFlota(1, 3, 2, 5, 4), new Oceano(8, 8),
                new TacticaSecuencial());
        List<Barco> originales = maquina.getFlota().getBarcos();
        maquina.colocarFlota();
        assertEquals(originales, maquina.getFlota().getBarcos());
        verificarDistribucion(maquina);
    }

    @Test
    void unaFlotaImposibleAgotaLosIntentosSinDejarColocacionesParciales() throws BatallaNavalException {
        Maquina maquina = new Maquina(crearFlota(1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
                new Oceano(6, 6), new TacticaSecuencial());
        // Con separacion incluso diagonal solo entran nueve barcos de una celda en 6 x 6.
        assertTimeoutPreemptively(Duration.ofSeconds(10),
                () -> assertThrows(BatallaNavalException.class, maquina::colocarFlota));
        assertTrue(maquina.getOceano().listarCuadrantesOcupados().isEmpty());
        for (Barco barco : maquina.getFlota().getBarcos()) {
            assertFalse(barco.estaUbicado());
            assertNull(barco.getOrientacion());
        }
    }

    @Test
    void unDespliegueParcialOCompletoNoSeSobrescribe() throws Exception {
        Maquina parcial = new Maquina(crearFlota(3, 2), new Oceano(6, 6), new TacticaSecuencial());
        parcial.getOceano().colocarBarco(parcial.getFlota().getBarcos().get(0),
                parcial.getOceano().getCuadrante(0, 0), Orientacion.HORIZONTAL);
        List<Cuadrante> antes = parcial.getOceano().listarCuadrantesOcupados();
        assertThrows(BatallaNavalException.class, parcial::colocarFlota);
        assertEquals(antes, parcial.getOceano().listarCuadrantesOcupados());
        assertFalse(parcial.getFlota().getBarcos().get(1).estaUbicado());

        Maquina completa = new Maquina(crearFlota(3, 2), new Oceano(6, 6), new TacticaSecuencial());
        completa.colocarFlota();
        List<Cuadrante> ocupados = completa.getOceano().listarCuadrantesOcupados();
        assertThrows(BatallaNavalException.class, completa::colocarFlota);
        assertEquals(ocupados, completa.getOceano().listarCuadrantesOcupados());
        verificarDistribucion(completa);
    }

    @Test
    void barcosInvalidosNoModificanElOceanoNiLaFlota() throws BatallaNavalException {
        for (Barco invalido : List.of(new Barco(null), new Barco(new TipoBarco("Invalido", 0)),
                new Barco(new TipoBarco("Demasiado largo", 13)))) {
            Maquina maquina = new Maquina(new Flota(List.of(invalido)), new Oceano(6, 6),
                    new TacticaSecuencial());
            assertThrows(BatallaNavalException.class, maquina::colocarFlota);
            assertFalse(invalido.estaUbicado());
            assertTrue(maquina.getOceano().listarCuadrantesOcupados().isEmpty());
        }
    }

    @Test
    void combateYTerminadaRechazanElDespliegueDeLaMaquina() throws BatallaNavalException {
        Batalla batalla = crearBatalla();
        for (EstadoBatalla estado : List.of(new EstadoEnCombate(batalla), new EstadoTerminada(batalla))) {
            batalla.setEstado(estado);
            assertThrows(BatallaNavalException.class, batalla::colocarFlotaMaquina);
            assertFalse(batalla.getFlotaMaquina().estaCompletamenteUbicada());
            assertTrue(batalla.getOceanoMaquina().listarCuadrantesOcupados().isEmpty());
        }
    }

    @Test
    void dosDesplieguesSimultaneosNoDuplicanNiReubicanLosBarcos() throws Exception {
        Batalla batalla = crearBatalla();
        CountDownLatch inicio = new CountDownLatch(1);
        var ejecutor = Executors.newFixedThreadPool(2);
        try {
            var primero = ejecutor.submit(() -> desplegar(batalla, inicio));
            var segundo = ejecutor.submit(() -> desplegar(batalla, inicio));
            inicio.countDown();
            assertNotEquals(primero.get(5, TimeUnit.SECONDS), segundo.get(5, TimeUnit.SECONDS));
            verificarDistribucion(batalla.getMaquina());
        } finally {
            inicio.countDown();
            ejecutor.shutdownNow();
        }
    }

    private boolean desplegar(Batalla batalla, CountDownLatch inicio) throws InterruptedException {
        inicio.await();
        try {
            batalla.colocarFlotaMaquina();
            return true;
        } catch (BatallaNavalException esperado) {
            return false;
        }
    }

    private Flota crearFlota(int... tamanios) throws BatallaNavalException {
        List<Barco> barcos = new ArrayList<>();
        for (int tamanio : tamanios) {
            barcos.add(new Barco(new TipoBarco("Prueba", tamanio)));
        }
        return new Flota(barcos);
    }

    private Batalla crearBatalla() throws BatallaNavalException {
        Flota propia = crearFlota(3, 2);
        Jugador jugador = new Jugador(propia, new Oceano(6, 6));
        Maquina maquina = new Maquina(propia.crearFlotaEquivalente(), new Oceano(6, 6),
                new TacticaSecuencial());
        return new Batalla("Prueba", jugador, maquina);
    }

    private void ubicarFlotaHumana(Batalla batalla) throws BatallaNavalException {
        batalla.colocarBarco(batalla.getFlotaJugador().getBarcos().get(0),
                batalla.getOceanoJugador().getCuadrante(0, 0), Orientacion.HORIZONTAL);
        batalla.colocarBarco(batalla.getFlotaJugador().getBarcos().get(1),
                batalla.getOceanoJugador().getCuadrante(3, 0), Orientacion.VERTICAL);
    }

    private List<Cuadrante> todosLosCuadrantes(Oceano oceano) {
        List<Cuadrante> cuadrantes = new ArrayList<>();
        for (int fila = 0; fila < oceano.getFilas(); fila++) {
            for (int columna = 0; columna < oceano.getColumnas(); columna++) {
                cuadrantes.add(oceano.getCuadrante(fila, columna));
            }
        }
        return cuadrantes;
    }

    private void verificarDistribucion(Maquina maquina) {
        Oceano oceano = maquina.getOceano();
        assertTrue(maquina.getFlota().estaCompletamenteUbicada());
        Set<Cuadrante> ocupados = new HashSet<>();
        for (Barco barco : maquina.getFlota().getBarcos()) {
            assertEquals(barco.getTamanio(), barco.getCuadrantes().size());
            assertNotNull(barco.getOrientacion());
            Cuadrante proa = barco.getCuadrantes().get(0);
            for (int i = 0; i < barco.getTamanio(); i++) {
                int fila = proa.getFila();
                int columna = proa.getColumna();
                if (barco.getOrientacion() == Orientacion.VERTICAL) {
                    fila += i;
                } else {
                    columna += i;
                }
                Cuadrante cuadrante = barco.getCuadrantes().get(i);
                assertSame(oceano.getCuadrante(fila, columna), cuadrante);
                assertSame(barco, cuadrante.getBarco());
                assertTrue(ocupados.add(cuadrante));
                for (int df = -1; df <= 1; df++) {
                    for (int dc = -1; dc <= 1; dc++) {
                        if (oceano.estaDentro(fila + df, columna + dc)) {
                            Barco vecino = oceano.getCuadrante(fila + df, columna + dc).getBarco();
                            assertTrue(vecino == null || vecino == barco, "Hay barcos en contacto");
                        }
                    }
                }
            }
        }
        assertEquals(ocupados, new HashSet<>(oceano.listarCuadrantesOcupados()));
        for (Cuadrante cuadrante : todosLosCuadrantes(oceano)) {
            assertEquals(ocupados.contains(cuadrante), cuadrante.estaOcupado());
            assertFalse(cuadrante.isDisparado());
        }
    }
}
