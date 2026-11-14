package uy.edu.ort.battleshipdemo.modelo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Flota {

    private List<Barco> barcos;
    private List<Barco> bajas;

    public Flota(List<Barco> barcos) throws BatallaNavalException {
        this.barcos = barcos;
        validar();
        this.barcos = new ArrayList<>(barcos);
        this.bajas = new ArrayList<>();
    }

    // Comprueba que la flota tenga barcos y que ninguno sea nulo.
    private void validar() throws BatallaNavalException {
        if (barcos == null || barcos.isEmpty()) {
            throw new BatallaNavalException("La flota debe contener al menos un barco.");
        }
        for (Barco barco : barcos) {
            if (barco == null) {
                throw new BatallaNavalException("La flota no puede tener barcos nulos.");
            }
        }
    }

    public void agregarBarco(Barco barco) throws BatallaNavalException {
        if (barco == null) {
            throw new BatallaNavalException("El barco no puede ser nulo.");
        }
        this.barcos.add(barco);
    }

    public List<Barco> getBarcos() {
        return new ArrayList<>(barcos);
    }



    // Determina si el barco pertenece a esta flota.
    public boolean contieneBarco(Barco barco) {
        return barcos.contains(barco);
    }

    // Busca un barco por su identificador dentro de esta flota.
    public Barco buscarBarco(int id) {
        for (Barco barco : barcos) {
            if (barco.getId() == id) {
                return barco;
            }
        }
        return null;
    }

    public List<Barco> listarBarcosPendientes() {
        List<Barco> pendientes = new ArrayList<>();
        for (Barco barco : barcos) {
            if (!barco.estaUbicado()) {
                pendientes.add(barco);
            }
        }
        pendientes.sort(Comparator.comparingInt(Barco::getTamanio).reversed());
        return pendientes;
    }

    public Flota crearFlotaEquivalente() throws BatallaNavalException {
        List<Barco> equivalentes = new ArrayList<>();
        for (Barco barco : barcos) {
            equivalentes.add(new Barco(barco.getTipoBarco()));
        }
        return new Flota(equivalentes);
    }

    public boolean estaCompletamenteUbicada() {
        for (Barco barco : barcos) {
            if (!barco.estaUbicado()) {
                return false;
            }
        }
        return true;
    }

    /*
     * Registra un barco propio que acaba de hundirse al final de la lista de bajas.
     * Cada registro conserva su orden de llegada y un mismo barco se agrega una vez.
     * Las condiciones de pertenencia y hundimiento protegen la coherencia de la flota.
     */
    public void registrarBaja(Barco barco) throws BatallaNavalException {
        if (!contieneBarco(barco)) {
            throw new BatallaNavalException("El barco no pertenece a esta flota.");
        }
        if (!barco.estaHundido()) {
            throw new BatallaNavalException("Solo se pueden registrar como bajas los barcos hundidos.");
        }
        if (!bajas.contains(barco)) {
            bajas.add(barco);
        }
    }

    // Devuelve una copia de las bajas en el orden en que se hundieron los barcos.
    public List<Barco> listarBajas() {
        return new ArrayList<>(bajas);
    }

    // Obtiene la cantidad directamente de la lista de bajas registradas.
    public int contarBajas() {
        return bajas.size();
    }

    public boolean quedoTodaHundida() {
        for (Barco barco : barcos) {
            if (!barco.estaHundido()) {
                return false;
            }
        }
        return true;
    }

    public int tamanioTotal() {
        int total = 0;
        for (Barco barco : barcos) {
            total = total + barco.getTamanio();
        }
        return total;
    }

    public int partesSanas(){
        int total = 0;
        for (Barco barco : barcos) {
            total = total + barco.partesSanas();
        }
        return total;
    }

}
