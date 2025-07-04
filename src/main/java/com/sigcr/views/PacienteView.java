package com.sigcr.views;

import com.sigcr.controllers.PacienteController;
import com.sigcr.models.Paciente;
import com.sigcr.models.User;
import com.sigcr.components.ToastNotification;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

/**
 * Vista completa para la gestion integral de pacientes (CU-01).
 * Permite crear, buscar, actualizar y dar de baja pacientes con validaciones
 * y control de acceso segun el rol del usuario.
 */
public class PacienteView {
    
    private PacienteController pacienteController;
    private User usuarioActual;
    private StackPane mainLayout;
    private VBox contentLayout;
    private TableView<Paciente> tablaPacientes;
    private ObservableList<Paciente> listaPacientes;
    
    // Campos del formulario
    private TextField nombreField;
    private TextField documentoField;
    private DatePicker fechaNacimientoPicker;
    private TextArea diagnosticoArea;
    private TextField habitacionField;
    private ComboBox<String> estadoCombo;
    private TextField busquedaField;
    private ComboBox<String> criterioCombo;
    
    private Paciente pacienteSeleccionado = null;

    /**
     * Constructor de la vista de pacientes
     * @param conn Conexion a la base de datos
     * @param usuarioActual Usuario autenticado
     */
    public PacienteView(Connection conn, User usuarioActual) {
        this.usuarioActual = usuarioActual;
        this.pacienteController = new PacienteController(conn, usuarioActual);
        this.listaPacientes = FXCollections.observableArrayList();
        inicializarVista();
        cargarPacientes();
    }

    /**
     * Inicializa todos los componentes de la vista con UX mejorada
     */
    private void inicializarVista() {
        // Layout principal para notificaciones toast
        mainLayout = new StackPane();
        
        // Layout de contenido
        contentLayout = new VBox(15);
        contentLayout.setPadding(new Insets(20));

        // Titulo mejorado
        Label titulo = new Label("Gestion de Pacientes");
        titulo.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        // Panel de busqueda mejorado
        HBox panelBusqueda = crearPanelBusqueda();

        // Formulario de paciente mejorado
        VBox formulario = crearFormularioPaciente();

        // Tabla de pacientes
        tablaPacientes = crearTablaPacientes();

        // Panel de botones mejorado
        HBox panelBotones = crearPanelBotones();

        contentLayout.getChildren().addAll(titulo, panelBusqueda, formulario, tablaPacientes, panelBotones);
        mainLayout.getChildren().add(contentLayout);
        
        // Aplicar estilo de fondo
        mainLayout.setStyle("-fx-background-color: #ecf0f1;");
    }

    /**
     * Crea el panel de busqueda de pacientes
     */
    private HBox crearPanelBusqueda() {
        HBox panel = new HBox(10);
        panel.setPadding(new Insets(10));
        panel.setStyle("-fx-background-color: #f0f0f0; -fx-background-radius: 5;");

        Label lblBuscar = new Label("Buscar:");
        criterioCombo = new ComboBox<>();
        criterioCombo.getItems().addAll("nombre", "documento", "estado");
        criterioCombo.setValue("nombre");

        busquedaField = new TextField();
        busquedaField.setPromptText("Ingrese termino de busqueda...");

        Button btnBuscar = new Button("Buscar");
        btnBuscar.setOnAction(e -> realizarBusqueda());

        Button btnMostrarTodos = new Button("Mostrar Todos");
        btnMostrarTodos.setOnAction(e -> cargarPacientes());

        panel.getChildren().addAll(lblBuscar, criterioCombo, busquedaField, btnBuscar, btnMostrarTodos);
        return panel;
    }

    /**
     * Crea el formulario para datos del paciente
     */
    private VBox crearFormularioPaciente() {
        VBox form = new VBox(10);
        form.setPadding(new Insets(10));
        form.setStyle("-fx-border-color: #cccccc; -fx-border-radius: 5; -fx-background-color: #ffffff;");

        Label lblForm = new Label("Datos del Paciente");
        lblForm.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        // Campos del formulario
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        // Nombre
        grid.add(new Label("Nombre*:"), 0, 0);
        nombreField = new TextField();
        nombreField.setPromptText("Nombre completo del paciente");
        grid.add(nombreField, 1, 0);

        // Documento
        grid.add(new Label("Documento*:"), 0, 1);
        documentoField = new TextField();
        documentoField.setPromptText("Numero de documento");
        grid.add(documentoField, 1, 1);

        // Fecha de nacimiento
        grid.add(new Label("Fecha Nacimiento:"), 0, 2);
        fechaNacimientoPicker = new DatePicker();
        grid.add(fechaNacimientoPicker, 1, 2);

        // Diagnostico
        grid.add(new Label("Diagnostico*:"), 0, 3);
        diagnosticoArea = new TextArea();
        diagnosticoArea.setPromptText("Diagnostico medico");
        diagnosticoArea.setPrefRowCount(3);
        grid.add(diagnosticoArea, 1, 3);

        // Habitacion
        grid.add(new Label("Habitacion:"), 0, 4);
        habitacionField = new TextField();
        habitacionField.setPromptText("Numero de habitacion");
        grid.add(habitacionField, 1, 4);

        // Estado
        grid.add(new Label("Estado:"), 0, 5);
        estadoCombo = new ComboBox<>();
        estadoCombo.getItems().addAll("Activo", "Alta", "Baja");
        estadoCombo.setValue("Activo");
        grid.add(estadoCombo, 1, 5);

        form.getChildren().addAll(lblForm, grid);
        return form;
    }

    /**
     * Crea la tabla para mostrar pacientes
     */
    private TableView<Paciente> crearTablaPacientes() {
        TableView<Paciente> tabla = new TableView<>();
        tabla.setItems(listaPacientes);

        // Columnas de la tabla
        TableColumn<Paciente, String> colNombre = new TableColumn<>("Nombre");
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colNombre.setPrefWidth(150);

        TableColumn<Paciente, String> colDocumento = new TableColumn<>("Documento");
        colDocumento.setCellValueFactory(new PropertyValueFactory<>("documento"));
        colDocumento.setPrefWidth(100);

        TableColumn<Paciente, String> colDiagnostico = new TableColumn<>("Diagnostico");
        colDiagnostico.setCellValueFactory(new PropertyValueFactory<>("diagnostico"));
        colDiagnostico.setPrefWidth(200);

        TableColumn<Paciente, String> colHabitacion = new TableColumn<>("Habitacion");
        colHabitacion.setCellValueFactory(new PropertyValueFactory<>("habitacion"));
        colHabitacion.setPrefWidth(80);

        TableColumn<Paciente, String> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        colEstado.setPrefWidth(80);

        tabla.getColumns().addAll(colNombre, colDocumento, colDiagnostico, colHabitacion, colEstado);

        // Listener para seleccion en tabla
        tabla.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                cargarPacienteEnFormulario(newSelection);
                pacienteSeleccionado = newSelection;
            }
        });

        return tabla;
    }

    /**
     * Crea el panel de botones de accion
     */
    private HBox crearPanelBotones() {
        HBox panel = new HBox(10);
        panel.setPadding(new Insets(10, 0, 0, 0));

        Button btnNuevo = new Button("Nuevo Paciente");
        btnNuevo.setOnAction(e -> limpiarFormulario());
        btnNuevo.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");

        Button btnGuardar = new Button("Guardar");
        btnGuardar.setOnAction(e -> guardarPaciente());
        btnGuardar.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");

        Button btnActualizar = new Button("Actualizar");
        btnActualizar.setOnAction(e -> actualizarPaciente());
        btnActualizar.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white;");

        Button btnDarBaja = new Button("Dar de Baja");
        btnDarBaja.setOnAction(e -> darDeBajaPaciente());
        btnDarBaja.setStyle("-fx-background-color: #F44336; -fx-text-fill: white;");

        // Solo mostrar botones si es ADMIN
        if (usuarioActual.getRole().equals("ADMIN")) {
            panel.getChildren().addAll(btnNuevo, btnGuardar, btnActualizar, btnDarBaja);
        } else {
            Label lblSoloLectura = new Label("Modo solo lectura - Contacte al administrador para realizar cambios");
            lblSoloLectura.setStyle("-fx-text-fill: #666666; -fx-font-style: italic;");
            panel.getChildren().add(lblSoloLectura);
        }

        return panel;
    }

    /**
     * Carga un paciente seleccionado en el formulario
     */
    private void cargarPacienteEnFormulario(Paciente paciente) {
        nombreField.setText(paciente.getNombre());
        documentoField.setText(paciente.getDocumento());
        fechaNacimientoPicker.setValue(paciente.getFechaNacimiento());
        diagnosticoArea.setText(paciente.getDiagnostico());
        habitacionField.setText(paciente.getHabitacion());
        estadoCombo.setValue(paciente.getEstado());
    }

    /**
     * Limpia el formulario para un nuevo paciente
     */
    private void limpiarFormulario() {
        nombreField.clear();
        documentoField.clear();
        fechaNacimientoPicker.setValue(null);
        diagnosticoArea.clear();
        habitacionField.clear();
        estadoCombo.setValue("Activo");
        pacienteSeleccionado = null;
        tablaPacientes.getSelectionModel().clearSelection();
    }

    /**
     * Carga todos los pacientes en la tabla
     */
    private void cargarPacientes() {
        try {
            List<Paciente> pacientes = pacienteController.obtenerTodosPacientes();
            listaPacientes.clear();
            listaPacientes.addAll(pacientes);
        } catch (Exception e) {
            mostrarError("Error al cargar pacientes", e.getMessage());
        }
    }

    /**
     * Realiza busqueda de pacientes segun criterio
     */
    private void realizarBusqueda() {
        String criterio = criterioCombo.getValue();
        String valor = busquedaField.getText().trim();

        if (valor.isEmpty()) {
            cargarPacientes();
            return;
        }

        try {
            List<Paciente> resultados = pacienteController.buscarPacientes(criterio, valor);
            listaPacientes.clear();
            listaPacientes.addAll(resultados);
        } catch (Exception e) {
            mostrarError("Error en busqueda", e.getMessage());
        }
    }

    /**
     * Guarda un nuevo paciente con feedback visual mejorado
     */
    private void guardarPaciente() {
        try {
            Paciente paciente = crearPacienteDesdeFormulario();
            
            if (pacienteController.crearPaciente(paciente)) {
                ToastNotification.showSuccess(mainLayout, "✅ Paciente creado correctamente");
                limpiarFormulario();
                cargarPacientes();
            }
        } catch (Exception e) {
            ToastNotification.showError(mainLayout, "❌ Error al crear paciente: " + e.getMessage());
        }
    }

    /**
     * Actualiza un paciente existente
     */
    private void actualizarPaciente() {
        if (pacienteSeleccionado == null) {
            mostrarAdvertencia("Seleccione un paciente", "Debe seleccionar un paciente de la tabla para actualizar");
            return;
        }

        try {
            Paciente paciente = crearPacienteDesdeFormulario();
            paciente.setId(pacienteSeleccionado.getId());
            
            if (pacienteController.actualizarPaciente(paciente)) {
                mostrarInformacion("Exito", "Paciente actualizado correctamente");
                cargarPacientes();
            }
        } catch (Exception e) {
            mostrarError("Error al actualizar paciente", e.getMessage());
        }
    }

    /**
     * Da de baja a un paciente
     */
    private void darDeBajaPaciente() {
        if (pacienteSeleccionado == null) {
            mostrarAdvertencia("Seleccione un paciente", "Debe seleccionar un paciente de la tabla para dar de baja");
            return;
        }

        // Confirmacion
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar baja");
        confirmacion.setHeaderText("¿Esta seguro de dar de baja al paciente?");
        confirmacion.setContentText("Esta accion cambiara el estado del paciente a 'Baja' y generara notificaciones.");

        Optional<ButtonType> resultado = confirmacion.showAndWait();
        if (resultado.isPresent() && resultado.get() == ButtonType.OK) {
            try {
                if (pacienteController.darDeBajaPaciente(pacienteSeleccionado.getId())) {
                    mostrarInformacion("Exito", "Paciente dado de baja correctamente");
                    limpiarFormulario();
                    cargarPacientes();
                }
            } catch (Exception e) {
                mostrarError("Error al dar de baja paciente", e.getMessage());
            }
        }
    }

    /**
     * Crea un objeto Paciente a partir de los datos del formulario
     */
    private Paciente crearPacienteDesdeFormulario() throws IllegalArgumentException {
        String nombre = nombreField.getText().trim();
        String documento = documentoField.getText().trim();
        LocalDate fechaNacimiento = fechaNacimientoPicker.getValue();
        String diagnostico = diagnosticoArea.getText().trim();
        String habitacion = habitacionField.getText().trim();
        String estado = estadoCombo.getValue();

        return new Paciente(nombre, documento, fechaNacimiento, diagnostico, habitacion, estado);
    }

    /**
     * Muestra mensaje de error
     */
    private void mostrarError(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    /**
     * Muestra mensaje de informacion
     */
    private void mostrarInformacion(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    /**
     * Muestra mensaje de advertencia
     */
    private void mostrarAdvertencia(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    /**
     * Obtiene la vista principal
     * @return Componente principal de la vista
     */
    public StackPane getView() {
        return mainLayout;
    }
}