package uy.edu.ort.battleshipdemo.modelo;

import java.util.ArrayList;
import java.util.List;

/*
Su nombre y tamaño los conoce su TipoBarco.
 */
public class Barco {

    public enum Orientacion {
        VERTICAL,
        HORIZONTAL
    }


    private static int proximoId = 1;
    private final int id;

    private TipoBarco tipoBarco;

    private Orientacion orientacion;

    // Cuadrantes que ocupa en el océano. El primero sería la proa
    private List<Cuadrante> cuadrantes;

    public Barco(TipoBarco tipoBarco) throws BatallaNavalException {
        this.tipoBarco = tipoBarco;
        this.cuadrantes = new ArrayList<>();
        validar();
        this.id = generarId();
    }

    // Comprueba que el barco tenga un tipo asignado al construirse.
    private void validar() throws BatallaNavalException {
        if (tipoBarco == null) {
            throw new BatallaNavalException("El barco debe tener un tipo.");
        }
    }

    private static int generarId() {
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

    /*
     * Devuelve la proa guardada al comienzo de la ubicacion del barco.
     * Mientras el barco siga sin ubicar devuelve null.
     */
    public Cuadrante getProa() {
        if (!estaUbicado()) {
            return null;
        }
        return cuadrantes.get(0);
    }

    public String getNombre() {
        return tipoBarco.getNombre();
    }

    public int getTamanio() {
        return tipoBarco.getTamanio();
    }

    /*
     * Recibe los cuadrantes ya validados por el oceano, empezando por la proa.
     * Guarda la orientacion y agrega cada cuadrante a su ubicacion.
     * Cada cuadrante guarda a su vez la referencia a este barco.
     * Ambos objetos quedan vinculados a la misma colocacion.
     */
    public void ubicar(List<Cuadrante> cuadrantesAOcupar, Orientacion orientacion) {
        this.orientacion = orientacion;
        for (Cuadrante cuadrante : cuadrantesAOcupar) {
            cuadrantes.add(cuadrante);
            cuadrante.setBarco(this);
        }
    }

    /*
     * Deshace la colocacion de una copia durante la busqueda por retroceso.
     * Libera en cada cuadrante la referencia a este barco, vacia su ubicacion y borra
     * la orientacion para que pueda recibir una nueva proa.
     * La busqueda lo utiliza cuando una posicion valida del barco actual impide
     * completar las ubicaciones de los barcos siguientes.
     */
    void desubicar() {
        for (Cuadrante cuadrante : cuadrantes) {
            cuadrante.setBarco(null);
        }
        cuadrantes.clear();
        orientacion = null;
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
