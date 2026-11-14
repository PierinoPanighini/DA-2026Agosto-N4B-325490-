package uy.edu.ort.battleshipdemo.presentador;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import uy.edu.ort.battleshipdemo.config.ManejadorGlobalExcepciones;
import uy.edu.ort.battleshipdemo.modelo.*;
import uy.edu.ort.battleshipdemo.modelo.Barco.Orientacion;

class PresentadorCU03Test {
    private Batalla batalla;
    private Fachada fachada;
    private ConexionNavegador conexion;
    private PresentadorCU03 presentador;
    private MockMvc mvc;

    private Batalla crearCombate(int tamanio, int filaHumano) throws Exception {
        Barco propio = new Barco(new TipoBarco("Barco de prueba", tamanio));
        Flota flota = new Flota(List.of(propio));
        Oceano propioOceano = new Oceano(6, 6);
        Oceano enemigoOceano = new Oceano(6, 6);
        Maquina maquina = new Maquina(flota.crearFlotaEquivalente(), enemigoOceano,
                new TacticaSecuencial());
        Batalla nueva = new Batalla("Prueba de observadores", new Jugador(flota, propioOceano), maquina);
        nueva.colocarBarco(propio, propioOceano.getCuadrante(filaHumano, 0), Orientacion.HORIZONTAL);
        enemigoOceano.colocarBarco(maquina.getFlota().getBarcos().get(0),
                enemigoOceano.getCuadrante(1, 1), Orientacion.HORIZONTAL);
        nueva.iniciarCombate();
        return nueva;
    }

    private void prepararCombate(int tamanio, int filaHumano) throws Exception {
        batalla = crearCombate(tamanio, filaHumano);
        fachada = mock(Fachada.class);
        when(fachada.buscarBatalla(batalla.getId())).thenReturn(batalla);
        when(fachada.disparar(anyInt(), anyInt(), anyInt())).thenAnswer(invocacion -> {
            batalla.generarDisparo(invocacion.getArgument(1), invocacion.getArgument(2));
            return batalla;
        });
        conexion = mock(ConexionNavegador.class);
        presentador = new PresentadorCU03(fachada, conexion);
        presentador.inicializar(batalla.getId());
        mvc = MockMvcBuilders.standaloneSetup(presentador)
                .setControllerAdvice(new ManejadorGlobalExcepciones()).build();
    }

    private org.springframework.test.web.servlet.ResultActions disparar(int fila, int columna) throws Exception {
        return mvc.perform(post("/cu03/disparar")
                .param("idBatalla", String.valueOf(batalla.getId()))
                .param("coordenadaVertical", String.valueOf(fila))
                .param("coordenadaHorizontal", String.valueOf(columna)));
    }

    private Commands ultimoEnvio(ConexionNavegador destinatario, int cantidad) {
        ArgumentCaptor<Commands> captor = ArgumentCaptor.forClass(Commands.class);
        verify(destinatario, times(cantidad)).enviarJSON(captor.capture());
        return captor.getAllValues().get(cantidad - 1);
    }

    private Object parametro(Commands comandos, String nombre) {
        return comandos.getCommands().stream().filter(comando -> comando.getId().equals(nombre))
                .findFirst().orElseThrow().getParametro();
    }

    @Test
    void rondaEnviaImpactoYAguaPorSseSinDuplicarlosEnLaRespuestaHttp() throws Exception {
        prepararCombate(2, 5);
        disparar(1, 1).andExpect(status().isOk())
                .andExpect(jsonPath("$.commands").isEmpty());
        Commands envio = ultimoEnvio(conexion, 1);
        assertEquals(List.of("impacto", "agua"), parametro(envio, "reproducirSonidos"));
        assertEquals(2, parametro(envio, "cargarNumeroRonda"));
    }

    @Test
    void victoriaEnviaHundimientoMelodiaEstadoYBajasSinDisparoDeMaquina() throws Exception {
        prepararCombate(2, 5);
        disparar(1, 1);
        disparar(1, 2).andExpect(status().isOk());
        Commands envio = ultimoEnvio(conexion, 2);
        assertEquals(List.of("hundido", "victoria"), parametro(envio, "reproducirSonidos"));
        assertEquals("Ganada", parametro(envio, "cargarEstado"));
        assertEquals("{2}", parametro(envio, "bajasEnemigas"));
        assertNull(batalla.getRondas().get(1).getDisparoMaquina());
    }

    @Test
    void derrotaEnviaAmbosDisparosYElEstadoFinal() throws Exception {
        prepararCombate(1, 0);
        disparar(5, 5).andExpect(status().isOk());
        Commands envio = ultimoEnvio(conexion, 1);
        assertEquals(List.of("agua", "hundido", "derrota"), parametro(envio, "reproducirSonidos"));
        assertEquals("Perdida", parametro(envio, "cargarEstado"));
    }

    @Test
    void abrirUnaBatallaTerminadaNoReproduceElHistorialNiLaVictoria() throws Exception {
        prepararCombate(1, 5);
        disparar(1, 1);
        assertTrue(batalla.fueGanada());
        clearInvocations(conexion);
        mvc.perform(post("/cu03/iniciarBatalla").param("idBatalla", String.valueOf(batalla.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commands[?(@.id == 'reproducirSonidos')]").isEmpty());
        verifyNoInteractions(conexion);
    }

    @Test
    void disparoRepetidoNoNotificaNiAgregaOtraRonda() throws Exception {
        prepararCombate(2, 5);
        disparar(1, 1);
        clearInvocations(conexion);
        disparar(1, 1).andExpect(status().is(299));
        assertEquals(1, batalla.getRondas().size());
        verifyNoInteractions(conexion);
    }

    @Test
    void dosPresentadoresRecibenLaMismaRondaCadaUnoEnSuConexion() throws Exception {
        prepararCombate(2, 5);
        ConexionNavegador otraConexion = mock(ConexionNavegador.class);
        PresentadorCU03 otro = new PresentadorCU03(fachada, otraConexion);
        otro.inicializar(batalla.getId());
        // Volver a abrir la misma batalla no debe duplicar la suscripcion.
        otro.inicializar(batalla.getId());
        disparar(1, 1).andExpect(status().isOk());
        Commands primero = ultimoEnvio(conexion, 1);
        Commands segundo = ultimoEnvio(otraConexion, 1);
        assertEquals(parametro(primero, "cargarNumeroRonda"), parametro(segundo, "cargarNumeroRonda"));
        assertEquals(parametro(primero, "reproducirSonidos"), parametro(segundo, "reproducirSonidos"));
    }

    @Test
    void salirDesuscribeEsePresentadorYConservaAlOtro() throws Exception {
        prepararCombate(2, 5);
        ConexionNavegador otraConexion = mock(ConexionNavegador.class);
        PresentadorCU03 otro = new PresentadorCU03(fachada, otraConexion);
        when(otraConexion.getConexionSSE()).thenReturn(new SseEmitter());
        otro.registrarSSE(batalla.getId());
        otro.finCasoUso(batalla.getId());
        verify(otraConexion).cerrarConexion();
        clearInvocations(otraConexion);
        disparar(1, 1).andExpect(status().isOk());
        ultimoEnvio(conexion, 1);
        verifyNoInteractions(otraConexion);
    }

    @Test
    void cambiarDeBatallaDejaDeRecibirAvisosDeLaAnterior() throws Exception {
        prepararCombate(2, 5);
        Batalla otraBatalla = crearCombate(2, 5);
        when(fachada.buscarBatalla(otraBatalla.getId())).thenReturn(otraBatalla);
        presentador.inicializar(otraBatalla.getId());
        batalla.generarDisparo(1, 1);
        verifyNoInteractions(conexion);
        otraBatalla.generarDisparo(1, 1);
        ultimoEnvio(conexion, 1);
    }

    @Test
    void finCasoUsoDeOtraBatallaNoCierraLaBatallaActual() throws Exception {
        prepararCombate(2, 5);
        presentador.finCasoUso(batalla.getId() + 1);
        verifyNoInteractions(conexion);
        disparar(1, 1).andExpect(status().isOk());
        ultimoEnvio(conexion, 1);
    }

    @Test
    void noPermiteDispararDespuesDeSalirDelCombate() throws Exception {
        prepararCombate(2, 5);
        presentador.salir();
        clearInvocations(conexion);
        disparar(1, 1).andExpect(status().is(299));
        assertTrue(batalla.getRondas().isEmpty());
        verifyNoInteractions(conexion);
    }

    @Test
    void salirDeCombateNoCierraUnCanalNuevoDeDespliegue() throws Exception {
        prepararCombate(2, 5);
        when(conexion.getConexionSSE()).thenReturn(new SseEmitter());
        presentador.registrarSSE(batalla.getId());
        when(conexion.getConexionSSE()).thenReturn(new SseEmitter());
        clearInvocations(conexion);
        presentador.finCasoUso(batalla.getId());
        verify(conexion, never()).cerrarConexion();
        clearInvocations(conexion);
        batalla.generarDisparo(1, 1);
        verifyNoInteractions(conexion);
    }
}
