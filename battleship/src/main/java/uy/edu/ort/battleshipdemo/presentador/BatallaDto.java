package uy.edu.ort.battleshipdemo.presentador;

import java.util.ArrayList;
import java.util.List;

import uy.edu.ort.battleshipdemo.modelo.Batalla;
import uy.edu.ort.battleshipdemo.modelo.Flota;

public class BatallaDto {

    private int id;
    private String lugar;
    private String dimensiones;
    private String estado; // "En preparación"
    private String integridadPropia;
    private String integridadEnemiga;

    public BatallaDto(Batalla batalla) {
        id = batalla.getId();

        lugar = batalla.getLugar();

        // Los dos oceanos miden lo mismo: alcanza con uno.
        dimensiones = batalla.getOceanoJugador().getColumnas() + " x " + batalla.getOceanoJugador().getFilas();

        estado = textoEstado(batalla);

        integridadPropia = integridad(batalla.getFlotaJugador());

        integridadEnemiga = integridad(batalla.getFlotaMaquina());
    }

    // Una sola cuenta para las dos flotas, para no repetir codigo.
    private String integridad(Flota flota) {
        return flota.partesSanas() + " / " + flota.tamanioTotal();
    }


  private String textoEstado(Batalla batalla) {
    if (batalla.fueGanada()) {
        return "Ganada";
    }
    if (batalla.fuePerdida()) {
        return "Perdida";
    }
    if (batalla.getEstado() == Batalla.Estados.EN_COMBATE) {
        return "En combate";
    }
    return "En preparación";
}
    //geters para que el presentador pueda acceder a los datos del dto y mostrarlos en la vista
    public int getId() {
        return id;
    }

    public String getLugar() {
        return lugar;
    }

    public String getDimensiones() {
        return dimensiones;
    }

    public String getEstado() {
        return estado;
    }

    public String getIntegridadPropia() {
        return integridadPropia;
    }

    public String getIntegridadEnemiga() {
        return integridadEnemiga;
    }

    // convertimos la lista del modelo en una lista dtos.
    public static List<BatallaDto> listaDtos(List<Batalla> batallas) {
        List<BatallaDto> dtos = new ArrayList<>();
        for (Batalla batalla : batallas) {
            BatallaDto batallaDto = new BatallaDto(batalla);
            dtos.add(batallaDto);
        }
        return dtos;
    }
}
