package uy.edu.ort.battleshipdemo.modelo;

import java.util.ArrayList;
import java.util.List;

public class Flota {

    private List<Barco> barcos;

    public Flota(List<Barco> barcos) throws BatallaNavalException {
        if (barcos == null || barcos.isEmpty()) {
            throw new BatallaNavalException("La flota debe contener al menos un barco.");
        }
        for (Barco barco : barcos) {
            if (barco == null) {
                throw new IllegalArgumentException("La flota no puede tener barcos nulos.");
            }
        }
        this.barcos = new ArrayList<>(barcos);
    }

    public void agregarBarco(Barco barco) {
        if (barco == null) {
            throw new IllegalArgumentException("El barco no puede ser nulo.");
        }
        this.barcos.add(barco);
    }

    public List<Barco> getBarcos() {
        return new ArrayList<>(barcos);
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
