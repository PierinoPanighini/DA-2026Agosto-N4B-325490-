package uy.edu.ort.battleshipdemo.modelo;

import uy.edu.ort.battleshipdemo.modelo.BatallaNavalException;
import uy.edu.ort.battleshipdemo.modelo.Jugador;
import uy.edu.ort.battleshipdemo.modelo.Barco.Orientacion;

public class EstadoEnPreparacion extends EstadoBatalla {

    public EstadoEnPreparacion(Batalla b) {
        super(b, Batalla.Estados.EN_PREPARACION);
    }

    @Override
    public void colocarFlotaMaquina() throws BatallaNavalException {
        getBatalla().getMaquina().colocarFlota();
    }

    @Override
    public void disparar(int fila, int columna) throws BatallaNavalException {
        throw new BatallaNavalException("La batalla esta en preparación");
    }

    @Override
    public void colocarBarco(Barco barco, Cuadrante proa, Orientacion orientacion) throws BatallaNavalException {

        Jugador jugador = getBatalla().getJugador();

        if (!jugador.getFlota().contieneBarco(barco)) {
            throw new BatallaNavalException("No puede ubicar un barco que no pertenece a su flota.");
        }
        jugador.getOceano().colocarBarco(barco, proa, orientacion);
    }

    /*
     * Comprueba una vez que el jugador haya colocado todos sus barcos.
     * Si falta el despliegue de la maquina, lo resuelve antes de pasar a combate.
     * Un fallo durante el despliegue interrumpe el proceso antes de cambiar el estado.
     */
    @Override
    public void iniciarCombate() throws BatallaNavalException {
        Batalla batalla = getBatalla();

        if (!batalla.getFlotaJugador().estaCompletamenteUbicada()) {
            throw new BatallaNavalException(
                    "Debe posicionar todos sus barcos primero");
        }

        if (!batalla.getFlotaMaquina().estaCompletamenteUbicada()) {
            colocarFlotaMaquina();
        }

        // colocarFlotaMaquina() retorna cuando completo el despliegue y resolvio sus intentos internos.
        batalla.setEstado(new EstadoEnCombate(batalla));
    }
}
