package uy.edu.ort.battleshipdemo.modelo;

import uy.edu.ort.battleshipdemo.modelo.Barco.Orientacion;

public class EstadoTerminada extends EstadoBatalla {

    public EstadoTerminada (Batalla b){
        super(b, Batalla.Estados.TERMINADA);
    }

    @Override
    public void iniciarCombate() throws BatallaNavalException {
        throw new BatallaNavalException("La batalla ya finalizo.");
    }

    @Override
    public void colocarFlotaMaquina() throws BatallaNavalException {
        throw new BatallaNavalException("Solo se puede colocar la flota de la maquina durante la preparacion.");
    }

    @Override
    public void colocarBarco(Barco barco, Cuadrante proa, Orientacion orientacion) throws BatallaNavalException{
         throw new BatallaNavalException("Solo se pueden colocar barcos durante la preparacion.");
    }

    @Override
    public void disparar(int fila, int columna) throws BatallaNavalException {
        throw new BatallaNavalException("La batalla ya finalizó");
    }

}
