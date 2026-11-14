package uy.edu.ort.battleshipdemo.modelo;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TacticaCazaAtaque extends Tactica {

    // Referencia al cuadrante real elegido en el turno anterior.
    private Cuadrante ultimoElegido;
    // Impactos acumulados mientras se persigue un barco que sigue a flote.
    private List<Cuadrante> impactos;
    private Random azar;

    /*
     * Inicializa el nombre de esta especializacion de tactica.
     * Comienza sin una eleccion anterior, con la lista de impactos vacia y su generador de azar.
     */
    public TacticaCazaAtaque() {
        super("Caza y ataque");
        this.ultimoElegido = null;
        this.impactos = new ArrayList<>();
        this.azar = new Random();
    }

    /*
     * Crea otra instancia de esta especializacion con su propio objetivo y progreso de partida.
     */
    @Override
    public Tactica crearNueva() {
        return new TacticaCazaAtaque();
    }

    /*
     * Combina una busqueda al azar con la persecucion de un barco alcanzado.
     * Primero incorpora lo ocurrido en el disparo elegido en el turno anterior.
     * Con impactos pendientes intenta continuar por vecinos del barco.
     * Si no hay impactos o no consigue una continuacion, elige al azar.
     * Guarda la referencia elegida para consultar su resultado en la proxima llamada,
     * una vez que el combate haya efectuado el disparo sobre ese cuadrante.
     */
    @Override
    public Cuadrante elegirCuadrante() throws BatallaNavalException {
        Oceano oceano = getOceanoObjetivo();
        // Consulta los efectos del disparo anterior antes de elegir el siguiente.
        actualizarImpactos();
        Cuadrante elegido = null;
        // Con impactos pendientes intenta continuar el ataque.
        if (!impactos.isEmpty()) {
            elegido = buscarVecinoLibre(oceano);
        }
        // Sin un candidato de ataque, busca entre todos los cuadrantes sin disparar.
        if (elegido == null) {
            elegido = elegirAlAzar(oceano);
        }
        // Conserva la referencia para incorporar su resultado en el proximo turno.
        ultimoElegido = elegido;
        return elegido;
    }

    /*
     * Consulta el cuadrante elegido anteriormente, cuyo disparo ya se resolvio.
     * En el primer turno o ante agua conserva los impactos que ya tuviera.
     * Si el barco alcanzado esta hundido, vacia la lista y vuelve a la caza.
     * Si sigue a flote, agrega el cuadrante a los impactos para orientar el ataque.
     * La referencia al cuadrante real permite ver los cambios que hizo el combate.
     */
    private void actualizarImpactos() {
        if (ultimoElegido == null || !ultimoElegido.estaOcupado()) {
            return;
        }
        if (ultimoElegido.getBarco().estaHundido()) {
            impactos.clear();
        } else {
            impactos.add(ultimoElegido);
        }
    }

    /*
     * Con un impacto consulta sus cuatro vecinos mediante vecinaLibre().
     * Con dos o mas, deduce la orientacion comparando las filas de los dos primeros.
     * Obtiene las posiciones menor y mayor de los impactos sobre esa linea.
     * Intenta extenderla primero por el extremo menor y luego por el mayor.
     * Cada candidato debe estar dentro del oceano y sin disparar.
     * Devuelve null si se agotan ambas continuaciones, permitiendo volver al azar.
     */
    private Cuadrante buscarVecinoLibre(Oceano oceano) throws BatallaNavalException {
        Cuadrante primero = impactos.get(0);
        if (impactos.size() == 1) {
            return vecinaLibre(oceano, primero);
        }
        // La coincidencia de filas de los dos primeros impactos determina la orientacion horizontal.
        boolean horizontal = primero.getFila() == impactos.get(1).getFila();
        int menor = posicionEnLinea(primero, horizontal);
        int mayor = menor;
        // Obtiene los extremos considerando todos los impactos, cualquiera sea su orden.
        for (Cuadrante impacto : impactos) {
            int posicion = posicionEnLinea(impacto, horizontal);
            if (posicion < menor) {
                menor = posicion;
            }
            if (posicion > mayor) {
                mayor = posicion;
            }
        }
        // En horizontal mantiene fija la fila y prueba las columnas exteriores al tramo alcanzado.
        if (horizontal) {
            Cuadrante extremo = libre(oceano, primero.getFila(), menor - 1);
            if (extremo == null) {
                extremo = libre(oceano, primero.getFila(), mayor + 1);
            }
            return extremo;
        }
        // En vertical mantiene fija la columna y prueba las filas exteriores al tramo alcanzado.
        Cuadrante extremo = libre(oceano, menor - 1, primero.getColumna());
        if (extremo == null) {
            extremo = libre(oceano, mayor + 1, primero.getColumna());
        }
        return extremo;
    }

    /*
     * Con un unico impacto todavia debe descubrir la orientacion del barco.
     * Prueba los vecinos en el orden fila +1, fila -1, columna +1 y columna -1.
     * Devuelve el primero que este dentro del oceano y sin disparar.
     * Un vecino rechazado permite consultar el siguiente. Agotar los cuatro devuelve null.
     */
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

    /*
     * Obtiene la coordenada que varia a lo largo del barco perseguido.
     * Para la orientacion horizontal utiliza la columna y para la vertical la fila.
     * Asi ambos extremos se calculan con el mismo recorrido de la lista de impactos.
     */
    private int posicionEnLinea(Cuadrante cuadrante, boolean horizontal) {
        if (horizontal) {
            return cuadrante.getColumna();
        }
        return cuadrante.getFila();
    }

    /*
     * Devuelve el cuadrante real cuando esta dentro del oceano y sin disparar.
     * El operador && evalua primero los limites y evita acceder a coordenadas exteriores.
     * Un cuadrante exterior o ya disparado devuelve null para probar otro candidato.
     */
    private Cuadrante libre(Oceano oceano, int fila, int columna) throws BatallaNavalException {
        if (oceano.estaDentro(fila, columna) && !oceano.getCuadrante(fila, columna).isDisparado()) {

            return oceano.getCuadrante(fila, columna);
        }
        return null;
    }

    /*
     * Reune todos los cuadrantes sin disparar recorriendo las filas y columnas.
     * Elige un indice entre cero y el tamanio de la lista menos uno.
     * Cada cuadrante disponible aparece una vez y tiene la misma probabilidad de eleccion.
     * Si la lista esta vacia, informa que se agotaron los cuadrantes.
     */
    private Cuadrante elegirAlAzar(Oceano oceano) throws BatallaNavalException {
        List<Cuadrante> libres = new ArrayList<>();

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
