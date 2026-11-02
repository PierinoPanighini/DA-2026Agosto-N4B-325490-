# Pendientes de presentadores

Primero completar y revisar las operaciones del modelo que necesita cada pantalla. Conversar el diseño de los presentadores antes de implementarlos. Pierino completa manualmente los cuerpos de sus operaciones salvo que indique otra cosa.

## Organización propuesta

Cuatro presentadores para tres casos de uso: el CU02 se divide en sus dos pasos. Los nombres de las clases nuevas son propuestas para conversar.

| Pantalla | Caso de uso | Presentador |
| --- | --- | --- |
| Sala de operaciones | CU01 | PresentadorCU01 (estructura existente) |
| Configuración de lugar, táctica, océano y flota | CU02, paso 1 | PresentadorConfiguracionBatalla |
| Despliegue de los barcos | CU02, paso 2 | PresentadorDespliegueFlota |
| Desarrollo y resultado de la batalla | CU03 | PresentadorCombate |

## Modelo antes de los presentadores

- [x] Consultar los cuadrantes efectivamente ocupados: `Oceano.listarCuadrantesOcupados()`. Devuelve una lista nueva con referencias a los cuadrantes del océano que tienen barco. Incluye los ocupados que hayan recibido disparos; la presentación decidirá cómo dibujarlos.
- [ ] Completar Fachada para acceder al SistemaBatallas compartido y delegar las consultas y acciones del modelo.
- [ ] Calcular en Flota su tamaño total y sus partes sanas para el listado del CU01.
- [x] Consultar los barcos pendientes de ubicación y comprobar cuándo está lista la preparación: `Flota.listarBarcosPendientes()` y `estaCompletamenteUbicada()`.
- [x] Identificar cada barco mediante `Barco.getId()` y recuperarlo con `Flota.buscarBarco(id)`. El CU02 busca exclusivamente dentro de la flota humana.
- [ ] Completar la creación de batallas desde la configuración: validaciones, dos flotas independientes y registro al avanzar al despliegue.
- [ ] Completar la colocación automática de la flota de la máquina y el paso a combate.
- [ ] Completar la coordinación de rondas, resultados finales y eventos del modelo.

## PresentadorCU01: sala de operaciones

- [ ] Conectar Fachada.
- [ ] Definir los DTO de resumen de batalla y estadísticas.
- [ ] Completar inicializar(): listado con lugar, dimensiones, estado e integridad de ambas flotas, y estadísticas de campaña.
- [ ] Iniciar el formulario de configuración de una nueva batalla.
- [ ] Ingresar a una batalla existente, contemplando búsqueda sin resultado y el destino según su estado.
- [ ] Actualizar el listado y las estadísticas cuando cambie el modelo.

## PresentadorConfiguracionBatalla: CU02, paso 1

- [ ] Mostrar los tipos de barco y las tácticas disponibles consultando Fachada.
- [ ] Recibir lugar, táctica, filas, columnas y cantidades por tipo de barco.
- [ ] Solicitar al modelo la creación y validación de la batalla.
- [ ] Informar errores mediante comandos, manteniendo los datos del formulario para corregirlos.
- [ ] Avanzar al despliegue cuando la configuración sea válida.
- [ ] Volver a la sala o abandonar este paso sin registrar una preparación incompleta.

## PresentadorDespliegueFlota: CU02, paso 2

- [ ] Obtener la batalla y el estado actual de preparación mediante Fachada.
- [ ] Inicializar BattleShipBoard con las dimensiones y los escuchadores correspondientes.
- [ ] Implementar mostrarBarcosColocados(): consultar los cuadrantes ocupados del océano propio y producir un comando mostrarAguasPropias con ValorDto y valor barco por cada cuadrante.
- [ ] Usar esa representación al abrir o retomar la preparación y después de cada colocación exitosa.
- [ ] Mostrar barcos pendientes de mayor a menor longitud y seleccionar el primero.
- [ ] Usar orientación horizontal inicialmente y mantener la selección de orientación de la vista.
- [ ] Solicitar la colocación mediante Fachada, identificando batalla, barco, proa y orientación.
- [ ] Conectar la vista con `POST /cu02/ubicarBarco`: enviar `idBarco`, `coordenadaVertical`, `coordenadaHorizontal` y `orientacion`. El componente original solo envía las coordenadas; la integración de la vista debe agregar selección y orientación sin modificar las bibliotecas.
- [ ] Registrar los handlers `barcosDisponibles` y `mostrarBarcoUbicado`: reciben `BarcoDto` con ID, nombre, tamaño y cuadrantes (`ValorDto`); dibujar cada cuadrante con `mostrarAguasPropias`.
- [ ] Ante errores de colocación, mostrar el mensaje y conservar la representación válida del tablero.
- [ ] Después de una colocación válida, actualizar barcos pendientes, selección y leyenda de acción.
- [ ] Mostrar Batalla preparada cuando no queden barcos pendientes.
- [ ] Procesar Librar batalla y mostrar el error si todavía falta posicionar barcos.
- [ ] Volver a la sala conservando la preparación ya registrada.
- [ ] Responder con el error correspondiente a interacciones sobre aguas enemigas.

## PresentadorCombate: CU03

- [ ] Reconstruir la vista de batallas en combate y de las ya ganadas o perdidas.
- [ ] Mostrar lugar, estado, número de ronda, océanos y bajas enemigas cronológicas.
- [ ] Solicitar al modelo una ronda al recibir el disparo humano y representar todos sus efectos.
- [ ] Representar barco, impacto y agua según el estado del cuadrante.
- [ ] Mantener ocultas las posiciones enemigas todavía no descubiertas y revelarlas cuando se pierda.
- [ ] Informar los intentos inválidos, incluidos disparos repetidos o sobre el océano propio.
- [ ] Volver a la sala conservando la partida.

## Compartido entre vistas

- [ ] Traducir resultados del modelo a DTO y comandos; mantener reglas de juego y cálculos de dominio en el modelo.
- [ ] Definir los comandos y sus handlers en las vistas, reutilizando CommandDispatcher y BattleShipBoard originales.
- [ ] Conectar eventos del modelo con actualizaciones automáticas de todas las vistas afectadas.
- [ ] Definir el contexto de cada vista o sesión para permitir navegadores en distintas batallas y cerrar sus suscripciones al salir.
- [ ] Coordinar los cambios de pantalla sin recargar la página durante el caso de uso.
- [ ] Mantener los controles habilitados y mostrar los errores de las acciones inválidas.
