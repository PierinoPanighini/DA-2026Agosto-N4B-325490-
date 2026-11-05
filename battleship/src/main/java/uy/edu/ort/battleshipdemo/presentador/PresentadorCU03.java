package uy.edu.ort.battleshipdemo.presentador;


import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import uy.edu.ort.battleshipdemo.modelo.Barco;
import uy.edu.ort.battleshipdemo.modelo.Batalla;
import uy.edu.ort.battleshipdemo.modelo.Cuadrante;
import uy.edu.ort.battleshipdemo.modelo.BatallaNavalException;
import uy.edu.ort.battleshipdemo.modelo.Fachada;
import uy.edu.ort.battleshipdemo.modelo.Oceano;

@RestController
@RequestMapping("/cu03")
public class PresentadorCU03 {

    private Fachada fachada;

    public PresentadorCU03(Fachada fachada) {
        this.fachada = fachada;
    }

    @PostMapping("/iniciarBatalla")
    public Commands inicializar(@RequestParam("idBatalla") int idBatalla) throws BatallaNavalException {
        Batalla batalla = fachada.buscarBatalla(idBatalla);
        Oceano oceano = batalla.getOceanoJugador();
        InicializarDto tablero = new InicializarDto(oceano.getFilas(), oceano.getColumnas(),
                "/cu03/aguasPropias", "/cu03/disparar");
        Commands comandos = Commands.create(new Command("inicializar", tablero),
                new Command("lugarCombate", batalla.getLugar()));
        agregarSituacion(batalla, comandos);
        return comandos;

    }

    @PostMapping("/aguasPropias")
    public Commands aguasPropias() {
        return Commands.create(new Command("mensaje", "No puede disparar en sus propias aguas"));
    }

    @PostMapping("/disparar")
    public Commands disparar(
            @RequestParam("idBatalla") int idBatalla,
            @RequestParam("coordenadaVertical") int fila,
            @RequestParam("coordenadaHorizontal") int columna,
            @RequestParam("numeroRonda") int numeroRondaEsperado) throws BatallaNavalException {
        Batalla batalla = fachada.disparar(idBatalla, fila, columna, numeroRondaEsperado);
        Commands comandos = Commands.create();
        agregarSituacion(batalla, comandos);
        return comandos;
    }



    public Command cargarEstado(String estado) {
        return new Command("cargarEstado", estado);
    }

    public Command cargarNumeroRonda(int numeroDeRonda) {
        return new Command("cargarNumeroRonda", numeroDeRonda);
    }


    private Command cargarBajasEnemigas(Batalla batalla) {
        String bajas = "";

        for (Barco barco : batalla.getBajasEnemigas()) {
            if (!bajas.isEmpty()) {
                bajas += " ";
            }

            bajas += "{" + barco.getTamanio() + "}";
        }

        return new Command("bajasEnemigas", bajas);
    }

    private void agregarSituacion(Batalla batalla, Commands comandos) throws BatallaNavalException {
        dibujarAguasPropias(batalla, comandos);
        dibujarAguasEnemigas(batalla, comandos);
        comandos.add(cargarEstado(new BatallaDto(batalla).getEstado()));
        comandos.add(cargarNumeroRonda(batalla.getNumeroRonda()));
        comandos.add(cargarBajasEnemigas(batalla));
    }

    private void dibujarAguasPropias(Batalla batalla, Commands comandos) throws BatallaNavalException {
        for (Cuadrante cuadrante : batalla.getOceanoJugador().listarCuadrantesOcupados()) {
            comandos.add(marcar("mostrarAguasPropias", cuadrante, "barco"));
        }
        for (Cuadrante cuadrante : batalla.getOceanoJugador().listarCuadrantesDisparados()) {
            comandos.add(marcar("mostrarAguasPropias", cuadrante, resultado(cuadrante)));
        }
    }

    private void dibujarAguasEnemigas(Batalla batalla, Commands comandos) throws BatallaNavalException {
        if (batalla.fuePerdida()) {
            for (Cuadrante cuadrante : batalla.getOceanoMaquina().listarCuadrantesOcupados()) {
                comandos.add(marcar("mostrarAguasEnemigas", cuadrante, "barco"));
            }
        }
        for (Cuadrante cuadrante : batalla.getOceanoMaquina().listarCuadrantesDisparados()) {
            comandos.add(marcar("mostrarAguasEnemigas", cuadrante, resultado(cuadrante)));
        }
    }

    private String resultado(Cuadrante cuadrante) {
        if (cuadrante.estaOcupado()) {
            return "impacto";
        }
        return "agua";
    }

    private Command marcar(String comando, Cuadrante cuadrante, String valor) {
        return new Command(comando, new ValorDto(cuadrante.getFila(), cuadrante.getColumna(), valor));
    }

}
