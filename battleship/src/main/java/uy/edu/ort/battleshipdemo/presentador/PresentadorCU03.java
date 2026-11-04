package uy.edu.ort.battleshipdemo.presentador;

import java.util.List;
import java.util.StringJoiner;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import uy.edu.ort.battleshipdemo.modelo.Barco;
import uy.edu.ort.battleshipdemo.modelo.Batalla;
import uy.edu.ort.battleshipdemo.modelo.Cuadrante;
import uy.edu.ort.battleshipdemo.modelo.Disparo;
import uy.edu.ort.battleshipdemo.modelo.ResultadoDisparo;
import uy.edu.ort.battleshipdemo.modelo.BatallaNavalException;
import uy.edu.ort.battleshipdemo.modelo.Fachada;
import uy.edu.ort.battleshipdemo.modelo.Ronda;

@RestController
@RequestMapping("/cu03")
public class PresentadorCU03 {

    private Fachada fachada;

    public PresentadorCU03(Fachada fachada) {
        this.fachada = fachada;
    }

    @PostMapping("/iniciarBatalla")
    public Commands inicializar() {
        int numeroDeRonda = 1;
        return Commands.create(cargarEstado("Combate"), cargarNumeroRonda(numeroDeRonda));
    }

    @PostMapping("/disparar")
    public Commands disparar(
            @RequestParam("idBatalla") int idBatalla,
            @RequestParam("coordenadaVertical") int fila,
            @RequestParam("coordenadaHorizontal") int columna, @RequestParam("numeroRonda") int numeroRondaEsperado) {

        try {
            Batalla batalla = fachada.disparar(idBatalla, fila, columna, numeroRondaEsperado);

            List<Ronda> rondas = batalla.getRondas();
            Ronda ronda = rondas.get(rondas.size() - 1);

            Commands comandos = Commands.create(
                    mostrarDisparo(
                            "mostrarAguasEnemigas",
                            ronda.getDisparoJugador()));

            // Si el humano gano, la maquina no realiza su disparo.
            if (ronda.getDisparoMaquina() != null) {
                comandos.add(mostrarDisparo(
                        "mostrarAguasPropias",
                        ronda.getDisparoMaquina()));
            }

            String estado = "En combate";
            int numeroRonda = batalla.getNumeroProximaRonda();

            if (batalla.fueGanada()) {
                estado = "Ganada";
                numeroRonda = ronda.getNumero();
            } else if (batalla.fuePerdida()) {
                estado = "Perdida";
                numeroRonda = ronda.getNumero();

                // Revelamos los fragmentos enemigos todavia no descubiertos.
                for (Cuadrante cuadrante : batalla.getOceanoMaquina().listarCuadrantesOcupados()) {

                    if (!cuadrante.isDisparado()) {
                        comandos.add(new Command(
                                "mostrarAguasEnemigas",
                                new ValorDto(
                                        cuadrante.getFila(),
                                        cuadrante.getColumna(),
                                        "barco")));
                    }
                }
            }

            comandos.add(
                    cargarEstado(estado),
                    cargarNumeroRonda(numeroRonda),
                    cargarBajasEnemigas(batalla));

            return comandos;

        } catch (BatallaNavalException ex) {
            return Commands.create(
                    new Command("mensaje", ex.getMessage()));
        }
    }

    @PostMapping("/volverSalaOperaciones")
    public Commands entrarBatalla(@RequestParam("id") int id) {

        return Commands.create();
    }

    public Command cargarEstado(String estado) {
        return new Command("cargarEstado", estado);
    }

    public Command cargarNumeroRonda(int numeroDeRonda) {
        return new Command("cargarNumeroRonda", numeroDeRonda);
    }

    private Command mostrarDisparo(String comando, Disparo disparo) {
        Cuadrante cuadrante = disparo.getCuadrante();

        String valor = "impacto";

        if (disparo.getResultado() == ResultadoDisparo.AGUA) {
            valor = "agua";
        }

        return new Command(
                comando,
                new ValorDto(
                        cuadrante.getFila(),
                        cuadrante.getColumna(),
                        valor));
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
}
