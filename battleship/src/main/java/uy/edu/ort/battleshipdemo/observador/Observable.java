package uy.edu.ort.battleshipdemo.observador;

import java.util.ArrayList;
import java.util.List;

/**
 * Observable
 */
public class Observable {

    private List<Observador> observadores = new ArrayList<>();

    public void agregarObservador(Observador obs){
        if(!observadores.contains(obs)){
            observadores.add(obs);
        }
    }
    public void quitarObservador(Observador obs){
        observadores.remove(obs);
    }
    public void notificar(Object evento){
        List<Observador> copia = new ArrayList<>(observadores);
        for(Observador obs:copia){
            try{
                obs.actualizar(evento, this);
            }catch(Exception e){
                System.err.println("Error al notificar evento: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

}
