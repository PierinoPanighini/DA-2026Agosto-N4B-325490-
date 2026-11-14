package uy.edu.ort.battleshipdemo.presentador.DTOs;

import java.util.ArrayList;
import java.util.List;

import uy.edu.ort.battleshipdemo.modelo.Tactica;

public class TacticaDto {

    private final String nombre;

    public TacticaDto(Tactica tactica) {
        this.nombre = tactica.getNombre();
    }

    public String getNombre() {
        return nombre;
    }

    public static List<TacticaDto> listaDtos(List<Tactica> tacticas) {
        List<TacticaDto> dtos = new ArrayList<>();
        for (Tactica tactica : tacticas) {
            dtos.add(new TacticaDto(tactica));
        }
        return dtos;
    }
}
