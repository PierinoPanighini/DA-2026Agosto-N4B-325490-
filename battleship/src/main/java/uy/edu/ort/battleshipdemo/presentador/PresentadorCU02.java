package uy.edu.ort.battleshipdemo.presentador;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import uy.edu.ort.battleshipdemo.modelo.Barco;
import uy.edu.ort.battleshipdemo.modelo.Batalla;
import uy.edu.ort.battleshipdemo.modelo.Cuadrante;
import uy.edu.ort.battleshipdemo.modelo.BatallaNavalException;
import uy.edu.ort.battleshipdemo.modelo.Fachada;
import uy.edu.ort.battleshipdemo.modelo.Orientacion;

@RestController
@RequestMapping("/cu02")
public class PresentadorCU02 {

    private Fachada fachada;
    private Batalla enPreparacion; //FIXME: ATENTO A AGREGAR SESSION PARA ARREGLAR QUE NO QUEDE LA MISMA TEMPORAL PARA TODOS LOS NAVEGADORES

    public PresentadorCU02(Fachada fachada) {
        this.fachada = fachada;
    }


    //-------------------------------PARTE 1
    @PostMapping("/configurarBatalla")
    public Commands panelCU2() {
       return Commands.create(listaTacticas());
    }

    @PostMapping("/crearBatalla")
    public Commands crearBatallaPreparacion(@RequestParam(required = false) String nombre, @RequestParam String tactica, @RequestParam int filas,
        @RequestParam int columnas, @RequestParam int portaaviones, @RequestParam int acorazados, @RequestParam int cruceros,
        @RequestParam int submarinos, @RequestParam int destructores ) {

        try{
            enPreparacion = fachada.prepararBatalla(nombre, tactica, filas, columnas,
        portaaviones, acorazados, cruceros, submarinos, destructores);
        } catch (BatallaNavalException ex){
            return Commands.create(mensaje(ex.getMessage()));
        }


        return Commands.create(posicionarBarcos(), barcosDisponibles());
    }



    //---------------------------------PARTE 2

    @PostMapping("/ubicarBarco")
    public Commands ubicarBarco(
            @RequestParam("idBarco") int idBarco,
            @RequestParam("coordenadaVertical") int fila,
            @RequestParam("coordenadaHorizontal") int columna,
            @RequestParam("orientacion") Orientacion orientacion) {
        try {
            validarBatallaSeleccionada();
            Barco barco = enPreparacion.getFlotaJugador().buscarBarco(idBarco);
            if (barco == null) {
                throw new BatallaNavalException("No se encontro ese barco en la flota del jugador.");
            }
            Cuadrante proa = enPreparacion.getOceanoJugador().getCuadrante(fila, columna);
            enPreparacion.colocarBarco(barco, proa, orientacion);
            return Commands.create(mostrarBarcoUbicado(barco), barcosDisponibles());
        } catch (BatallaNavalException ex) {
            return Commands.create(mensaje(ex.getMessage()));
        }
    }

    @PostMapping("/entrarBatalla")
    public Commands entrarBatalla() {
        try {
            validarBatallaSeleccionada();
            enPreparacion.iniciarCombate();
            return Commands.create(iniciarBatalla());
        } catch (BatallaNavalException ex) {
            return Commands.create(mensaje(ex.getMessage()));
        }
    }



    private void validarBatallaSeleccionada() throws BatallaNavalException {
        if (enPreparacion == null) {
            throw new BatallaNavalException("Debe seleccionar una batalla antes de continuar.");
        }
    }

    private Command barcosDisponibles() {
        List<BarcoDto> pendientes = new ArrayList<>();
        for (Barco barco : enPreparacion.getFlotaJugador().listarBarcosPendientes()) {
            pendientes.add(new BarcoDto(barco));
        }
        return new Command("barcosDisponibles", pendientes);
    }

    private Command mensaje(String texto) {
        return new Command("mensaje", texto);
    }

    private Command posicionarBarcos() {
        return new Command("posicionarBarcos", true);
    }

    private Command listaTacticas() {
        return new Command("listaTacticas", fachada.listarTacticas());
    }

    private Command iniciarBatalla(){
        return new Command("iniciarBatalla", true);
    }

    private Command mostrarBarcoUbicado(Barco barco){
        return new Command("mostrarBarcoUbicado", new BarcoDto(barco));
    }


}
