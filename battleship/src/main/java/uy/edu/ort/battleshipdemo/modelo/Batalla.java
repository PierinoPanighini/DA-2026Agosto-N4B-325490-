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
import uy.edu.ort.battleshipdemo.observador.Observable;
import uy.edu.ort.battleshipdemo.modelo.Barco.Orientacion;

public class Batalla extends Observable {

    private static int proximoId = 1;

    private final int id;
    private String lugar;
    private Jugador jugador;
    private Maquina maquina;
    private EstadoBatalla estado;
    private List<Ronda> rondas;

    public Batalla(String lugar, Jugador jugador, Maquina maquina) throws BatallaNavalException {
        this.lugar = lugar;
        this.jugador = jugador;
        this.maquina = maquina;
        this.estado = new EstadoEnPreparacion(this);
        this.rondas = new ArrayList<>();
        validar();
        maquina.getTactica().configurarObjetivo(jugador.getOceano());
        this.id = generarId();
    }

    // Comprueba que haya dos participantes con flotas y oceanos independientes.
    private void validar() throws BatallaNavalException {
        if (this.jugador == null || this.maquina == null) {
            throw new BatallaNavalException("La batalla debe tener un jugador y una maquina.");
        }
        if (this.jugador.getFlota() == this.maquina.getFlota()
                || this.jugador.getOceano() == this.maquina.getOceano()) {
            throw new BatallaNavalException("Los participantes deben tener flotas y oceanos independientes.");
        }
    }

    private static int generarId() {
        return proximoId++;
    }

    public enum Estados {
        EN_PREPARACION, // sin barcos posicionados o posicionándolos (CU02)
        EN_COMBATE, // se están jugando rondas (CU03)
        TERMINADA
    }

    public enum Eventos {
        RONDA_COMPLETADA,
        BARCO_COLOCADO,
        COMBATE_INICIADO,
        BATALLA_TERMINADA
    }

    // Delega en el jugador la busqueda de un barco propio por su identificador.
    public Barco buscarBarcoJugador(int idBarco) throws BatallaNavalException {
        return jugador.buscarBarco(idBarco);
    }

    // Delega en el jugador la consulta de un cuadrante de su oceano.
    public Cuadrante getCuadranteJugador(int fila, int columna) throws BatallaNavalException {
        return jugador.getCuadrante(fila, columna);
    }

    public void colocarBarco(Barco barco, Cuadrante proa, Orientacion orientacion) throws BatallaNavalException {
        estado.colocarBarco(barco, proa, orientacion);
        notificar(Eventos.BARCO_COLOCADO);
    }

    public void colocarFlotaMaquina() throws BatallaNavalException {
        estado.colocarFlotaMaquina();
    }

    // Solicita al estado resolver el disparo y comunica la ronda completada y el eventual fin.
    public void generarDisparo(int fila, int columna) throws BatallaNavalException {
        estado.disparar(fila, columna);
        //SI SE EFECTUO BIEN AHI ACTUALIZO LOS DISPAROS Y SI TERMINA TAMBIEN LOS TOTALES DE BATALLS DE LA SALA DE OPERACIONES
        notificar(Eventos.RONDA_COMPLETADA);
        if (estado.getEstado() == Estados.TERMINADA) {
            notificar(Eventos.BATALLA_TERMINADA);
        }
    }

    // Determina si la batalla terminada tiene mas bajas enemigas que propias.
    public boolean fueGanada() {
        return estado.getEstado() == Estados.TERMINADA
                && maquina.contarBajas() > jugador.contarBajas();
    }

    // Determina si la batalla terminada tiene mas bajas propias que enemigas.
    public boolean fuePerdida() {
        return estado.getEstado() == Estados.TERMINADA
                && jugador.contarBajas() > maquina.contarBajas();
    }

    // sobre ESTADO de batalla: saber si está en preparación
    public boolean estaEnPreparacion() {
        return estado.getEstado() == Estados.EN_PREPARACION;
    }

    public void iniciarCombate() throws BatallaNavalException {
        estado.iniciarCombate();
        // Avisa a los presentadores de esta preparacion para que pasen a la pantalla de combate.
        notificar(Eventos.COMBATE_INICIADO);
    }

    // SETTERS

    protected void setEstado(EstadoBatalla estado) {
        this.estado = estado;
    }

    // GETTERS
    public int getId() {
        return id;
    }

    public int getNumeroRonda() {
        if (estado.getEstado() == Estados.TERMINADA) {
            return rondas.size();
        }
        return getNumeroProximaRonda();
    }

    public int getNumeroProximaRonda() {
        return rondas.size() + 1;
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

    public Estados getEstado() {
        return estado.getEstado();
    }

    public List<Ronda> getRondas() {
        return rondas;
    }

    // Obtiene las bajas enemigas a traves de la maquina y su flota.
    public List<Barco> getBajasEnemigas() {
        return maquina.listarBajas();
    }

    // Obtiene las bajas amigas a traves del jugador y su flota.
    public List<Barco> getBajasAmigas() {
        return jugador.listarBajas();
    }

    public boolean flotaAmigaUbicada() {
        return this.getJugador().flotaUbicada();
    }

}
