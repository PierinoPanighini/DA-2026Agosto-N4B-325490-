package uy.edu.ort.battleshipdemo.presentador;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Scope;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Conexion SSE de la sesion, basada en el ejemplo del profesor.
 * Spring convierte los objetos a JSON con la configuracion del proyecto.
 */
@Component
// Se inyecta la conexion real en el presentador de sesion. Asi, cuando otra
// sesion provoca una notificacion, el observador conserva su propio destinatario.
@Scope("session")
public class ConexionNavegador {

    private SseEmitter conexionSSE;

    public void conectarSSE() {
        cerrarConexion();
        long tiempoEspera = 30L * 60 * 1000;
        conexionSSE = new SseEmitter(tiempoEspera);
    }

    public SseEmitter getConexionSSE() {
        return conexionSSE;
    }

    public void cerrarConexion() {
        if (conexionSSE != null) {
            SseEmitter conexionAnterior = conexionSSE;
            conexionSSE = null;
            conexionAnterior.complete();
        }
    }

    public void enviarJSON(Object informacion) {
        enviar(informacion, MediaType.APPLICATION_JSON);
    }

    public void enviarMensaje(String mensaje) {
        enviar(mensaje, MediaType.TEXT_PLAIN);
    }

    private void enviar(Object informacion, MediaType tipo) {
        if (conexionSSE == null) {
            return;
        }
        try {
            conexionSSE.send(informacion, tipo);
        } catch (IOException | IllegalStateException ex) {
            System.err.println("Error al enviar mensaje SSE: " + ex.getMessage());
            cerrarConexion();
        }
    }
}
