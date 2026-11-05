package uy.edu.ort.battleshipdemo.modelo;

import java.util.ArrayList;
import java.util.List;

public class Oceano {

    private int filas;
    private int columnas;
    private Cuadrante[][] cuadrantes;

    public Oceano(int filas, int columnas) throws BatallaNavalException {
        if (filas < 6 || filas > 12 || columnas < 6 || columnas > 12) {
            throw new BatallaNavalException("Las filas y columnas deben estar entre 6 y 12.");
        }

        this.filas = filas;
        this.columnas = columnas;
        this.cuadrantes = new Cuadrante[filas][columnas];

        for (int fila = 0; fila < filas; fila++) {
            for (int columna = 0; columna < columnas; columna++) {
                this.cuadrantes[fila][columna] = new Cuadrante(fila, columna);
            }
        }
    }

    public int getFilas() {
        return filas;
    }

    public int getColumnas() {
        return columnas;
    }

    public Cuadrante getCuadrante (int fila, int columna) throws BatallaNavalException {
        if (!estaDentro(fila, columna)) {
            throw new BatallaNavalException("Las coordenadas estan fuera del oceano.");//
            // cambiè el if para usar el metodo estaDentro
        }

        return cuadrantes[fila][columna];
    }

    public List<Cuadrante> listarCuadrantesOcupados() throws BatallaNavalException {
        List<Cuadrante> ocupados = new ArrayList<>();
        for (int fila = 0; fila < filas; fila++) {
            for (int columna = 0; columna < columnas; columna++) {
                Cuadrante cuadrante = getCuadrante(fila, columna);
                if (cuadrante.estaOcupado()) {
                    ocupados.add(cuadrante);
                }
            }
        }
        return ocupados;
    }

    public boolean estaDentro(int fila, int columna) {
        return fila >= 0 && fila < filas && columna >= 0 && columna < columnas;
    }

    // Se valida toda la ubicacion antes de modificar el barco o el tablero.
    public void colocarBarco(Barco barco, Cuadrante proa, Orientacion orientacion)
            throws BatallaNavalException {
        validarSolicitud(barco, proa, orientacion);
        List<Cuadrante> candidatos = calcularCuadrantes(barco, proa, orientacion);
        validarCuadrantesLibres(candidatos);
        validarSeparacion(candidatos);
        barco.ubicar(candidatos, orientacion);
    }

    private void validarSolicitud(Barco barco, Cuadrante proa, Orientacion orientacion)
            throws BatallaNavalException {
        if (barco == null) {
            throw new BatallaNavalException("Debe seleccionar un barco.");
        }
        if (proa == null) {
            throw new BatallaNavalException("Debe seleccionar un cuadrante de proa.");
        }
        if (orientacion == null) {
            throw new BatallaNavalException("Debe seleccionar una orientacion.");
        }
        if (barco.estaUbicado()) {
            throw new BatallaNavalException("El barco ya esta ubicado.");
        }
        if (barco.getTipoBarco() == null || barco.getTamanio() <= 0) {
            throw new BatallaNavalException("El barco debe tener un tipo y un tamanio valido.");
        }
        int fila = proa.getFila();
        int columna = proa.getColumna();
        if (!estaDentro(fila, columna) || getCuadrante(fila, columna) != proa) {
            throw new BatallaNavalException("La proa no está dentro de este oceano.");
        }
    }

    private List<Cuadrante> calcularCuadrantes(Barco barco, Cuadrante proa, Orientacion orientacion)
            throws BatallaNavalException {
        List<Cuadrante> candidatos = new ArrayList<>();
        for (int i = 0; i < barco.getTamanio(); i++) {
            int fila = proa.getFila();
            int columna = proa.getColumna();
            if (orientacion == Orientacion.VERTICAL) {
                fila = fila + i;
            }
            if (orientacion == Orientacion.HORIZONTAL) {
                columna = columna + i;
            }
            if (!estaDentro(fila, columna)) {
                throw new BatallaNavalException("El barco queda fuera de los limites del oceano.");
            }
            candidatos.add(getCuadrante(fila, columna));
        }
        return candidatos;
    }

    private void validarCuadrantesLibres(List<Cuadrante> candidatos) throws BatallaNavalException {
        for (Cuadrante cuadrante : candidatos) {
            if (cuadrante.estaOcupado()) {
                throw new BatallaNavalException("El barco se superpone con otro barco.");
            }
        }
    }

    private void validarSeparacion(List<Cuadrante> candidatos) throws BatallaNavalException {
        for (Cuadrante cuadrante : candidatos) {
            for (int deltaFila = -1; deltaFila <= 1; deltaFila++) {
                for (int deltaColumna = -1; deltaColumna <= 1; deltaColumna++) {
                    if (deltaFila != 0 || deltaColumna != 0) {
                        int filaVecina = cuadrante.getFila() + deltaFila;
                        int columnaVecina = cuadrante.getColumna() + deltaColumna;
                        if (estaDentro(filaVecina, columnaVecina)
                                && getCuadrante(filaVecina, columnaVecina).estaOcupado()) {
                            throw new BatallaNavalException(
                                    "Debe haber al menos un cuadrante de agua entre los barcos, incluso en diagonal.");
                        }
                    }

                }
            }
        }
    }

    /*CU03 */
    public List<Cuadrante> listarCuadrantesDisparados() throws BatallaNavalException {
    List<Cuadrante> disparados = new ArrayList<>();
    for (int fila = 0; fila < filas; fila++) {
        for (int columna = 0; columna < columnas; columna++) {
            Cuadrante cuadrante = getCuadrante(fila, columna);
            if (cuadrante.isDisparado()) {
                disparados.add(cuadrante);
            }
        }
    }
    return disparados;
}

}
