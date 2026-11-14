package uy.edu.ort.battleshipdemo;

import uy.edu.ort.battleshipdemo.presentador.Command;
import uy.edu.ort.battleshipdemo.presentador.Commands;
import uy.edu.ort.battleshipdemo.presentador.DTOs.InicializarDto;
import uy.edu.ort.battleshipdemo.presentador.DTOs.ValorDto;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoTableroBatallaNavalPresenter {

    @PostMapping("/inicializarDemo")
    public Commands inicializarDemo() {
        Command hola = new Command("mostrarMensaje", "hola mundo!");
        Command inicializar = new Command("inicializar", new InicializarDto(10, 10, "/ubicarBarco", "/disparar"));
        Command bajas = new Command("bajasEnemigas", "{5},{3},{3}"); 
        return Commands.create(hola, inicializar, bajas);
    }

    @PostMapping("/accion1")
    public Commands accion1() {
        Command accion = new Command("mostrarMensaje", "Accion 1 ejecutada!");
        return Commands.create(accion);
    }

    @PostMapping("/accion2")
    public Commands accion2() {
        Command accion = new Command("mostrarMensaje", "Accion 2 ejecutada!");
        return Commands.create(accion);
    }

    @PostMapping("/accion3")
    public Commands accion3() {
        Command accion = new Command("mostrarMensaje", "Accion 3 ejecutada!");
        return Commands.create(accion);
    }

    @PostMapping("/accion4")
    public Commands accion4() {
        Command accion = new Command("mostrarMensaje", "Accion 4 ejecutada!");
        return Commands.create(accion);
    }

    @PostMapping("/ubicarBarco")
    public Commands ubicarBarco(int coordenadaVertical, int coordenadaHorizontal) {
        System.out.println("Ubicar barco en " + coordenadaVertical + " y " + coordenadaHorizontal);
        Command barcoPropio = new Command("mostrarAguasPropias",new ValorDto(coordenadaVertical, coordenadaHorizontal, "barco"));
        return Commands.create(barcoPropio);
    }
    
    @PostMapping("/disparar")
    public Commands disparar(int coordenadaVertical, int coordenadaHorizontal) {
        System.out.println("Disparar " + coordenadaVertical + " y " + coordenadaHorizontal);
        Command agua = new Command("mostrarAguasEnemigas",new ValorDto(coordenadaVertical, coordenadaHorizontal, "agua"));
        return Commands.create(agua);
    }
}
