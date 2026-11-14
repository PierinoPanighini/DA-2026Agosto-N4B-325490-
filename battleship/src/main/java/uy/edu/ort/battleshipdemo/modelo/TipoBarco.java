package uy.edu.ort.battleshipdemo.modelo;

public class TipoBarco {

    private String nombre;
    private int tamanio;

    public TipoBarco(String nombre, int tamanio) throws BatallaNavalException {
        this.nombre = nombre;
        this.tamanio = tamanio;
        validar();
    }

    // Comprueba que el tipo de barco tenga un tamanio positivo.
    private void validar() throws BatallaNavalException {
        if (tamanio <= 0) {
            throw new BatallaNavalException("El tamanio del tipo de barco debe ser positivo.");
        }
    }

    public String getNombre() {
        return nombre;
    }

    public int getTamanio() {
        return tamanio;
    }
}
