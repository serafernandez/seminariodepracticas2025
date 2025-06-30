package com.sigcr.views;

import com.sigcr.controllers.AuthController;
import com.sigcr.models.User;
import com.sigcr.views.MainApplicationView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.Connection;

/**
 * Vista para autenticacion de usuarios (CU-03).
 * Maneja el login y redireccion segun roles del sistema SIGCR.
 */
public class LoginView {

    private VBox view;
    private AuthController authController;
    private Stage primaryStage;

    public LoginView(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.authController = new AuthController();
        inicializarVista();
    }

    /**
     * Inicializa todos los componentes de la vista de login
     */
    private void inicializarVista() {
        view = new VBox(20);
        view.setAlignment(Pos.CENTER);
        view.setPadding(new Insets(40));
        view.setStyle("-fx-background-color: #f5f5f5;");

        // Titulo del sistema
        Label lblTitulo = new Label("SIGCR");
        lblTitulo.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label lblSubtitulo = new Label("Sistema Integral de Gestion para Clinicas de Rehabilitacion");
        lblSubtitulo.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d; -fx-font-style: italic;");

        // Panel de login
        VBox panelLogin = crearPanelLogin();

        // Panel de credenciales de ejemplo
        VBox panelCredenciales = crearPanelCredenciales();

        view.getChildren().addAll(lblTitulo, lblSubtitulo, panelLogin, panelCredenciales);
    }

    /**
     * Crea el panel principal de login
     */
    private VBox crearPanelLogin() {
        VBox panel = new VBox(15);
        panel.setMaxWidth(350);
        panel.setStyle("-fx-background-color: white; -fx-padding: 30; -fx-background-radius: 10; " +
                      "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 2);");

        Label lblLogin = new Label("Iniciar Sesion");
        lblLogin.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        // Formulario
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(15);
        grid.setAlignment(Pos.CENTER);

        // Campo usuario
        Label lblUsername = new Label("Usuario:");
        lblUsername.setStyle("-fx-font-weight: bold;");
        TextField txtUsername = new TextField();
        txtUsername.setPromptText("Ingrese su usuario");
        txtUsername.setStyle("-fx-pref-width: 200; -fx-pref-height: 35;");

        // Campo contraseña
        Label lblPassword = new Label("Contraseña:");
        lblPassword.setStyle("-fx-font-weight: bold;");
        PasswordField txtPassword = new PasswordField();
        txtPassword.setPromptText("Ingrese su contraseña");
        txtPassword.setStyle("-fx-pref-width: 200; -fx-pref-height: 35;");

        grid.add(lblUsername, 0, 0);
        grid.add(txtUsername, 1, 0);
        grid.add(lblPassword, 0, 1);
        grid.add(txtPassword, 1, 1);

        // Boton de login
        Button btnLogin = new Button("Ingresar");
        btnLogin.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; " +
                         "-fx-font-size: 14px; -fx-font-weight: bold; -fx-pref-width: 200; -fx-pref-height: 40;");

        // Label para mensajes
        Label lblMessage = new Label("");
        lblMessage.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");

        // Evento de login
        btnLogin.setOnAction(event -> {
            String user = txtUsername.getText().trim();
            String pass = txtPassword.getText();
            
            if (user.isEmpty() || pass.isEmpty()) {
                mostrarMensaje(lblMessage, "Por favor complete todos los campos", "error");
                return;
            }

            // Deshabilitar boton durante el proceso
            btnLogin.setDisable(true);
            
            AuthController.ResultadoAutenticacion resultado = authController.autenticar(user, pass);
            
            if (resultado.isExitoso()) {
                User usuarioActual = resultado.getUsuario();
                mostrarMensaje(lblMessage, "Bienvenido, " + usuarioActual.getUsername(), "success");
                
                // Simular carga y redirigir
                new Thread(() -> {
                    try {
                        Thread.sleep(800); // Simular carga
                        javafx.application.Platform.runLater(() -> {
                            redirigirSegunRol(usuarioActual);
                        });
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
            } else {
                mostrarMensaje(lblMessage, resultado.getMensaje(), "error");
                txtPassword.clear();
                
                // Re-habilitar boton despues de un delay
                new Thread(() -> {
                    try {
                        Thread.sleep(1000);
                        javafx.application.Platform.runLater(() -> {
                            btnLogin.setDisable(false);
                        });
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        });

        // Permitir login con Enter
        txtPassword.setOnAction(e -> btnLogin.fire());

        panel.getChildren().addAll(lblLogin, grid, btnLogin, lblMessage);
        return panel;
    }

    /**
     * Crea panel con credenciales de ejemplo para testing
     */
    private VBox crearPanelCredenciales() {
        VBox panel = new VBox(10);
        panel.setMaxWidth(350);
        panel.setStyle("-fx-background-color: #ecf0f1; -fx-padding: 20; -fx-background-radius: 5;");

        Label lblTitulo = new Label("Credenciales de Prueba:");
        lblTitulo.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #34495e;");

        Label lblCredenciales = new Label(
            "• Admin: admin / admin123\n" +
            "• Medico: dr.juarez / medico123\n" +
            "• Terapeuta: luz.terapeuta / terapia123\n" +
            "• Enfermeria: pablo.enfermero / enfermeria123"
        );
        lblCredenciales.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d;");

        panel.getChildren().addAll(lblTitulo, lblCredenciales);
        return panel;
    }

    /**
     * Muestra mensaje con estilo segun tipo
     */
    private void mostrarMensaje(Label label, String mensaje, String tipo) {
        label.setText(mensaje);
        if ("error".equals(tipo)) {
            label.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
        } else if ("success".equals(tipo)) {
            label.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
        }
    }

    /**
     * Redirige al usuario segun su rol despues del login exitoso
     */
    private void redirigirSegunRol(User usuario) {
        try {
            // Obtener conexion para las vistas
            Connection conn = new com.sigcr.repositories.UserRepository().getConnection();
            
            // Crear la vista principal unificada
            MainApplicationView mainApp = new MainApplicationView(conn, usuario, authController, primaryStage);
            
            // Cambiar a la vista principal
            primaryStage.getScene().setRoot(mainApp.getView());
            primaryStage.setTitle("SIGCR - Sistema Integral Gestion Clinica - " + usuario.getUsername());
            primaryStage.setResizable(true);
            primaryStage.setMaximized(true);
            
        } catch (Exception e) {
            mostrarError("Error al cargar aplicacion principal: " + e.getMessage());
        }
    }



    /**
     * Muestra un error en dialog
     */
    private void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    /**
     * Obtiene la vista principal
     */
    public VBox getView() {
        return view;
    }
}