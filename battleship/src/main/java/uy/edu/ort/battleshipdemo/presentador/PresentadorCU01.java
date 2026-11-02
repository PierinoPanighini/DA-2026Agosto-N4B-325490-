package uy.edu.ort.battleshipdemo.presentador;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import uy.edu.ort.battleshipdemo.modelo.Fachada;

@RestController
@RequestMapping("/cu01")
public class PresentadorCU01 {

    @PostMapping("/inicializar")
    public Commands inicializar() {
        return Commands.create(sala());
    }

    @PostMapping("/iniciarNuevaBatalla")
    public Commands iniciarNuevaBatalla() {
        // TODO: preparar el comando para mostrar la preparacion de una batalla.
        return Commands.create();
    }

    @PostMapping("/entrarBatalla")
    public Commands entrarBatalla(@RequestParam("id") int id) {
        // TODO: buscar la batalla y preparar los comandos segun su estado.
        return Commands.create();
    }

    private Command sala() {
        return new Command("sala", new SalaDto(Fachada.getInstancia()));
    }

}
