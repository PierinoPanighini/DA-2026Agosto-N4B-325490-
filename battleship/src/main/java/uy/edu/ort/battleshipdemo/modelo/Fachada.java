package uy.edu.ort.battleshipdemo.modelo;

import java.util.List;

import uy.edu.ort.battleshipdemo.observador.Observable;



public class Fachada extends Observable {
    public enum Eventos {
        BATALLAS_ACTUALIZADAS
    }

    private static Fachada instanciaFachada;


    private SistemaBatallas sistemaBatallas = new SistemaBatallas();

    private Fachada() {
    }

    public static Fachada getInstancia() {
        if (instanciaFachada == null) {
            instanciaFachada = new Fachada();
        }
        return instanciaFachada;
    }


    public void cargarDatosIniciales() throws BatallaNavalException {
        Precargas.cargar(sistemaBatallas);
    }


    //CU1
    public List<Batalla> listarBatallas() {
        return sistemaBatallas.listarBatallas();
    }

    public Batalla buscarBatalla(int id) throws BatallaNavalException {
        return sistemaBatallas.buscarBatalla(id);
    }

    public int contarBatallasGanadas() {
        return sistemaBatallas.contarBatallasGanadas();
    }

    public int contarBatallasPerdidas() {
        return sistemaBatallas.contarBatallasPerdidas();
    }

    public int contarBatallasTerminadas() {
        return sistemaBatallas.contarBatallasTerminadas();
    }

    public double calcularExitoCampania() {
        return sistemaBatallas.calcularExitoCampania();
    }

    // Datos precargados (tipos barcos y tácticas)
    public List<TipoBarco> listarTiposBarco() {
        return sistemaBatallas.listarTiposBarco();
    }




    //CU02
    public List<Tactica> listarTacticas() {
        return sistemaBatallas.listarTacticas();
    }

    public Batalla prepararBatalla(String nombre, String tactica, int filas, int columnas, int portaaviones,
            int acorazados, int cruceros, int submarinos, int destructores) throws BatallaNavalException{
                return sistemaBatallas.prepararBatalla(nombre, tactica, filas, columnas, portaaviones, acorazados, cruceros, submarinos, destructores);
            }



    public Batalla iniciarCombate(int idBatalla) throws BatallaNavalException {
        return sistemaBatallas.iniciarCombate(idBatalla);
    }

    //CU03
    public Batalla disparar(int idBatalla, int fila, int columna) throws BatallaNavalException {
        return sistemaBatallas.disparar(idBatalla, fila, columna);
    }

}
