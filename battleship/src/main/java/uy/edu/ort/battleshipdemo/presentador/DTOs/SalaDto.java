package uy.edu.ort.battleshipdemo.presentador.DTOs;

import java.util.List;

public class SalaDto {

    private int ganadas;
    private int perdidas;
    private String exito;
    private String detalleExito;
    private List<BatallaDto> batallas;

    // Guarda los datos de la sala que el presentador ya preparo para la vista.
    public SalaDto(int ganadas, int perdidas, String exito, String detalleExito, List<BatallaDto> batallas) {
        this.ganadas = ganadas;
        this.perdidas = perdidas;
        this.exito = exito;
        this.detalleExito = detalleExito;
        this.batallas = batallas;
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
