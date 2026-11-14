package uy.edu.ort.battleshipdemo.modelo;

import java.util.List;

public class Jugador {

    private Flota flota;
    private Oceano oceano;

    public Jugador(Flota flota, Oceano oceano) throws BatallaNavalException {
        this.flota = flota;
        this.oceano = oceano;
        validar();
    }

    // Comprueba las referencias obligatorias y solicita al oceano validar la densidad de la flota.
    private void validar() throws BatallaNavalException {
        if (flota == null || oceano == null) {
            throw new BatallaNavalException("El jugador debe tener una flota y un oceano.");
        }
        oceano.validarDensidad(flota);
    }

    // Busca un barco de la flota propia e informa si el identificador no corresponde a ninguno.
    public Barco buscarBarco(int idBarco) throws BatallaNavalException {
        Barco barco = flota.buscarBarco(idBarco);
        if (barco == null) {
            throw new BatallaNavalException("No se encontro ese barco en la flota del jugador.");
        }
        return barco;
    }

    // Solicita al oceano propio el cuadrante indicado, con su validacion de coordenadas.
    public Cuadrante getCuadrante(int fila, int columna) throws BatallaNavalException {
        return oceano.getCuadrante(fila, columna);
    }

    // Consulta en la flota los barcos del jugador que estan hundidos.
    public List<Barco> listarBajas() {
        return flota.listarBajas();
    }

    // Consulta en la flota la cantidad de barcos del jugador hundidos.
    public int contarBajas() {
        return flota.contarBajas();
    }

    public Flota getFlota() {
        return flota;
    }

    public Oceano getOceano() {
        return oceano;
    }

    public boolean flotaUbicada(){
        return this.getFlota().estaCompletamenteUbicada();
    }
}
