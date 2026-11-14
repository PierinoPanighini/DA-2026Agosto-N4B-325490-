package uy.edu.ort.battleshipdemo.modelo;

public abstract class Tactica {

    private String nombre;
    private Oceano oceanoObjetivo;

    /*
     * Guarda el nombre que identifica esta opcion en el catalogo.
     * El oceano objetivo se asigna despues a la instancia creada para una batalla.
     */
    protected Tactica(String nombre) {
        this.nombre = nombre;
    }

    /*
     * Devuelve el nombre con el que se identifica la tactica en la seleccion.
     */
    public String getNombre() {
        return nombre;
    }

    /*
     * Asocia esta instancia con el oceano sobre el que la maquina disparara.
     * Rechaza un objetivo nulo y el cambio a otro oceano si ya habia uno configurado.
     * Permite recibir nuevamente el mismo oceano. Cada instancia de batalla conserva
     * su objetivo y consulta las marcas que el proceso de combate actualiza en el.
     */
    public void configurarObjetivo(Oceano oceanoObjetivo) throws BatallaNavalException {
        if (oceanoObjetivo == null) {
            throw new BatallaNavalException("El oceano objetivo no puede ser nulo.");
        }
        if (this.oceanoObjetivo != null && this.oceanoObjetivo != oceanoObjetivo) {
            throw new BatallaNavalException("La tactica ya tiene otro oceano objetivo. Cree una instancia nueva.");
        }
        this.oceanoObjetivo = oceanoObjetivo;
    }

    /*
     * Entrega a las especializaciones el oceano configurado para esta batalla.
     * Si falta configurarlo, interrumpe la eleccion con una excepcion de dominio.
     */
    protected Oceano getOceanoObjetivo() throws BatallaNavalException {
        if (oceanoObjetivo == null) {
            throw new BatallaNavalException("La tactica no tiene un oceano objetivo configurado.");
        }
        return oceanoObjetivo;
    }

    /*
     * Cada especializacion elige un cuadrante real del objetivo que siga sin disparar.
     * El proceso de combate recibe esa referencia, efectua el disparo y actualiza
     * su marca y el resultado. La siguiente eleccion consulta esas marcas.
     */
    public abstract Cuadrante elegirCuadrante() throws BatallaNavalException;

    /*
     * Crea una instancia de la misma especializacion para una batalla.
     * La nueva instancia comienza con su propio progreso y debe configurar su objetivo.
     */
    public abstract Tactica crearNueva();
}
