package uy.edu.ort.battleshipdemo.modelo;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

class TacticasObjetivoTest {

    private List<Tactica> opciones() {
        return List.of(new TacticaSecuencial(), new TacticaEspiral(), new TacticaCazaAtaque());
    }

    private Batalla crearBatalla(Tactica seleccionada, int filas, int columnas) throws BatallaNavalException {
        Flota propia = new Flota(List.of(new Barco(new TipoBarco("Submarino", 2))));
        Jugador jugador = new Jugador(propia, new Oceano(filas, columnas));
        Maquina maquina = new Maquina(propia.crearFlotaEquivalente(), new Oceano(filas, columnas), seleccionada);
        return new Batalla("Prueba", jugador, maquina);
    }

    @Test
    void lasOpcionesDelCatalogoRequierenUnObjetivoAntesDeElegir() {
        for (Tactica tactica : opciones()) {
            assertThrows(BatallaNavalException.class, () -> tactica.elegirCuadrante());
        }
    }

    @Test
    void batallaConfiguraElObjetivoHumanoSinVincularElCatalogo() throws Exception {
        for (Tactica seleccionada : opciones()) {
            Batalla batalla = crearBatalla(seleccionada, 6, 8);
            Tactica tactica = batalla.getMaquina().getTactica();
            assertNotSame(seleccionada, tactica);
            assertEquals(seleccionada.getClass(), tactica.getClass());
            Cuadrante objetivo = tactica.elegirCuadrante();
            assertSame(batalla.getOceanoJugador().getCuadrante(objetivo.getFila(), objetivo.getColumna()), objetivo);
            assertNotSame(batalla.getOceanoMaquina().getCuadrante(objetivo.getFila(), objetivo.getColumna()), objetivo);
            assertFalse(objetivo.isDisparado());
            assertThrows(BatallaNavalException.class, () -> seleccionada.elegirCuadrante());
        }
    }

    @Test
    void consultaElEstadoActualDelObjetivoEnTodasLasDimensionesSinRepetirDisparos() throws Exception {
        for (int filas = 6; filas <= 12; filas++) {
            for (int columnas = 6; columnas <= 12; columnas++) {
                for (Tactica tactica : opciones()) {
                    Oceano oceano = new Oceano(filas, columnas);
                    tactica.configurarObjetivo(oceano);
                    for (int i = 0; i < filas * columnas; i++) {
                        Cuadrante elegido = tactica.elegirCuadrante();
                        assertSame(oceano.getCuadrante(elegido.getFila(), elegido.getColumna()), elegido);
                        assertFalse(elegido.isDisparado(), "Elegir no debe marcar ni repetir cuadrantes");
                        if (tactica instanceof TacticaSecuencial) {
                            assertEquals(i / columnas, elegido.getFila());
                            assertEquals(i % columnas, elegido.getColumna());
                        }
                        assertEquals(ResultadoDisparo.AGUA, elegido.recibirDisparo());
                    }
                    assertThrows(BatallaNavalException.class, () -> tactica.elegirCuadrante());
                }
            }
        }
    }

    @Test
    void espiralConservaSuRecorridoDesdeElCentroEnUnRectangulo() throws Exception {
        Oceano oceano = new Oceano(6, 8);
        Tactica tactica = new TacticaEspiral();
        tactica.configurarObjetivo(oceano);
        int[][] esperados = {{3, 3}, {3, 4}, {2, 4}, {2, 3}, {2, 2}, {3, 2}, {4, 2}, {4, 3}, {4, 4}};
        for (int[] posicion : esperados) {
            Cuadrante elegido = tactica.elegirCuadrante();
            assertSame(oceano.getCuadrante(posicion[0], posicion[1]), elegido);
            elegido.recibirDisparo();
        }
    }

    @Test
    void crearNuevaNoCopiaElObjetivoDeLaInstanciaAnterior() throws Exception {
        for (Tactica anterior : opciones()) {
            Oceano primero = new Oceano(6, 8);
            anterior.configurarObjetivo(primero);
            anterior.elegirCuadrante().recibirDisparo();
            Tactica nueva = anterior.crearNueva();
            assertNotSame(anterior, nueva);
            assertThrows(BatallaNavalException.class, () -> nueva.elegirCuadrante());
            Oceano segundo = new Oceano(8, 6);
            nueva.configurarObjetivo(segundo);
            Cuadrante elegido = nueva.elegirCuadrante();
            assertSame(segundo.getCuadrante(elegido.getFila(), elegido.getColumna()), elegido);
            assertFalse(elegido.isDisparado());
        }
    }

    @Test
    void dosBatallasConLaMismaSeleccionTienenObjetivosYProgresosSeparados() throws Exception {
        for (Tactica seleccionada : opciones()) {
            Batalla primera = crearBatalla(seleccionada, 6, 8);
            Batalla segunda = crearBatalla(seleccionada, 8, 6);
            Tactica tacticaPrimera = primera.getTactica();
            Tactica tacticaSegunda = segunda.getTactica();
            assertNotSame(tacticaPrimera, tacticaSegunda);
            for (int i = 0; i < 48; i++) {
                tacticaPrimera.elegirCuadrante().recibirDisparo();
            }
            assertThrows(BatallaNavalException.class, () -> tacticaPrimera.elegirCuadrante());
            Cuadrante elegido = tacticaSegunda.elegirCuadrante();
            assertSame(segunda.getOceanoJugador().getCuadrante(elegido.getFila(), elegido.getColumna()), elegido);
            assertFalse(elegido.isDisparado());
            assertThrows(BatallaNavalException.class, () -> seleccionada.elegirCuadrante());
        }
    }

    @Test
    void noPermiteCambiarElObjetivoDeUnaTacticaYaVinculada() throws Exception {
        for (Tactica tactica : opciones()) {
            Oceano oceano = new Oceano(6, 8);
            tactica.configurarObjetivo(oceano);
            Cuadrante primero = tactica.elegirCuadrante();
            primero.recibirDisparo();
            tactica.configurarObjetivo(oceano);
            assertThrows(IllegalStateException.class, () -> tactica.configurarObjetivo(new Oceano(8, 6)));
            assertThrows(IllegalArgumentException.class, () -> tactica.configurarObjetivo(null));
            Cuadrante siguiente = tactica.elegirCuadrante();
            assertSame(oceano.getCuadrante(siguiente.getFila(), siguiente.getColumna()), siguiente);
            assertNotSame(primero, siguiente);
            assertFalse(siguiente.isDisparado());
        }
    }

    @Test
    void cazaYataqueLeeLosImpactosDelMismoObjetivoHastaHundir() throws Exception {
        Oceano oceano = new Oceano(6, 8);
        Barco barco = new Barco(new TipoBarco("Submarino", 2));
        oceano.colocarBarco(barco, oceano.getCuadrante(2, 2), Orientacion.HORIZONTAL);
        for (int fila = 0; fila < oceano.getFilas(); fila++) {
            for (int columna = 0; columna < oceano.getColumnas(); columna++) {
                Cuadrante cuadrante = oceano.getCuadrante(fila, columna);
                if (!cuadrante.estaOcupado()) cuadrante.recibirDisparo();
            }
        }
        Tactica tactica = new TacticaCazaAtaque();
        tactica.configurarObjetivo(oceano);
        Cuadrante primero = tactica.elegirCuadrante();
        assertEquals(ResultadoDisparo.IMPACTO, primero.recibirDisparo());
        Cuadrante segundo = tactica.elegirCuadrante();
        assertNotSame(primero, segundo);
        assertEquals(ResultadoDisparo.HUNDIDO, segundo.recibirDisparo());
        assertTrue(barco.estaHundido());
        assertThrows(BatallaNavalException.class, () -> tactica.elegirCuadrante());
    }
}
