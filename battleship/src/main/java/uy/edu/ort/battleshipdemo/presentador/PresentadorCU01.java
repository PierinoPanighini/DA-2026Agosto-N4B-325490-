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

import uy.edu.ort.battleshipdemo.modelo.Batalla;
import uy.edu.ort.battleshipdemo.modelo.BatallaNavalException;
import uy.edu.ort.battleshipdemo.modelo.Fachada;
import uy.edu.ort.battleshipdemo.observador.Observable;
import uy.edu.ort.battleshipdemo.observador.Observador;
import uy.edu.ort.battleshipdemo.presentador.DTOs.SalaDto;
import uy.edu.ort.battleshipdemo.presentador.DTOs.BatallaDto;

@RestController
@SessionScope
@RequestMapping("/cu01")
public class PresentadorCU01 implements Observador {

    private Fachada fachada;
    private ConexionNavegador conexion;
    private SseEmitter emisorSala;
    private boolean observando;

    public PresentadorCU01(Fachada fachada, ConexionNavegador conexion) {
        this.fachada = fachada;
        this.conexion = conexion;
    }

    @GetMapping(value = "/registrarSSE", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter registrarSSE() {
        salir();
        Commands iniciales = inicializar();
        conexion.conectarSSE();
        emisorSala = conexion.getConexionSSE();
        SseEmitter emisor = emisorSala;
        emisor.onCompletion(() -> finalizarConexion(emisor));
        emisor.onTimeout(() -> finalizarConexion(emisor));
        emisor.onError(error -> finalizarConexion(emisor));
        conexion.enviarJSON(iniciales);
        return emisor;
    }

    private void finalizarConexion(SseEmitter emisor) {
        if (emisorSala == emisor) {
            salir();
        }
    }

    // Consulta la fachada, prepara los datos de presentacion y envia el DTO de la sala.
    @PostMapping("/inicializar")
    public Commands inicializar() {
        fachada.agregarObservador(this);
        observando = true;
        int ganadas = fachada.contarBatallasGanadas();
        int perdidas = fachada.contarBatallasPerdidas();
        int terminadas = fachada.contarBatallasTerminadas();
        double porcentajeExito = fachada.calcularExitoCampania();
        List<Batalla> batallas = fachada.listarBatallas();

        String exito = Math.round(porcentajeExito) + "%";
        String detalleExito = "(" + ganadas + " de " + terminadas + " batallas)";
        List<BatallaDto> datosBatallas = new ArrayList<>();
        for (Batalla batalla : batallas) {
            String dimensiones = batalla.getOceanoJugador().getColumnas() + " x "
                    + batalla.getOceanoJugador().getFilas();
            String estado = FormatoBatalla.textoEstado(batalla);
            String integridadPropia = FormatoBatalla.integridad(batalla.getFlotaJugador());
            String integridadEnemiga = FormatoBatalla.integridad(batalla.getFlotaMaquina());
            datosBatallas.add(new BatallaDto(batalla.getId(), batalla.getLugar(), dimensiones,
                    estado, integridadPropia, integridadEnemiga));
        }

        SalaDto sala = new SalaDto(ganadas, perdidas, exito, detalleExito, datosBatallas);
        return Commands.create(new Command("sala", sala));
    }

    @PostMapping("/finCasoUso")
    @PreDestroy
    public void salir() {
        if (observando) {
            fachada.quitarObservador(this);
            observando = false;
        }
        SseEmitter anterior = emisorSala;
        emisorSala = null;
        if (anterior != null && conexion.getConexionSSE() == anterior) {
            conexion.cerrarConexion();
        }
    }

    @PostMapping("/iniciarNuevaBatalla")
    public Commands iniciarNuevaBatalla() {
        salir();
        return Commands.create(new Command("irA", "/preparar.html"));
    }

    @PostMapping("/entrarBatalla")
    public Commands entrarBatalla(@RequestParam("id") int id) throws BatallaNavalException {
        Batalla batalla = fachada.buscarBatalla(id);
        String destino;
        if (batalla.estaEnPreparacion()) {
            destino = "/preparar.html?batalla=" + id;
        } else {
            destino = "/batalla.html?batalla=" + id;
        }
        salir();
        return Commands.create(new Command("irA", destino));
    }

    @Override
    public void actualizar(Object evento, Observable origen) {
        if (observando && origen == fachada && evento == Fachada.Eventos.BATALLAS_ACTUALIZADAS) {
            conexion.enviarJSON(inicializar());
        }
    }

}
