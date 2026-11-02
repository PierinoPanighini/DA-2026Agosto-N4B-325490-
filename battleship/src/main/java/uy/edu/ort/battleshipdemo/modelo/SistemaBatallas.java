package uy.edu.ort.battleshipdemo.modelo;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import modelo.Barco;
import modelo.Batalla;


public class SistemaBatallas {

    private List<Batalla> batallas;
    private List<TipoBarco> tiposBarco;
    private List<Tactica> tacticas;

    public SistemaBatallas() {
        this.batallas = new ArrayList<>();
        this.tiposBarco = new ArrayList<>();
        this.tacticas = new ArrayList<>();
    }

     // ---------------------------METODOS CU03--------------------------------------------//

    public Batalla disparar(int idBatalla,int fila, int columna, int numeroRondaEsperado) throws BatallaNavalException {

    Batalla batalla = buscarBatalla(idBatalla);

    batalla.generarDisparo(fila, columna, numeroRondaEsperado);
    return batalla;
}

    // ---------------------------METODOS CU02--------------------------------------------//
    // hacer batalla para preparacion
    public Batalla prepararBatalla(String nombre, String tactica, int filas, int columnas, int portaaviones,
            int acorazados, int cruceros, int submarinos, int destructores) throws BatallaNavalException {

            String nombreProvisto = nombre;
            if (nombreProvisto == null || nombreProvisto.isBlank()) {
                nombreProvisto = generarNombreAleatorio();
            } else {
                nombreProvisto = nombreProvisto.trim();
            }

            if(nombreYaUsado(nombreProvisto)){
                throw new BatallaNavalException("No se puede librar mas de una batalla en el mismo lugar, cambie de nombre");
            }

            if (portaaviones > 6 || acorazados > 6 || cruceros > 6 || submarinos > 6 || destructores > 6 ){
                throw new BatallaNavalException("No se puede utilizar mas de 6 barcos de cada tipo");
            }

            if(portaaviones == 0 && acorazados == 0 && cruceros == 0 && submarinos == 0 && destructores == 0 ){
                throw new BatallaNavalException("La flota no puede estar vacía");
            }



            Oceano oceanoHumano = new Oceano(filas, columnas);
            Oceano oceanoMaquina = new Oceano(filas, columnas);

            List<Barco> barcosFlota = generarBarcos(portaaviones, acorazados, cruceros, submarinos, destructores);


            Flota flotaHumano = new Flota(barcosFlota);
            Flota flotaMaquina = flotaHumano.crearFlotaEquivalente();

            if (flotaHumano.tamanioTotal() * 4 >= filas * columnas) {
                    throw new BatallaNavalException( "La flota debe ocupar menos del 25% del oceano.");
            }

            Tactica tacticaSeleccionada = buscarTactica(tactica);

            Maquina jugadorMaquina = new Maquina(flotaMaquina, oceanoMaquina, tacticaSeleccionada);
            Jugador jugadorHumano = new Jugador(flotaHumano, oceanoHumano);

            Batalla nueva = new Batalla(nombreProvisto, jugadorHumano, jugadorMaquina);

            nueva.validarBatalla();

            registrarBatalla(nueva);

            return nueva;
  

    }

    private boolean nombreYaUsado(String nombre) {
        for (Batalla batalla : batallas) {
            if (nombre.equalsIgnoreCase(batalla.getLugar())) {
                return true;
            }
        }

        return false;
    }

    private String generarNombreAleatorio() {
        Random azar = new Random();
        String letras = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String nombre;

        do {
            StringBuilder resultado = new StringBuilder();

            for (int i = 0; i < 6; i++) {
                int posicion = azar.nextInt(letras.length());
                resultado.append(letras.charAt(posicion));
            }

            nombre = resultado.toString();
        } while (nombreYaUsado(nombre));

        return nombre;
    }

    private List<Barco> generarBarcos(int portaaviones, int acorazados, int cruceros, int submarinos, int destructores)
            throws BatallaNavalException {

        List<Barco> barcos = new ArrayList<>();

        agregarBarcos(barcos, "Portaaviones", portaaviones);
        agregarBarcos(barcos, "Acorazado", acorazados);
        agregarBarcos(barcos, "Crucero", cruceros);
        agregarBarcos(barcos, "Submarino", submarinos);
        agregarBarcos(barcos, "Destructor", destructores);

        if (barcos.isEmpty()) {
            throw new BatallaNavalException("La flota debe contener al menos un barco.");
        }

        return barcos;
    }

    private void agregarBarcos(List<Barco> barcos, String nombreTipo, int cantidad) throws BatallaNavalException {

        if (cantidad < 0 || cantidad > 6) {
            throw new BatallaNavalException(
                    "La cantidad de " + nombreTipo + " debe estar entre 0 y 6.");
        }

        for (TipoBarco tipo : tiposBarco) {
            if (tipo.getNombre().equals(nombreTipo)) {
                for (int i = 0; i < cantidad; i++) {
                    barcos.add(new Barco(tipo));
                }
                return;
            }
        }

        throw new BatallaNavalException(
                "No se encontro el tipo de barco: " + nombreTipo);
    }

    private Tactica buscarTactica(String nombre)
            throws BatallaNavalException {

        if (nombre == null || nombre.isBlank()) {
            throw new BatallaNavalException(
                    "Debe seleccionar una tactica.");
        }

        for (Tactica tactica : tacticas) {
            if (tactica.getNombre().equalsIgnoreCase(nombre.trim())) {
                return tactica;
            }
        }

        throw new BatallaNavalException( "No se encontro la tactica seleccionada.");
    }

    // Gestion de batallas
    public void registrarBatalla(Batalla batalla) {
        this.batallas.add(batalla);
    }



    // ---------------------------METODOS CU01--------------------------------------------//
    public List<Batalla> listarBatallas() {
        return new ArrayList<>(batallas);
    }

    public Batalla buscarBatalla(int id) {
        for (Batalla batalla : batallas) {
            if (batalla.getId() == id) {
                return batalla;
            }
        }
        return null;
    }

    public int contarBatallasGanadas() {
        int cantidad = 0;
        for (Batalla batalla : batallas) {
            if (batalla.fueGanada()) {
                cantidad++;
            }
        }
        return cantidad;
    }

    public int contarBatallasPerdidas() {
        int cantidad = 0;
        for (Batalla batalla : batallas) {
            if (batalla.fuePerdida()) {
                cantidad++;
            }
        }
        return cantidad;
    }

    public double calcularExitoCampania() {
        int ganadas = contarBatallasGanadas();
        int perdidas = contarBatallasPerdidas();
        int terminadas = ganadas + perdidas;

        if (terminadas == 0) {
            return 0.0;
        }

        return ganadas * 100.0 / terminadas;
    }


    //----------------------------------------METODOS PRECARGAS

    // Tipos barco
    public void registrarTipoBarco(TipoBarco tipoBarco) {
        this.tiposBarco.add(tipoBarco);
    }



    // Tacticas

    public void registrarTactica(Tactica tactica) {
        this.tacticas.add(tactica);
    }


    //----------------------------------------METODOS LISTAR
    public List<Tactica> listarTacticas() {
        return new ArrayList<>(tacticas);
    }

    public List<TipoBarco> listarTiposBarco() {
        return new ArrayList<>(tiposBarco);
    }

}
