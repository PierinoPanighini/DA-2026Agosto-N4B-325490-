package uy.edu.ort.battleshipdemo.modelo;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

class PrepararBatallaTest {

    private SistemaBatallas cargar() throws BatallaNavalException {
        SistemaBatallas sistema = new SistemaBatallas();
        Precargas.cargar(sistema);
        return sistema;
    }

    @Test
    void creaYRegistraPreparacionConNombreNormalizadoYParticipantesIndependientes() throws Exception {
        SistemaBatallas sistema = cargar();
        Batalla batalla = sistema.prepararBatalla("  Atlantico  ", " Secuencial ", 8, 10,
                1, 0, 2, 1, 3);
        assertEquals("Atlantico", batalla.getLugar());
        assertEquals(Batalla.Estados.EN_PREPARACION, batalla.getEstado());
        assertEquals(5, sistema.listarBatallas().size());
        assertSame(batalla, sistema.buscarBatalla(batalla.getId()));
        assertEquals(8, batalla.getOceanoJugador().getFilas());
        assertEquals(10, batalla.getOceanoJugador().getColumnas());
        assertNotSame(batalla.getOceanoJugador(), batalla.getOceanoMaquina());
        assertNotSame(batalla.getFlotaJugador(), batalla.getFlotaMaquina());
        List<Barco> propios = batalla.getFlotaJugador().getBarcos();
        List<Barco> enemigos = batalla.getFlotaMaquina().getBarcos();
        Map<String, Integer> cantidades = new HashMap<>();
        for (int i = 0; i < propios.size(); i++) {
            Barco propio = propios.get(i);
            Barco enemigo = enemigos.get(i);
            cantidades.merge(propio.getNombre(), 1, Integer::sum);
            assertNotSame(propio, enemigo);
            assertSame(propio.getTipoBarco(), enemigo.getTipoBarco());
            assertFalse(propio.estaUbicado());
            assertFalse(enemigo.estaUbicado());
        }
        assertEquals(Map.of("Portaaviones", 1, "Crucero", 2, "Submarino", 1, "Destructor", 3), cantidades);
        assertInstanceOf(TacticaSecuencial.class, batalla.getTactica());
        assertFalse(sistema.listarTacticas().contains(batalla.getTactica()));
        assertTrue(batalla.getRondas().isEmpty());
        assertTrue(batalla.getOceanoJugador().listarCuadrantesOcupados().isEmpty());
        assertTrue(batalla.getOceanoMaquina().listarCuadrantesOcupados().isEmpty());
    }

    @Test
    void nombresNoProporcionadosGeneranSeisLetrasMayusculasSinRepetirse() throws Exception {
        SistemaBatallas sistema = cargar();
        Set<String> nombres = new HashSet<>();
        for (Batalla batalla : sistema.listarBatallas()) {
            nombres.add(batalla.getLugar());
        }
        for (String entrada : new String[] {null, "", "   "}) {
            Batalla batalla = sistema.prepararBatalla(entrada, "Espiral", 6, 6, 0, 0, 1, 0, 0);
            assertTrue(batalla.getLugar().matches("[A-Z]{6}"));
            assertTrue(nombres.add(batalla.getLugar()));
        }
        assertEquals(7, sistema.listarBatallas().size());
    }

    @Test
    void rechazaLugaresRepetidosIncluyendoPrecargasSinRegistrarUnaBatallaInvalida() throws Exception {
        SistemaBatallas sistema = cargar();
        assertThrows(BatallaNavalException.class,
                () -> sistema.prepararBatalla("  bAhIa SeReNa ", "Secuencial", 6, 6, 0, 0, 1, 0, 0));
        assertEquals(4, sistema.listarBatallas().size());
        sistema.prepararBatalla("Atlantico", "Secuencial", 6, 6, 0, 0, 1, 0, 0);
        assertThrows(BatallaNavalException.class,
                () -> sistema.prepararBatalla("atlantico", "Secuencial", 6, 6, 0, 0, 1, 0, 0));
        assertEquals(5, sistema.listarBatallas().size());
    }

    @Test
    void ocupacionDebeSerEstrictamenteMenorAlVeinticincoPorCiento() throws Exception {
        SistemaBatallas sistema = cargar();
        // Nueve partes en 6 x 6 es exactamente 25% y debe rechazarse.
        assertThrows(BatallaNavalException.class,
                () -> sistema.prepararBatalla("Limite", "Secuencial", 6, 6, 0, 0, 3, 0, 0));
        assertThrows(BatallaNavalException.class,
                () -> sistema.prepararBatalla("Exceso", "Secuencial", 6, 6, 0, 0, 3, 0, 1));
        assertEquals(4, sistema.listarBatallas().size());
        Batalla valida = sistema.prepararBatalla("Valida", "Secuencial", 6, 6, 0, 0, 2, 1, 0);
        assertEquals(8, valida.getFlotaJugador().tamanioTotal());
        assertEquals(5, sistema.listarBatallas().size());
    }

    @Test
    void cadaParticipanteValidaLaDensidadConLasDimensionesDeSuOceano() throws Exception {
        Flota flota = new Flota(List.of(new Barco(new TipoBarco("Crucero", 3)),
                new Barco(new TipoBarco("Crucero", 3)), new Barco(new TipoBarco("Crucero", 3))));
        Oceano chico = new Oceano(6, 6);
        Oceano grande = new Oceano(6, 8);
        assertThrows(BatallaNavalException.class, () -> chico.validarDensidad(flota));
        assertThrows(BatallaNavalException.class, () -> new Jugador(flota, chico));
        assertThrows(BatallaNavalException.class,
                () -> new Maquina(flota.crearFlotaEquivalente(), chico, new TacticaSecuencial()));
        assertDoesNotThrow(() -> grande.validarDensidad(flota));
        assertDoesNotThrow(() -> new Jugador(flota, grande));
        assertDoesNotThrow(() -> new Maquina(flota.crearFlotaEquivalente(), grande, new TacticaSecuencial()));
    }

    @Test
    void constructoresRechazanDatosInvalidosConExcepcionesDelDominio() throws Exception {
        assertThrows(BatallaNavalException.class, () -> new TipoBarco("Invalido", 0));
        assertThrows(BatallaNavalException.class, () -> new TipoBarco("Invalido", -1));
        assertThrows(BatallaNavalException.class, () -> new Barco(null));
        assertThrows(BatallaNavalException.class, () -> new Flota(null));
        assertThrows(BatallaNavalException.class, () -> new Flota(List.of()));
        List<Barco> barcos = new ArrayList<>();
        barcos.add(null);
        assertThrows(BatallaNavalException.class, () -> new Flota(barcos));
        Flota flota = new Flota(List.of(new Barco(new TipoBarco("Destructor", 1))));
        Oceano oceano = new Oceano(6, 6);
        assertThrows(BatallaNavalException.class, () -> new Jugador(null, oceano));
        assertThrows(BatallaNavalException.class, () -> new Jugador(flota, null));
        assertThrows(BatallaNavalException.class, () -> new Maquina(null, oceano, new TacticaSecuencial()));
        assertThrows(BatallaNavalException.class, () -> new Maquina(flota, null, new TacticaSecuencial()));
        assertThrows(BatallaNavalException.class, () -> new Maquina(flota, oceano, null));
        assertThrows(BatallaNavalException.class, () -> new Oceano(-1, 6));
        assertThrows(BatallaNavalException.class, () -> new Oceano(6, -1));
    }

    @Test
    void datosInvalidosSePropaganComoExcepcionDeDominioSinRegistrarBatallas() throws Exception {
        SistemaBatallas sistema = cargar();
        int[][] configuraciones = {
                {5, 6, 0, 0, 1, 0, 0}, {6, 13, 0, 0, 1, 0, 0},
                {6, 6, -1, 0, 1, 0, 0}, {6, 6, 0, 0, 0, 0, 7},
                {6, 6, 0, 0, 0, 0, 0}
        };
        for (int[] configuracion : configuraciones) {
            assertThrows(BatallaNavalException.class, () -> sistema.prepararBatalla("Invalida", "Secuencial",
                    configuracion[0], configuracion[1], configuracion[2], configuracion[3],
                    configuracion[4], configuracion[5], configuracion[6]));
            assertEquals(4, sistema.listarBatallas().size());
        }
        for (String tactica : new String[] {null, "", "Desconocida"}) {
            assertThrows(BatallaNavalException.class,
                    () -> sistema.prepararBatalla("Invalida", tactica, 6, 6, 0, 0, 1, 0, 0));
            assertEquals(4, sistema.listarBatallas().size());
        }
        assertDoesNotThrow(() -> sistema.prepararBatalla("Valida", "Secuencial", 6, 6, 0, 0, 0, 0, 6));
    }
}
