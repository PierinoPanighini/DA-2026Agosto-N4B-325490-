package uy.edu.ort.battleshipdemo.modelo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import uy.edu.ort.battleshipdemo.modelo.Barco.Orientacion;

public class Maquina {

    private static int MAX_INTENTOS_POR_BARCO = 500;
    private static int MAX_INTENTOS_FLOTA = 100;

    private Flota flota;
    private Oceano oceano;
    private Tactica tactica;

    public Maquina(Flota flota, Oceano oceano, Tactica tactica) throws BatallaNavalException {
        this.flota = flota;
        this.oceano = oceano;
        this.tactica = tactica;
        validar();
        // Cada maquina recibe una instancia propia del algoritmo seleccionado.
        this.tactica = tactica.crearNueva();
    }

    // Comprueba las referencias obligatorias y solicita al oceano validar la densidad de la flota.
    private void validar() throws BatallaNavalException {
        if (flota == null || oceano == null || tactica == null) {
            throw new BatallaNavalException("La maquina debe tener flota, oceano y tactica.");
        }
        oceano.validarDensidad(flota);
    }

    /*
     * Organiza el despliegue completo de la flota de la maquina.
     * Obtiene una copia de la lista de barcos y la ordena del mas grande al mas pequeno.
     * Prueba hasta MAX_INTENTOS_FLOTA distribuciones aleatorias completas.
     * Cada intento utiliza barcos y un oceano temporales para ensayar las posiciones.
     * La primera distribucion completa se traslada a los barcos reales.
     * Si se agotan los intentos, busca mediante retroceso hasta encontrar una solucion
     * o comprobar que las reglas impiden distribuir esa configuracion.
     */
    public void colocarFlota() throws BatallaNavalException {

        // La lista es nueva y contiene las referencias a los barcos reales de la flota.
        List<Barco> barcos = flota.getBarcos();
        // Los grandes se colocan primero porque necesitan espacios continuos mas largos.
        barcos.sort(Comparator.comparingInt(Barco::getTamanio).reversed());
        Random azar = new Random();

        // Este limite cuenta distribuciones completas. Cada barco tiene su propio limite interno.
        for (int intento = 0; intento < MAX_INTENTOS_FLOTA; intento++) {
            List<Barco> distribucion = probarDistribucion(barcos, azar);
            // Una lista indica que todas las copias consiguieron una posicion valida.
            if (distribucion != null) {
                aplicarDistribucion(barcos, distribucion);
                return;
            }
        }
        // El retroceso comienza cuando se agotan las distribuciones aleatorias.
        List<Barco> distribucion = buscarDistribucionConRetroceso(barcos);
        aplicarDistribucion(barcos, distribucion);
    }

    /*
     * Prepara un oceano vacio con las dimensiones del real y una copia de cada barco.
     * Las copias conservan el orden por tamanio y comienzan sin ubicacion.
     * Llama a colocarConRetroceso() desde el indice cero para resolver toda la flota.
     * Un resultado true conserva las posiciones de todas las copias y devuelve la lista.
     * Un resultado false significa que se agotaron las combinaciones y comunica
     * que deben cambiarse la flota o las dimensiones para respetar las reglas.
     */
    private List<Barco> buscarDistribucionConRetroceso(List<Barco> barcos) throws BatallaNavalException {
        Oceano temporal = new Oceano(oceano.getFilas(), oceano.getColumnas());
        List<Barco> distribucion = new ArrayList<>();
        for (Barco original : barcos) {
            distribucion.add(new Barco(original.getTipoBarco()));
        }
        // El exito conserva todas las ubicaciones de la solucion en el oceano temporal.
        if (colocarConRetroceso(distribucion, temporal, 0)) {
            return distribucion;
        }
        throw new BatallaNavalException(
                "La flota elegida no puede distribuirse respetando la separacion entre barcos. Cambie la flota o las dimensiones del oceano.");
    }

    /*
     * Busca una ubicacion para el barco indicado y para todos los que lo siguen.
     * Los barcos de indices anteriores ya estan colocados en el oceano temporal.
     * Recorre todas las filas, columnas y orientaciones como propuestas para la proa.
     * Cada propuesta se valida y coloca mediante Oceano.colocarBarco().
     * Una propuesta rechazada permite pasar a la siguiente.
     *
     * Con una posicion valida llama a este mismo metodo con el indice siguiente.
     * Si el resto de los barcos consigue ubicacion, devuelve true y conserva la solucion.
     * Si el resto devuelve false, desubica el barco actual y prueba otra posicion para el.
     * Este deshacer permite revisar una decision previa que bloqueaba a los siguientes.
     * Al alcanzar el tamanio de la lista ya estan colocados todos y devuelve true.
     * Agotar las propuestas de un nivel devuelve false al nivel que lo llamo.
     */
    private boolean colocarConRetroceso(List<Barco> barcos, Oceano temporal, int indiceBarco) throws BatallaNavalException {
        // Caso de finalizacion de la recursion. Se supero el indice del ultimo barco.
        if (indiceBarco == barcos.size()) {
            return true;
        }
        Barco barco = barcos.get(indiceBarco);
        // Cada nivel prueba todas las proas y las dos orientaciones para su barco.
        for (int fila = 0; fila < temporal.getFilas(); fila++) {
            for (int columna = 0; columna < temporal.getColumnas(); columna++) {
                for (Orientacion orientacion : Orientacion.values()) {
                    try {
                        temporal.colocarBarco(barco, temporal.getCuadrante(fila, columna), orientacion);
                    } catch (BatallaNavalException ubicacionRechazada) {
                        // El oceano rechazo esta propuesta. Sigue con otra orientacion o proa.
                        continue;
                    }
                    // Con el barco actual colocado intenta resolver el resto de la flota.
                    if (colocarConRetroceso(barcos, temporal, indiceBarco + 1)) {
                        return true;
                    }
                    // El resto fallo. Libera esta decision para probar otra combinacion.
                    barco.desubicar();
                }
            }
        }
        return false;
    }

    /*
     * Realiza un intento aleatorio de distribuir toda la flota en un oceano temporal.
     * Crea una copia del mismo tipo por cada barco real e intenta colocarla.
     * Cada copia conserva en la lista el mismo indice que su barco real.
     * Si una copia agota sus intentos, devuelve null y se descarta la distribucion entera,
     * incluidas las copias que ya estaban colocadas. El siguiente intento comienza vacio.
     * Si consigue ubicar todas las copias, devuelve sus posiciones para aplicarlas.
     */
    private List<Barco> probarDistribucion(List<Barco> barcos, Random azar) throws BatallaNavalException {
        Oceano temporal = new Oceano(oceano.getFilas(), oceano.getColumnas());
        List<Barco> distribucion = new ArrayList<>();
        for (Barco original : barcos) {
            Barco copia = new Barco(original.getTipoBarco());
            if (!intentarColocarBarco(copia, temporal, azar)) {
                // Se descarta todo este intento; los barcos reales todavia no fueron
                // modificados.
                return null;
            }
            distribucion.add(copia);
        }
        return distribucion;
    }

    /*
     * Prueba hasta MAX_INTENTOS_POR_BARCO propuestas para una copia de barco.
     * Cada propuesta elige una fila y una columna dentro del oceano y sortea la orientacion.
     * Una proa interior aun puede fallar por el largo, la superposicion o la separacion.
     * Oceano.colocarBarco() aplica las mismas reglas que en la colocacion humana.
     * Una excepcion de ubicacion se captura aqui para generar otra propuesta.
     * La primera aceptada devuelve true. Agotar el limite devuelve false y hace que
     * probarDistribucion() descarte la distribucion completa.
     */
    private boolean intentarColocarBarco(Barco barco, Oceano temporal, Random azar) {
        for (int intento = 0; intento < MAX_INTENTOS_POR_BARCO; intento++) {
            // nextInt(limite) produce un entero desde cero hasta limite - 1 para cada coordenada.
            int fila = azar.nextInt(temporal.getFilas());
            int columna = azar.nextInt(temporal.getColumnas());
            // nextBoolean() elige entre ambas orientaciones con la misma probabilidad.
            Orientacion orientacion = Orientacion.HORIZONTAL;
            if (azar.nextBoolean()) {
                orientacion = Orientacion.VERTICAL;
            }
            try {
                // Obtiene la proa del mismo oceano temporal donde se valida y coloca esta copia.
                temporal.colocarBarco(barco, temporal.getCuadrante(fila, columna), orientacion);
                return true;
            } catch (BatallaNavalException ubicacionRechazada) {
                // Se vuelve a intentar con otra proa y orientacion.
            }
        }
        return false;
    }

    /*
     * Traslada al oceano real una distribucion ya comprobada en el temporal.
     * Relaciona cada barco real con su copia mediante el mismo indice en ambas listas.
     * La primera pasada obtiene las coordenadas de cada proa temporal y encuentra
     * el cuadrante correspondiente en el oceano real. Valida todas las posiciones
     * contra el estado actual antes de comenzar a colocar barcos reales.
     *
     * La segunda pasada obtiene nuevamente esas proas y coloca cada barco real
     * con la orientacion de su copia. Oceano.colocarBarco() comprueba la ubicacion
     * considerando los barcos reales que ya se colocaron en esta pasada.
     * La separacion entre copias se comprobo al construir la distribucion.
     * Cada barco real queda asociado a cuadrantes de su propio oceano.
     */
    private void aplicarDistribucion(List<Barco> barcos, List<Barco> distribucion) throws BatallaNavalException {
        for (int i = 0; i < barcos.size(); i++) {
            Barco barcoTemporal = distribucion.get(i);
            Cuadrante proaTemporal = barcoTemporal.getProa();
            Cuadrante proaReal = oceano.getCuadrante(proaTemporal.getFila(), proaTemporal.getColumna());
            // Primera pasada que comprueba cada propuesta antes de aplicar la distribucion real.
            oceano.validarColocacion(barcos.get(i), proaReal, barcoTemporal.getOrientacion());
        }
        for (int i = 0; i < barcos.size(); i++) {
            Barco barcoTemporal = distribucion.get(i);
            Cuadrante proaTemporal = barcoTemporal.getProa();
            Cuadrante proaReal = oceano.getCuadrante(proaTemporal.getFila(), proaTemporal.getColumna());
            // Segunda pasada que aplica cada posicion a su barco y sus cuadrantes reales.
            oceano.colocarBarco(barcos.get(i), proaReal, barcoTemporal.getOrientacion());
        }
    }

    // Delega en la flota propia el registro de un barco hundido.
    public void registrarBaja(Barco barco) throws BatallaNavalException {
        flota.registrarBaja(barco);
    }

    // Consulta en la flota las bajas propias en orden cronologico.
    public List<Barco> listarBajas() {
        return flota.listarBajas();
    }

    // Consulta en la flota la cantidad de barcos de la maquina hundidos.
    public int contarBajas() {
        return flota.contarBajas();
    }

    public Flota getFlota() {
        return flota;
    }

    public Oceano getOceano() {
        return oceano;
    }

    public Tactica getTactica() {
        return tactica;
    }
}
