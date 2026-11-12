package uy.edu.ort.battleshipdemo.presentador;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import uy.edu.ort.battleshipdemo.config.ManejadorGlobalExcepciones;
import uy.edu.ort.battleshipdemo.modelo.*;

class PresentadorCU02Test {
    private MockMvc mvc;
    private Fachada fachada;
    private ConexionNavegador conexion;
    private PresentadorCU02 presentador;
    private Batalla batalla;
    private Barco grande;
    private Barco pequeno;

    @BeforeEach
    void preparar() throws Exception {
        pequeno = new Barco(new TipoBarco("Submarino", 2));
        grande = new Barco(new TipoBarco("Crucero", 3));
        Flota propia = new Flota(List.of(pequeno, grande));
        Jugador jugador = new Jugador(propia, new Oceano(6, 6));
        Maquina maquina = new Maquina(propia.crearFlotaEquivalente(), new Oceano(6, 6),
                new TacticaSecuencial());
        batalla = new Batalla("Atlantico", jugador, maquina);
        fachada = mock(Fachada.class);
        when(fachada.prepararBatalla(nullable(String.class), eq("Secuencial"), eq(6), eq(6),
                eq(0), eq(0), eq(1), eq(1), eq(0))).thenReturn(batalla);
        when(fachada.buscarBatalla(batalla.getId())).thenReturn(batalla);
        when(fachada.iniciarCombate(batalla.getId())).thenAnswer(invocacion -> {
            batalla.iniciarCombate();
            return batalla;
        });
        conexion = nuevaConexion();
        presentador = new PresentadorCU02(fachada, conexion);
        mvc = MockMvcBuilders.standaloneSetup(presentador)
                .setControllerAdvice(new ManejadorGlobalExcepciones()).build();
    }

    private ConexionNavegador nuevaConexion() {
        ConexionNavegador nueva = mock(ConexionNavegador.class);
        when(nueva.getConexionSSE()).thenReturn(new SseEmitter());
        return nueva;
    }

    private Commands ultimoEnvio(ConexionNavegador destinatario, int cantidad) {
        ArgumentCaptor<Commands> captor = ArgumentCaptor.forClass(Commands.class);
        verify(destinatario, times(cantidad)).enviarJSON(captor.capture());
        return captor.getAllValues().get(cantidad - 1);
    }

    private Object parametro(Commands comandos, String nombre) {
        return comandos.getCommands().stream().filter(c -> c.getId().equals(nombre))
                .findFirst().orElseThrow().getParametro();
    }

    private void observar() throws Exception {
        presentador.registrarSSE(batalla.getId());
        clearInvocations(conexion);
    }

    @Test
    void creacionAbreElDespliegueYElCanalEnviaLosBarcosOrdenados() throws Exception {
        crearPreparacion().andExpect(jsonPath("$.commands[0].id").value("abrirDespliegue"))
                .andExpect(jsonPath("$.commands[0].parametro").value(batalla.getId()));
        presentador.registrarSSE(batalla.getId());
        Commands inicial = ultimoEnvio(conexion, 1);
        List<?> pendientes = (List<?>) parametro(inicial, "barcosDisponibles");
        assertEquals(2, pendientes.size());
        assertEquals(grande.getId(), ((BarcoDto) pendientes.get(0)).getId());
        assertEquals(pequeno.getId(), ((BarcoDto) pendientes.get(1)).getId());
        assertTrue(((BarcoDto) pendientes.get(0)).getCuadrantes().isEmpty());
        assertSame(pequeno, batalla.getFlotaJugador().getBarcos().get(0));
    }

    @Test
    void ubicacionEnviaDtoPorSseSinDuplicarLaRespuestaHttp() throws Exception {
        observar();
        ubicar(grande.getId(), 1, 1, "HORIZONTAL").andExpect(jsonPath("$.commands").isEmpty());
        Commands envio = ultimoEnvio(conexion, 1);
        BarcoDto dto = (BarcoDto) parametro(envio, "mostrarBarcoUbicado");
        assertEquals(grande.getId(), dto.getId());
        assertEquals(3, dto.getCuadrantes().size());
        assertEquals(1, dto.getCuadrantes().get(0).getCoordenadaVertical());
        assertEquals(3, dto.getCuadrantes().get(2).getCoordenadaHorizontal());
        assertEquals("barco", dto.getCuadrantes().get(0).getValor());
        assertEquals(1, ((List<?>) parametro(envio, "barcosDisponibles")).size());
        assertEquals(List.of("colocar"), parametro(envio, "reproducirSonidos"));
        for (int columna = 1; columna <= 3; columna++) {
            assertSame(grande, batalla.getOceanoJugador().getCuadrante(1, columna).getBarco());
        }
    }

    @Test
    void ultimoBarcoDejaLaBatallaPreparadaYPermiteIniciarElCombate() throws Exception {
        observar();
        ubicar(grande.getId(), 1, 1, "HORIZONTAL");
        ubicar(pequeno.getId(), 4, 0, "VERTICAL");
        Commands envio = ultimoEnvio(conexion, 2);
        assertTrue(((List<?>) parametro(envio, "barcosDisponibles")).isEmpty());
        assertEquals("Batalla preparada", parametro(envio, "leyenda"));
        assertEquals(2, envio.getCommands().stream().filter(c -> c.getId().equals("mostrarBarcoUbicado")).count());
        assertTrue(batalla.flotaAmigaUbicada());
        assertTrue(batalla.getOceanoMaquina().listarCuadrantesOcupados().isEmpty());
        mvc.perform(post("/cu02/entrarBatalla")).andExpect(status().isOk())
                .andExpect(jsonPath("$.commands").isEmpty());
        Commands inicio = ultimoEnvio(conexion, 3);
        assertEquals(batalla.getId(), parametro(inicio, "iniciarBatalla"));
        assertEquals(Batalla.Estados.EN_COMBATE, batalla.getEstado());
        verify(conexion).cerrarConexion();
    }

    @Test
    void barcoEnemigoNoSeColocaNiNotifica() throws Exception {
        observar();
        int enemigo = batalla.getFlotaMaquina().getBarcos().get(0).getId();
        ubicar(enemigo, 1, 1, "HORIZONTAL").andExpect(jsonPath("$.commands[0].id").value("mensaje"));
        assertTrue(batalla.getOceanoJugador().listarCuadrantesOcupados().isEmpty());
        verifyNoInteractions(conexion);
    }

    @Test
    void ubicacionInvalidaNoNotificaNiColocaParcialmente() throws Exception {
        observar();
        ubicar(grande.getId(), 5, 5, "HORIZONTAL").andExpect(jsonPath("$.commands[0].id").value("mensaje"));
        ubicar(grande.getId(), 6, 0, "HORIZONTAL").andExpect(jsonPath("$.commands[0].id").value("mensaje"));
        assertFalse(grande.estaUbicado());
        assertTrue(batalla.getOceanoJugador().listarCuadrantesOcupados().isEmpty());
        verifyNoInteractions(conexion);
    }

    @Test
    void sinPreparacionSeleccionadaDevuelveMensaje() throws Exception {
        ubicar(grande.getId(), 1, 1, "HORIZONTAL").andExpect(jsonPath("$.commands[0].id").value("mensaje"));
        mvc.perform(post("/cu02/entrarBatalla")).andExpect(status().isOk())
                .andExpect(jsonPath("$.commands[0].id").value("mensaje"));
    }

    @Test
    void reubicarElMismoIdNoNotificaYConservaLaPosicion() throws Exception {
        observar();
        ubicar(grande.getId(), 1, 1, "HORIZONTAL");
        clearInvocations(conexion);
        ubicar(grande.getId(), 3, 2, "HORIZONTAL").andExpect(jsonPath("$.commands[0].id").value("mensaje"));
        assertSame(grande, batalla.getOceanoJugador().getCuadrante(1, 1).getBarco());
        assertNull(batalla.getOceanoJugador().getCuadrante(3, 2).getBarco());
        verifyNoInteractions(conexion);
    }

    @Test
    void nombrePuedeOmitirseParaQueElModeloLoGenere() throws Exception {
        mvc.perform(post("/cu02/crearBatalla")
                .param("tactica", "Secuencial").param("filas", "6").param("columnas", "6")
                .param("portaaviones", "0").param("acorazados", "0").param("cruceros", "1")
                .param("submarinos", "1").param("destructores", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commands[0].id").value("abrirDespliegue"));
    }

    @Test
    void dosSesionesRecibenLaColocacionSinCambiarLaOrientacionDeLaOtra() throws Exception {
        observar();
        ConexionNavegador otraConexion = nuevaConexion();
        PresentadorCU02 otro = new PresentadorCU02(fachada, otraConexion);
        otro.registrarSSE(batalla.getId());
        otro.leyenda(Orientacion.VERTICAL);
        clearInvocations(otraConexion);
        ubicar(grande.getId(), 1, 1, "HORIZONTAL");
        Commands primero = ultimoEnvio(conexion, 1);
        Commands segundo = ultimoEnvio(otraConexion, 1);
        assertEquals("Posicione su Submarino en forma horizontal", parametro(primero, "leyenda"));
        assertEquals("Posicione su Submarino en forma vertical", parametro(segundo, "leyenda"));
        assertEquals(List.of("colocar"), parametro(segundo, "reproducirSonidos"));
    }

    @Test
    void salirQuitaElObservadorSinAfectarALaOtraSesion() throws Exception {
        observar();
        ConexionNavegador otraConexion = nuevaConexion();
        PresentadorCU02 otro = new PresentadorCU02(fachada, otraConexion);
        otro.registrarSSE(batalla.getId());
        otro.finCasoUso(batalla.getId());
        clearInvocations(otraConexion);
        ubicar(grande.getId(), 1, 1, "HORIZONTAL");
        ultimoEnvio(conexion, 1);
        verifyNoInteractions(otraConexion);
    }

    @Test
    void retomarReconstruyeLosBarcosSinReproducirElHistorial() throws Exception {
        observar();
        ubicar(grande.getId(), 1, 1, "HORIZONTAL");
        clearInvocations(conexion);
        presentador.registrarSSE(batalla.getId());
        Commands inicial = ultimoEnvio(conexion, 1);
        assertEquals(grande.getId(), ((BarcoDto) parametro(inicial, "mostrarBarcoUbicado")).getId());
        assertEquals(1, ((List<?>) parametro(inicial, "barcosDisponibles")).size());
        assertTrue(inicial.getCommands().stream().noneMatch(c -> c.getId().equals("reproducirSonidos")));
        clearInvocations(conexion);
        ubicar(pequeno.getId(), 4, 0, "VERTICAL");
        ultimoEnvio(conexion, 1);
    }

    @Test
    void volverAlFormularioDejaDeObservarElDespliegue() throws Exception {
        observar();
        presentador.panelCU2(null);
        clearInvocations(conexion);
        batalla.colocarBarco(grande, batalla.getOceanoJugador().getCuadrante(1, 1), Orientacion.HORIZONTAL);
        verifyNoInteractions(conexion);
    }

    @Test
    void unCierreTardioDeDespliegueNoCierraElCanalDeCombate() throws Exception {
        observar();
        ubicar(grande.getId(), 1, 1, "HORIZONTAL");
        ubicar(pequeno.getId(), 4, 0, "VERTICAL");
        presentador.entrarBatalla();
        when(conexion.getConexionSSE()).thenReturn(new SseEmitter());
        clearInvocations(conexion);
        presentador.finCasoUso(batalla.getId());
        verifyNoInteractions(conexion);
    }

    @Test
    void noPermiteAbrirDespliegueDeUnaBatallaEnCombate() throws Exception {
        observar();
        ubicar(grande.getId(), 1, 1, "HORIZONTAL");
        ubicar(pequeno.getId(), 4, 0, "VERTICAL");
        presentador.entrarBatalla();
        assertThrows(BatallaNavalException.class, () -> presentador.registrarSSE(batalla.getId()));
    }

    @Test
    void iniciarCombateAvisaUnaVezACadaSesionYQuitaLasSuscripciones() throws Exception {
        observar();
        ConexionNavegador otraConexion = nuevaConexion();
        PresentadorCU02 otro = new PresentadorCU02(fachada, otraConexion);
        otro.registrarSSE(batalla.getId());
        ubicar(grande.getId(), 1, 1, "HORIZONTAL");
        ubicar(pequeno.getId(), 4, 0, "VERTICAL");
        clearInvocations(conexion, otraConexion);

        mvc.perform(post("/cu02/entrarBatalla")).andExpect(status().isOk())
                .andExpect(jsonPath("$.commands").isEmpty());
        assertEquals(batalla.getId(), parametro(ultimoEnvio(conexion, 1), "iniciarBatalla"));
        assertEquals(batalla.getId(), parametro(ultimoEnvio(otraConexion, 1), "iniciarBatalla"));
        verify(conexion).cerrarConexion();
        verify(otraConexion).cerrarConexion();

        clearInvocations(conexion, otraConexion);
        batalla.notificar(Batalla.Eventos.COMBATE_INICIADO);
        verifyNoInteractions(conexion, otraConexion);
    }

    @Test
    void avisoDeOtraBatallaNoRedirigeNiCierraElDespliegueActual() throws Exception {
        observar();
        Batalla ajena = mock(Batalla.class);
        presentador.actualizar(Batalla.Eventos.COMBATE_INICIADO, ajena);
        verifyNoInteractions(conexion);
        ubicar(grande.getId(), 1, 1, "HORIZONTAL");
        ultimoEnvio(conexion, 1);
    }

    @Test
    void iniciarConBarcosPendientesMuestraErrorYConservaLaSuscripcion() throws Exception {
        observar();
        mvc.perform(post("/cu02/entrarBatalla")).andExpect(status().isOk())
                .andExpect(jsonPath("$.commands[0].id").value("mensaje"));
        assertTrue(batalla.estaEnPreparacion());
        verifyNoInteractions(conexion);
        ubicar(grande.getId(), 1, 1, "HORIZONTAL");
        ultimoEnvio(conexion, 1);
    }

    private ResultActions crearPreparacion() throws Exception {
        return mvc.perform(post("/cu02/crearBatalla")
                .param("nombre", "Atlantico").param("tactica", "Secuencial")
                .param("filas", "6").param("columnas", "6")
                .param("portaaviones", "0").param("acorazados", "0").param("cruceros", "1")
                .param("submarinos", "1").param("destructores", "0"))
                .andExpect(status().isOk());
    }

    private ResultActions ubicar(int id, int fila, int columna, String orientacion) throws Exception {
        return mvc.perform(post("/cu02/ubicarBarco")
                .param("idBarco", String.valueOf(id))
                .param("coordenadaVertical", String.valueOf(fila))
                .param("coordenadaHorizontal", String.valueOf(columna))
                .param("orientacion", orientacion))
                .andExpect(status().isOk());
    }
}
