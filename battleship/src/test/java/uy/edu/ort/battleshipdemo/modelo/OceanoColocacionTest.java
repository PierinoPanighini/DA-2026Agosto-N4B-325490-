package uy.edu.ort.battleshipdemo.modelo;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

class OceanoColocacionTest {

    private Barco crearBarco(int tamanio) {
        return new Barco(new TipoBarco("Prueba", tamanio));
    }

    @Test
    void colocaHorizontalEnElBordeDeUnTableroRectangular() throws Exception {
        Oceano oceano = new Oceano(6, 8);
        Barco barco = crearBarco(3);
        oceano.colocarBarco(barco, oceano.getCuadrante(5, 5), Orientacion.HORIZONTAL);
        assertEquals(List.of(oceano.getCuadrante(5, 5), oceano.getCuadrante(5, 6),
                oceano.getCuadrante(5, 7)), barco.getCuadrantes());
        assertEquals(Orientacion.HORIZONTAL, barco.getOrientacion());
        assertTrue(barco.estaUbicado());
        assertEquals(3, barco.partesSanas());
        for (Cuadrante cuadrante : barco.getCuadrantes()) {
            assertSame(barco, cuadrante.getBarco());
            assertFalse(cuadrante.isDisparado());
        }
    }

    @Test
    void colocaVerticalDesdeLaProaInferiorHastaElBordeSuperior() throws Exception {
        Oceano oceano = new Oceano(6, 8);
        Barco barco = crearBarco(3);
        oceano.colocarBarco(barco, oceano.getCuadrante(3, 7), Orientacion.VERTICAL);
        assertEquals(List.of(oceano.getCuadrante(3, 7), oceano.getCuadrante(4, 7),
                oceano.getCuadrante(5, 7)), barco.getCuadrantes());
        assertEquals(Orientacion.VERTICAL, barco.getOrientacion());
        for (Cuadrante cuadrante : barco.getCuadrantes()) {
            assertSame(barco, cuadrante.getBarco());
        }
    }

    @Test
    void rechazaSalidasDelTableroSinAplicarLasPartesQueEntraban() throws BatallaNavalException {
        for (Orientacion orientacion : Orientacion.values()) {
            Oceano oceano = new Oceano(6, 8);
            Barco barco = crearBarco(3);
            Cuadrante proa = orientacion == Orientacion.HORIZONTAL
                    ? oceano.getCuadrante(2, 6) : oceano.getCuadrante(4, 2);
            List<Barco> antes = ocupacion(oceano);
            assertThrows(BatallaNavalException.class, () -> oceano.colocarBarco(barco, proa, orientacion));
            assertEquals(antes, ocupacion(oceano));
            verificarSinUbicar(barco);
        }
    }

    @Test
    void rechazaLaSuperposicionAunqueOcurraEnLaUltimaParte() throws Exception {
        Oceano oceano = new Oceano(6, 8);
        Barco original = crearBarco(1);
        oceano.colocarBarco(original, oceano.getCuadrante(2, 4), Orientacion.HORIZONTAL);
        List<Barco> antes = ocupacion(oceano);
        Barco nuevo = crearBarco(3);
        assertThrows(BatallaNavalException.class,
                () -> oceano.colocarBarco(nuevo, oceano.getCuadrante(2, 2), Orientacion.HORIZONTAL));
        assertEquals(antes, ocupacion(oceano));
        assertSame(original, oceano.getCuadrante(2, 4).getBarco());
        verificarSinUbicar(nuevo);
    }

    @Test
    void rechazaElContactoEnLasOchoDirecciones() throws Exception {
        int[][] vecinos = {{-1, -1}, {-1, 0}, {-1, 1}, {0, -1}, {0, 1}, {1, -1}, {1, 0}, {1, 1}};
        for (int[] vecino : vecinos) {
            Oceano oceano = new Oceano(6, 8);
            Barco original = crearBarco(1);
            oceano.colocarBarco(original, oceano.getCuadrante(2, 3), Orientacion.HORIZONTAL);
            List<Barco> antes = ocupacion(oceano);
            Barco nuevo = crearBarco(1);
            Cuadrante proa = oceano.getCuadrante(2 + vecino[0], 3 + vecino[1]);
            assertThrows(BatallaNavalException.class,
                    () -> oceano.colocarBarco(nuevo, proa, Orientacion.VERTICAL));
            assertEquals(antes, ocupacion(oceano));
            verificarSinUbicar(nuevo);
        }
    }

    @Test
    void elContactoDeLaUltimaParteTampocoDejaUnaUbicacionParcial() throws Exception {
        Oceano oceano = new Oceano(6, 8);
        oceano.colocarBarco(crearBarco(1), oceano.getCuadrante(2, 3), Orientacion.HORIZONTAL);
        List<Barco> antes = ocupacion(oceano);
        Barco nuevo = crearBarco(3);
        assertThrows(BatallaNavalException.class,
                () -> oceano.colocarBarco(nuevo, oceano.getCuadrante(1, 0), Orientacion.HORIZONTAL));
        assertEquals(antes, ocupacion(oceano));
        verificarSinUbicar(nuevo);
    }

    @Test
    void permiteSeparacionConAguaYPosicionesEnLasCuatroEsquinas() throws Exception {
        Oceano separado = new Oceano(6, 8);
        separado.colocarBarco(crearBarco(1), separado.getCuadrante(3, 3), Orientacion.HORIZONTAL);
        separado.colocarBarco(crearBarco(1), separado.getCuadrante(3, 1), Orientacion.HORIZONTAL);
        separado.colocarBarco(crearBarco(1), separado.getCuadrante(1, 1), Orientacion.VERTICAL);
        assertEquals(3L, ocupacion(separado).stream().filter(Objects::nonNull).count());

        Oceano esquinas = new Oceano(6, 8);
        for (int[] coordenada : new int[][] {{0, 0}, {0, 7}, {5, 0}, {5, 7}}) {
            esquinas.colocarBarco(crearBarco(1), esquinas.getCuadrante(coordenada[0], coordenada[1]),
                    Orientacion.HORIZONTAL);
        }
        assertEquals(4L, ocupacion(esquinas).stream().filter(Objects::nonNull).count());
    }

    @Test
    void rechazaProasAjenasAunqueTenganCoordenadasValidas() throws BatallaNavalException {
        Oceano oceano = new Oceano(6, 8);
        Oceano otro = new Oceano(6, 8);
        Cuadrante[] proas = {otro.getCuadrante(2, 2), new Cuadrante(2, 2),
                new Cuadrante(-1, 0), new Cuadrante(6, 0), new Cuadrante(0, 8)};
        List<Barco> antes = ocupacion(oceano);
        List<Barco> antesOtro = ocupacion(otro);
        for (Cuadrante proa : proas) {
            Barco barco = crearBarco(3);
            assertThrows(BatallaNavalException.class,
                    () -> oceano.colocarBarco(barco, proa, Orientacion.HORIZONTAL));
            verificarSinUbicar(barco);
        }
        assertEquals(antes, ocupacion(oceano));
        assertEquals(antesOtro, ocupacion(otro));
    }

    @Test
    void rechazaSolicitudesIncompletasYBarcosSinTamanioValido() throws BatallaNavalException {
        Oceano oceano = new Oceano(6, 8);
        Cuadrante proa = oceano.getCuadrante(2, 2);
        Barco barco = crearBarco(3);
        List<Barco> antes = ocupacion(oceano);
        assertThrows(BatallaNavalException.class, () -> oceano.colocarBarco(null, proa, Orientacion.HORIZONTAL));
        assertThrows(BatallaNavalException.class, () -> oceano.colocarBarco(barco, null, Orientacion.HORIZONTAL));
        assertThrows(BatallaNavalException.class, () -> oceano.colocarBarco(barco, proa, null));
        for (Barco invalido : List.of(crearBarco(0), crearBarco(-1), new Barco(null))) {
            assertThrows(BatallaNavalException.class,
                    () -> oceano.colocarBarco(invalido, proa, Orientacion.HORIZONTAL));
            verificarSinUbicar(invalido);
        }
        verificarSinUbicar(barco);
        assertEquals(antes, ocupacion(oceano));
    }

    @Test
    void rechazaReubicarUnBarcoYConservaSuUbicacionOriginal() throws Exception {
        Oceano oceano = new Oceano(6, 8);
        Barco barco = crearBarco(2);
        oceano.colocarBarco(barco, oceano.getCuadrante(0, 0), Orientacion.HORIZONTAL);
        List<Barco> antes = ocupacion(oceano);
        List<Cuadrante> ubicacion = new ArrayList<>(barco.getCuadrantes());
        assertThrows(BatallaNavalException.class,
                () -> oceano.colocarBarco(barco, oceano.getCuadrante(3, 4), Orientacion.VERTICAL));
        assertEquals(antes, ocupacion(oceano));
        assertEquals(ubicacion, barco.getCuadrantes());
        assertEquals(Orientacion.HORIZONTAL, barco.getOrientacion());
        Oceano otro = new Oceano(6, 8);
        List<Barco> antesOtro = ocupacion(otro);
        assertThrows(BatallaNavalException.class,
                () -> otro.colocarBarco(barco, otro.getCuadrante(3, 4), Orientacion.VERTICAL));
        assertEquals(antesOtro, ocupacion(otro));
    }

    @Test
    void batallaSoloColocaBarcosDelJugadorEnSuOceano() throws Exception {
        Batalla batalla = crearBatalla();
        Barco propio = batalla.getFlotaJugador().getBarcos().get(0);
        Barco enemigo = batalla.getFlotaMaquina().getBarcos().get(0);
        Oceano oceano = batalla.getOceanoJugador();
        List<Barco> antes = ocupacion(oceano);
        assertThrows(BatallaNavalException.class,
                () -> batalla.colocarBarco(enemigo, oceano.getCuadrante(2, 2), Orientacion.HORIZONTAL));
        assertThrows(BatallaNavalException.class,
                () -> batalla.colocarBarco(crearBarco(3), oceano.getCuadrante(2, 2), Orientacion.HORIZONTAL));
        assertThrows(BatallaNavalException.class, () -> batalla.colocarBarco(propio,
                batalla.getOceanoMaquina().getCuadrante(2, 2), Orientacion.HORIZONTAL));
        assertEquals(antes, ocupacion(oceano));
        verificarSinUbicar(propio);
        verificarSinUbicar(enemigo);
        batalla.colocarBarco(propio, oceano.getCuadrante(2, 2), Orientacion.HORIZONTAL);
        assertTrue(propio.estaUbicado());
        assertFalse(enemigo.estaUbicado());
    }

    @Test
    void batallaRechazaColocacionesFueraDePreparacionSinCambios() throws Exception {
        Batalla batalla = crearBatalla();
        Barco barco = batalla.getFlotaJugador().getBarcos().get(0);
        Oceano oceano = batalla.getOceanoJugador();
        List<Barco> antes = ocupacion(oceano);
        for (EstadoBatalla estado : List.of(
                new EstadoEnCombate(batalla), new EstadoTerminada(batalla))) {
            batalla.setEstado(estado);
            assertThrows(BatallaNavalException.class,
                    () -> batalla.colocarBarco(barco, oceano.getCuadrante(2, 2), Orientacion.HORIZONTAL));
            assertEquals(antes, ocupacion(oceano));
            verificarSinUbicar(barco);
        }
    }

    @Test
    void dosColocacionesSimultaneasNoPuedenDejarBarcosAdyacentes() throws Exception {
        Oceano oceano = new Oceano(6, 8);
        Barco primero = crearBarco(1);
        Barco segundo = crearBarco(1);
        CountDownLatch inicio = new CountDownLatch(1);
        var ejecutor = Executors.newFixedThreadPool(2);
        try {
            var resultadoPrimero = ejecutor.submit(() -> {
                inicio.await();
                try {
                    oceano.colocarBarco(primero, oceano.getCuadrante(2, 2), Orientacion.HORIZONTAL);
                    return true;
                } catch (BatallaNavalException esperada) {
                    return false;
                }
            });
            var resultadoSegundo = ejecutor.submit(() -> {
                inicio.await();
                try {
                    oceano.colocarBarco(segundo, oceano.getCuadrante(2, 3), Orientacion.HORIZONTAL);
                    return true;
                } catch (BatallaNavalException esperada) {
                    return false;
                }
            });
            inicio.countDown();
            int exitos = (resultadoPrimero.get(5, TimeUnit.SECONDS) ? 1 : 0)
                    + (resultadoSegundo.get(5, TimeUnit.SECONDS) ? 1 : 0);
            assertEquals(1, exitos);
            assertEquals(1L, ocupacion(oceano).stream().filter(Objects::nonNull).count());
            assertNotEquals(primero.estaUbicado(), segundo.estaUbicado());
        } finally {
            inicio.countDown();
            ejecutor.shutdownNow();
        }
    }

    private Batalla crearBatalla() throws BatallaNavalException {
        Flota propia = new Flota(List.of(crearBarco(3)));
        Jugador jugador = new Jugador(propia, new Oceano(6, 8));
        Maquina maquina = new Maquina(propia.crearFlotaEquivalente(), new Oceano(6, 8),
                new TacticaSecuencial());
        return new Batalla("Prueba", jugador, maquina);
    }

    private List<Barco> ocupacion(Oceano oceano) throws BatallaNavalException {
        List<Barco> barcos = new ArrayList<>();
        for (int fila = 0; fila < oceano.getFilas(); fila++) {
            for (int columna = 0; columna < oceano.getColumnas(); columna++) {
                barcos.add(oceano.getCuadrante(fila, columna).getBarco());
            }
        }
        return barcos;
    }

    private void verificarSinUbicar(Barco barco) {
        assertFalse(barco.estaUbicado());
        assertTrue(barco.getCuadrantes().isEmpty());
        assertNull(barco.getOrientacion());
    }
}
