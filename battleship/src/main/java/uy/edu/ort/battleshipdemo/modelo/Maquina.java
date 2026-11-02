package uy.edu.ort.battleshipdemo.modelo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class Maquina {

    private static final int MAX_INTENTOS_POR_BARCO = 500;
    private static final int MAX_INTENTOS_FLOTA = 100;

    private Flota flota;
    private Oceano oceano;
    private Tactica tactica;

    public Maquina(Flota flota, Oceano oceano, Tactica tactica) {
        if (flota == null || oceano == null || tactica == null) {
            throw new IllegalArgumentException("La maquina debe tener flota, oceano y tactica.");
        }
        this.flota = flota;
        this.oceano = oceano;
        // Cada maquina recibe una instancia propia del algoritmo seleccionado.
        this.tactica = tactica.crearNueva();
    }

    public void colocarFlota() throws BatallaNavalException {
        // El bloqueo impide que otra colocacion cambie el oceano durante el despliegue.
        synchronized (oceano) {
            List<Barco> barcos = flota.getBarcos();
            validarDespliegueInicial(barcos);
            barcos.sort(Comparator.comparingInt(Barco::getTamanio).reversed());
            Random azar = new Random();

            for (int intento = 0; intento < MAX_INTENTOS_FLOTA; intento++) {
                List<Barco> distribucion = probarDistribucion(barcos, azar);
                if (distribucion != null) {
                    aplicarDistribucion(barcos, distribucion);
                    return;
                }
            }
            throw new BatallaNavalException(
                    "No se pudo colocar toda la flota de la maquina. Intente nuevamente.");
        }
    }

    private void validarDespliegueInicial(List<Barco> barcos) throws BatallaNavalException {
        for (Barco barco : barcos) {
            if (barco.estaUbicado()) {
                throw new BatallaNavalException("La flota de la maquina ya tiene barcos colocados.");
            }
            if (barco.getTipoBarco() == null || barco.getTamanio() <= 0) {
                throw new BatallaNavalException("Los barcos de la maquina deben tener un tipo y tamanio validos.");
            }
            if (barco.getTamanio() > oceano.getFilas() && barco.getTamanio() > oceano.getColumnas()) {
                throw new BatallaNavalException("Un barco de la maquina no entra en el oceano.");
            }
        }
        if (!oceano.listarCuadrantesOcupados().isEmpty()) {
            throw new BatallaNavalException("El oceano de la maquina debe estar vacio antes del despliegue.");
        }
    }

    private List<Barco> probarDistribucion(List<Barco> barcos, Random azar) throws BatallaNavalException {
        Oceano temporal = new Oceano(oceano.getFilas(), oceano.getColumnas());
        List<Barco> distribucion = new ArrayList<>();
        for (Barco original : barcos) {
            Barco copia = new Barco(original.getTipoBarco());
            if (!intentarColocarBarco(copia, temporal, azar)) {
                // Se descarta todo este intento; los barcos reales todavia no fueron modificados.
                return null;
            }
            distribucion.add(copia);
        }
        return distribucion;
    }

    private boolean intentarColocarBarco(Barco barco, Oceano temporal, Random azar) {
        for (int intento = 0; intento < MAX_INTENTOS_POR_BARCO; intento++) {
            int fila = azar.nextInt(temporal.getFilas());
            int columna = azar.nextInt(temporal.getColumnas());
            Orientacion orientacion = Orientacion.HORIZONTAL;
            if (azar.nextBoolean()) {
                orientacion = Orientacion.VERTICAL;
            }
            try {
                temporal.colocarBarco(barco, temporal.getCuadrante(fila, columna), orientacion);
                return true;
            } catch (BatallaNavalException ubicacionRechazada) {
                // Se vuelve a intentar con otra proa y orientacion.
            }
        }
        return false;
    }

    private void aplicarDistribucion(List<Barco> barcos, List<Barco> distribucion)
            throws BatallaNavalException {
        for (int i = 0; i < barcos.size(); i++) {
            Barco ubicado = distribucion.get(i);
            Cuadrante proaTemporal = ubicado.getCuadrantes().get(0);
            Cuadrante proaReal = oceano.getCuadrante(proaTemporal.getFila(), proaTemporal.getColumna());
            oceano.colocarBarco(barcos.get(i), proaReal, ubicado.getOrientacion());
        }
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
