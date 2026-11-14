package uy.edu.ort.battleshipdemo.presentador.DTOs;

import lombok.Data;

@Data 
public class ValorDto {
    private int coordenadaVertical;
    private int coordenadaHorizontal;
    private String valor;

    public ValorDto(int coordenadaVertical, int coordenadaHorizontal, String valor) {
        this.coordenadaHorizontal = coordenadaHorizontal;
        this.coordenadaVertical = coordenadaVertical;
        this.valor = valor;
    }
}
