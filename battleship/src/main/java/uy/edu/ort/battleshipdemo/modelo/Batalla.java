package uy.edu.ort.battleshipdemo.modelo;

import java.util.ArrayList;
import java.util.List;

import uy.edu.ort.battleshipdemo.modelo.BatallaNavalException;
import uy.edu.ort.battleshipdemo.modelo.Flota;
import uy.edu.ort.battleshipdemo.modelo.Jugador;
import uy.edu.ort.battleshipdemo.modelo.Maquina;
import uy.edu.ort.battleshipdemo.modelo.Oceano;
import uy.edu.ort.battleshipdemo.modelo.Ronda;
import uy.edu.ort.battleshipdemo.modelo.Tactica;

public class Batalla {

    private static  int proximoId = 1;

    private final int id;
    private String lugar;
    private Jugador jugador;
    private Maquina maquina;
    private EstadoBatalla estado;
    private List<Ronda> rondas;
    private List<Barco> bajasEnemigas;
    private List<Barco> bajasAmigas;

    public Batalla(String lugar, Jugador jugador, Maquina maquina) {
        maquina.getTactica().configurarObjetivo(jugador.getOceano());
        this.id = generarId();
        this.lugar = lugar;
        this.jugador = jugador;
        this.maquina = maquina;
        this.estado = new EstadoEnPreparacion(this);
        this.rondas = new ArrayList<>();
        this.bajasEnemigas = new ArrayList<>();
        this.bajasAmigas = new ArrayList<>();
    }

    public void validarBatalla() { // FIXME: Chequear que tan necesarias son estas validaciones
        if (this.jugador == null || this.maquina == null) {
            throw new IllegalArgumentException("La batalla debe tener un jugador y una maquina.");
        }
        if (this.jugador.getFlota() == this.maquina.getFlota()
                || this.jugador.getOceano() == this.maquina.getOceano()) {
            throw new IllegalArgumentException("Los participantes deben tener flotas y oceanos independientes.");
        }
    }

    private static synchronized int generarId() {
        return proximoId++;
    }

    public enum Estados {
        EN_PREPARACION, // sin barcos posicionados o posicionándolos (CU02)
        EN_COMBATE, // se están jugando rondas (CU03)
        TERMINADA
    }

    public void colocarBarco(Barco barco, Cuadrante proa, Orientacion orientacion)
            throws BatallaNavalException {
        estado.colocarBarco(barco, proa, orientacion);
    }

    public void colocarFlotaMaquina() throws BatallaNavalException {
        estado.colocarFlotaMaquina();
    }

    public void validarFlotasColocadas() throws BatallaNavalException {
        if (!jugador.getFlota().estaCompletamenteUbicada()) {
            throw new BatallaNavalException("Faltan barcos por colocar en la flota del jugador.");
        }
        if (!maquina.getFlota().estaCompletamenteUbicada()) {
            throw new BatallaNavalException("Faltan barcos por colocar en la flota de la maquina.");
        }
    }

    // La solicitud debe traer el numero de ronda que estaba mostrando la vista.
    public synchronized  void generarDisparo(int fila, int columna, int numeroRondaEsperado)
            throws BatallaNavalException {
        if (numeroRondaEsperado != getNumeroProximaRonda()) {
            throw new BatallaNavalException(
                    "La ronda indicada ya no corresponde. Actualice la informacion de la batalla.");
        }
        estado.disparar(fila, columna);
        }

    public int getNumeroProximaRonda() {
        return rondas.size() + 1;
    }

    public boolean fueGanada() {
        return estado.getEstado() == Estados.TERMINADA
                && bajasEnemigas.size() > bajasAmigas.size();
    }

    public boolean fuePerdida() {
        return estado.getEstado() == Estados.TERMINADA
                && bajasAmigas.size() > bajasEnemigas.size();
    }

    // ESTADO batalla conoce su estado, saber si está en preparación
    public boolean estaEnPreparacion() {
        return estado.getEstado() == Estados.EN_PREPARACION;
    }

    public synchronized void iniciarCombate()
            throws BatallaNavalException {
        estado.iniciarCombate();
    }

    // SETTERS

    protected void setEstado(EstadoBatalla estado) {
        this.estado = estado;
    }

    // GETTERS
    public int getId() {
        return id;
    }

    public String getLugar() {
        return lugar;
    }

    public Jugador getJugador() {
        return jugador;
    }

    public Maquina getMaquina() {
        return maquina;
    }

    public Tactica getTactica() {
        return maquina.getTactica();
    }

    public Flota getFlotaJugador() {
        return jugador.getFlota();
    }

    public Flota getFlotaMaquina() {
        return maquina.getFlota();
    }

    public Oceano getOceanoJugador() {
        return jugador.getOceano();
    }

    public Oceano getOceanoMaquina() {
        return maquina.getOceano();
    }

    public synchronized Estados getEstado() {
        return estado.getEstado();
    }

    public List<Ronda> getRondas() {
        return rondas;
    }

    public List<Barco> getBajasEnemigas() {
        return bajasEnemigas;
    }

    public List<Barco> getBajasAmigas() {
        return bajasAmigas;
    }

    public boolean flotaAmigaUbicada() {
        return this.getJugador().flotaUbicada();
    }

}
