package uy.edu.ort.battleshipdemo.modelo;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import uy.edu.ort.battleshipdemo.observador.Observador;

class SistemaBatallasEventosTest {
    private SistemaBatallas cargar() throws BatallaNavalException {
        SistemaBatallas sistema = new SistemaBatallas();
        Precargas.cargar(sistema);
        return sistema;
    }

    private Cuadrante aguaLibre(Oceano oceano) throws BatallaNavalException {
        for (int fila = 0; fila < oceano.getFilas(); fila++) {
            for (int columna = 0; columna < oceano.getColumnas(); columna++) {
                Cuadrante cuadrante = oceano.getCuadrante(fila, columna);
                if (!cuadrante.estaOcupado()) {
                    return cuadrante;
                }
            }
        }
        throw new IllegalStateException("Debe haber agua en el escenario de prueba.");
    }

    @Test
    void sistemaPublicaCreacionInicioRondasYVictoriaMedianteLaFachada() throws Exception {
        SistemaBatallas sistema = cargar();
        Fachada fachada = Fachada.getInstancia();
        List<Object> eventos = new ArrayList<>();
        List<Object> origenes = new ArrayList<>();
        Observador observador = (evento, origen) -> {
            eventos.add(evento);
            origenes.add(origen);
        };
        fachada.agregarObservador(observador);
        try {
            Batalla batalla = sistema.prepararBatalla("Avisos de victoria", "Secuencial", 6, 6, 0, 0, 0, 0, 1);
            assertEquals(1, eventos.size());
            assertTrue(sistema.listarBatallas().contains(batalla));
            assertThrows(BatallaNavalException.class, () -> sistema.iniciarCombate(batalla.getId()));
            assertThrows(BatallaNavalException.class, () -> sistema.disparar(batalla.getId(), 1, 1, 1));
            assertEquals(1, eventos.size());

            batalla.colocarBarco(batalla.getFlotaJugador().getBarcos().get(0),
                    batalla.getOceanoJugador().getCuadrante(5, 5), Orientacion.HORIZONTAL);
            assertSame(batalla, sistema.iniciarCombate(batalla.getId()));
            assertEquals(2, eventos.size());
            assertEquals(Batalla.Estados.EN_COMBATE, batalla.getEstado());
            assertThrows(BatallaNavalException.class, () -> sistema.iniciarCombate(batalla.getId()));
            assertEquals(2, eventos.size());

            Cuadrante agua = aguaLibre(batalla.getOceanoMaquina());
            sistema.disparar(batalla.getId(), agua.getFila(), agua.getColumna(), 1);
            assertEquals(3, eventos.size());
            assertEquals(1, batalla.getRondas().size());
            assertThrows(BatallaNavalException.class,
                    () -> sistema.disparar(batalla.getId(), agua.getFila(), agua.getColumna(), 2));
            assertEquals(3, eventos.size());

            Cuadrante barco = batalla.getFlotaMaquina().getBarcos().get(0).getCuadrantes().get(0);
            sistema.disparar(batalla.getId(), barco.getFila(), barco.getColumna(), 2);
            assertEquals(4, eventos.size());
            assertTrue(batalla.fueGanada());
            assertEquals(2, sistema.contarBatallasGanadas());
            assertEquals(200.0 / 3, sistema.calcularExitoCampania(), 0.001);
            assertTrue(eventos.stream().allMatch(evento -> evento == Fachada.Eventos.BATALLAS_ACTUALIZADAS));
            assertTrue(origenes.stream().allMatch(origen -> origen == fachada));
        } finally {
            fachada.quitarObservador(observador);
        }
    }

    @Test
    void avisoDeDerrotaYaPermiteConsultarLasEstadisticasFinales() throws Exception {
        SistemaBatallas sistema = cargar();
        Fachada fachada = Fachada.getInstancia();
        List<Double> porcentajesAlAvisar = new ArrayList<>();
        Observador observador = (evento, origen) -> porcentajesAlAvisar.add(sistema.calcularExitoCampania());
        fachada.agregarObservador(observador);
        try {
            Batalla batalla = sistema.prepararBatalla("Avisos de derrota", "Secuencial", 6, 6, 0, 0, 0, 0, 1);
            batalla.colocarBarco(batalla.getFlotaJugador().getBarcos().get(0),
                    batalla.getOceanoJugador().getCuadrante(0, 0), Orientacion.HORIZONTAL);
            sistema.iniciarCombate(batalla.getId());
            Cuadrante agua = aguaLibre(batalla.getOceanoMaquina());
            sistema.disparar(batalla.getId(), agua.getFila(), agua.getColumna(), 1);
            assertTrue(batalla.fuePerdida());
            assertEquals(2, sistema.contarBatallasPerdidas());
            assertEquals(3, porcentajesAlAvisar.size());
            assertEquals(100.0 / 3, porcentajesAlAvisar.get(2), 0.001);
        } finally {
            fachada.quitarObservador(observador);
        }
    }
}
