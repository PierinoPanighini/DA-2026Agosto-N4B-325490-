package uy.edu.ort.battleshipdemo.presentador.DTOs;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import uy.edu.ort.battleshipdemo.modelo.Barco;
import uy.edu.ort.battleshipdemo.modelo.Cuadrante;

@Getter
public class BarcoDto {
    private final int id;
    private final String nombre;
    private final int tamanio;
    private final List<ValorDto> cuadrantes;

    public BarcoDto(Barco barco) {
        this.id = barco.getId();
        this.nombre = barco.getNombre();
        this.tamanio = barco.getTamanio();
        this.cuadrantes = new ArrayList<>();
        for (Cuadrante cuadrante : barco.getCuadrantes()) {
            cuadrantes.add(new ValorDto(cuadrante.getFila(), cuadrante.getColumna(), "barco"));
        }
    }
}
