package uy.edu.ort.battleshipdemo.modelo;

import uy.edu.ort.battleshipdemo.modelo.Barco.Orientacion;
import uy.edu.ort.battleshipdemo.modelo.Disparo.ResultadoDisparo;

public class EstadoEnCombate extends EstadoBatalla{


    public EstadoEnCombate (Batalla b){
        super(b, Batalla.Estados.EN_COMBATE);
    }

    @Override
    public void iniciarCombate() throws BatallaNavalException {
        throw new BatallaNavalException("La batalla ya esta en combate.");
    }

    @Override
    public void colocarFlotaMaquina() throws BatallaNavalException {
        throw new BatallaNavalException("Solo se puede colocar la flota de la maquina durante la preparacion.");
    }

    @Override
    public void colocarBarco(Barco barco, Cuadrante proa, Orientacion orientacion) throws BatallaNavalException{
         throw new BatallaNavalException("Solo se pueden colocar barcos durante la preparacion.");
    }


    // Ronda completa: dispara el jugador y, si no gano, responde la maquina con su
    // tactica.
    @Override
    public void disparar(int fila, int columna) throws BatallaNavalException {
        Batalla batalla = getBatalla();
        Maquina maquina = batalla.getMaquina();
        Jugador jugador = batalla.getJugador();

        Disparo disparoJugador = generarDisparo(maquina.getOceano().getCuadrante(fila, columna));
        Barco bajaEnemiga = disparoJugador.getBarcoHundido();
        if (bajaEnemiga != null) {
            maquina.registrarBaja(bajaEnemiga);
        }
        Disparo disparoMaquina = null;
        if (maquina.getFlota().quedoTodaHundida()) {
            batalla.setEstado(new EstadoTerminada(batalla));
        } else {
            Cuadrante elegido = maquina.getTactica().elegirCuadrante();
            disparoMaquina = generarDisparo(elegido);
            Barco bajaAmiga = disparoMaquina.getBarcoHundido();
            if (bajaAmiga != null) {
                jugador.registrarBaja(bajaAmiga);
            }
            if (jugador.getFlota().quedoTodaHundida()) {
                batalla.setEstado(new EstadoTerminada(batalla));
            }
        }
        batalla.getRondas().add(new Ronda(batalla.getRondas().size() + 1, disparoJugador, disparoMaquina));
    }

    // Disparar es igual para el humano y para la máquina: solo cambia quién elige
    // el cuadrante.
    private Disparo generarDisparo(Cuadrante cuadrante) throws BatallaNavalException {
        ResultadoDisparo resultado = cuadrante.recibirDisparo();
        return new Disparo(cuadrante, resultado);
    }


}
