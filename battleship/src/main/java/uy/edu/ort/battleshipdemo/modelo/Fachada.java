package uy.edu.ort.battleshipdemo.modelo;

import java.util.List;



public class Fachada {
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



    //CU03
    public Batalla disparar(
        int idBatalla,
        int fila,
        int columna,
        int numeroRondaEsperado) throws BatallaNavalException {

        return sistemaBatallas.disparar(idBatalla, fila, columna, numeroRondaEsperado);
    }

}
