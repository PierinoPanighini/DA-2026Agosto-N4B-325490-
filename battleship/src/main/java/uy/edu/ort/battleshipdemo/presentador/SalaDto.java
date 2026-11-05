package uy.edu.ort.battleshipdemo.presentador;

import java.util.List;

import uy.edu.ort.battleshipdemo.modelo.Fachada;

// Todo lo que muestra la sala de operaciones (como AgendaDto en la agenda).
public class SalaDto {

    private int ganadas;
    private int perdidas;
    private String exito;
    private String detalleExito;
    private List<BatallaDto> batallas;

    public SalaDto(Fachada fachada) {
        ganadas = fachada.contarBatallasGanadas();
        perdidas = fachada.contarBatallasPerdidas();
        exito = Math.round(fachada.calcularExitoCampania()) + "%";
        detalleExito = "(" + ganadas + " de " + (ganadas + perdidas) + " batallas)";
        batallas = BatallaDto.listaDtos(fachada.listarBatallas());
    }

    public int getGanadas() {
        return ganadas;
    }

    public int getPerdidas() {
        return perdidas;
    }

    public String getExito() {
        return exito;
    }

    public List<BatallaDto> getBatallas() {
        return batallas;
    }

    public String getDetalleExito() {
        return detalleExito;
    }
}
