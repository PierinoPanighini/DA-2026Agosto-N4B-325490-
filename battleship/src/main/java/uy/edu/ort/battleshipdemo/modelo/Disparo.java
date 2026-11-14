package uy.edu.ort.battleshipdemo.modelo;

/*
 Un disparo contra una celda del océano del oponente.
 Quién disparó (humano o máquina)
 */
public class Disparo {

    public enum ResultadoDisparo {
        AGUA,
        IMPACTO,
        HUNDIDO
    }


    // Celda a la que se disparó.
    private Cuadrante cuadrante;

    // Agua, impacto o hundido.
    private ResultadoDisparo resultado;

    public Disparo(Cuadrante cuadrante, ResultadoDisparo resultado) {
        this.cuadrante = cuadrante;
        this.resultado = resultado;
    }

    public Cuadrante getCuadrante() {
        return cuadrante;
    }

    public ResultadoDisparo getResultado() {
        return resultado;
    }

}
