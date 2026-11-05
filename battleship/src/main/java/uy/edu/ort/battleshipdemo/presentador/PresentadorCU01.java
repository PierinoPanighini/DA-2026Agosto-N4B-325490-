package uy.edu.ort.battleshipdemo.presentador;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import uy.edu.ort.battleshipdemo.modelo.Batalla;
import uy.edu.ort.battleshipdemo.modelo.BatallaNavalException;
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
        return Commands.create(new Command("irA", "/preparar.html"));
    }

    @PostMapping("/entrarBatalla")
    public Commands entrarBatalla(@RequestParam("id") int id) throws BatallaNavalException {
        Batalla batalla = Fachada.getInstancia().buscarBatalla(id);
        if (batalla.estaEnPreparacion()) {
            return Commands.create(new Command("irA", "/preparar.html?batalla=" + id));
        }
        return Commands.create(new Command("irA", "/batalla.html?batalla=" + id));
    }

    private Command sala() {
        return new Command("sala", new SalaDto(Fachada.getInstancia()));
    }

}
