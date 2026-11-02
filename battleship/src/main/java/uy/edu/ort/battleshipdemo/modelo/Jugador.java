package uy.edu.ort.battleshipdemo.modelo;

public class Jugador {

    private Flota flota;
    private Oceano oceano;

    public Jugador(Flota flota, Oceano oceano) {
        if (flota == null || oceano == null) {
            throw new IllegalArgumentException("El jugador debe tener una flota y un oceano.");
        }
        this.flota = flota;
        this.oceano = oceano;
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
