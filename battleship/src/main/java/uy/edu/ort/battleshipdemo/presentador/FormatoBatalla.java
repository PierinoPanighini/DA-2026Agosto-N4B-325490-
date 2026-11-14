package uy.edu.ort.battleshipdemo.presentador;

import uy.edu.ort.battleshipdemo.modelo.Batalla;
import uy.edu.ort.battleshipdemo.modelo.Flota;

public class FormatoBatalla {

    private FormatoBatalla() {
    }

    public static String textoEstado(Batalla batalla) {
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

    public static String integridad(Flota flota) {
        return flota.partesSanas() + " / " + flota.tamanioTotal();
    }
}
