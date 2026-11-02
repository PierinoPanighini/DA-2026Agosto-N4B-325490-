package uy.edu.ort.battleshipdemo.presentador;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import uy.edu.ort.battleshipdemo.modelo.*;

class PresentadorCU02Test {
    private MockMvc mvc;
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
        Fachada fachada = mock(Fachada.class);
        when(fachada.prepararBatalla(nullable(String.class), eq("Secuencial"), eq(6), eq(6),
                eq(0), eq(0), eq(1), eq(1), eq(0))).thenReturn(batalla);
        mvc = MockMvcBuilders.standaloneSetup(new PresentadorCU02(fachada)).build();
    }

    @Test
    void creacionEnviaIdsYBarcosDisponiblesOrdenadosPorTamanio() throws Exception {
        crearPreparacion()
                .andExpect(jsonPath("$.commands[0].id").value("posicionarBarcos"))
                .andExpect(jsonPath("$.commands[1].id").value("barcosDisponibles"))
                .andExpect(jsonPath("$.commands[1].parametro.length()").value(2))
                .andExpect(jsonPath("$.commands[1].parametro[0].id").value(grande.getId()))
                .andExpect(jsonPath("$.commands[1].parametro[0].nombre").value("Crucero"))
                .andExpect(jsonPath("$.commands[1].parametro[0].cuadrantes.length()").value(0))
                .andExpect(jsonPath("$.commands[1].parametro[1].id").value(pequeno.getId()));
        assertSame(pequeno, batalla.getFlotaJugador().getBarcos().get(0));
    }

    @Test
    void ubicacionRecibeIdYCoordenadasYSerializaUnDtoSinReferenciasCirculares() throws Exception {
        crearPreparacion();
        ubicar(grande.getId(), 1, 1, "HORIZONTAL")
                .andExpect(jsonPath("$.commands[0].id").value("mostrarBarcoUbicado"))
                .andExpect(jsonPath("$.commands[0].parametro.id").value(grande.getId()))
                .andExpect(jsonPath("$.commands[0].parametro.tamanio").value(3))
                .andExpect(jsonPath("$.commands[0].parametro.cuadrantes.length()").value(3))
                .andExpect(jsonPath("$.commands[0].parametro.cuadrantes[0].coordenadaVertical").value(1))
                .andExpect(jsonPath("$.commands[0].parametro.cuadrantes[0].coordenadaHorizontal").value(1))
                .andExpect(jsonPath("$.commands[0].parametro.cuadrantes[2].coordenadaHorizontal").value(3))
                .andExpect(jsonPath("$.commands[0].parametro.cuadrantes[0].valor").value("barco"))
                .andExpect(jsonPath("$.commands[0].parametro.cuadrantes[0].barco").doesNotExist())
                .andExpect(jsonPath("$.commands[1].parametro.length()").value(1))
                .andExpect(jsonPath("$.commands[1].parametro[0].id").value(pequeno.getId()));
        assertSame(grande, batalla.getFlotaJugador().buscarBarco(grande.getId()));
        for (int columna = 1; columna <= 3; columna++) {
            assertSame(grande, batalla.getOceanoJugador().getCuadrante(1, columna).getBarco());
        }
    }

    @Test
    void ultimoBarcoTambienSeDibujaYElInicioSeDelegaAlDominio() throws Exception {
        crearPreparacion();
        ubicar(grande.getId(), 1, 1, "HORIZONTAL");
        ubicar(pequeno.getId(), 4, 0, "VERTICAL")
                .andExpect(jsonPath("$.commands.length()").value(2))
                .andExpect(jsonPath("$.commands[0].id").value("mostrarBarcoUbicado"))
                .andExpect(jsonPath("$.commands[0].parametro.id").value(pequeno.getId()))
                .andExpect(jsonPath("$.commands[0].parametro.cuadrantes[1].coordenadaVertical").value(5))
                .andExpect(jsonPath("$.commands[1].id").value("barcosDisponibles"))
                .andExpect(jsonPath("$.commands[1].parametro.length()").value(0));
        assertTrue(batalla.flotaAmigaUbicada());
        assertEquals(Batalla.Estados.EN_PREPARACION, batalla.getEstado());
        assertTrue(batalla.getOceanoMaquina().listarCuadrantesOcupados().isEmpty());
        mvc.perform(post("/cu02/entrarBatalla"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commands[0].id").value("iniciarBatalla"));
        assertEquals(Batalla.Estados.EN_COMBATE, batalla.getEstado());
    }

    @Test
    void unIdDeLaFlotaEnemigaSeRechazaSinModificarElOceano() throws Exception {
        crearPreparacion();
        int enemigo = batalla.getFlotaMaquina().getBarcos().get(0).getId();
        assertNull(batalla.getFlotaJugador().buscarBarco(enemigo));
        ubicar(enemigo, 1, 1, "HORIZONTAL")
                .andExpect(jsonPath("$.commands.length()").value(1))
                .andExpect(jsonPath("$.commands[0].id").value("mensaje"));
        assertTrue(batalla.getOceanoJugador().listarCuadrantesOcupados().isEmpty());
    }

    @Test
    void ubicacionesInvalidasDevuelvenMensajeSinDibujarNiColocarParcialmente() throws Exception {
        crearPreparacion();
        ubicar(grande.getId(), 5, 5, "HORIZONTAL")
                .andExpect(jsonPath("$.commands[0].id").value("mensaje"));
        ubicar(grande.getId(), 6, 0, "HORIZONTAL")
                .andExpect(jsonPath("$.commands[0].id").value("mensaje"));
        assertFalse(grande.estaUbicado());
        assertTrue(batalla.getOceanoJugador().listarCuadrantesOcupados().isEmpty());
    }

    @Test
    void sinPreparacionSeleccionadaDevuelveMensajeEnLugarDeErrorDelServidor() throws Exception {
        ubicar(grande.getId(), 1, 1, "HORIZONTAL")
                .andExpect(jsonPath("$.commands[0].id").value("mensaje"));
        mvc.perform(post("/cu02/entrarBatalla"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commands[0].id").value("mensaje"));
    }

    @Test
    void reubicarElMismoIdSeRechazaYConservaLosCuadrantesOriginales() throws Exception {
        crearPreparacion();
        ubicar(grande.getId(), 1, 1, "HORIZONTAL");
        ubicar(grande.getId(), 3, 2, "HORIZONTAL")
                .andExpect(jsonPath("$.commands[0].id").value("mensaje"));
        assertSame(grande, batalla.getOceanoJugador().getCuadrante(1, 1).getBarco());
        assertNull(batalla.getOceanoJugador().getCuadrante(3, 2).getBarco());
        assertEquals(3, grande.getCuadrantes().size());
    }

    @Test
    void nombrePuedeOmitirseParaQueElModeloLoGenere() throws Exception {
        mvc.perform(post("/cu02/crearBatalla")
                .param("tactica", "Secuencial").param("filas", "6").param("columnas", "6")
                .param("portaaviones", "0").param("acorazados", "0").param("cruceros", "1")
                .param("submarinos", "1").param("destructores", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commands[0].id").value("posicionarBarcos"));
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
