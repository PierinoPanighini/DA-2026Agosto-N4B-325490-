package uy.edu.ort.battleshipdemo.presentador.DTOs;

public class BatallaDto {

    private int id;
    private String lugar;
    private String dimensiones;
    private String estado;
    private String integridadPropia;
    private String integridadEnemiga;

    // Guarda los datos de una batalla que el presentador ya preparo para el listado.
    public BatallaDto(int id, String lugar, String dimensiones, String estado,
            String integridadPropia, String integridadEnemiga) {
        this.id = id;
        this.lugar = lugar;
        this.dimensiones = dimensiones;
        this.estado = estado;
        this.integridadPropia = integridadPropia;
        this.integridadEnemiga = integridadEnemiga;
    }

    public int getId() {
        return id;
    }

    public String getLugar() {
        return lugar;
    }

    public String getDimensiones() {
        return dimensiones;
    }

    public String getEstado() {
        return estado;
    }

    public String getIntegridadPropia() {
        return integridadPropia;
    }

    public String getIntegridadEnemiga() {
        return integridadEnemiga;
    }
}
