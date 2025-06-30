package com.sigcr.views;

import com.sigcr.controllers.NotificacionController;
import com.sigcr.models.Notificacion;
import com.sigcr.models.Notification.TipoNotificacion;
import com.sigcr.models.Notification.RolDestinatario;
import com.sigcr.models.User;


import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Vista completa para la gestion de notificaciones del sistema (CU-04).
 * Permite consultar, filtrar, marcar como leidas y administrar notificaciones
 * con diferentes funcionalidades segun el rol del usuario.
 */
public class NotificacionView {
    
    private NotificacionController notificacionController;
    private User usuarioActual;
    private VBox mainLayout;
    private TableView<Notificacion> tablaNotificaciones;
    private ObservableList<Notificacion> listaNotificaciones;
    
    // Componentes de filtrado
    private ComboBox<String> filtroTipoCombo;
    private ComboBox<String> filtroEstadoCombo;
    private TextField filtroTextoField;
    private DatePicker filtroFechaDesde;
    private DatePicker filtroFechaHasta;
    private TextField filtroPacienteField;
    
    // Componentes de informacion
    private Label lblResumen;
    private Label lblNoLeidas;
    private TextArea areaDetalleNotificacion;
    
    // Componentes de administracion (solo para ADMIN)
    private VBox panelAdmin;
    private TextArea areaNuevaNotificacion;
    private ComboBox<TipoNotificacion> comboTipoNueva;
    private ComboBox<RolDestinatario> comboDestinatarioNueva;
    private TextField campoPacienteIdNueva;
    private Spinner<Integer> spinnerDiasLimpieza;

    private Notificacion notificacionSeleccionada = null;

    /**
     * Constructor de la vista de notificaciones
     * @param conn Conexion a la base de datos
     * @param usuarioActual Usuario autenticado
     */
    public NotificacionView(Connection conn, User usuarioActual) {
        this.usuarioActual = usuarioActual;
        this.notificacionController = new NotificacionController(conn, usuarioActual);
        this.listaNotificaciones = FXCollections.observableArrayList();
        inicializarVista();
        cargarNotificaciones();
        actualizarResumen();
    }

    /**
     * Inicializa todos los componentes de la vista
     */
    private void inicializarVista() {
        mainLayout = new VBox(10);
        mainLayout.setPadding(new Insets(20));

        // Titulo y informacion de sesion
        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        
        Label titulo = new Label("Sistema de Notificaciones");
        titulo.setFont(Font.font("System", FontWeight.BOLD, 24));
        titulo.setTextFill(Color.DARKBLUE);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // TODO: Agregar SessionInfoPanel cuando este disponible el AuthController
        headerBox.getChildren().addAll(titulo, spacer);

        // Panel de resumen
        crearPanelResumen();

        // Panel de filtros
        crearPanelFiltros();

        // Tabla de notificaciones
        crearTablaNotificaciones();

        // Panel de detalles
        crearPanelDetalles();

        // Panel de administracion (solo para ADMIN)
        if (usuarioActual.getRole().equals("ADMIN")) {
            crearPanelAdministracion();
        }

        // Botones principales
        crearBotonesPrincipales();

        // Ensamblar layout principal
        mainLayout.getChildren().addAll(headerBox, crearPanelResumen(), 
                                      crearPanelFiltros(), tablaNotificaciones,
                                      crearPanelDetalles());
        
        if (usuarioActual.getRole().equals("ADMIN")) {
            mainLayout.getChildren().add(panelAdmin);
        }
        
        mainLayout.getChildren().add(crearBotonesPrincipales());
    }

    /**
     * Crea el panel de resumen de notificaciones
     */
    private VBox crearPanelResumen() {
        VBox panel = new VBox(5);
        panel.setPadding(new Insets(10));
        panel.setStyle("-fx-background-color: #f0f8ff; -fx-border-color: #b0c4de; -fx-border-radius: 5; -fx-background-radius: 5;");

        Label tituloResumen = new Label("Resumen de Notificaciones");
        tituloResumen.setFont(Font.font("System", FontWeight.BOLD, 14));

        lblResumen = new Label();
        lblNoLeidas = new Label();
        lblNoLeidas.setStyle("-fx-text-fill: #d32f2f; -fx-font-weight: bold;");

        panel.getChildren().addAll(tituloResumen, lblResumen, lblNoLeidas);
        return panel;
    }

    /**
     * Crea el panel de filtros
     */
    private VBox crearPanelFiltros() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10));
        panel.setStyle("-fx-background-color: #f9f9f9; -fx-border-color: #cccccc; -fx-border-radius: 5; -fx-background-radius: 5;");

        Label tituloFiltros = new Label("Filtros de Busqueda");
        tituloFiltros.setFont(Font.font("System", FontWeight.BOLD, 14));

        // Primera fila de filtros
        HBox fila1 = new HBox(10);
        fila1.setAlignment(Pos.CENTER_LEFT);

        filtroTipoCombo = new ComboBox<>();
        filtroTipoCombo.getItems().addAll("Todos", "Paciente Creado", "Paciente Actualizado", 
                                        "Paciente Baja", "Cronograma Cambio", "Plan Creado", 
                                        "Plan Actualizado", "General");
        filtroTipoCombo.setValue("Todos");

        filtroEstadoCombo = new ComboBox<>();
        filtroEstadoCombo.getItems().addAll("Todas", "No Leidas", "Leidas");
        filtroEstadoCombo.setValue("Todas");

        filtroTextoField = new TextField();
        filtroTextoField.setPromptText("Buscar en mensaje...");
        filtroTextoField.setPrefWidth(200);

        fila1.getChildren().addAll(
            new Label("Tipo:"), filtroTipoCombo,
            new Label("Estado:"), filtroEstadoCombo,
            new Label("Texto:"), filtroTextoField
        );

        // Segunda fila de filtros
        HBox fila2 = new HBox(10);
        fila2.setAlignment(Pos.CENTER_LEFT);

        filtroFechaDesde = new DatePicker();
        filtroFechaDesde.setPromptText("Desde");
        filtroFechaHasta = new DatePicker();
        filtroFechaHasta.setPromptText("Hasta");

        filtroPacienteField = new TextField();
        filtroPacienteField.setPromptText("ID Paciente");
        filtroPacienteField.setPrefWidth(100);

        Button btnFiltrar = new Button("Filtrar");
        btnFiltrar.setOnAction(e -> aplicarFiltros());
        btnFiltrar.setStyle("-fx-background-color: #2196f3; -fx-text-fill: white;");

        Button btnLimpiarFiltros = new Button("Limpiar");
        btnLimpiarFiltros.setOnAction(e -> limpiarFiltros());

        fila2.getChildren().addAll(
            new Label("Desde:"), filtroFechaDesde,
            new Label("Hasta:"), filtroFechaHasta,
            new Label("Paciente:"), filtroPacienteField,
            btnFiltrar, btnLimpiarFiltros
        );

        panel.getChildren().addAll(tituloFiltros, fila1, fila2);
        return panel;
    }

    /**
     * Crea la tabla de notificaciones
     */
    private TableView<Notificacion> crearTablaNotificaciones() {
        tablaNotificaciones = new TableView<>();
        tablaNotificaciones.setItems(listaNotificaciones);
        tablaNotificaciones.setPrefHeight(300);

        // Columna de icono
        TableColumn<Notificacion, String> colIcono = new TableColumn<>("📱");
        colIcono.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(data.getValue().getIconoTipo()));
        colIcono.setPrefWidth(40);

        // Columna de tipo
        TableColumn<Notificacion, String> colTipo = new TableColumn<>("Tipo");
        colTipo.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(data.getValue().getDescripcionTipo()));
        colTipo.setPrefWidth(150);

        // Columna de mensaje
        TableColumn<Notificacion, String> colMensaje = new TableColumn<>("Mensaje");
        colMensaje.setCellValueFactory(new PropertyValueFactory<>("mensaje"));
        colMensaje.setPrefWidth(400);

        // Columna de fecha
        TableColumn<Notificacion, String> colFecha = new TableColumn<>("Fecha");
        colFecha.setCellValueFactory(data -> {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            return new javafx.beans.property.SimpleStringProperty(
                data.getValue().getFechaHora().format(formatter));
        });
        colFecha.setPrefWidth(120);

        // Columna de estado
        TableColumn<Notificacion, String> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(
                data.getValue().isLeida() ? "Leida" : "No Leida"));
        colEstado.setPrefWidth(80);

        // Columna de destinatario
        TableColumn<Notificacion, String> colDestinatario = new TableColumn<>("Para");
        colDestinatario.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(
                data.getValue().getDestinatarioRol().toString()));
        colDestinatario.setPrefWidth(100);

        tablaNotificaciones.getColumns().addAll(colIcono, colTipo, colMensaje, 
                                              colFecha, colEstado, colDestinatario);

        // Personalizar filas segun estado de lectura
        tablaNotificaciones.setRowFactory(tv -> {
            TableRow<Notificacion> row = new TableRow<>();
            row.itemProperty().addListener((obs, oldItem, newItem) -> {
                if (newItem == null) {
                    row.setStyle("");
                } else if (!newItem.isLeida()) {
                    row.setStyle("-fx-background-color: #fff3cd; -fx-font-weight: bold; -fx-text-fill: black;");
                } else {
                    row.setStyle("-fx-background-color: #f8f9fa; -fx-text-fill: black;");
                }
            });
            
            // Manejar el estilo cuando la fila es seleccionada
            row.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                if (row.getItem() != null) {
                    if (isSelected) {
                        // Estilo para fila seleccionada manteniendo texto negro
                        if (!row.getItem().isLeida()) {
                            row.setStyle("-fx-background-color: #e6cc00; -fx-font-weight: bold; -fx-text-fill: black;");
                        } else {
                            row.setStyle("-fx-background-color: #d0d0d0; -fx-text-fill: black;");
                        }
                    } else {
                        // Restaurar estilo normal
                        if (!row.getItem().isLeida()) {
                            row.setStyle("-fx-background-color: #fff3cd; -fx-font-weight: bold; -fx-text-fill: black;");
                        } else {
                            row.setStyle("-fx-background-color: #f8f9fa; -fx-text-fill: black;");
                        }
                    }
                }
            });
            
            return row;
        });

        // Manejar seleccion
        tablaNotificaciones.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                notificacionSeleccionada = newSelection;
                mostrarDetalleNotificacion();
            }
        );

        // Doble clic para marcar como leida
        tablaNotificaciones.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && notificacionSeleccionada != null) {
                marcarComoLeida();
            }
        });

        return tablaNotificaciones;
    }

    /**
     * Crea el panel de detalles de la notificacion seleccionada
     */
    private VBox crearPanelDetalles() {
        VBox panel = new VBox(5);
        panel.setPadding(new Insets(10));
        panel.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; -fx-border-radius: 5; -fx-background-radius: 5;");

        Label tituloDetalle = new Label("Detalles de la Notificacion");
        tituloDetalle.setFont(Font.font("System", FontWeight.BOLD, 14));

        areaDetalleNotificacion = new TextArea();
        areaDetalleNotificacion.setEditable(false);
        areaDetalleNotificacion.setPrefRowCount(3);
        areaDetalleNotificacion.setWrapText(true);

        panel.getChildren().addAll(tituloDetalle, areaDetalleNotificacion);
        return panel;
    }

    /**
     * Crea el panel de administracion (solo para ADMIN)
     */
    private VBox crearPanelAdministracion() {
        panelAdmin = new VBox(10);
        panelAdmin.setPadding(new Insets(10));
        panelAdmin.setStyle("-fx-background-color: #fff9c4; -fx-border-color: #fbc02d; -fx-border-radius: 5; -fx-background-radius: 5;");

        Label tituloAdmin = new Label("Panel de Administracion");
        tituloAdmin.setFont(Font.font("System", FontWeight.BOLD, 14));
        tituloAdmin.setTextFill(Color.DARKORANGE);

        // Seccion para crear nueva notificacion
        Label lblNueva = new Label("Crear Nueva Notificacion:");
        lblNueva.setFont(Font.font("System", FontWeight.BOLD, 12));

        areaNuevaNotificacion = new TextArea();
        areaNuevaNotificacion.setPromptText("Mensaje de la notificacion...");
        areaNuevaNotificacion.setPrefRowCount(2);

        HBox filaNueva = new HBox(10);
        filaNueva.setAlignment(Pos.CENTER_LEFT);

        comboTipoNueva = new ComboBox<>();
        comboTipoNueva.getItems().addAll(TipoNotificacion.values());
        comboTipoNueva.setValue(TipoNotificacion.GENERAL);

        comboDestinatarioNueva = new ComboBox<>();
        comboDestinatarioNueva.getItems().addAll(RolDestinatario.values());
        comboDestinatarioNueva.setValue(RolDestinatario.TODOS);

        campoPacienteIdNueva = new TextField();
        campoPacienteIdNueva.setPromptText("ID Paciente (opcional)");
        campoPacienteIdNueva.setPrefWidth(120);

        Button btnCrearNotificacion = new Button("Crear Notificacion");
        btnCrearNotificacion.setOnAction(e -> crearNotificacion());
        btnCrearNotificacion.setStyle("-fx-background-color: #4caf50; -fx-text-fill: white;");

        filaNueva.getChildren().addAll(
            new Label("Tipo:"), comboTipoNueva,
            new Label("Para:"), comboDestinatarioNueva,
            new Label("Paciente:"), campoPacienteIdNueva,
            btnCrearNotificacion
        );

        // Seccion para limpieza de notificaciones
        Label lblLimpieza = new Label("Limpieza de Notificaciones:");
        lblLimpieza.setFont(Font.font("System", FontWeight.BOLD, 12));

        HBox filaLimpieza = new HBox(10);
        filaLimpieza.setAlignment(Pos.CENTER_LEFT);

        spinnerDiasLimpieza = new Spinner<>(1, 365, 30);
        spinnerDiasLimpieza.setPrefWidth(80);

        Button btnLimpiar = new Button("Limpiar Antiguas");
        btnLimpiar.setOnAction(e -> limpiarNotificacionesAntiguas());
        btnLimpiar.setStyle("-fx-background-color: #ff5722; -fx-text-fill: white;");

        filaLimpieza.getChildren().addAll(
            new Label("Eliminar notificaciones de mas de"), spinnerDiasLimpieza,
            new Label("dias"), btnLimpiar
        );

        panelAdmin.getChildren().addAll(tituloAdmin, lblNueva, areaNuevaNotificacion, 
                                       filaNueva, new Separator(), lblLimpieza, filaLimpieza);
        return panelAdmin;
    }

    /**
     * Crea los botones principales
     */
    private HBox crearBotonesPrincipales() {
        HBox botones = new HBox(10);
        botones.setAlignment(Pos.CENTER);
        botones.setPadding(new Insets(10));

        Button btnMarcarLeida = new Button("Marcar como Leida");
        btnMarcarLeida.setOnAction(e -> marcarComoLeida());
        btnMarcarLeida.setStyle("-fx-background-color: #2196f3; -fx-text-fill: white;");

        Button btnMarcarTodasLeidas = new Button("Marcar Todas como Leidas");
        btnMarcarTodasLeidas.setOnAction(e -> marcarTodasComoLeidas());
        btnMarcarTodasLeidas.setStyle("-fx-background-color: #ff9800; -fx-text-fill: white;");

        Button btnActualizar = new Button("Actualizar");
        btnActualizar.setOnAction(e -> {
            cargarNotificaciones();
            actualizarResumen();
        });
        btnActualizar.setStyle("-fx-background-color: #4caf50; -fx-text-fill: white;");

        Button btnExportar = new Button("Exportar");
        btnExportar.setOnAction(e -> exportarNotificaciones());

        botones.getChildren().addAll(btnMarcarLeida, btnMarcarTodasLeidas, btnActualizar, btnExportar);
        return botones;
    }

    /**
     * Carga las notificaciones segun el rol del usuario
     */
    private void cargarNotificaciones() {
        try {
            List<Notificacion> notificaciones = notificacionController.obtenerNotificacionesParaUsuarioActual(false);
            listaNotificaciones.clear();
            listaNotificaciones.addAll(notificaciones);
        } catch (Exception e) {
            mostrarError("Error al cargar notificaciones", e.getMessage());
        }
    }

    /**
     * Actualiza el resumen de notificaciones
     */
    private void actualizarResumen() {
        try {
            NotificacionController.ResumenNotificaciones resumen = notificacionController.obtenerResumen();
            
            lblResumen.setText(String.format("Total de notificaciones: %d", listaNotificaciones.size()));
            lblNoLeidas.setText(String.format("⚠️ Notificaciones no leidas: %d", resumen.getNoLeidas()));
            
        } catch (Exception e) {
            lblResumen.setText("Error al cargar resumen");
            lblNoLeidas.setText("");
        }
    }

    /**
     * Aplica los filtros seleccionados
     */
    private void aplicarFiltros() {
        try {
            List<Notificacion> notificacionesFiltradas;
            
            // Determinar que filtros aplicar segun los valores seleccionados
            if (!filtroEstadoCombo.getValue().equals("Todas") && 
                filtroEstadoCombo.getValue().equals("No Leidas")) {
                notificacionesFiltradas = notificacionController.obtenerNotificacionesParaUsuarioActual(true);
            } else {
                notificacionesFiltradas = notificacionController.obtenerNotificacionesParaUsuarioActual(false);
            }

            // Aplicar filtro de texto si hay
            String textoFiltro = filtroTextoField.getText().trim();
            if (!textoFiltro.isEmpty()) {
                notificacionesFiltradas = notificacionesFiltradas.stream()
                    .filter(n -> n.getMensaje().toLowerCase().contains(textoFiltro.toLowerCase()))
                    .collect(java.util.stream.Collectors.toList());
            }

            // Aplicar filtro de tipo
            if (!filtroTipoCombo.getValue().equals("Todos")) {
                String tipoSeleccionado = filtroTipoCombo.getValue();
                TipoNotificacion tipo = convertirTextoATipo(tipoSeleccionado);
                if (tipo != null) {
                    notificacionesFiltradas = notificacionesFiltradas.stream()
                        .filter(n -> n.getTipo() == tipo)
                        .collect(java.util.stream.Collectors.toList());
                }
            }

            // Aplicar filtro de ID de paciente
            String pacienteId = filtroPacienteField.getText().trim();
            if (!pacienteId.isEmpty()) {
                try {
                    int id = Integer.parseInt(pacienteId);
                    notificacionesFiltradas = notificacionesFiltradas.stream()
                        .filter(n -> n.getPacienteId() != null && n.getPacienteId() == id)
                        .collect(java.util.stream.Collectors.toList());
                } catch (NumberFormatException e) {
                    mostrarAdvertencia("Filtro invalido", "El ID del paciente debe ser un numero");
                    return;
                }
            }

            listaNotificaciones.clear();
            listaNotificaciones.addAll(notificacionesFiltradas);
            
        } catch (Exception e) {
            mostrarError("Error al aplicar filtros", e.getMessage());
        }
    }

    /**
     * Limpia todos los filtros
     */
    private void limpiarFiltros() {
        filtroTipoCombo.setValue("Todos");
        filtroEstadoCombo.setValue("Todas");
        filtroTextoField.clear();
        filtroFechaDesde.setValue(null);
        filtroFechaHasta.setValue(null);
        filtroPacienteField.clear();
        cargarNotificaciones();
    }

    /**
     * Muestra los detalles de la notificacion seleccionada
     */
    private void mostrarDetalleNotificacion() {
        if (notificacionSeleccionada == null) {
            areaDetalleNotificacion.clear();
            return;
        }

        StringBuilder detalle = new StringBuilder();
        detalle.append("ID: ").append(notificacionSeleccionada.getId()).append("\n");
        detalle.append("Tipo: ").append(notificacionSeleccionada.getDescripcionTipo()).append("\n");
        detalle.append("Estado: ").append(notificacionSeleccionada.isLeida() ? "Leida" : "No Leida").append("\n");
        detalle.append("Destinatario: ").append(notificacionSeleccionada.getDestinatarioRol()).append("\n");
        
        if (notificacionSeleccionada.getPacienteId() != null) {
            detalle.append("Paciente ID: ").append(notificacionSeleccionada.getPacienteId()).append("\n");
        }
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        detalle.append("Fecha: ").append(notificacionSeleccionada.getFechaHora().format(formatter)).append("\n\n");
        detalle.append("Mensaje:\n").append(notificacionSeleccionada.getMensaje());

        areaDetalleNotificacion.setText(detalle.toString());
    }

    /**
     * Marca la notificacion seleccionada como leida
     */
    private void marcarComoLeida() {
        if (notificacionSeleccionada == null) {
            mostrarAdvertencia("Sin seleccion", "Seleccione una notificacion para marcar como leida");
            return;
        }

        if (notificacionSeleccionada.isLeida()) {
            mostrarInformacion("Ya leida", "Esta notificacion ya esta marcada como leida");
            return;
        }

        try {
            notificacionController.marcarComoLeida(notificacionSeleccionada.getId());
            notificacionSeleccionada.setLeida(true);
            tablaNotificaciones.refresh();
            actualizarResumen();
            mostrarInformacion("Exito", "Notificacion marcada como leida");
        } catch (Exception e) {
            mostrarError("Error", e.getMessage());
        }
    }

    /**
     * Marca todas las notificaciones como leidas
     */
    private void marcarTodasComoLeidas() {
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar");
        confirmacion.setHeaderText("¿Marcar todas las notificaciones como leidas?");
        confirmacion.setContentText("Esta accion no se puede deshacer");

        Optional<ButtonType> resultado = confirmacion.showAndWait();
        if (resultado.isPresent() && resultado.get() == ButtonType.OK) {
            try {
                notificacionController.marcarTodasComoLeidas();
                
                // Actualizar la vista
                for (Notificacion notif : listaNotificaciones) {
                    notif.setLeida(true);
                }
                tablaNotificaciones.refresh();
                actualizarResumen();
                
                mostrarInformacion("Exito", "Todas las notificaciones marcadas como leidas");
            } catch (Exception e) {
                mostrarError("Error", e.getMessage());
            }
        }
    }

    /**
     * Crea una nueva notificacion (solo ADMIN)
     */
    private void crearNotificacion() {
        String mensaje = areaNuevaNotificacion.getText().trim();
        if (mensaje.isEmpty()) {
            mostrarAdvertencia("Mensaje vacio", "Ingrese un mensaje para la notificacion");
            return;
        }

        try {
            Integer pacienteId = null;
            String pacienteIdTexto = campoPacienteIdNueva.getText().trim();
            if (!pacienteIdTexto.isEmpty()) {
                pacienteId = Integer.parseInt(pacienteIdTexto);
            }

            Notificacion nueva = new Notificacion(
                pacienteId,
                mensaje,
                comboTipoNueva.getValue(),
                comboDestinatarioNueva.getValue()
            );

            notificacionController.crearNotificacion(nueva);
            
            // Limpiar formulario
            areaNuevaNotificacion.clear();
            campoPacienteIdNueva.clear();
            comboTipoNueva.setValue(TipoNotificacion.GENERAL);
            comboDestinatarioNueva.setValue(RolDestinatario.TODOS);
            
            cargarNotificaciones();
            actualizarResumen();
            
            mostrarInformacion("Exito", "Notificacion creada exitosamente");
            
        } catch (NumberFormatException e) {
            mostrarAdvertencia("ID invalido", "El ID del paciente debe ser un numero");
        } catch (Exception e) {
            mostrarError("Error al crear notificacion", e.getMessage());
        }
    }

    /**
     * Limpia notificaciones antiguas (solo ADMIN)
     */
    private void limpiarNotificacionesAntiguas() {
        int dias = spinnerDiasLimpieza.getValue();
        
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar limpieza");
        confirmacion.setHeaderText(String.format("¿Eliminar notificaciones de mas de %d dias?", dias));
        confirmacion.setContentText("Esta accion no se puede deshacer");

        Optional<ButtonType> resultado = confirmacion.showAndWait();
        if (resultado.isPresent() && resultado.get() == ButtonType.OK) {
            try {
                int eliminadas = notificacionController.limpiarNotificacionesAntiguas(dias);
                cargarNotificaciones();
                actualizarResumen();
                mostrarInformacion("Limpieza completada", 
                    String.format("Se eliminaron %d notificaciones antiguas", eliminadas));
            } catch (Exception e) {
                mostrarError("Error en limpieza", e.getMessage());
            }
        }
    }

    /**
     * Exporta las notificaciones a texto plano
     */
    private void exportarNotificaciones() {
        if (listaNotificaciones.isEmpty()) {
            mostrarAdvertencia("Sin datos", "No hay notificaciones para exportar");
            return;
        }

        StringBuilder export = new StringBuilder();
        export.append("REPORTE DE NOTIFICACIONES - SIGCR\n");
        export.append("Generado: ").append(LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))).append("\n");
        export.append("Usuario: ").append(usuarioActual.getUsername()).append("\n");
        export.append("Total: ").append(listaNotificaciones.size()).append(" notificaciones\n\n");
        export.append("=".repeat(80)).append("\n\n");

        for (Notificacion notif : listaNotificaciones) {
            export.append("ID: ").append(notif.getId()).append("\n");
            export.append("Tipo: ").append(notif.getDescripcionTipo()).append("\n");
            export.append("Fecha: ").append(notif.getFechaHora().format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))).append("\n");
            export.append("Estado: ").append(notif.isLeida() ? "Leida" : "No Leida").append("\n");
            export.append("Para: ").append(notif.getDestinatarioRol()).append("\n");
            if (notif.getPacienteId() != null) {
                export.append("Paciente ID: ").append(notif.getPacienteId()).append("\n");
            }
            export.append("Mensaje: ").append(notif.getMensaje()).append("\n");
            export.append("-".repeat(50)).append("\n\n");
        }

        // Mostrar en ventana de texto
        TextArea areaExport = new TextArea(export.toString());
        areaExport.setEditable(false);
        areaExport.setPrefSize(800, 600);

        Alert dialog = new Alert(Alert.AlertType.INFORMATION);
        dialog.setTitle("Exportacion de Notificaciones");
        dialog.setHeaderText("Reporte generado exitosamente");
        dialog.getDialogPane().setContent(areaExport);
        dialog.getDialogPane().setPrefSize(850, 650);
        dialog.showAndWait();
    }

    /**
     * Convierte texto del combo a tipo de notificacion
     */
    private TipoNotificacion convertirTextoATipo(String texto) {
        switch (texto) {
            case "Paciente Creado": return TipoNotificacion.PACIENTE_CREADO;
            case "Paciente Actualizado": return TipoNotificacion.PACIENTE_ACTUALIZADO;
            case "Paciente Baja": return TipoNotificacion.PACIENTE_BAJA;
            case "Cronograma Cambio": return TipoNotificacion.CRONOGRAMA_CAMBIO;
            case "Plan Creado": return TipoNotificacion.PLAN_CREADO;
            case "Plan Actualizado": return TipoNotificacion.PLAN_ACTUALIZADO;
            case "General": return TipoNotificacion.GENERAL;
            default: return null;
        }
    }

    /**
     * Obtiene el componente principal de la vista
     */
    public VBox getView() {
        return mainLayout;
    }

    /**
     * Metodos de utilidad para mostrar mensajes
     */
    private void mostrarError(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void mostrarAdvertencia(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void mostrarInformacion(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
} 