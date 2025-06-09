package com.sigcr.views;

import com.sigcr.controllers.AuthController;
import com.sigcr.models.User;
import com.sigcr.components.SessionInfoPanel;
import com.sigcr.components.ToastNotification;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.sql.Connection;

/**
 * Vista principal unificada de la aplicación SIGCR.
 * Proporciona navegación centralizada y UX mejorada con diseño moderno.
 */
public class MainApplicationView {
    
    private BorderPane mainLayout;
    private TabPane tabPane;
    private VBox sidebar;
    private HBox topBar;
    private StackPane contentArea;
    private Label statusLabel;
    private ProgressIndicator loadingIndicator;
    private javafx.scene.control.TextArea areaResultadosReportes;
    
    private Connection conn;
    private User usuarioActual;
    private AuthController authController;
    private Stage primaryStage;
    
    // Referencias a las vistas
    private PacienteView pacienteView;
    private CronogramaView cronogramaView;
    private NotificacionView notificacionView;
    private GestionUsuariosView gestionUsuariosView;

    public MainApplicationView(Connection conn, User usuarioActual, AuthController authController, Stage primaryStage) {
        this.conn = conn;
        this.usuarioActual = usuarioActual;
        this.authController = authController;
        this.primaryStage = primaryStage;
        inicializarVista();
        aplicarTemaModerno();
        configurarShortcuts();
    }

    /**
     * Inicializa la vista principal con diseño moderno
     */
    private void inicializarVista() {
        mainLayout = new BorderPane();
        
        // Crear componentes principales
        crearTopBar();
        crearSidebar();
        crearContentArea();
        crearStatusBar();
        
        // Ensamblar layout
        mainLayout.setTop(topBar);
        mainLayout.setLeft(sidebar);
        mainLayout.setCenter(contentArea);
        mainLayout.setBottom(crearStatusBar());
        
        // Cargar vista inicial según el rol
        cargarVistaInicial();
    }

    /**
     * Crea la barra superior con información de sesión y acciones rápidas
     */
    private void crearTopBar() {
        topBar = new HBox(20);
        topBar.setPadding(new Insets(10, 20, 10, 20));
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setStyle("-fx-background-color: #2c3e50; -fx-border-color: #34495e; -fx-border-width: 0 0 1 0;");
        
        // Logo y título
        Label logo = new Label("SIGCR");
        logo.setFont(Font.font("System", FontWeight.BOLD, 18));
        logo.setTextFill(Color.WHITE);
        
        Label subtitulo = new Label("Sistema Integral de Gestión Clínica");
        subtitulo.setFont(Font.font("System", 12));
        subtitulo.setTextFill(Color.web("#bdc3c7"));
        
        VBox tituloBox = new VBox(2);
        tituloBox.getChildren().addAll(logo, subtitulo);
        
        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // Panel de sesión con botón de logout
        SessionInfoPanel sessionPanel = new SessionInfoPanel(authController, this::cerrarSesion);
        sessionPanel.setCustomStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        
        // Botón de notificaciones (si hay notificaciones)
        Button btnNotificaciones = new Button("🔔");
        btnNotificaciones.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-background-radius: 15; -fx-min-width: 30; -fx-min-height: 30;");
        btnNotificaciones.setOnAction(e -> mostrarNotificaciones());
        
        topBar.getChildren().addAll(tituloBox, spacer, sessionPanel, btnNotificaciones);
    }

    /**
     * Crea la barra lateral de navegación
     */
    private void crearSidebar() {
        sidebar = new VBox(5);
        sidebar.setPrefWidth(200);
        sidebar.setPadding(new Insets(20, 10, 20, 10));
        sidebar.setStyle("-fx-background-color: #34495e; -fx-border-color: #2c3e50; -fx-border-width: 0 1 0 0;");
        
        Label menuLabel = new Label("MENÚ PRINCIPAL");
        menuLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        menuLabel.setTextFill(Color.web("#bdc3c7"));
        menuLabel.setPadding(new Insets(0, 0, 10, 10));
        
        sidebar.getChildren().add(menuLabel);
        
        // Crear botones de navegación según el rol
        crearBotonesNavegacion();
    }

    /**
     * Crea los botones de navegación según el rol del usuario
     */
    private void crearBotonesNavegacion() {
        // Siempre disponible: Panel principal
        Button btnDashboard = crearBotonNavegacion("📊 Dashboard", "dashboard");
        btnDashboard.setOnAction(e -> mostrarDashboard());
        sidebar.getChildren().add(btnDashboard);
        
        switch (usuarioActual.getRole()) {
            case "ADMIN":
                // Admin tiene acceso a todo
                agregarBotonNavegacion("👥 Gestión Usuarios", "usuarios", e -> mostrarGestionUsuarios());
                agregarBotonNavegacion("🏥 Gestión Pacientes", "pacientes", e -> mostrarPacientes());
                agregarBotonNavegacion("📅 Cronogramas", "cronogramas", e -> mostrarCronogramas());
                agregarBotonNavegacion("🔔 Notificaciones", "notificaciones", e -> mostrarNotificaciones());
                agregarBotonNavegacion("📋 Reportes", "reportes", e -> mostrarReportes());
                break;
                
            case "MEDICO":
                // Médico: pacientes, cronogramas, reportes
                agregarBotonNavegacion("🏥 Gestión Pacientes", "pacientes", e -> mostrarPacientes());
                agregarBotonNavegacion("📅 Cronogramas", "cronogramas", e -> mostrarCronogramas());
                agregarBotonNavegacion("📋 Reportes", "reportes", e -> mostrarReportes());
                agregarBotonNavegacion("🔔 Notificaciones", "notificaciones", e -> mostrarNotificaciones());
                break;
                
            case "TERAPEUTA":
            case "ENFERMERIA":
                // Staff: solo vista de pacientes y agenda
                agregarBotonNavegacion("🏥 Ver Pacientes", "pacientes", e -> mostrarPacientes());
                agregarBotonNavegacion("📅 Mi Agenda", "agenda", e -> mostrarAgenda());
                agregarBotonNavegacion("🔔 Notificaciones", "notificaciones", e -> mostrarNotificaciones());
                break;
        }
        
        // Separador
        Separator separator = new Separator();
        separator.setPadding(new Insets(10, 0, 10, 0));
        sidebar.getChildren().add(separator);
        
        // Acciones del sistema
        agregarBotonNavegacion("⚙️ Configuración", "config", e -> mostrarConfiguracion());
        agregarBotonNavegacion("❓ Ayuda", "ayuda", e -> mostrarAyuda());
    }

    /**
     * Agrega un botón de navegación al sidebar
     */
    private void agregarBotonNavegacion(String texto, String id, javafx.event.EventHandler<javafx.event.ActionEvent> action) {
        Button btn = crearBotonNavegacion(texto, id);
        btn.setOnAction(action);
        sidebar.getChildren().add(btn);
    }

    /**
     * Crea un botón de navegación con estilo consistente
     */
    private Button crearBotonNavegacion(String texto, String id) {
        Button btn = new Button(texto);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setPadding(new Insets(12, 15, 12, 15));
        btn.setStyle(
            "-fx-background-color: transparent; " +
            "-fx-text-fill: #ecf0f1; " +
            "-fx-font-size: 13px; " +
            "-fx-background-radius: 5; " +
            "-fx-border-radius: 5;"
        );
        
        // Efectos hover
        btn.setOnMouseEntered(e -> 
            btn.setStyle(
                "-fx-background-color: #3498db; " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 13px; " +
                "-fx-background-radius: 5; " +
                "-fx-border-radius: 5;"
            )
        );
        
        btn.setOnMouseExited(e -> 
            btn.setStyle(
                "-fx-background-color: transparent; " +
                "-fx-text-fill: #ecf0f1; " +
                "-fx-font-size: 13px; " +
                "-fx-background-radius: 5; " +
                "-fx-border-radius: 5;"
            )
        );
        
        return btn;
    }

    /**
     * Crea el área de contenido principal
     */
    private void crearContentArea() {
        contentArea = new StackPane();
        contentArea.setPadding(new Insets(20));
        contentArea.setStyle("-fx-background-color: #ecf0f1;");
        
        // Indicador de carga
        loadingIndicator = new ProgressIndicator();
        loadingIndicator.setVisible(false);
        loadingIndicator.setStyle("-fx-progress-color: #3498db;");
        
        contentArea.getChildren().add(loadingIndicator);
    }

    /**
     * Crea la barra de estado
     */
    private HBox crearStatusBar() {
        HBox statusBar = new HBox(10);
        statusBar.setPadding(new Insets(5, 20, 5, 20));
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.setStyle("-fx-background-color: #95a5a6; -fx-border-color: #7f8c8d; -fx-border-width: 1 0 0 0;");
        
        statusLabel = new Label("Listo");
        statusLabel.setTextFill(Color.WHITE);
        statusLabel.setFont(Font.font("System", 11));
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Label versionLabel = new Label("SIGCR v1.0");
        versionLabel.setTextFill(Color.web("#ecf0f1"));
        versionLabel.setFont(Font.font("System", 10));
        
        statusBar.getChildren().addAll(statusLabel, spacer, versionLabel);
        return statusBar;
    }

    /**
     * Carga la vista inicial según el rol del usuario
     */
    private void cargarVistaInicial() {
        mostrarDashboard();
    }

    /**
     * Muestra indicador de carga
     */
    private void mostrarCarga(boolean mostrar) {
        Platform.runLater(() -> {
            loadingIndicator.setVisible(mostrar);
            if (mostrar) {
                statusLabel.setText("Cargando...");
            } else {
                statusLabel.setText("Listo");
            }
        });
    }

    /**
     * Cambia el contenido del área principal con feedback visual mejorado
     */
    private void cambiarContenido(Pane nuevaVista, String titulo) {
        mostrarCarga(true);
        
        // Simular carga en background con feedback visual
        new Thread(() -> {
            try {
                Thread.sleep(300); // Simular carga
                Platform.runLater(() -> {
                    contentArea.getChildren().clear();
                    contentArea.getChildren().addAll(loadingIndicator, nuevaVista);
                    primaryStage.setTitle("SIGCR - " + titulo + " - " + usuarioActual.getUsername());
                    mostrarCarga(false);
                    
                    // Mostrar notificación de navegación exitosa
                    ToastNotification.showInfo(contentArea, "Vista cargada: " + titulo);
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    ToastNotification.showError(contentArea, "Error al cargar vista: " + titulo);
                    mostrarCarga(false);
                });
            }
        }).start();
    }

    // Métodos de navegación
    private void mostrarDashboard() {
        VBox dashboard = crearDashboard();
        cambiarContenido(dashboard, "Dashboard");
    }

    private void mostrarPacientes() {
        if (pacienteView == null) {
            pacienteView = new PacienteView(conn, usuarioActual);
        }
        cambiarContenido(pacienteView.getView(), "Gestión de Pacientes");
    }

    private void mostrarCronogramas() {
        if (cronogramaView == null) {
            cronogramaView = new CronogramaView(conn, usuarioActual);
        }
        cambiarContenido(cronogramaView.getView(), "Cronogramas Terapéuticos");
    }

    private void mostrarNotificaciones() {
        if (notificacionView == null) {
            notificacionView = new NotificacionView(conn, usuarioActual);
        }
        cambiarContenido(notificacionView.getView(), "Notificaciones");
    }

    private void mostrarGestionUsuarios() {
        if (gestionUsuariosView == null) {
            gestionUsuariosView = new GestionUsuariosView(conn, authController);
        }
        cambiarContenido(gestionUsuariosView.getView(), "Gestión de Usuarios");
    }

    private void mostrarReportes() {
        VBox reportes = crearVistaReportes();
        cambiarContenido(reportes, "Reportes");
    }

    private void mostrarAgenda() {
        VBox agenda = crearVistaAgenda();
        cambiarContenido(agenda, "Mi Agenda");
    }

    private void mostrarConfiguracion() {
        VBox config = crearVistaConfiguracion();
        cambiarContenido(config, "Configuración");
    }

    private void mostrarAyuda() {
        VBox ayuda = crearVistaAyuda();
        cambiarContenido(ayuda, "Ayuda");
    }

    /**
     * Crea un dashboard personalizado con datos reales de la base de datos
     */
    private VBox crearDashboard() {
        VBox dashboard = new VBox(20);
        dashboard.setPadding(new Insets(20));
        
        Label titulo = new Label("Bienvenido, " + usuarioActual.getUsername());
        titulo.setFont(Font.font("System", FontWeight.BOLD, 24));
        titulo.setTextFill(Color.web("#2c3e50"));
        
        Label subtitulo = new Label("Rol: " + usuarioActual.getRole());
        subtitulo.setFont(Font.font("System", 16));
        subtitulo.setTextFill(Color.web("#7f8c8d"));
        
        // Cards de resumen con datos reales
        HBox cardsBox = new HBox(20);
        cardsBox.getChildren().addAll(
            crearCardConDatos("👥 Pacientes", obtenerTotalPacientes(), "#3498db"),
            crearCardConDatos("📅 Sesiones Hoy", obtenerSesionesHoy(), "#e74c3c"),
            crearCardConDatos("🔔 Notificaciones", obtenerNotificacionesNoLeidas(), "#f39c12")
        );
        
        // Acciones rápidas funcionales
        Label accionesLabel = new Label("Acciones Rápidas");
        accionesLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        accionesLabel.setTextFill(Color.web("#2c3e50"));
        
        HBox accionesBox = new HBox(15);
        Button btnNuevoPaciente = new Button("+ Nuevo Paciente");
        Button btnVerAgenda = new Button("📅 Ver Agenda");
        Button btnReportes = new Button("📋 Ver Reportes");
        
        // Estilo para botones de acción
        String estiloBoton = "-fx-background-color: #3498db; -fx-text-fill: white; -fx-padding: 12 24; -fx-background-radius: 8; -fx-font-size: 14px; -fx-font-weight: bold;";
        btnNuevoPaciente.setStyle(estiloBoton);
        btnVerAgenda.setStyle(estiloBoton + " -fx-background-color: #27ae60;");
        btnReportes.setStyle(estiloBoton + " -fx-background-color: #8e44ad;");
        
        // Funcionalidad de botones
        btnNuevoPaciente.setOnAction(e -> {
            mostrarPacientes();
            ToastNotification.showInfo(contentArea, "📋 Navegando a Gestión de Pacientes");
        });
        
        btnVerAgenda.setOnAction(e -> {
            if (usuarioActual.getRole().equals("TERAPEUTA") || usuarioActual.getRole().equals("ENFERMERIA")) {
                mostrarAgenda();
            } else {
                mostrarCronogramas();
            }
            ToastNotification.showInfo(contentArea, "📅 Cargando vista de agenda");
        });
        
        btnReportes.setOnAction(e -> {
            mostrarReportes();
            ToastNotification.showInfo(contentArea, "📊 Accediendo a reportes");
        });
        
        // Efectos hover para botones
        aplicarEfectoHover(btnNuevoPaciente);
        aplicarEfectoHover(btnVerAgenda);
        aplicarEfectoHover(btnReportes);
        
        accionesBox.getChildren().addAll(btnNuevoPaciente, btnVerAgenda, btnReportes);
        
        dashboard.getChildren().addAll(titulo, subtitulo, cardsBox, accionesLabel, accionesBox);
        return dashboard;
    }

    /**
     * Crea una card informativa con datos reales
     */
    private VBox crearCardConDatos(String titulo, String valor, String color) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 2);");
        card.setPrefWidth(200);
        
        Label tituloLabel = new Label(titulo);
        tituloLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        tituloLabel.setTextFill(Color.web(color));
        
        Label valorLabel = new Label(valor);
        valorLabel.setFont(Font.font("System", FontWeight.BOLD, 20));
        valorLabel.setTextFill(Color.web("#2c3e50"));
        
        card.getChildren().addAll(tituloLabel, valorLabel);
        return card;
    }

    /**
     * Obtiene el total de pacientes activos desde la base de datos
     */
    private String obtenerTotalPacientes() {
        try {
            com.sigcr.controllers.PacienteController pacienteController = 
                new com.sigcr.controllers.PacienteController(conn, usuarioActual);
            java.util.List<com.sigcr.models.Paciente> pacientes = pacienteController.obtenerTodosPacientes();
            long activos = pacientes.stream()
                .filter(p -> "Activo".equals(p.getEstado()))
                .count();
            return activos + " activos";
        } catch (Exception e) {
            return "N/A";
        }
    }

    /**
     * Obtiene las sesiones programadas para hoy
     */
    private String obtenerSesionesHoy() {
        try {
            com.sigcr.dao.SesionDAO sesionDAO = new com.sigcr.dao.SesionDAO(conn);
            java.util.List<com.sigcr.models.Sesion> sesionesHoy = 
                sesionDAO.obtenerSesionesPorRango(java.time.LocalDate.now(), java.time.LocalDate.now());
            return sesionesHoy.size() + " hoy";
        } catch (Exception e) {
            return "N/A";
        }
    }

    /**
     * Obtiene el número de notificaciones no leídas
     */
    private String obtenerNotificacionesNoLeidas() {
        try {
            com.sigcr.controllers.NotificacionController notifController = 
                new com.sigcr.controllers.NotificacionController(conn, usuarioActual);
            java.util.List<com.sigcr.models.Notificacion> noLeidas = 
                notifController.obtenerNotificacionesParaUsuarioActual(true);
            return noLeidas.size() + " nuevas";
        } catch (Exception e) {
            return "N/A";
        }
    }

    /**
     * Aplica efectos hover a los botones
     */
    private void aplicarEfectoHover(Button boton) {
        String estiloOriginal = boton.getStyle();
        boton.setOnMouseEntered(e -> {
            boton.setStyle(estiloOriginal + " -fx-scale-x: 1.05; -fx-scale-y: 1.05;");
        });
        boton.setOnMouseExited(e -> {
            boton.setStyle(estiloOriginal);
        });
    }

    /**
     * Crea una vista funcional de reportes básicos
     */
    private VBox crearVistaReportes() {
        VBox reportesView = new VBox(20);
        reportesView.setPadding(new Insets(20));
        
        // Título
        Label titulo = new Label("📊 Reportes del Sistema");
        titulo.setFont(Font.font("System", FontWeight.BOLD, 24));
        titulo.setTextFill(Color.web("#2c3e50"));
        
        // Resumen ejecutivo
        VBox resumenBox = new VBox(10);
        resumenBox.setStyle("-fx-background-color: white; -fx-padding: 20; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 2);");
        
        Label resumenTitulo = new Label("📈 Resumen Ejecutivo");
        resumenTitulo.setFont(Font.font("System", FontWeight.BOLD, 18));
        resumenTitulo.setTextFill(Color.web("#2c3e50"));
        
        // Cards de métricas
        HBox metricas = new HBox(15);
        metricas.getChildren().addAll(
            crearCardConDatos("👥 Total Pacientes", obtenerTotalPacientes(), "#3498db"),
            crearCardConDatos("✅ Pacientes Activos", obtenerPacientesActivos(), "#27ae60"),
            crearCardConDatos("📅 Sesiones Mes", obtenerSesionesMes(), "#e74c3c"),
            crearCardConDatos("💊 En Tratamiento", obtenerEnTratamiento(), "#9b59b6")
        );
        
        resumenBox.getChildren().addAll(resumenTitulo, metricas);
        
        // Reportes disponibles
        VBox reportesDisponibles = new VBox(15);
        reportesDisponibles.setStyle("-fx-background-color: white; -fx-padding: 20; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 2);");
        
        Label reportesTitulo = new Label("📋 Reportes Disponibles");
        reportesTitulo.setFont(Font.font("System", FontWeight.BOLD, 18));
        reportesTitulo.setTextFill(Color.web("#2c3e50"));
        
        // Botones de reportes
        HBox botonesReportes = new HBox(15);
        
        Button btnReportePacientes = new Button("👥 Reporte de Pacientes");
        Button btnReporteSesiones = new Button("📅 Reporte de Sesiones");
        Button btnReporteEstadisticas = new Button("📊 Estadísticas Generales");
        
        String estiloBotonReporte = "-fx-background-color: #3498db; -fx-text-fill: white; -fx-padding: 15 25; -fx-background-radius: 8; -fx-font-size: 14px;";
        btnReportePacientes.setStyle(estiloBotonReporte);
        btnReporteSesiones.setStyle(estiloBotonReporte + " -fx-background-color: #27ae60;");
        btnReporteEstadisticas.setStyle(estiloBotonReporte + " -fx-background-color: #8e44ad;");
        
        // Funcionalidad de botones
        btnReportePacientes.setOnAction(e -> mostrarReportePacientes());
        btnReporteSesiones.setOnAction(e -> mostrarReporteSesiones());
        btnReporteEstadisticas.setOnAction(e -> mostrarReporteEstadisticas());
        
        botonesReportes.getChildren().addAll(btnReportePacientes, btnReporteSesiones, btnReporteEstadisticas);
        
        // Área de resultados
        areaResultadosReportes = new javafx.scene.control.TextArea();
        areaResultadosReportes.setPromptText("Los resultados de los reportes aparecerán aquí...");
        areaResultadosReportes.setPrefRowCount(15);
        areaResultadosReportes.setEditable(false);
        areaResultadosReportes.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12px;");
        
        reportesDisponibles.getChildren().addAll(reportesTitulo, botonesReportes, areaResultadosReportes);
        
        reportesView.getChildren().addAll(titulo, resumenBox, reportesDisponibles);
        return reportesView;
    }

    private VBox crearVistaAgenda() {
        VBox agendaView = new VBox(20);
        agendaView.setPadding(new Insets(20));
        
        // Título
        Label titulo = new Label("📅 Mi Agenda Diaria");
        titulo.setFont(Font.font("System", FontWeight.BOLD, 24));
        titulo.setTextFill(Color.web("#2c3e50"));
        
        // Información del usuario y fecha
        VBox infoBox = new VBox(10);
        infoBox.setStyle("-fx-background-color: white; -fx-padding: 20; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        
        Label infoUsuario = new Label(String.format("👤 %s (%s)", usuarioActual.getUsername(), usuarioActual.getRole()));
        infoUsuario.setFont(Font.font("System", FontWeight.BOLD, 16));
        infoUsuario.setTextFill(Color.web("#2c3e50"));
        
        Label fechaHoy = new Label("📅 " + java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("EEEE, dd 'de' MMMM 'de' yyyy", java.util.Locale.forLanguageTag("es"))));
        fechaHoy.setFont(Font.font("System", 14));
        fechaHoy.setTextFill(Color.web("#7f8c8d"));
        
        infoBox.getChildren().addAll(infoUsuario, fechaHoy);
        
        // Selector de fecha
        HBox selectorFecha = new HBox(15);
        selectorFecha.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Label lblFecha = new Label("Ver agenda del día:");
        lblFecha.setFont(Font.font("System", FontWeight.BOLD, 14));
        
        DatePicker fechaPicker = new DatePicker(java.time.LocalDate.now());
        fechaPicker.setPrefWidth(150);
        
        Button btnCargarFecha = new Button("🔄 Cargar");
        btnCargarFecha.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-padding: 8 16; -fx-background-radius: 6px;");
        
        selectorFecha.getChildren().addAll(lblFecha, fechaPicker, btnCargarFecha);
        
        // Área de sesiones
        VBox sesionesBox = new VBox(15);
        sesionesBox.setStyle("-fx-background-color: white; -fx-padding: 20; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        
        Label tituloSesiones = new Label("🗓️ Sesiones Programadas");
        tituloSesiones.setFont(Font.font("System", FontWeight.BOLD, 18));
        tituloSesiones.setTextFill(Color.web("#2c3e50"));
        
        // Área de contenido de sesiones
        VBox contenidoSesiones = new VBox(10);
        
        // Función para cargar sesiones
        Runnable cargarSesiones = () -> {
            java.time.LocalDate fechaSeleccionada = fechaPicker.getValue();
            cargarSesionesDelDia(contenidoSesiones, fechaSeleccionada);
        };
        
        // Cargar sesiones iniciales
        cargarSesiones.run();
        
        // Evento del botón
        btnCargarFecha.setOnAction(e -> cargarSesiones.run());
        
        sesionesBox.getChildren().addAll(tituloSesiones, contenidoSesiones);
        
        agendaView.getChildren().addAll(titulo, infoBox, selectorFecha, sesionesBox);
        return agendaView;
    }

    private VBox crearVistaConfiguracion() {
        return crearVistaPlaceholder("Configuración", "Configuraciones del sistema");
    }

    private VBox crearVistaAyuda() {
        return crearVistaPlaceholder("Ayuda", "Manual de usuario y soporte");
    }

    private VBox crearVistaPlaceholder(String titulo, String descripcion) {
        VBox vista = new VBox(20);
        vista.setPadding(new Insets(50));
        vista.setAlignment(Pos.CENTER);
        
        Label tituloLabel = new Label(titulo);
        tituloLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
        tituloLabel.setTextFill(Color.web("#2c3e50"));
        
        Label descripcionLabel = new Label(descripcion);
        descripcionLabel.setFont(Font.font("System", 16));
        descripcionLabel.setTextFill(Color.web("#7f8c8d"));
        
        vista.getChildren().addAll(tituloLabel, descripcionLabel);
        return vista;
    }

    /**
     * Aplica tema visual moderno
     */
    private void aplicarTemaModerno() {
        // El tema se aplica mediante CSS inline en cada componente
        mainLayout.setStyle("-fx-font-family: 'Segoe UI', 'Helvetica Neue', Arial, sans-serif;");
    }

    /**
     * Configura shortcuts de teclado
     */
    private void configurarShortcuts() {
        mainLayout.setOnKeyPressed(event -> {
            if (event.isControlDown()) {
                switch (event.getCode()) {
                    case DIGIT1:
                        mostrarDashboard();
                        break;
                    case DIGIT2:
                        mostrarPacientes();
                        break;
                    case DIGIT3:
                        if (usuarioActual.getRole().equals("ADMIN") || usuarioActual.getRole().equals("MEDICO")) {
                            mostrarCronogramas();
                        }
                        break;
                    case N:
                        mostrarNotificaciones();
                        break;
                    case Q:
                        cerrarSesion();
                        break;
                }
            }
        });
        
        // Focus para capturar eventos de teclado
        mainLayout.setFocusTraversable(true);
        Platform.runLater(() -> mainLayout.requestFocus());
    }

    /**
     * Métodos para obtener datos adicionales para reportes
     */
    private String obtenerPacientesActivos() {
        try {
            com.sigcr.controllers.PacienteController pacienteController = 
                new com.sigcr.controllers.PacienteController(conn, usuarioActual);
            java.util.List<com.sigcr.models.Paciente> pacientes = pacienteController.obtenerTodosPacientes();
            long activos = pacientes.stream()
                .filter(p -> "Activo".equals(p.getEstado()))
                .count();
            return String.valueOf(activos);
        } catch (Exception e) {
            return "N/A";
        }
    }

    private String obtenerSesionesMes() {
        try {
            com.sigcr.dao.SesionDAO sesionDAO = new com.sigcr.dao.SesionDAO(conn);
            java.time.LocalDate inicioMes = java.time.LocalDate.now().withDayOfMonth(1);
            java.time.LocalDate finMes = java.time.LocalDate.now().withDayOfMonth(
                java.time.LocalDate.now().lengthOfMonth());
            java.util.List<com.sigcr.models.Sesion> sesionesMes = 
                sesionDAO.obtenerSesionesPorRango(inicioMes, finMes);
            return String.valueOf(sesionesMes.size());
        } catch (Exception e) {
            return "N/A";
        }
    }

    private String obtenerEnTratamiento() {
        try {
            com.sigcr.controllers.PacienteController pacienteController = 
                new com.sigcr.controllers.PacienteController(conn, usuarioActual);
            java.util.List<com.sigcr.models.Paciente> pacientes = pacienteController.obtenerTodosPacientes();
            long enTratamiento = pacientes.stream()
                .filter(p -> "Activo".equals(p.getEstado()) && 
                            p.getDiagnostico() != null && 
                            !p.getDiagnostico().trim().isEmpty())
                .count();
            return String.valueOf(enTratamiento);
        } catch (Exception e) {
            return "N/A";
        }
    }

    /**
     * Métodos para generar reportes
     */
    private void mostrarReportePacientes() {
        try {
            com.sigcr.controllers.PacienteController pacienteController = 
                new com.sigcr.controllers.PacienteController(conn, usuarioActual);
            java.util.List<com.sigcr.models.Paciente> pacientes = pacienteController.obtenerTodosPacientes();
            
            StringBuilder reporte = new StringBuilder();
            reporte.append("=== REPORTE DE PACIENTES ===\n");
            reporte.append("Generado: ").append(java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("\n\n");
            
            reporte.append("RESUMEN:\n");
            reporte.append("- Total de pacientes: ").append(pacientes.size()).append("\n");
            long activos = pacientes.stream().filter(p -> "Activo".equals(p.getEstado())).count();
            reporte.append("- Pacientes activos: ").append(activos).append("\n");
            long alta = pacientes.stream().filter(p -> "Alta".equals(p.getEstado())).count();
            reporte.append("- Pacientes de alta: ").append(alta).append("\n");
            long baja = pacientes.stream().filter(p -> "Baja".equals(p.getEstado())).count();
            reporte.append("- Pacientes de baja: ").append(baja).append("\n\n");
            
            reporte.append("DETALLE DE PACIENTES ACTIVOS:\n");
            reporte.append("----------------------------------------\n");
            for (com.sigcr.models.Paciente p : pacientes) {
                if ("Activo".equals(p.getEstado())) {
                    reporte.append("ID: ").append(p.getId()).append(" | ");
                    reporte.append("Nombre: ").append(p.getNombre()).append(" | ");
                    reporte.append("Documento: ").append(p.getDocumento()).append(" | ");
                    reporte.append("Habitación: ").append(p.getHabitacion() != null ? p.getHabitacion() : "N/A").append("\n");
                    reporte.append("Diagnóstico: ").append(p.getDiagnostico() != null ? p.getDiagnostico().substring(0, Math.min(50, p.getDiagnostico().length())) + "..." : "N/A").append("\n\n");
                }
            }
            
            areaResultadosReportes.setText(reporte.toString());
            ToastNotification.showSuccess(contentArea, "📊 Reporte de pacientes generado");
        } catch (Exception e) {
            areaResultadosReportes.setText("Error al generar reporte: " + e.getMessage());
            ToastNotification.showError(contentArea, "❌ Error al generar reporte");
        }
    }

    private void mostrarReporteSesiones() {
        try {
            com.sigcr.dao.SesionDAO sesionDAO = new com.sigcr.dao.SesionDAO(conn);
            java.time.LocalDate inicioMes = java.time.LocalDate.now().withDayOfMonth(1);
            java.time.LocalDate finMes = java.time.LocalDate.now().withDayOfMonth(
                java.time.LocalDate.now().lengthOfMonth());
            java.util.List<com.sigcr.models.Sesion> sesiones = 
                sesionDAO.obtenerSesionesPorRango(inicioMes, finMes);
            
            StringBuilder reporte = new StringBuilder();
            reporte.append("=== REPORTE DE SESIONES DEL MES ===\n");
            reporte.append("Período: ").append(inicioMes.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                   .append(" - ").append(finMes.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("\n");
            reporte.append("Generado: ").append(java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("\n\n");
            
            reporte.append("RESUMEN:\n");
            reporte.append("- Total de sesiones: ").append(sesiones.size()).append("\n");
            
            // Agrupar por tipo de terapia
            java.util.Map<String, Long> porTipo = sesiones.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    com.sigcr.models.Sesion::getTipoTerapia, java.util.stream.Collectors.counting()));
            
            reporte.append("- Por tipo de terapia:\n");
            for (java.util.Map.Entry<String, Long> entry : porTipo.entrySet()) {
                reporte.append("  • ").append(entry.getKey()).append(": ").append(entry.getValue()).append(" sesiones\n");
            }
            
            reporte.append("\nDETALLE DE SESIONES:\n");
            reporte.append("----------------------------------------\n");
            for (com.sigcr.models.Sesion s : sesiones) {
                reporte.append("Fecha: ").append(s.getFechaHora().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append(" | ");
                reporte.append("Paciente: ").append(s.getPacienteId()).append(" | ");
                reporte.append("Terapeuta: ").append(s.getTerapeuta()).append(" | ");
                reporte.append("Tipo: ").append(s.getTipoTerapia()).append(" | ");
                reporte.append("Duración: ").append(s.getDuracion()).append(" min\n");
            }
            
            areaResultadosReportes.setText(reporte.toString());
            ToastNotification.showSuccess(contentArea, "📅 Reporte de sesiones generado");
        } catch (Exception e) {
            areaResultadosReportes.setText("Error al generar reporte: " + e.getMessage());
            ToastNotification.showError(contentArea, "❌ Error al generar reporte");
        }
    }

    private void mostrarReporteEstadisticas() {
        try {
            StringBuilder reporte = new StringBuilder();
            reporte.append("=== ESTADÍSTICAS GENERALES ===\n");
            reporte.append("Generado: ").append(java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("\n\n");
            
            // Estadísticas de pacientes
            com.sigcr.controllers.PacienteController pacienteController = 
                new com.sigcr.controllers.PacienteController(conn, usuarioActual);
            java.util.List<com.sigcr.models.Paciente> pacientes = pacienteController.obtenerTodosPacientes();
            
            reporte.append("PACIENTES:\n");
            reporte.append("- Total: ").append(pacientes.size()).append("\n");
            reporte.append("- Activos: ").append(pacientes.stream().filter(p -> "Activo".equals(p.getEstado())).count()).append("\n");
            reporte.append("- De alta: ").append(pacientes.stream().filter(p -> "Alta".equals(p.getEstado())).count()).append("\n");
            reporte.append("- De baja: ").append(pacientes.stream().filter(p -> "Baja".equals(p.getEstado())).count()).append("\n\n");
            
            // Estadísticas de sesiones
            com.sigcr.dao.SesionDAO sesionDAO = new com.sigcr.dao.SesionDAO(conn);
            java.time.LocalDate inicioMes = java.time.LocalDate.now().withDayOfMonth(1);
            java.time.LocalDate finMes = java.time.LocalDate.now().withDayOfMonth(
                java.time.LocalDate.now().lengthOfMonth());
            java.util.List<com.sigcr.models.Sesion> sesiones = 
                sesionDAO.obtenerSesionesPorRango(inicioMes, finMes);
                
            reporte.append("SESIONES (MES ACTUAL):\n");
            reporte.append("- Total del mes: ").append(sesiones.size()).append("\n");
            reporte.append("- Promedio por día: ").append(String.format("%.1f", 
                sesiones.size() / (double) java.time.LocalDate.now().getDayOfMonth())).append("\n");
            
            if (!sesiones.isEmpty()) {
                double duracionPromedio = sesiones.stream().mapToInt(com.sigcr.models.Sesion::getDuracion).average().orElse(0);
                reporte.append("- Duración promedio: ").append(String.format("%.0f", duracionPromedio)).append(" minutos\n");
            }
            
            // Estadísticas de notificaciones
            com.sigcr.controllers.NotificacionController notifController = 
                new com.sigcr.controllers.NotificacionController(conn, usuarioActual);
            java.util.List<com.sigcr.models.Notificacion> notificaciones = 
                notifController.obtenerNotificacionesParaUsuarioActual(false);
            java.util.List<com.sigcr.models.Notificacion> noLeidas = 
                notifController.obtenerNotificacionesParaUsuarioActual(true);
                
            reporte.append("\nNOTIFICACIONES:\n");
            reporte.append("- Total: ").append(notificaciones.size()).append("\n");
            reporte.append("- No leídas: ").append(noLeidas.size()).append("\n");
            reporte.append("- Leídas: ").append(notificaciones.size() - noLeidas.size()).append("\n");
            
            areaResultadosReportes.setText(reporte.toString());
            ToastNotification.showSuccess(contentArea, "📈 Estadísticas generadas");
        } catch (Exception e) {
            areaResultadosReportes.setText("Error al generar estadísticas: " + e.getMessage());
            ToastNotification.showError(contentArea, "❌ Error al generar estadísticas");
        }
    }

    /**
     * Carga las sesiones del día seleccionado según el rol del usuario
     */
    private void cargarSesionesDelDia(VBox contenidoSesiones, java.time.LocalDate fecha) {
        contenidoSesiones.getChildren().clear();
        
        // Mostrar indicador de carga
        Label cargandoLabel = new Label("🔄 Cargando sesiones...");
        cargandoLabel.setFont(Font.font("System", 14));
        cargandoLabel.setTextFill(Color.web("#3498db"));
        cargandoLabel.setStyle("-fx-padding: 20; -fx-alignment: center;");
        contenidoSesiones.getChildren().add(cargandoLabel);
        
        try {
            // Debug: Mostrar información de la consulta
            System.out.println("🔍 Debug Agenda:");
            System.out.println("  - Usuario: " + usuarioActual.getUsername() + " (" + usuarioActual.getRole() + ")");
            System.out.println("  - Fecha consultada: " + fecha);
            System.out.println("  - Conexión DB: " + (conn != null ? "✅ Conectada" : "❌ Nula"));
            
            com.sigcr.dao.SesionDAO sesionDAO = new com.sigcr.dao.SesionDAO(conn);
            java.util.List<com.sigcr.models.Sesion> sesiones = 
                sesionDAO.obtenerSesionesPorRango(fecha, fecha);
            
            System.out.println("  - Sesiones encontradas en BD: " + sesiones.size());
            
            // Mostrar sesiones encontradas para debug
            for (com.sigcr.models.Sesion s : sesiones) {
                System.out.println("    • " + s.getFechaHora() + " - " + s.getTerapeuta() + " - " + s.getTipoTerapia());
            }
            
            // Filtrar sesiones según el rol del usuario
            java.util.List<com.sigcr.models.Sesion> sesionesFiltradas = new java.util.ArrayList<>();
            
            if (usuarioActual.getRole().equals("TERAPEUTA") || usuarioActual.getRole().equals("ENFERMERIA")) {
                // Terapeutas solo ven sus propias sesiones
                sesionesFiltradas = sesiones.stream()
                    .filter(s -> s.getTerapeuta().equalsIgnoreCase(usuarioActual.getUsername()))
                    .collect(java.util.stream.Collectors.toList());
                System.out.println("  - Sesiones filtradas para " + usuarioActual.getUsername() + ": " + sesionesFiltradas.size());
            } else {
                // Médicos y administradores ven todas las sesiones
                sesionesFiltradas = sesiones;
                System.out.println("  - Usuario admin/médico: mostrando todas las sesiones");
            }
            
            // Limpiar indicador de carga
            contenidoSesiones.getChildren().clear();
            
            if (sesionesFiltradas.isEmpty()) {
                // Mensaje simple cuando no hay sesiones
                VBox infoBox = new VBox(15);
                infoBox.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 30; -fx-background-radius: 10; -fx-alignment: center;");
                
                Label mensajeVacio = new Label("📭 No hay sesiones programadas para esta fecha");
                mensajeVacio.setFont(Font.font("System", FontWeight.BOLD, 18));
                mensajeVacio.setTextFill(Color.web("#6c757d"));
                
                Label sugerencia = new Label("Selecciona otra fecha o verifica tu cronograma");
                sugerencia.setFont(Font.font("System", 14));
                sugerencia.setTextFill(Color.web("#9ca3af"));
                
                infoBox.getChildren().addAll(mensajeVacio, sugerencia);
                contenidoSesiones.getChildren().add(infoBox);
            } else {
                // Ordenar sesiones por hora
                sesionesFiltradas.sort((s1, s2) -> s1.getFechaHora().compareTo(s2.getFechaHora()));
                
                // Crear card para cada sesión
                for (com.sigcr.models.Sesion sesion : sesionesFiltradas) {
                    VBox cardSesion = crearCardSesion(sesion);
                    contenidoSesiones.getChildren().add(cardSesion);
                }
                
                // Resumen del día
                HBox resumen = new HBox(20);
                resumen.setStyle("-fx-background-color: #ecf0f1; -fx-padding: 15; -fx-background-radius: 8; -fx-alignment: center;");
                
                Label lblResumen = new Label(String.format("📊 Total: %d sesiones programadas", sesionesFiltradas.size()));
                lblResumen.setFont(Font.font("System", FontWeight.BOLD, 14));
                lblResumen.setTextFill(Color.web("#2c3e50"));
                
                int totalMinutos = sesionesFiltradas.stream().mapToInt(com.sigcr.models.Sesion::getDuracion).sum();
                Label lblTiempo = new Label(String.format("⏱️ Tiempo total: %d horas %d minutos", 
                    totalMinutos / 60, totalMinutos % 60));
                lblTiempo.setFont(Font.font("System", 14));
                lblTiempo.setTextFill(Color.web("#7f8c8d"));
                
                resumen.getChildren().addAll(lblResumen, lblTiempo);
                contenidoSesiones.getChildren().add(resumen);
                
                System.out.println("✅ Agenda cargada exitosamente con " + sesionesFiltradas.size() + " sesiones");
            }
            
        } catch (Exception e) {
            // Limpiar indicador de carga
            contenidoSesiones.getChildren().clear();
            
            System.err.println("❌ Error al cargar agenda: " + e.getMessage());
            e.printStackTrace();
            
            VBox errorBox = new VBox(10);
            errorBox.setStyle("-fx-background-color: #f8d7da; -fx-padding: 20; -fx-background-radius: 8; -fx-border-color: #f5c6cb; -fx-border-radius: 8; -fx-border-width: 1;");
            
            Label errorLabel = new Label("❌ Error al cargar sesiones");
            errorLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
            errorLabel.setTextFill(Color.web("#721c24"));
            
            Label detalleError = new Label("Detalle: " + e.getMessage());
            detalleError.setFont(Font.font("System", 12));
            detalleError.setTextFill(Color.web("#721c24"));
            detalleError.setWrapText(true);
            
            errorBox.getChildren().addAll(errorLabel, detalleError);
            contenidoSesiones.getChildren().add(errorBox);
        }
    }

    /**
     * Crea una card visual para mostrar información de una sesión
     */
    private VBox crearCardSesion(com.sigcr.models.Sesion sesion) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 8; -fx-border-color: #e9ecef; -fx-border-radius: 8; -fx-border-width: 1; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 3, 0, 0, 1);");
        
        // Encabezado con hora y tipo
        HBox encabezado = new HBox(10);
        encabezado.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Label lblHora = new Label(sesion.getFechaHora().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
        lblHora.setFont(Font.font("System", FontWeight.BOLD, 18));
        lblHora.setTextFill(Color.web("#2c3e50"));
        lblHora.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-padding: 8 12; -fx-background-radius: 6;");
        
        Label lblTipo = new Label("🏥 " + sesion.getTipoTerapia());
        lblTipo.setFont(Font.font("System", FontWeight.BOLD, 14));
        lblTipo.setTextFill(Color.web("#27ae60"));
        
        Label lblDuracion = new Label(sesion.getDuracion() + " min");
        lblDuracion.setFont(Font.font("System", 12));
        lblDuracion.setTextFill(Color.web("#7f8c8d"));
        lblDuracion.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 4 8; -fx-background-radius: 4;");
        
        encabezado.getChildren().addAll(lblHora, lblTipo, lblDuracion);
        
        // Información del paciente y terapeuta
        VBox info = new VBox(5);
        
        Label lblPaciente = new Label("👤 Paciente ID: " + sesion.getPacienteId());
        lblPaciente.setFont(Font.font("System", 14));
        lblPaciente.setTextFill(Color.web("#2c3e50"));
        
        Label lblTerapeuta = new Label("👨‍⚕️ Terapeuta: " + sesion.getTerapeuta());
        lblTerapeuta.setFont(Font.font("System", 14));
        lblTerapeuta.setTextFill(Color.web("#7f8c8d"));
        
        // Si el usuario es el terapeuta, destacar
        if (usuarioActual.getUsername().equalsIgnoreCase(sesion.getTerapeuta())) {
            lblTerapeuta.setTextFill(Color.web("#27ae60"));
            lblTerapeuta.setText("👨‍⚕️ Terapeuta: " + sesion.getTerapeuta() + " (Yo)");
            lblTerapeuta.setFont(Font.font("System", FontWeight.BOLD, 14));
        }
        
        info.getChildren().addAll(lblPaciente, lblTerapeuta);
        
        card.getChildren().addAll(encabezado, info);
        return card;
    }

    /**
     * Cierra la sesión y vuelve al login
     */
    private void cerrarSesion() {
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Cerrar Sesión");
        confirmacion.setHeaderText("¿Está seguro de cerrar la sesión?");
        confirmacion.setContentText("Se perderán los cambios no guardados.");

        if (confirmacion.showAndWait().get() == ButtonType.OK) {
            authController.cerrarSesion();
            
            // Volver al login
            LoginView loginView = new LoginView(primaryStage);
            primaryStage.getScene().setRoot(loginView.getView());
            primaryStage.setTitle("SIGCR - Sistema Integral Gestión Clínica Rehabilitación");
            primaryStage.setResizable(false);
            primaryStage.centerOnScreen();
        }
    }

    public BorderPane getView() {
        return mainLayout;
    }
} 