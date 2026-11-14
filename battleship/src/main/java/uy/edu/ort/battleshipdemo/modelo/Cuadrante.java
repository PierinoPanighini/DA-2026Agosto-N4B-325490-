package uy.edu.ort.battleshipdemo.modelo;

import uy.edu.ort.battleshipdemo.modelo.Disparo.ResultadoDisparo;

public class Cuadrante {

    private int fila;
    private int columna;

    //Barco que ocupa esta celda, si es null, la celda es agua.
    private Barco barco;

    private boolean disparado;

    public Cuadrante(int fila, int columna) {
        this.fila = fila;
        this.columna = columna;
        this.barco = null;
        this.disparado = false;
    }

    public int getFila() {
        return fila;
    }

    public int getColumna() {
        return columna;
    }

    public Barco getBarco() {
        return barco;
    }

    // Setters:

    public void setBarco(Barco barco) {
        this.barco = barco; //cuando se ubica en el océano.
    }

    public boolean isDisparado() {
        return disparado;
    }

    public boolean estaOcupado() {
        return barco != null;
    }

    // Marca la celda como disparada y devuelve el resultado.
    // Si el disparo alcanza la ultima parte sana, el barco queda hundido.
    public ResultadoDisparo recibirDisparo() throws BatallaNavalException {
        if (disparado) {
            throw new BatallaNavalException("Esa celda ya fue disparada");
        }
        disparado = true;
        if (!estaOcupado()) {
            return ResultadoDisparo.AGUA;
        }
        if (barco.estaHundido()) {
            return ResultadoDisparo.HUNDIDO;
        }
        return ResultadoDisparo.IMPACTO;
    }

}
