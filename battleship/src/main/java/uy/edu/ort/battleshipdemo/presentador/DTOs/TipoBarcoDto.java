package uy.edu.ort.battleshipdemo.presentador.DTOs;

import java.util.ArrayList;
import java.util.List;

import uy.edu.ort.battleshipdemo.modelo.TipoBarco;

public class TipoBarcoDto {

    private final String nombre;
    private final int tamanio;

    public TipoBarcoDto(TipoBarco tipoBarco) {
        this.nombre = tipoBarco.getNombre();
        this.tamanio = tipoBarco.getTamanio();
    }

    public String getNombre() {
        return nombre;
    }

    public int getTamanio() {
        return tamanio;
    }

    public static List<TipoBarcoDto> listaDtos(List<TipoBarco> tiposBarco) {
        List<TipoBarcoDto> dtos = new ArrayList<>();
        for (TipoBarco tipoBarco : tiposBarco) {
            dtos.add(new TipoBarcoDto(tipoBarco));
        }
        return dtos;
    }
}
