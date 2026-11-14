package uy.edu.ort.battleshipdemo.modelo;

public class Ronda {

    private int numero;
    private Disparo disparoJugador;
    private Disparo disparoMaquina;

    public Ronda(int numero, Disparo disparoJugador, Disparo disparoMaquina) throws BatallaNavalException {
        if (numero <= 0) {
            throw new BatallaNavalException("El numero de ronda debe ser positivo.");
        }
        if (disparoJugador == null) {
            throw new BatallaNavalException("La ronda debe tener un disparo del jugador.");
        }

        this.numero = numero;
        this.disparoJugador = disparoJugador;
        this.disparoMaquina = disparoMaquina;
    }

    public int getNumero() {
        return numero;
    }

    public Disparo getDisparoJugador() {
        return disparoJugador;
    }

    public Disparo getDisparoMaquina() {
        return disparoMaquina;
    }
}
