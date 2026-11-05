package uy.edu.ort.battleshipdemo.modelo;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TacticaCazaAtaque extends Tactica {

    private Cuadrante ultimoElegido;
    private List<Cuadrante> impactos;
    private Random azar;

    public TacticaCazaAtaque() {
        super("Caza y ataque");
        this.ultimoElegido = null;
        this.impactos = new ArrayList<>();
        this.azar = new Random();
    }

    @Override
    public Tactica crearNueva() {
        return new TacticaCazaAtaque();
    } // nueva instancia de la misma clase de la tactica.

    @Override
    public Cuadrante elegirCuadrante() throws BatallaNavalException {
        Oceano oceano = getOceanoObjetivo();
        actualizarImpactos();
        Cuadrante elegido = null;
        if (!impactos.isEmpty()) { // estoy en modo ataque
            elegido = buscarVecinoLibre(oceano);
        }
        if (elegido == null) { // caza (o ataque sin vecinas libres)
            elegido = elegirAlAzar(oceano);
        }
        ultimoElegido = elegido;
        return elegido;
    }

    private void actualizarImpactos() {
        if (ultimoElegido == null || !ultimoElegido.estaOcupado()) {// primer turno o fue agua
            return;
        }
        if (ultimoElegido.getBarco().estaHundido()) {
            impactos.clear(); // vuelve a caza
        } else {
            impactos.add(ultimoElegido); // fue impacto
        }
    }

    private Cuadrante buscarVecinoLibre(Oceano oceano) throws BatallaNavalException {
        Cuadrante primero = impactos.get(0);
        if (impactos.size() == 1) {
            return vecinaLibre(oceano, primero);
        }
        boolean horizontal = primero.getFila() == impactos.get(1).getFila();
        int menor = posicionEnLinea(primero, horizontal);
        int mayor = menor;
        for (Cuadrante impacto : impactos) {
            int posicion = posicionEnLinea(impacto, horizontal);
            if (posicion < menor) {
                menor = posicion;
            }
            if (posicion > mayor) {
                mayor = posicion;
            }
        }
        if (horizontal) {
            Cuadrante extremo = libre(oceano, primero.getFila(), menor - 1);
            if (extremo == null) {
                extremo = libre(oceano, primero.getFila(), mayor + 1);
            }
            return extremo;
        }
        Cuadrante extremo = libre(oceano, menor - 1, primero.getColumna());
        if (extremo == null) {
            extremo = libre(oceano, mayor + 1, primero.getColumna());
        }
        return extremo;
    }

    // Con un solo impacto no se sabe la orientación: se prueban las 4 vecinas.
    private Cuadrante vecinaLibre(Oceano oceano, Cuadrante impacto) throws BatallaNavalException {
        int f = impacto.getFila();
        int c = impacto.getColumna();
        Cuadrante vecino = libre(oceano, f + 1, c);
        if (vecino == null) {
            vecino = libre(oceano, f - 1, c);
        }
        if (vecino == null) {
            vecino = libre(oceano, f, c + 1);
        }
        if (vecino == null) {
            vecino = libre(oceano, f, c - 1);
        }
        return vecino;
    }

    // En un barco horizontal lo que cambia es la columna; en uno vertical, la fila.
    private int posicionEnLinea(Cuadrante cuadrante, boolean horizontal) {
        if (horizontal) {
            return cuadrante.getColumna();
        }
        return cuadrante.getFila();
    }

    // Devuelve el cuadrante si está dentro y sin disparar; si no null.
    private Cuadrante libre(Oceano oceano, int fila, int columna) throws BatallaNavalException {
        if (oceano.estaDentro(fila, columna) && !oceano.getCuadrante(fila, columna).isDisparado()) { // dentro y no
                                                                                                     // disparado
            return oceano.getCuadrante(fila, columna);
        }
        return null;
    }

    private Cuadrante elegirAlAzar(Oceano oceano) throws BatallaNavalException {
        List<Cuadrante> libres = new ArrayList<>();
        // recorrer el océano como en la Secuencial y agregar a "libres" los no
        // disparados
        for (int f = 0; f < oceano.getFilas(); f++) {
            for (int c = 0; c < oceano.getColumnas(); c++) {
                Cuadrante cuadrante = oceano.getCuadrante(f, c);
                if (!cuadrante.isDisparado()) {
                    libres.add(cuadrante);
                }
            }
        }

        if (libres.isEmpty()) {
            throw new BatallaNavalException("No hay cuadrantes disponibles para disparar");
        }
        return libres.get(azar.nextInt(libres.size()));
    }
}
