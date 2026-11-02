package uy.edu.ort.battleshipdemo.modelo;

import java.util.List;

public class Precargas {

    public static void cargar(SistemaBatallas sistema) throws BatallaNavalException {
        TipoBarco portaaviones = new TipoBarco("Portaaviones", 5);
        TipoBarco acorazado = new TipoBarco("Acorazado", 4);
        TipoBarco crucero = new TipoBarco("Crucero", 3);
        TipoBarco submarino = new TipoBarco("Submarino", 2);
        TipoBarco destructor = new TipoBarco("Destructor", 1);
        for (TipoBarco tipo : List.of(portaaviones, acorazado, crucero, submarino, destructor)) {
            sistema.registrarTipoBarco(tipo);
        }

        Tactica secuencial = new TacticaSecuencial();
        Tactica espiral = new TacticaEspiral();
        Tactica cazaAtaque = new TacticaCazaAtaque();
        for (Tactica tactica : List.of(secuencial, espiral, cazaAtaque)) {
            sistema.registrarTactica(tactica);
        }

        Batalla preparacion = crearBatalla("Bahia Serena", 8, 8, cazaAtaque, crucero, submarino);
        sistema.registrarBatalla(preparacion);

        Batalla combate = crearBatalla("Cabo Norte", 8, 10, secuencial, crucero, submarino);
        ubicarFlotasDeEjemplo(combate);
        precargarRonda(combate, combate.getOceanoMaquina().getCuadrante(2, 2), true);
        precargarRonda(combate, combate.getOceanoMaquina().getCuadrante(0, 0), true);
        combate.setEstado(new EstadoEnCombate(combate));
        sistema.registrarBatalla(combate);

        Batalla ganada = crearBatalla("Isla Victoria", 10, 8, espiral, crucero, submarino);
        ubicarFlotasDeEjemplo(ganada);
        int[][] objetivos = {{2, 2}, {2, 3}, {2, 4}, {5, 1}, {5, 2}};
        for (int i = 0; i < objetivos.length; i++) {
            Cuadrante objetivo = ganada.getOceanoMaquina().getCuadrante(objetivos[i][0], objetivos[i][1]);
            precargarRonda(ganada, objetivo, i < objetivos.length - 1);
        }
        ganada.setEstado(new EstadoTerminada(ganada));
        sistema.registrarBatalla(ganada);

        Batalla perdida = crearBatalla("Punta Brava", 10, 10, secuencial, crucero, submarino);
        ubicarFlotasDeEjemplo(perdida);
        while (!todosHundidos(perdida.getFlotaJugador())) {
            Cuadrante objetivo = buscarAguaLibre(perdida.getOceanoMaquina());
            precargarRonda(perdida, objetivo, true);
        }
        perdida.setEstado(new EstadoTerminada(perdida));
        sistema.registrarBatalla(perdida);
    }

    private static Batalla crearBatalla(String lugar, int filas, int columnas, Tactica tactica,
            TipoBarco crucero, TipoBarco submarino) throws BatallaNavalException {
        Flota flotaJugador = new Flota(List.of(new Barco(crucero), new Barco(submarino)));
        Jugador jugador = new Jugador(flotaJugador, new Oceano(filas, columnas));
        Maquina maquina = new Maquina(flotaJugador.crearFlotaEquivalente(),
                new Oceano(filas, columnas), tactica);
        return new Batalla(lugar, jugador, maquina);
    }

    // Estas posiciones fijas son datos de ejemplo: estan dentro del tablero y separadas.
    private static void ubicarFlotasDeEjemplo(Batalla batalla) throws BatallaNavalException {
        List<Barco> propios = batalla.getFlotaJugador().getBarcos();
        ubicar(propios.get(0), batalla.getOceanoJugador(), 1, 1, Orientacion.HORIZONTAL);
        ubicar(propios.get(1), batalla.getOceanoJugador(), 4, 5, Orientacion.VERTICAL);

        List<Barco> enemigos = batalla.getFlotaMaquina().getBarcos();
        ubicar(enemigos.get(0), batalla.getOceanoMaquina(), 2, 2, Orientacion.HORIZONTAL);
        ubicar(enemigos.get(1), batalla.getOceanoMaquina(), 5, 1, Orientacion.HORIZONTAL);
    }

    private static void ubicar(Barco barco, Oceano oceano, int fila, int columna, Orientacion orientacion)
            throws BatallaNavalException {
        oceano.colocarBarco(barco, oceano.getCuadrante(fila, columna), orientacion);
    }

    // Construye directamente el historial de ejemplo antes de asignar su estado final.
    private static void precargarRonda(Batalla batalla, Cuadrante objetivoJugador, boolean respondeMaquina)
            throws BatallaNavalException {
        Disparo disparoJugador = disparar(objetivoJugador, batalla.getBajasEnemigas());
        Disparo disparoMaquina = null;
        if (respondeMaquina) {
            Cuadrante objetivoMaquina = batalla.getTactica().elegirCuadrante();
            disparoMaquina = disparar(objetivoMaquina, batalla.getBajasAmigas());
        }
        batalla.getRondas().add(new Ronda(batalla.getRondas().size() + 1, disparoJugador, disparoMaquina));
    }

    private static Disparo disparar(Cuadrante cuadrante, List<Barco> bajas) throws BatallaNavalException {
        ResultadoDisparo resultado = cuadrante.recibirDisparo();
        if (resultado == ResultadoDisparo.HUNDIDO) {
            bajas.add(cuadrante.getBarco());
        }
        return new Disparo(cuadrante, resultado);
    }

    private static boolean todosHundidos(Flota flota) {
        for (Barco barco : flota.getBarcos()) {
            if (!barco.estaHundido()) {
                return false;
            }
        }
        return true;
    }

    private static Cuadrante buscarAguaLibre(Oceano oceano) throws BatallaNavalException {
        for (int fila = 0; fila < oceano.getFilas(); fila++) {
            for (int columna = 0; columna < oceano.getColumnas(); columna++) {
                Cuadrante cuadrante = oceano.getCuadrante(fila, columna);
                if (!cuadrante.estaOcupado() && !cuadrante.isDisparado()) {
                    return cuadrante;
                }
            }
        }
        throw new IllegalStateException("No hay agua libre para construir la batalla perdida de ejemplo.");
    }
}
