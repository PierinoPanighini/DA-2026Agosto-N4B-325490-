package uy.edu.ort.battleshipdemo.modelo;

public class TacticaEspiral extends Tactica {

    /*
     * Inicializa el nombre de esta especializacion de tactica.
     */
    public TacticaEspiral() {
        super("Espiral");
    }

    /*
     * Crea otra instancia de esta especializacion con su propio objetivo y progreso de partida.
     */
    @Override
    public Tactica crearNueva() {
        return new TacticaEspiral();
    }

    /*
     * Reconstruye una espiral desde el centro del oceano en cada llamada.
     * Utiliza tramos de longitudes 1, 1, 2, 2, 3, 3 y asi sucesivamente.
     * Al completar un tramo cambia de direccion y, cada dos tramos, aumenta su largo.
     * Las direcciones cambian columna +1, fila -1, columna -1 y fila +1.
     *
     * El recorrido puede pasar por coordenadas exteriores en tableros rectangulares.
     * Esas coordenadas forman parte de los pasos, pero solo se cuentan como revisadas
     * las que pertenecen al oceano. La espiral sigue expandiendose hasta cubrirlo.
     * Devuelve el primer cuadrante real sin disparar y salta los usados anteriormente.
     * Cada turno parte del mismo centro y se apoya en las marcas existentes.
     * Si recorre todos los cuadrantes reales, informa que no quedan disponibles.
     */
    @Override
    public Cuadrante elegirCuadrante() throws BatallaNavalException {
        Oceano oceano = getOceanoObjetivo();
        // La division entera fija el inicio central. Para 6 filas y 8 columnas se inicia en (3, 3).
        int fila = oceano.getFilas() / 2;
        int columna = (oceano.getColumnas() - 1) / 2;
        // Las direcciones 0, 1, 2 y 3 cambian columna +1, fila -1, columna -1 y fila +1.
        int direccion = 0;
        // Cantidad de pasos requerida para completar el tramo actual.
        int largo = 1;
        // Cantidad de pasos ya dados en el tramo actual.
        int pasos = 0;
        // Cuenta los tramos completados con el mismo largo. Cada dos tramos el largo aumenta.
        int usos = 0;
        // Cuenta solamente los cuadrantes del recorrido que pertenecen al oceano.
        int revisadas = 0;
        int total = oceano.getFilas() * oceano.getColumnas();

        while (revisadas < total) {
            // Solo consulta la matriz cuando la posicion del recorrido esta dentro del oceano.
            if (oceano.estaDentro(fila, columna)) {
                revisadas++;
                Cuadrante cuadrante = oceano.getCuadrante(fila, columna);
                if (!cuadrante.isDisparado()) {
                    return cuadrante;
                }
            }

            // Da un paso en la direccion actual, incluso si temporalmente sale de los limites.
            if (direccion == 0) {
                columna++;
            } else if (direccion == 1) {
                fila--;
            } else if (direccion == 2) {
                columna--;
            } else {
                fila++;
            }
            pasos++;

            // Al completar el tramo gira y reinicia la cuenta de pasos.
            if (pasos == largo) {
                if (direccion < 3) {
                    direccion = direccion + 1;
                } else {
                    direccion = 0;
                }
                pasos = 0;
                usos++;
                // Aumenta el largo cada dos giros para expandir la espiral.
                if (usos == 2) {
                    largo++;
                    usos = 0;
                }
            }
        }
        throw new BatallaNavalException("No hay cuadrantes disponibles para disparar");
    }
}
