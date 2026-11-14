package uy.edu.ort.battleshipdemo.modelo;
import uy.edu.ort.battleshipdemo.modelo.Batalla.Estados;
import uy.edu.ort.battleshipdemo.modelo.Barco.Orientacion;



public abstract class EstadoBatalla  {

    private Batalla batalla;
    protected  Batalla.Estados estado;


    public EstadoBatalla(Batalla batalla, Estados estado) {
        this.batalla = batalla;
        this.estado = estado;
    }

    public Batalla getBatalla() {
        return batalla;
    }


    public Batalla.Estados getEstado() {
        return estado;
    }


    public abstract void colocarFlotaMaquina() throws BatallaNavalException;

    public abstract void disparar(int fila, int columna) throws BatallaNavalException;

    public abstract void colocarBarco(Barco barco, Cuadrante proa, Orientacion orientacion) throws BatallaNavalException;

    public abstract void iniciarCombate() throws BatallaNavalException;

}
