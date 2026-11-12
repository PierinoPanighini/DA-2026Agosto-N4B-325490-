package uy.edu.ort.battleshipdemo.observador;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Observable
 */
public class Observable {

    private CopyOnWriteArrayList<Observador> observadores = new CopyOnWriteArrayList<>();

    public void agregarObservador(Observador obs){
        observadores.addIfAbsent(obs);
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
