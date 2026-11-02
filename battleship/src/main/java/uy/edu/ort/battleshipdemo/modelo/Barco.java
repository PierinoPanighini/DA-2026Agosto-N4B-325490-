package uy.edu.ort.battleshipdemo.modelo;

import java.util.ArrayList;
import java.util.List;

/*
Su nombre y tamaño los conoce su TipoBarco.
 */
public class Barco {

    private static int proximoId = 1;
    private final int id;

    private TipoBarco tipoBarco;

    private Orientacion orientacion;

    // Cuadrantes que ocupa en el océano. El primero sería la proa
    private List<Cuadrante> cuadrantes;

    public Barco(TipoBarco tipoBarco) {
        this.id = generarId();
        this.tipoBarco = tipoBarco;
        this.cuadrantes = new ArrayList<>();
    }

    private static synchronized int generarId() {
        return proximoId++;
    }

    public int getId() {
        return id;
    }

    public TipoBarco getTipoBarco() {
        return tipoBarco;
    }

    public Orientacion getOrientacion() {
        return orientacion;
    }

    public List<Cuadrante> getCuadrantes() {
        return cuadrantes;
    }

    public String getNombre() {
        return tipoBarco.getNombre();
    }

    public int getTamanio() {
        return tipoBarco.getTamanio();
    }

    // Recibe los cuadrantes ya validados por el oceano, empezando por la proa.
    public void ubicar(List<Cuadrante> cuadrantesAOcupar, Orientacion orientacion) {
        this.orientacion = orientacion;
        for (Cuadrante cuadrante : cuadrantesAOcupar) {
            cuadrantes.add(cuadrante);
            cuadrante.setBarco(this);
        }
    }

    public boolean estaUbicado() {
        return !cuadrantes.isEmpty();
    }

    // Un barco está sano porq aun no lo ubicaron, y está roto si su cuadrante fue disparado
    public int partesSanas() {
        if (!estaUbicado()) {
            return getTamanio();
        }
        int sanas = 0;
        for (Cuadrante cuadrante : cuadrantes) {
            if (!cuadrante.isDisparado()) {
                sanas++;
            }
        }
        return sanas;
    }

    public boolean estaHundido() {
        return estaUbicado() && partesSanas() == 0;
    }
}
