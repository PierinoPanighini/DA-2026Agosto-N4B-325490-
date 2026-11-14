package uy.edu.ort.battleshipdemo.presentador.DTOs;

import lombok.Data;

@Data 
public class InicializarDto {
    private int numeroCeldasVerticales;
    private int numeroCeldasHorizontales;
    private String escuchadorOceanoPropio;
    private String escuchadorOceanoEnemigo;

    public InicializarDto(int numeroCeldasVerticales, int numeroCeldasHorizontales) {
        this(numeroCeldasVerticales, numeroCeldasHorizontales, null, null);
    }

    public InicializarDto(int numeroCeldasVerticales, int numeroCeldasHorizontales, String escuchadorOceanoPropio, String escuchadorOceanoEnemigo) {
        this.numeroCeldasVerticales = numeroCeldasVerticales;
        this.numeroCeldasHorizontales = numeroCeldasHorizontales;
        this.escuchadorOceanoPropio = escuchadorOceanoPropio;
        this.escuchadorOceanoEnemigo = escuchadorOceanoEnemigo;
    }
}
