package com.sigcr.views;

import com.sigcr.controllers.CronogramaController;
import com.sigcr.models.PlanTratamiento;
import com.sigcr.models.Sesion;
import com.sigcr.models.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.util.StringConverter;

import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;

/**
 * Vista completa para la planificacion de cronogramas terapeuticos (CU-02).
 * Permite a los medicos fisiatras crear planes de tratamiento y asignar
 * sesiones semanales a pacientes con validacion de horas requeridas.
 */
public class CronogramaView {
    
    private CronogramaController cronogramaController;
    private User usuarioActual;
    private VBox mainLayout;
    
    // Componentes principales
    private ComboBox<PlanTratamiento> comboPacientes;
    private DatePicker fechaSemanaPicker;
    private TableView<Sesion> tablaSesiones;
    private ObservableList<Sesion> listaSesiones;
    private VBox panelResumen;
    private TextArea areaAlertas;
    
    // Panel de plan de tratamiento
    private Label lblPlanActual;
    private TextArea textAreaPlan;
    private VBox panelPlan;
    
    // Panel de nueva sesion
    private ComboBox<String> comboTipoTerapia;
    private ComboBox<String> comboTerapeuta;
    private ComboBox<String> comboDiaSemana;
    private Spinner<Integer> spinnerHora;
    private Spinner<Integer> spinnerMinuto;
    private Spinner<Integer> spinnerDuracion;
    
    private PlanTratamiento planActual;

    /**
     * Constructor de la vista de cronogramas
     */
    public CronogramaView(Connection conn, User usuarioActual) {
        this.usuarioActual = usuarioActual;
        this.cronogramaController = new CronogramaController(conn, usuarioActual);
        this.listaSesiones = FXCollections.observableArrayList();
        inicializarVista();
        cargarPacientesConPlan();
    }

    /**
     * Inicializa todos los componentes de la vista
     */
    private void inicializarVista() {
        mainLayout = new VBox(15);
        mainLayout.setPadding(new Insets(20));

        // Titulo
        Label titulo = new Label("Planificacion de Cronogramas Terapeuticos");
        titulo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        // Panel de seleccion
        VBox panelSeleccion = crearPanelSeleccion();
        
        // Panel del plan de tratamiento
        panelPlan = crearPanelPlan();
        
        // Panel de sesiones
        VBox panelSesiones = crearPanelSesiones();
        
        // Panel de resumen y alertas
        panelResumen = crearPanelResumen();

        mainLayout.getChildren().addAll(titulo, panelSeleccion, panelPlan, panelSesiones, panelResumen);
    }

    /**
     * Crea el panel de seleccion de paciente y semana
     */
    private VBox crearPanelSeleccion() {
        VBox panel = new VBox(10);
        panel.setStyle("-fx-background-color: #ecf0f1; -fx-padding: 15; -fx-background-radius: 5;");

        Label lblSeleccion = new Label("Seleccion de Paciente y Semana");
        lblSeleccion.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        HBox filaSeleccion = new HBox(15);
        filaSeleccion.setAlignment(Pos.CENTER_LEFT);

        // Combo de pacientes
        Label lblPaciente = new Label("Paciente:");
        comboPacientes = new ComboBox<>();
        comboPacientes.setPromptText("Seleccione un paciente...");
        comboPacientes.setPrefWidth(250);
        comboPacientes.setConverter(new StringConverter<PlanTratamiento>() {
            @Override
            public String toString(PlanTratamiento plan) {
                return plan != null ? plan.getNombrePaciente() : "";
            }
            @Override
            public PlanTratamiento fromString(String string) { return null; }
        });

        // DatePicker para semana
        Label lblSemana = new Label("Semana (Lunes):");
        fechaSemanaPicker = new DatePicker(LocalDate.now().with(java.time.DayOfWeek.MONDAY));
        fechaSemanaPicker.setPrefWidth(150);

        // Boton cargar
        Button btnCargar = new Button("Cargar Cronograma");
        btnCargar.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
        btnCargar.setOnAction(e -> cargarCronograma());

        filaSeleccion.getChildren().addAll(lblPaciente, comboPacientes, lblSemana, fechaSemanaPicker, btnCargar);
        panel.getChildren().addAll(lblSeleccion, filaSeleccion);

        return panel;
    }

    /**
     * Crea el panel que muestra el plan de tratamiento actual
     */
    private VBox crearPanelPlan() {
        VBox panel = new VBox(15);
        panel.setStyle("-fx-background-color: #ffffff; -fx-padding: 20; -fx-border-color: #bdc3c7; -fx-border-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);");

        Label lblTitulo = new Label("📋 Plan de Tratamiento Activo");
        lblTitulo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        // Usar TextArea para mejor visualizacion
        textAreaPlan = new TextArea("Seleccione un paciente para ver su plan");
        textAreaPlan.setEditable(false);
        textAreaPlan.setPrefRowCount(8);
        textAreaPlan.setWrapText(true);
        textAreaPlan.setStyle("-fx-font-size: 13px; -fx-font-family: 'Courier New'; -fx-text-fill: #2c3e50; -fx-background-color: #f8f9fa; -fx-border-color: #e9ecef; -fx-border-radius: 5px;");
        
        lblPlanActual = new Label(); // Mantenemos para compatibilidad
        lblPlanActual.setVisible(false);
        
        panel.getChildren().addAll(lblTitulo, textAreaPlan, lblPlanActual);
        panel.setVisible(false);

        return panel;
    }

    /**
     * Crea el panel de gestion de sesiones
     */
    private VBox crearPanelSesiones() {
        VBox panel = new VBox(15);

        Label lblTitulo = new Label("Sesiones de la Semana");
        lblTitulo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        // Panel para agregar nueva sesion
        VBox panelNuevaSesion = crearPanelNuevaSesion();

        // Tabla de sesiones
        tablaSesiones = crearTablaSesiones();

        // Botones de accion
        HBox panelBotones = crearPanelBotonesSesiones();

        panel.getChildren().addAll(lblTitulo, panelNuevaSesion, tablaSesiones, panelBotones);
        return panel;
    }

    /**
     * Crea el panel para agregar nueva sesion
     */
    private VBox crearPanelNuevaSesion() {
        VBox panel = new VBox(10);
        panel.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 15; -fx-background-radius: 5;");

        Label lblTitulo = new Label("Agregar Nueva Sesion");
        lblTitulo.setStyle("-fx-font-weight: bold;");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        // Tipo de terapia
        grid.add(new Label("Tipo:"), 0, 0);
        comboTipoTerapia = new ComboBox<>();
        comboTipoTerapia.getItems().addAll(CronogramaController.TIPOS_TERAPIA);
        comboTipoTerapia.setPrefWidth(150);
        grid.add(comboTipoTerapia, 1, 0);

        // Terapeuta
        grid.add(new Label("Terapeuta:"), 2, 0);
        comboTerapeuta = new ComboBox<>();
        comboTerapeuta.getItems().addAll(CronogramaController.TERAPEUTAS);
        comboTerapeuta.setPrefWidth(150);
        grid.add(comboTerapeuta, 3, 0);

        // Dia de la semana
        grid.add(new Label("Dia:"), 0, 1);
        comboDiaSemana = new ComboBox<>();
        comboDiaSemana.getItems().addAll("Lunes", "Martes", "Miercoles", "Jueves", "Viernes", "Sabado");
        comboDiaSemana.setPrefWidth(120);
        grid.add(comboDiaSemana, 1, 1);

        // Hora
        grid.add(new Label("Hora:"), 2, 1);
        HBox horaBox = new HBox(5);
        spinnerHora = new Spinner<>(8, 18, 9);
        spinnerHora.setPrefWidth(70);
        spinnerMinuto = new Spinner<>(0, 59, 0, 15);
        spinnerMinuto.setPrefWidth(70);
        horaBox.getChildren().addAll(spinnerHora, new Label(":"), spinnerMinuto);
        grid.add(horaBox, 3, 1);

        // Duracion
        grid.add(new Label("Duracion (min):"), 0, 2);
        spinnerDuracion = new Spinner<>(15, 180, 60, 15);
        spinnerDuracion.setPrefWidth(100);
        grid.add(spinnerDuracion, 1, 2);

        // Boton agregar
        Button btnAgregar = new Button("Agregar Sesion");
        btnAgregar.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        btnAgregar.setOnAction(e -> agregarSesion());
        grid.add(btnAgregar, 2, 2);

        panel.getChildren().addAll(lblTitulo, grid);
        return panel;
    }

    /**
     * Crea la tabla de sesiones
     */
    private TableView<Sesion> crearTablaSesiones() {
        TableView<Sesion> tabla = new TableView<>();
        tabla.setItems(listaSesiones);
        tabla.setPrefHeight(200);

        TableColumn<Sesion, String> colDia = new TableColumn<>("Dia");
        colDia.setCellValueFactory(cellData -> {
            LocalDate fecha = cellData.getValue().getFechaHora().toLocalDate();
            String dia = fecha.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es"));
            return new javafx.beans.property.SimpleStringProperty(dia);
        });
        colDia.setPrefWidth(100);

        TableColumn<Sesion, String> colFechaHora = new TableColumn<>("Fecha y Hora");
        colFechaHora.setCellValueFactory(cellData -> {
            LocalDateTime fechaHora = cellData.getValue().getFechaHora();
            String formato = fechaHora.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            return new javafx.beans.property.SimpleStringProperty(formato);
        });
        colFechaHora.setPrefWidth(130);

        TableColumn<Sesion, String> colTipo = new TableColumn<>("Tipo de Terapia");
        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipoTerapia"));
        colTipo.setPrefWidth(150);

        TableColumn<Sesion, String> colTerapeuta = new TableColumn<>("Terapeuta");
        colTerapeuta.setCellValueFactory(new PropertyValueFactory<>("terapeuta"));
        colTerapeuta.setPrefWidth(130);

        TableColumn<Sesion, Integer> colDuracion = new TableColumn<>("Duracion (min)");
        colDuracion.setCellValueFactory(new PropertyValueFactory<>("duracion"));
        colDuracion.setPrefWidth(100);

        tabla.getColumns().addAll(colDia, colFechaHora, colTipo, colTerapeuta, colDuracion);
        return tabla;
    }

    /**
     * Crea los botones de accion para sesiones
     */
    private HBox crearPanelBotonesSesiones() {
        HBox panel = new HBox(10);

        Button btnEliminar = new Button("Eliminar Sesion");
        btnEliminar.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        btnEliminar.setOnAction(e -> eliminarSesionSeleccionada());

        Button btnLimpiar = new Button("Limpiar Todo");
        btnLimpiar.setOnAction(e -> limpiarSesiones());

        panel.getChildren().addAll(btnEliminar, btnLimpiar);
        return panel;
    }

    /**
     * Crea el panel de resumen y alertas
     */
    private VBox crearPanelResumen() {
        VBox panel = new VBox(15);
        panel.setStyle("-fx-background-color: #ffffff; -fx-padding: 20; -fx-border-color: #bdc3c7; -fx-border-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);");

        Label lblTitulo = new Label("📊 Resumen y Validacion del Cronograma");
        lblTitulo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        // Area de alertas mas grande y con mejor estilo
        areaAlertas = new TextArea();
        areaAlertas.setPrefRowCount(10);
        areaAlertas.setEditable(false);
        areaAlertas.setWrapText(true);
        areaAlertas.setPromptText("🔍 Las validaciones del cronograma apareceran aqui...\n\nPresione 'Validar' para analizar el cronograma actual.");
        areaAlertas.setStyle("-fx-font-size: 13px; -fx-font-family: 'Courier New'; -fx-background-color: #f8f9fa; -fx-border-color: #e9ecef; -fx-border-radius: 5px;");

        // Botones mejorados
        Button btnValidar = new Button("🔍 Validar Cronograma");
        btnValidar.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-padding: 10 20; -fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 8px;");
        btnValidar.setOnAction(e -> validarCronograma());

        Button btnGuardar = new Button("💾 Guardar Cronograma");
        btnGuardar.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-padding: 10 20; -fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 8px;");
        btnGuardar.setOnAction(e -> guardarCronograma());

        HBox panelBotones = new HBox(15, btnValidar, btnGuardar);
        panelBotones.setAlignment(javafx.geometry.Pos.CENTER);

        panel.getChildren().addAll(lblTitulo, areaAlertas, panelBotones);
        return panel;
    }

    /**
     * Carga los pacientes que tienen plan de tratamiento activo
     */
    private void cargarPacientesConPlan() {
        try {
            List<PlanTratamiento> planes = cronogramaController.obtenerPlanesActivos();
            comboPacientes.getItems().clear();
            comboPacientes.getItems().addAll(planes);
        } catch (Exception e) {
            mostrarError("Error al cargar pacientes", e.getMessage());
        }
    }

    /**
     * Carga el cronograma del paciente y semana seleccionados
     */
    private void cargarCronograma() {
        PlanTratamiento planSeleccionado = comboPacientes.getSelectionModel().getSelectedItem();
        LocalDate fechaSemana = fechaSemanaPicker.getValue();

        if (planSeleccionado == null) {
            mostrarAdvertencia("Seleccione un paciente", "Debe seleccionar un paciente de la lista");
            return;
        }

        if (fechaSemana == null) {
            mostrarAdvertencia("Seleccione una fecha", "Debe seleccionar la fecha de inicio de semana");
            return;
        }

        // Ajustar al lunes de la semana
        LocalDate lunes = fechaSemana.with(java.time.DayOfWeek.MONDAY);
        fechaSemanaPicker.setValue(lunes);

        try {
            // Cargar plan actual
            planActual = cronogramaController.obtenerPlanActivo(planSeleccionado.getPacienteId());
            mostrarPlanActual();

            // Cargar sesiones existentes de la semana
            List<Sesion> sesionesExistentes = cronogramaController.obtenerSesionesSemana(
                planSeleccionado.getPacienteId(), lunes);
            
            listaSesiones.clear();
            listaSesiones.addAll(sesionesExistentes);

            // Limpiar alertas
            areaAlertas.clear();

            panelPlan.setVisible(true);
        } catch (Exception e) {
            mostrarError("Error al cargar cronograma", e.getMessage());
        }
    }

    /**
     * Muestra el plan de tratamiento actual
     */
    private void mostrarPlanActual() {
        if (planActual != null) {
            StringBuilder texto = new StringBuilder();
            texto.append("=== PLAN DE TRATAMIENTO ===\n\n");
            texto.append(String.format("📋 Paciente: %s\n", planActual.getNombrePaciente()));
            texto.append(String.format("📅 Periodo: %s al %s\n", 
                planActual.getFechaInicio(), planActual.getFechaFin()));
            texto.append(String.format("⏱️ Total horas semanales: %d horas\n\n", planActual.getTotalHorasSemanales()));
            
            texto.append("=== DISTRIBUCION POR TIPO DE TERAPIA ===\n");
            texto.append("----------------------------------------\n");
            
            for (Map.Entry<String, Integer> entry : planActual.getHorasSemanalesPorTipo().entrySet()) {
                texto.append(String.format("🔸 %-20s: %2d horas semanales\n", 
                    entry.getKey(), entry.getValue()));
            }
            
            texto.append("\n=== OBJETIVOS DEL TRATAMIENTO ===\n");
            texto.append("• Cumplir con todas las horas asignadas por tipo\n");
            texto.append("• Distribuir las sesiones a lo largo de la semana\n");
            texto.append("• Respetar disponibilidad de terapeutas\n");
            texto.append("• Evitar sobrecarga en un solo dia\n");

            textAreaPlan.setText(texto.toString());
        }
    }

    /**
     * Agrega una nueva sesion a la lista
     */
    private void agregarSesion() {
        if (planActual == null) {
            mostrarAdvertencia("Sin plan", "Debe cargar un plan de tratamiento primero");
            return;
        }

        try {
            // Validar campos
            String tipoTerapia = comboTipoTerapia.getValue();
            String terapeuta = comboTerapeuta.getValue();
            String diaSemana = comboDiaSemana.getValue();
            
            if (tipoTerapia == null || terapeuta == null || diaSemana == null) {
                mostrarAdvertencia("Campos incompletos", "Complete todos los campos para agregar la sesion");
                return;
            }

            // Calcular fecha y hora
            LocalDate fechaSemana = fechaSemanaPicker.getValue();
            LocalDate fechaSesion = calcularFechaPorDia(fechaSemana, diaSemana);
            LocalTime horaSesion = LocalTime.of(spinnerHora.getValue(), spinnerMinuto.getValue());
            LocalDateTime fechaHoraSesion = LocalDateTime.of(fechaSesion, horaSesion);

            // Crear sesion
            Sesion nuevaSesion = new Sesion(
                planActual.getPacienteId(),
                terapeuta,
                tipoTerapia,
                fechaHoraSesion,
                spinnerDuracion.getValue()
            );

            listaSesiones.add(nuevaSesion);

            // Limpiar campos
            limpiarCamposNuevaSesion();

        } catch (Exception e) {
            mostrarError("Error al agregar sesion", e.getMessage());
        }
    }

    /**
     * Calcula la fecha especifica basada en el dia de la semana
     */
    private LocalDate calcularFechaPorDia(LocalDate fechaSemana, String diaSemana) {
        Map<String, Integer> diasMap = new HashMap<>();
        diasMap.put("Lunes", 0);
        diasMap.put("Martes", 1);
        diasMap.put("Miercoles", 2);
        diasMap.put("Jueves", 3);
        diasMap.put("Viernes", 4);
        diasMap.put("Sabado", 5);

        return fechaSemana.plusDays(diasMap.get(diaSemana));
    }

    /**
     * Elimina la sesion seleccionada
     */
    private void eliminarSesionSeleccionada() {
        Sesion sesionSeleccionada = tablaSesiones.getSelectionModel().getSelectedItem();
        if (sesionSeleccionada != null) {
            listaSesiones.remove(sesionSeleccionada);
        } else {
            mostrarAdvertencia("Sin seleccion", "Seleccione una sesion para eliminar");
        }
    }

    /**
     * Limpia todas las sesiones
     */
    private void limpiarSesiones() {
        listaSesiones.clear();
        areaAlertas.clear();
    }

    /**
     * Limpia los campos de nueva sesion
     */
    private void limpiarCamposNuevaSesion() {
        comboTipoTerapia.getSelectionModel().clearSelection();
        comboTerapeuta.getSelectionModel().clearSelection();
        comboDiaSemana.getSelectionModel().clearSelection();
    }

    /**
     * Valida el cronograma actual
     */
    private void validarCronograma() {
        if (planActual == null || listaSesiones.isEmpty()) {
            mostrarAdvertencia("Sin datos", "Debe cargar un plan y agregar sesiones para validar");
            return;
        }

        try {
            CronogramaController.ResultadoPlanificacion resultado = 
                cronogramaController.planificarSemana(
                    planActual.getPacienteId(),
                    fechaSemanaPicker.getValue(),
                    new ArrayList<>(listaSesiones)
                );

            mostrarResultadoValidacion(resultado);

        } catch (Exception e) {
            mostrarError("Error al validar", e.getMessage());
        }
    }

    /**
     * Guarda el cronograma en la base de datos
     */
    private void guardarCronograma() {
        if (planActual == null || listaSesiones.isEmpty()) {
            mostrarAdvertencia("Sin datos", "Debe cargar un plan y agregar sesiones para guardar");
            return;
        }

        try {
            CronogramaController.ResultadoPlanificacion resultado = 
                cronogramaController.planificarSemana(
                    planActual.getPacienteId(),
                    fechaSemanaPicker.getValue(),
                    new ArrayList<>(listaSesiones)
                );

            if (resultado.isGuardadoExitoso()) {
                mostrarInformacion("Exito", "Cronograma guardado correctamente. Se han enviado notificaciones a los terapeutas.");
                mostrarResultadoValidacion(resultado);
            } else {
                mostrarResultadoValidacion(resultado);
                mostrarAdvertencia("No guardado", "El cronograma tiene alertas criticas. Corrija los problemas antes de guardar.");
            }

        } catch (Exception e) {
            mostrarError("Error al guardar", e.getMessage());
        }
    }

    /**
     * Muestra el resultado de la validacion
     */
    private void mostrarResultadoValidacion(CronogramaController.ResultadoPlanificacion resultado) {
        StringBuilder texto = new StringBuilder();
        
        texto.append("=== RESUMEN DE HORAS ===\n");
        for (Map.Entry<String, Integer> entry : resultado.getHorasRequeridas().entrySet()) {
            String tipo = entry.getKey();
            int requeridas = entry.getValue();
            int asignadas = resultado.getHorasAsignadas().getOrDefault(tipo, 0);
            texto.append(String.format("%s: %d/%d horas\n", tipo, asignadas, requeridas));
        }

        if (resultado.tieneAlertas()) {
            texto.append("\n=== ALERTAS ===\n");
            for (String alerta : resultado.getAlertas()) {
                texto.append(alerta).append("\n");
            }
        } else {
            texto.append("\n✓ Cronograma valido - Cumple todas las horas requeridas");
        }

        areaAlertas.setText(texto.toString());

        // Cambiar color segun el tipo de alertas
        if (resultado.tieneAlertasCriticas()) {
            areaAlertas.setStyle("-fx-text-fill: #e74c3c;");
        } else if (resultado.tieneAlertas()) {
            areaAlertas.setStyle("-fx-text-fill: #f39c12;");
        } else {
            areaAlertas.setStyle("-fx-text-fill: #27ae60;");
        }
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

    private void mostrarInformacion(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
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

    /**
     * Obtiene la vista principal
     */
    public VBox getView() {
        return mainLayout;
    }
} 