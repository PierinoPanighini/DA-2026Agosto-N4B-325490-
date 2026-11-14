package uy.edu.ort.battleshipdemo.modelo;

import java.util.ArrayList;
import java.util.List;
import uy.edu.ort.battleshipdemo.modelo.Barco.Orientacion;

public class Oceano {

    private int filas;
    private int columnas;
    private Cuadrante[][] cuadrantes;

    public Oceano(int filas, int columnas) throws BatallaNavalException {
        this.filas = filas;
        this.columnas = columnas;
        validar();
        this.cuadrantes = new Cuadrante[filas][columnas];

        for (int fila = 0; fila < filas; fila++) {
            for (int columna = 0; columna < columnas; columna++) {
                this.cuadrantes[fila][columna] = new Cuadrante(fila, columna);
            }
        }
    }

    // Comprueba que las filas y columnas esten dentro de las dimensiones permitidas.
    private void validar() throws BatallaNavalException {
        if (filas < 6 || filas > 12 || columnas < 6 || columnas > 12) {
            throw new BatallaNavalException("Las filas y columnas deben estar entre 6 y 12.");
        }
    }

    // Comprueba que el tamanio total de la flota ocupe menos del 25% de este oceano.
    public void validarDensidad(Flota flota) throws BatallaNavalException {
        if (flota == null) {
            throw new BatallaNavalException("Debe indicar una flota para validar su densidad.");
        }
        if (flota.tamanioTotal() * 4 >= filas * columnas) {
            throw new BatallaNavalException("La flota debe ocupar menos del 25% del oceano.");
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
            throw new BatallaNavalException("Las coordenadas estan fuera del oceano.");
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

    /*
     * Valida la propuesta completa de un barco a partir de su proa y orientacion.
     * Recibe los candidatos cuando todas las comprobaciones terminaron y solicita
     * al barco guardar la orientacion y enlazarse con esos cuadrantes.
     * Se utiliza tanto con barcos reales como con copias de prueba.
     */
    public void colocarBarco(Barco barco, Cuadrante proa, Orientacion orientacion) throws BatallaNavalException {
        List<Cuadrante> candidatos = validarColocacion(barco, proa, orientacion);
        barco.ubicar(candidatos, orientacion);
    }

    /*
     * Reune las comprobaciones de una propuesta antes de aplicarla.
     * Comprueba los datos y que la proa pertenezca a este oceano.
     * Calcula los candidatos, verifica que esten libres y revisa su separacion
     * respecto a los barcos colocados. Devuelve la lista completa de candidatos.
     * La maquina tambien consulta este metodo para comprobar las posiciones reales
     * antes de comenzar a aplicar toda la distribucion.
     */
    List<Cuadrante> validarColocacion(Barco barco, Cuadrante proa, Orientacion orientacion) throws BatallaNavalException {
        validarSolicitud(barco, proa);
        List<Cuadrante> candidatos = calcularCuadrantes(barco, proa, orientacion);
        validarCuadrantesLibres(candidatos);
        validarSeparacion(candidatos);
        return candidatos;
    }

    /*
     * Comprueba que el barco siga sin ubicar y que la proa pertenezca a este oceano.
     * Examina las coordenadas de la proa y su identidad dentro de la matriz.
     * La comparacion de referencias exige que sea el cuadrante de este oceano,
     * incluso si otro cuadrante tiene los mismos valores de fila y columna.
     */
    private void validarSolicitud(Barco barco, Cuadrante proa) throws BatallaNavalException {
        if (barco.estaUbicado()) {
            throw new BatallaNavalException("El barco ya esta ubicado.");
        }
        int fila = proa.getFila();
        int columna = proa.getColumna();
        if (!estaDentro(fila, columna) || getCuadrante(fila, columna) != proa) {
            throw new BatallaNavalException("La proa no está dentro de este oceano.");
        }
    }

    /*
     * Construye tantos candidatos como partes tenga el barco.
     * En cada iteracion parte de la proa y suma el indice a la fila si es vertical
     * o a la columna si es horizontal. El indice cero corresponde a la proa.
     * Si una parte sale de los limites, rechaza la propuesta completa.
     * Los candidatos son referencias a los cuadrantes de la matriz de este oceano.
     */
    private List<Cuadrante> calcularCuadrantes(Barco barco, Cuadrante proa, Orientacion orientacion) throws BatallaNavalException {
        List<Cuadrante> candidatos = new ArrayList<>();
        // El indice representa la distancia a la proa y recorre desde cero hasta tamanio - 1.
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

    /*
     * Consulta cada cuadrante que ocuparia el barco.
     * Si alguno ya tiene un barco, rechaza toda la propuesta por superposicion.
     */
    private void validarCuadrantesLibres(List<Cuadrante> candidatos) throws BatallaNavalException {
        for (Cuadrante cuadrante : candidatos) {
            if (cuadrante.estaOcupado()) {
                throw new BatallaNavalException("El barco se superpone con otro barco.");
            }
        }
    }

    /*
     * Revisa los ocho vecinos de cada cuadrante candidato.
     * Combina desplazamientos de fila y columna entre -1 y 1 y omite el par (0, 0),
     * que representa el propio cuadrante. Primero comprueba que el vecino este dentro.
     * Un vecino ocupado rechaza la propuesta por falta de separacion.
     * Las combinaciones incluyen diagonales y exigen un cuadrante de agua entre barcos.
     * Los candidatos todavia no se colocaron, por lo que los barcos encontrados
     * pertenecen a las ubicaciones anteriores.
     */
    private void validarSeparacion(List<Cuadrante> candidatos) throws BatallaNavalException {
        for (Cuadrante cuadrante : candidatos) {
            for (int deltaFila = -1; deltaFila <= 1; deltaFila++) {
                for (int deltaColumna = -1; deltaColumna <= 1; deltaColumna++) {
                    // El unico par excluido es el que tiene ambos desplazamientos en cero.
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
