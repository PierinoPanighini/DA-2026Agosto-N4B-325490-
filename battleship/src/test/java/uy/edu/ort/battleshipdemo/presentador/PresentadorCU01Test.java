package uy.edu.ort.battleshipdemo.presentador;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import uy.edu.ort.battleshipdemo.config.ManejadorGlobalExcepciones;
import uy.edu.ort.battleshipdemo.modelo.Fachada;
import uy.edu.ort.battleshipdemo.modelo.Batalla;
import uy.edu.ort.battleshipdemo.presentador.DTOs.BatallaDto;
import uy.edu.ort.battleshipdemo.presentador.DTOs.SalaDto;

class PresentadorCU01Test {
    private Fachada fachada;
    private ConexionNavegador conexion;
    private PresentadorCU01 presentador;
    private MockMvc mvc;

    @BeforeEach
    void preparar() throws Exception {
        // Una fachada independiente permite comprobar la suscripcion sin alterar las precargas compartidas.
        var constructor = Fachada.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        fachada = constructor.newInstance();
        fachada.cargarDatosIniciales();
        conexion = nuevaConexion();
        presentador = new PresentadorCU01(fachada, conexion);
        mvc = MockMvcBuilders.standaloneSetup(presentador)
                .setControllerAdvice(new ManejadorGlobalExcepciones()).build();
    }

    private ConexionNavegador nuevaConexion() {
        ConexionNavegador nueva = mock(ConexionNavegador.class);
        when(nueva.getConexionSSE()).thenReturn(new SseEmitter());
        return nueva;
    }

    private SalaDto ultimoEnvio(ConexionNavegador destinatario) {
        ArgumentCaptor<Commands> captor = ArgumentCaptor.forClass(Commands.class);
        verify(destinatario).enviarJSON(captor.capture());
        assertEquals(1, captor.getValue().getCommands().size());
        Command comando = captor.getValue().getCommands().get(0);
        assertEquals("sala", comando.getId());
        return (SalaDto) comando.getParametro();
    }

    @Test
    void canalEnviaListadoYEstadisticasIniciales() throws Exception {
        presentador.registrarSSE();
        SalaDto sala = ultimoEnvio(conexion);
        assertEquals(4, sala.getBatallas().size());
        assertEquals(1, sala.getGanadas());
        assertEquals(1, sala.getPerdidas());
        assertEquals("50%", sala.getExito());
        assertEquals("(1 de 2 batallas)", sala.getDetalleExito());
        BatallaDto primera = sala.getBatallas().get(0);
        assertEquals("Bahia Serena", primera.getLugar());
        assertEquals("8 x 8", primera.getDimensiones());
        assertEquals("En preparación", primera.getEstado());
        assertEquals("5 / 5", primera.getIntegridadPropia());
        assertEquals("5 / 5", primera.getIntegridadEnemiga());
        assertEquals("En combate", sala.getBatallas().get(1).getEstado());
        assertEquals("Ganada", sala.getBatallas().get(2).getEstado());
        assertEquals("Perdida", sala.getBatallas().get(3).getEstado());

        mvc.perform(post("/cu01/inicializar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commands[0].id").value("sala"))
                .andExpect(jsonPath("$.commands[0].parametro.detalleExito").value("(1 de 2 batallas)"))
                .andExpect(jsonPath("$.commands[0].parametro.batallas[0].integridadPropia").value("5 / 5"));
    }

    @Test
    void avisoDeFachadaActualizaAmbasSesionesUnaVez() {
        presentador.registrarSSE();
        presentador.registrarSSE();
        ConexionNavegador otraConexion = nuevaConexion();
        PresentadorCU01 otro = new PresentadorCU01(fachada, otraConexion);
        otro.registrarSSE();
        clearInvocations(conexion, otraConexion);
        fachada.notificar(Fachada.Eventos.BATALLAS_ACTUALIZADAS);
        assertEquals(4, ultimoEnvio(conexion).getBatallas().size());
        assertEquals(4, ultimoEnvio(otraConexion).getBatallas().size());
    }

    @Test
    void salirQuitaLaSuscripcionYConservaUnCanalNuevoDeOtroCasoDeUso() {
        presentador.registrarSSE();
        when(conexion.getConexionSSE()).thenReturn(new SseEmitter());
        clearInvocations(conexion);
        presentador.salir();
        verify(conexion, never()).cerrarConexion();
        clearInvocations(conexion);
        fachada.notificar(Fachada.Eventos.BATALLAS_ACTUALIZADAS);
        verifyNoInteractions(conexion);
    }

    @Test
    void iniciarNuevaBatallaDesuscribeLaSalaYDevuelveLaNavegacion() throws Exception {
        presentador.registrarSSE();
        mvc.perform(post("/cu01/iniciarNuevaBatalla")).andExpect(status().isOk())
                .andExpect(jsonPath("$.commands[0].id").value("irA"))
                .andExpect(jsonPath("$.commands[0].parametro").value("/preparar.html"));
        verify(conexion).cerrarConexion();
        clearInvocations(conexion);
        fachada.notificar(Fachada.Eventos.BATALLAS_ACTUALIZADAS);
        verifyNoInteractions(conexion);
    }

    @Test
    void entradaInvalidaConservaLaSalaSuscrita() throws Exception {
        presentador.registrarSSE();
        clearInvocations(conexion);
        mvc.perform(post("/cu01/entrarBatalla").param("id", "-1")).andExpect(status().is(299));
        fachada.notificar(Fachada.Eventos.BATALLAS_ACTUALIZADAS);
        ultimoEnvio(conexion);
        verify(conexion, never()).cerrarConexion();
    }

    @Test
    void avisoReconstruyeLosDatosPreparadosDespuesDeUnaVictoria() throws Exception {
        presentador.registrarSSE();
        clearInvocations(conexion);
        Batalla combate = fachada.listarBatallas().stream()
                .filter(batalla -> batalla.getEstado() == Batalla.Estados.EN_COMBATE)
                .findFirst().orElseThrow();
        int[][] objetivos = {{2, 3}, {2, 4}, {5, 1}, {5, 2}};
        for (int[] objetivo : objetivos) {
            combate.generarDisparo(objetivo[0], objetivo[1]);
        }

        fachada.notificar(Fachada.Eventos.BATALLAS_ACTUALIZADAS);
        SalaDto sala = ultimoEnvio(conexion);
        assertEquals(2, sala.getGanadas());
        assertEquals(1, sala.getPerdidas());
        assertEquals("67%", sala.getExito());
        assertEquals("(2 de 3 batallas)", sala.getDetalleExito());
        BatallaDto fila = sala.getBatallas().stream()
                .filter(batalla -> batalla.getId() == combate.getId())
                .findFirst().orElseThrow();
        assertEquals("Ganada", fila.getEstado());
        assertEquals("0 / 5", fila.getIntegridadEnemiga());
    }

    @Test
    void avisoConOtroOrigenSeIgnora() {
        presentador.registrarSSE();
        clearInvocations(conexion);
        presentador.actualizar(Fachada.Eventos.BATALLAS_ACTUALIZADAS, mock(Fachada.class));
        verifyNoInteractions(conexion);
    }
}
