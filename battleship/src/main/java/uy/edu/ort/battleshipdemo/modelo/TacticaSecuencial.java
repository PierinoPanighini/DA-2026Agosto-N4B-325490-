package uy.edu.ort.battleshipdemo.modelo;

public class TacticaSecuencial extends Tactica {

    /*
     * Inicializa el nombre de esta especializacion de tactica.
     */
    public TacticaSecuencial() {
        super("Secuencial");
    }

    /*
     * Crea otra instancia de esta especializacion con su propio objetivo y progreso de partida.
     */
    @Override
    public Tactica crearNueva() {
        return new TacticaSecuencial();
    }

    /*
     * Recorre las filas desde cero y, dentro de cada fila, las columnas desde cero.
     * Devuelve el primer cuadrante que todavia no recibio un disparo.
     * Cada llamada reconstruye el recorrido desde el comienzo. Las marcas que el
     * combate dejo en turnos anteriores permiten saltar los cuadrantes utilizados.
     * Agotar todas las filas sin candidatos produce una excepcion de dominio.
     */
    @Override
    public Cuadrante elegirCuadrante() throws BatallaNavalException {
        Oceano oceano = getOceanoObjetivo();
        // El bucle exterior elige una fila y el interior recorre todas sus columnas.
        for (int fila = 0; fila < oceano.getFilas(); fila++) {
            for (int columna = 0; columna < oceano.getColumnas(); columna++) {
                Cuadrante cuadrante = oceano.getCuadrante(fila, columna);

                // Este primer candidato disponible termina la busqueda de este turno.
                if (!cuadrante.isDisparado()) {
                    return cuadrante;
                }
            }
        }
        throw new BatallaNavalException("No hay cuadrantes disponibles para disparar");
    }

}
