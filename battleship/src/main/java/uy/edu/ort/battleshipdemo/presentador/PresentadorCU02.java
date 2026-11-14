package uy.edu.ort.battleshipdemo.presentador;

import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.PreDestroy;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.SessionScope;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import uy.edu.ort.battleshipdemo.modelo.Barco;
import uy.edu.ort.battleshipdemo.modelo.Batalla;
import uy.edu.ort.battleshipdemo.modelo.Cuadrante;
import uy.edu.ort.battleshipdemo.modelo.BatallaNavalException;
import uy.edu.ort.battleshipdemo.modelo.Fachada;
import uy.edu.ort.battleshipdemo.modelo.Oceano;
import uy.edu.ort.battleshipdemo.modelo.Barco.Orientacion;
import uy.edu.ort.battleshipdemo.observador.Observable;
import uy.edu.ort.battleshipdemo.observador.Observador;
import uy.edu.ort.battleshipdemo.presentador.DTOs.BarcoDto;
import uy.edu.ort.battleshipdemo.presentador.DTOs.InicializarDto;
import uy.edu.ort.battleshipdemo.presentador.DTOs.TacticaDto;
import uy.edu.ort.battleshipdemo.presentador.DTOs.TipoBarcoDto;

@RestController
@SessionScope
@RequestMapping("/cu02")
public class PresentadorCU02 implements Observador {

    private Fachada fachada;
    private Batalla enPreparacion;
    private ConexionNavegador conexion;
    private SseEmitter emisorDespliegue;
    private Orientacion orientacionElegida = Orientacion.HORIZONTAL;

    public PresentadorCU02(Fachada fachada, ConexionNavegador conexion) {
        this.fachada = fachada;
        this.conexion = conexion;
    }

    // -------------------------------PARTE 1
    @PostMapping("/configurarBatalla")
    public Commands panelCU2(@RequestParam(value = "idBatalla", required = false) Integer idBatalla) throws BatallaNavalException {
        if (idBatalla != null) {
            Batalla seleccionada = buscarPreparacion(idBatalla);
            salir();
            enPreparacion = seleccionada;
            return Commands.create(abrirDespliegue());
        }
        salir();
        return Commands.create(listaTacticas(), listaTiposBarco());
    }

    @PostMapping("/crearBatalla")
    public Commands crearBatallaPreparacion(@RequestParam(required = false) String nombre, @RequestParam String tactica,
            @RequestParam int filas,
            @RequestParam int columnas, @RequestParam int portaaviones, @RequestParam int acorazados,
            @RequestParam int cruceros,
            @RequestParam int submarinos, @RequestParam int destructores) {

        try {
            Batalla nueva = fachada.prepararBatalla(nombre, tactica, filas, columnas,
                    portaaviones, acorazados, cruceros, submarinos, destructores);
            salir();
            enPreparacion = nueva;
        } catch (BatallaNavalException ex) {
            return Commands.create(mensaje(ex.getMessage()));
        }

        return Commands.create(abrirDespliegue());

    }

    // ---------------------------------PARTE 2

    @GetMapping(value = "/registrarSSE", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter registrarSSE(@RequestParam("idBatalla") int idBatalla) throws BatallaNavalException {
        Batalla seleccionada = buscarPreparacion(idBatalla);
        salir();
        enPreparacion = seleccionada;
        orientacionElegida = Orientacion.HORIZONTAL;
        enPreparacion.agregarObservador(this);
        conexion.conectarSSE();
        emisorDespliegue = conexion.getConexionSSE();
        SseEmitter emisor = emisorDespliegue;
        emisor.onCompletion(() -> finalizarConexion(emisor));
        emisor.onTimeout(() -> finalizarConexion(emisor));
        emisor.onError(error -> finalizarConexion(emisor));
        conexion.enviarJSON(mostrarDespliegue());
        return emisor;
    }

    private Batalla buscarPreparacion(int idBatalla) throws BatallaNavalException {
        Batalla batalla = fachada.buscarBatalla(idBatalla);
        if (!batalla.estaEnPreparacion()) {
            throw new BatallaNavalException("Solo puede desplegar barcos en una batalla en preparacion.");
        }
        return batalla;
    }

    private void finalizarConexion(SseEmitter emisor) {
        if (emisorDespliegue == emisor) {
            salir();
        }
    }

    @PostMapping("/finCasoUso")
    public void finCasoUso(@RequestParam("idBatalla") int idBatalla) {
        if (enPreparacion != null && enPreparacion.getId() == idBatalla) {
            salir();
        }
    }

    @PreDestroy
    public void salir() {
        if (enPreparacion != null) {
            enPreparacion.quitarObservador(this);
            enPreparacion = null;
        }
        SseEmitter anterior = emisorDespliegue;
        emisorDespliegue = null;
        // CU02 y CU03 comparten la conexion de sesion: no cerrar un canal de combate
        // nuevo.
        if (anterior != null && conexion.getConexionSSE() == anterior) {
            conexion.cerrarConexion();
        }
    }

    private Command abrirDespliegue() {
        return new Command("abrirDespliegue", enPreparacion.getId());
    }

    private Commands mostrarDespliegue() {
        Oceano oceano = enPreparacion.getOceanoJugador();
        InicializarDto tablero = new InicializarDto(oceano.getFilas(), oceano.getColumnas(),
                "/cu02/ubicarBarco", "/cu02/aguasEnemigas");
        Commands comandos = Commands.create(posicionarBarcos(),
                new Command("inicializar", tablero),
                new Command("lugarCombate", enPreparacion.getLugar()));
        agregarSituacionDespliegue(comandos);
        return comandos;
    }

    private void agregarSituacionDespliegue(Commands comandos) {
        for (Barco barco : enPreparacion.getFlotaJugador().getBarcos()) {
            if (barco.estaUbicado()) {
                comandos.add(mostrarBarcoUbicado(barco));
            }
        }
        comandos.add(barcosDisponibles());
        comandos.add(new Command("leyenda", textoLeyenda(orientacionElegida)));
    }

    @Override
    public void actualizar(Object evento, Observable origen) {
        if (origen != enPreparacion) {
            return;
        }

        if (evento == Batalla.Eventos.BARCO_COLOCADO) {
            Commands comandos = Commands.create();
            agregarSituacionDespliegue(comandos);
            comandos.add(sonidoColocacion());
            conexion.enviarJSON(comandos);
        } else if (evento == Batalla.Eventos.COMBATE_INICIADO) {
            Commands comandos = Commands.create(iniciarBatalla());
            conexion.enviarJSON(comandos);
            salir();
        }
    }

    // Solicita la colocacion al modelo; el observador envia la actualizacion y el endpoint informa los errores.
    @PostMapping("/ubicarBarco")
    public Commands ubicarBarco(
            @RequestParam("idBarco") int idBarco,
            @RequestParam("coordenadaVertical") int fila,
            @RequestParam("coordenadaHorizontal") int columna,
            @RequestParam("orientacion") Orientacion orientacion) {
        try {
            validarBatallaSeleccionada();
            Barco barco = enPreparacion.buscarBarcoJugador(idBarco);
            Cuadrante proa = enPreparacion.getCuadranteJugador(fila, columna);
            orientacionElegida = orientacion;
            enPreparacion.colocarBarco(barco, proa, orientacion);
            // El evento BARCO_COLOCADO ya actualizo a todos los observadores por SSE.
            return Commands.create();
        } catch (BatallaNavalException ex) {
            return Commands.create(mensaje(ex.getMessage()));
        }
    }

    private String textoLeyenda(Orientacion orientacion) {
        List<Barco> pendientes = enPreparacion.getFlotaJugador().listarBarcosPendientes();
        if (pendientes.isEmpty()) {
            return "Batalla preparada";
        }
        Barco primero = pendientes.get(0);
        return "Posicione su " + primero.getNombre() + " en forma " + orientacion.toString().toLowerCase();
    }

    @PostMapping("/aguasEnemigas")
    public Commands aguasEnemigas() {
        return Commands.create(mensaje("No puede ubicar barcos en aguas enemigas"));
    }

    @PostMapping("/leyenda")
    public Commands leyenda(@RequestParam("orientacion") Orientacion orientacion) throws BatallaNavalException {
        validarBatallaSeleccionada();
        orientacionElegida = orientacion;
        return Commands.create(new Command("leyenda", textoLeyenda(orientacionElegida)));
    }

    @PostMapping("/entrarBatalla")
    public Commands entrarBatalla() {
        try {
            validarBatallaSeleccionada();
            fachada.iniciarCombate(enPreparacion.getId());
            return Commands.create();
        } catch (BatallaNavalException ex) {
            return Commands.create(mensaje(ex.getMessage()));
        }
    }

    private void validarBatallaSeleccionada() throws BatallaNavalException {
        if (enPreparacion == null) {
            throw new BatallaNavalException("Debe seleccionar una batalla antes de continuar.");
        }
    }

    private Command barcosDisponibles() {
        List<BarcoDto> pendientes = new ArrayList<>();
        for (Barco barco : enPreparacion.getFlotaJugador().listarBarcosPendientes()) {
            pendientes.add(new BarcoDto(barco));
        }
        return new Command("barcosDisponibles", pendientes);
    }

    private Command mensaje(String texto) {
        return new Command("mensaje", texto);
    }

    private Command posicionarBarcos() {
        return new Command("posicionarBarcos", true);
    }

    private Command listaTacticas() {
        return new Command("listaTacticas", TacticaDto.listaDtos(fachada.listarTacticas()));
    }

    private Command listaTiposBarco() {
        return new Command("listaTiposBarco", TipoBarcoDto.listaDtos(fachada.listarTiposBarco()));
    }

    private Command iniciarBatalla() {
        return new Command("iniciarBatalla", enPreparacion.getId());
    }

    private Command mostrarBarcoUbicado(Barco barco) {
        return new Command("mostrarBarcoUbicado", new BarcoDto(barco));
    }

    // -------------------------------EXTRAS
    // SONIDOS----------------------------------//
    private Command sonidoColocacion() {
        return new Command("reproducirSonidos", List.of("colocar"));
    }
}
