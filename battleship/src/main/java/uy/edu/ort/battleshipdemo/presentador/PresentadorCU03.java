package uy.edu.ort.battleshipdemo.presentador;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jakarta.annotation.PreDestroy;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import uy.edu.ort.battleshipdemo.modelo.Barco;
import uy.edu.ort.battleshipdemo.modelo.Batalla;
import uy.edu.ort.battleshipdemo.modelo.Cuadrante;
import uy.edu.ort.battleshipdemo.modelo.BatallaNavalException;
import uy.edu.ort.battleshipdemo.modelo.Fachada;
import uy.edu.ort.battleshipdemo.modelo.Oceano;
import uy.edu.ort.battleshipdemo.modelo.Ronda;

import org.springframework.web.context.annotation.SessionScope;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import uy.edu.ort.battleshipdemo.observador.Observable;
import uy.edu.ort.battleshipdemo.observador.Observador;
import uy.edu.ort.battleshipdemo.presentador.DTOs.ValorDto;
import uy.edu.ort.battleshipdemo.presentador.DTOs.InicializarDto;

@RestController
@SessionScope
@RequestMapping("/cu03")
public class PresentadorCU03 implements Observador {

    private Fachada fachada;
    private ConexionNavegador conexion;
    private Batalla batallaObservada;
    private SseEmitter emisorCombate;

    public PresentadorCU03(Fachada fachada, ConexionNavegador conexion) {
        this.fachada = fachada;
        this.conexion = conexion;
    }

    @GetMapping(value = "/registrarSSE", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter registrarSSE(@RequestParam("idBatalla") int idBatalla) throws BatallaNavalException {
        Commands iniciales = inicializar(idBatalla);
        emisorCombate = null;
        conexion.conectarSSE();
        emisorCombate = conexion.getConexionSSE();
        SseEmitter emisor = emisorCombate;
        emisor.onCompletion(() -> finalizarConexion(emisor));
        emisor.onTimeout(() -> finalizarConexion(emisor));
        emisor.onError(error -> finalizarConexion(emisor));
        // Se envia primero el tablero completo, sin sonidos del historial.
        conexion.enviarJSON(iniciales);
        return emisor;
    }

    private void finalizarConexion(SseEmitter emisor) {
        // Una conexion anterior puede terminar despues de abrir una nueva.
        if (emisorCombate == emisor) {
            salir();
        }
    }

    @PostMapping("/finCasoUso")
    public void finCasoUso(@RequestParam("idBatalla") int idBatalla) {
        if (batallaObservada != null && batallaObservada.getId() == idBatalla) {
            salir();
        }
    }

    @PreDestroy
    public void salir() {
        if (batallaObservada != null) {
            batallaObservada.quitarObservador(this);
            batallaObservada = null;
        }
        SseEmitter anterior = emisorCombate;
        emisorCombate = null;
        if (anterior != null && conexion.getConexionSSE() == anterior) {
            conexion.cerrarConexion();
        }
    }

    @PostMapping("/iniciarBatalla")
    public Commands inicializar(@RequestParam("idBatalla") int idBatalla) throws BatallaNavalException {
        Batalla batalla = fachada.buscarBatalla(idBatalla);
        if (batallaObservada != null) {
            batallaObservada.quitarObservador(this);
        }

        batallaObservada = batalla;
        batallaObservada.agregarObservador(this);
        Oceano oceano = batalla.getOceanoJugador();
        InicializarDto tablero = new InicializarDto(oceano.getFilas(), oceano.getColumnas(),
                "/cu03/aguasPropias", "/cu03/disparar");
        Commands comandos = Commands.create(new Command("inicializar", tablero),
                new Command("lugarCombate", batalla.getLugar()));
        agregarSituacion(batalla, comandos);
        return comandos;

    }

    @PostMapping("/aguasPropias")
    public Commands aguasPropias() {
        return Commands.create(new Command("mensaje", "No puede disparar en sus propias aguas"));
    }

    // Recibe la batalla y las coordenadas y solicita el disparo a la fachada.
    @PostMapping("/disparar")
    public Commands disparar(
            @RequestParam("idBatalla") int idBatalla,
            @RequestParam("coordenadaVertical") int fila,
            @RequestParam("coordenadaHorizontal") int columna) throws BatallaNavalException {
        if (batallaObservada == null || batallaObservada.getId() != idBatalla) {
            throw new BatallaNavalException("Debe abrir esa batalla antes de disparar.");
        }
        fachada.disparar(idBatalla, fila, columna);
        // La notificacion de la batalla ya actualizo a todos sus observadores por SSE.
        return Commands.create();
    }

    public Command cargarEstado(String estado) {
        return new Command("cargarEstado", estado);
    }

    public Command cargarNumeroRonda(int numeroDeRonda) {
        return new Command("cargarNumeroRonda", numeroDeRonda);
    }

    private Command cargarBajasEnemigas(Batalla batalla) {
        String bajas = "";

        for (Barco barco : batalla.getBajasEnemigas()) {
            if (!bajas.isEmpty()) {
                bajas += " ";
            }

            bajas += "{" + barco.getTamanio() + "}";
        }

        return new Command("bajasEnemigas", bajas);
    }

    private void agregarSituacion(Batalla batalla, Commands comandos) throws BatallaNavalException {
        dibujarAguasPropias(batalla, comandos);
        dibujarAguasEnemigas(batalla, comandos);
        comandos.add(cargarEstado(FormatoBatalla.textoEstado(batalla)));
        comandos.add(cargarNumeroRonda(batalla.getNumeroRonda()));
        comandos.add(cargarBajasEnemigas(batalla));
    }

    private void dibujarAguasPropias(Batalla batalla, Commands comandos) throws BatallaNavalException {
        for (Cuadrante cuadrante : batalla.getOceanoJugador().listarCuadrantesOcupados()) {
            comandos.add(marcar("mostrarAguasPropias", cuadrante, "barco"));
        }
        for (Cuadrante cuadrante : batalla.getOceanoJugador().listarCuadrantesDisparados()) {
            comandos.add(marcar("mostrarAguasPropias", cuadrante, resultado(cuadrante)));
        }
    }

    private void dibujarAguasEnemigas(Batalla batalla, Commands comandos) throws BatallaNavalException {
        if (batalla.fuePerdida()) {
            for (Cuadrante cuadrante : batalla.getOceanoMaquina().listarCuadrantesOcupados()) {
                comandos.add(marcar("mostrarAguasEnemigas", cuadrante, "barco"));
            }
        }
        for (Cuadrante cuadrante : batalla.getOceanoMaquina().listarCuadrantesDisparados()) {
            comandos.add(marcar("mostrarAguasEnemigas", cuadrante, resultado(cuadrante)));
        }
    }

    private String resultado(Cuadrante cuadrante) {
        if (cuadrante.estaOcupado()) {
            return "impacto";
        }
        return "agua";
    }

    @Override
    public void actualizar(Object evento, Observable origen) {
        if (evento == Batalla.Eventos.RONDA_COMPLETADA
                && origen == batallaObservada) {
            try {
                Commands comandos = Commands.create();
                agregarSituacion(batallaObservada, comandos);
                comandos.add(sonidosRonda(batallaObservada, batallaObservada.getRondas().size()));
                conexion.enviarJSON(comandos);
            } catch (BatallaNavalException ex) {
                System.err.println(
                        "No se pudo actualizar la batalla: " + ex.getMessage());
            }
        }
    }

    private Command marcar(String comando, Cuadrante cuadrante, String valor) {
        return new Command(comando, new ValorDto(cuadrante.getFila(), cuadrante.getColumna(), valor));
    }

    //-------------------------------EXTRAS SONIDOS----------------------------------//
    // Solo se llama tras un disparo aceptado; reconstruir el tablero no emite
    // audio.
    private Command sonidosRonda(Batalla batalla, int numeroRonda) {
        Ronda ronda = batalla.getRondas().get(numeroRonda - 1);
        List<String> sonidos = new ArrayList<>();
        sonidos.add(ronda.getDisparoJugador().getResultado().name().toLowerCase(Locale.ROOT));
        if (ronda.getDisparoMaquina() != null) {
            sonidos.add(ronda.getDisparoMaquina().getResultado().name().toLowerCase(Locale.ROOT));
        }
        if (batalla.fueGanada()) {
            sonidos.add("victoria");
        } else if (batalla.fuePerdida()) {
            sonidos.add("derrota");
        }
        return new Command("reproducirSonidos", sonidos);
    }
}
