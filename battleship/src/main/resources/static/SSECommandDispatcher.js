/**
 * SSECommandDispatcher - Gestor de conexiones Server-Sent Events
 * Conecta automáticamente al instanciar y delega mensajes al CommandDispatcher
 * 
 * Uso:
 *   const sseConnection = new SSECommandDispatcher(
 *     "/menu/registrarSSE",
 *     CommandDispatcher,
 *     () => console.log("Conexión cerrada")
 *   );
 *   
 *   // Al terminar:
 *   sseConnection.disconnect();
 */

class SSECommandDispatcher {
  /**
   * Crea y conecta automáticamente al servidor SSE
   * @param {string} url - URL del endpoint SSE
   * @param {Object} commandDispatcher - Instancia de CommandDispatcher con método processResponses()
   * @param {Function} onConnectionClosed - Callback opcional cuando el servidor cierra la conexión
   */
  constructor(url, commandDispatcher, onConnectionClosed) {
    // Validar commandDispatcher (obligatorio)
    if (!commandDispatcher) {
      throw new Error('Invalid CommandDispatcher: commandDispatcher parameter is required');
    }
    
    if (typeof commandDispatcher.processCommands !== 'function') {
      throw new Error('Invalid CommandDispatcher: missing processCommands() method. Make sure you pass a valid CommandDispatcher instance.');
    }

    this.url = url;
    this.commandDispatcher = commandDispatcher;
    this.onConnectionClosed = onConnectionClosed;
    this.eventSource = null;

    // Conectar automáticamente
    this._connect();
  }

  /**
   * Establece la conexión SSE con el servidor
   * @private
   */
  _connect() {
    try {
      this.eventSource = new EventSource(this.url, { withCredentials: true });

      // Manejar mensajes recibidos
      this.eventSource.onmessage = (event) => {
        try {
          const json = JSON.parse(event.data);
          
          // Delegar al CommandDispatcher (maneja ambos formatos)
          this.commandDispatcher.processCommands(json);
        } catch (e) {
          console.error('SSE: Error al parsear mensaje:', event.data, e);
        }
      };

      // Manejar errores y cierre de conexión
      this.eventSource.onerror = (event) => {
        console.warn('SSE: Conexión cerrada por el servidor');
        this.disconnect();

        // Notificar cierre si hay callback
        if (this.onConnectionClosed && typeof this.onConnectionClosed === 'function') {
          try {
            this.onConnectionClosed(event);
          } catch (e) {
            console.error('SSE: Error en callback onConnectionClosed:', e);
          }
        }
      };

      // Confirmar conexión exitosa cuando se abre
      this.eventSource.onopen = () => {
        console.log(`SSE: Conectado exitosamente a ${this.url}`);
      };

    } catch (error) {
      console.error('SSE: Error al crear conexión:', error);
      throw error;
    }
  }

  /**
   * Cierra la conexión SSE
   */
  disconnect() {
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
      console.log('SSE: Conexión cerrada');
    }
  }

  /**
   * Indica si la conexión está activa
   * @returns {boolean}
   */
  isConnected() {
    return this.eventSource !== null && this.eventSource.readyState === EventSource.OPEN;
  }
}

// Exportar clase
export default SSECommandDispatcher;
export { SSECommandDispatcher };

// Para uso global en vanilla JS
if (typeof window !== 'undefined') {
  window.SSECommandDispatcher = SSECommandDispatcher;
}
if (typeof globalThis !== 'undefined') {
  globalThis.SSECommandDispatcher = SSECommandDispatcher;
}
