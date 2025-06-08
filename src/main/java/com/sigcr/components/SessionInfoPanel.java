package com.sigcr.components;

import com.sigcr.controllers.AuthController;
import com.sigcr.models.User;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Componente reutilizable que muestra información de la sesión actual.
 * Incluye datos del usuario, tiempo de sesión y botón de logout.
 * Implementa actualización automática de la información de sesión (CU-03).
 */
public class SessionInfoPanel extends VBox {
    
    private AuthController authController;
    private Label lblUsuario;
    private Label lblRol;
    private Label lblTiempoSesion;
    private Label lblEstadoSesion;
    private Button btnLogout;
    private Thread actualizadorHilo;
    private Runnable onLogout;

    /**
     * Constructor del panel de información de sesión
     * @param authController Controlador de autenticación
     * @param onLogout Acción a ejecutar al hacer logout
     */
    public SessionInfoPanel(AuthController authController, Runnable onLogout) {
        this.authController = authController;
        this.onLogout = onLogout;
        inicializarComponentes();
        iniciarActualizacionAutomatica();
        actualizarInformacion();
    }

    /**
     * Inicializa los componentes visuales del panel
     */
    private void inicializarComponentes() {
        this.setSpacing(5);
        this.setPadding(new Insets(10));
        this.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; -fx-border-radius: 5;");
        this.setAlignment(Pos.CENTER_LEFT);

        // Información del usuario
        lblUsuario = new Label();
        lblUsuario.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        lblRol = new Label();
        lblRol.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 11px;");

        // Información de la sesión
        lblTiempoSesion = new Label();
        lblTiempoSesion.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 10px;");

        lblEstadoSesion = new Label();
        lblEstadoSesion.setStyle("-fx-text-fill: #28a745; -fx-font-size: 10px;");

        // Botón de logout
        btnLogout = new Button("Cerrar Sesión");
        btnLogout.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-size: 11px;");
        btnLogout.setOnAction(e -> {
            if (onLogout != null) {
                onLogout.run();
            }
        });

        // Agregar componentes
        VBox infoBox = new VBox(2);
        infoBox.getChildren().addAll(lblUsuario, lblRol, lblTiempoSesion, lblEstadoSesion);

        HBox mainBox = new HBox(15);
        mainBox.setAlignment(Pos.CENTER_LEFT);
        mainBox.getChildren().addAll(infoBox, btnLogout);

        this.getChildren().add(mainBox);
    }

    /**
     * Actualiza la información mostrada en el panel
     */
    private void actualizarInformacion() {
        if (!authController.estaAutenticado()) {
            mostrarNoAutenticado();
            return;
        }

        User usuario = authController.getUsuarioActual();
        if (usuario != null) {
            lblUsuario.setText("Usuario: " + usuario.getUsername());
            lblRol.setText("Rol: " + usuario.getRole());
            
            String infoSesion = authController.getInfoSesion();
            // Extraer información específica del string de sesión
            if (infoSesion.contains("Sesión:")) {
                String[] partes = infoSesion.split("\\|");
                for (String parte : partes) {
                    parte = parte.trim();
                    if (parte.startsWith("Sesión:")) {
                        lblTiempoSesion.setText("Tiempo de sesión: " + parte.substring(8));
                    } else if (parte.startsWith("Inactividad:")) {
                        String inactividad = parte.substring(13);
                        lblEstadoSesion.setText("Última actividad: hace " + inactividad);
                        
                        // Cambiar color según inactividad
                        if (inactividad.contains("0 min")) {
                            lblEstadoSesion.setStyle("-fx-text-fill: #28a745; -fx-font-size: 10px;"); // Verde
                        } else if (inactividad.contains("1") || inactividad.contains("2")) {
                            lblEstadoSesion.setStyle("-fx-text-fill: #ffc107; -fx-font-size: 10px;"); // Amarillo
                        } else {
                            lblEstadoSesion.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 10px;"); // Rojo
                        }
                    }
                }
            } else {
                lblTiempoSesion.setText("Información de sesión no disponible");
                lblEstadoSesion.setText("Estado: Activa");
            }
        } else {
            mostrarNoAutenticado();
        }
    }

    /**
     * Muestra estado cuando no hay usuario autenticado
     */
    private void mostrarNoAutenticado() {
        lblUsuario.setText("No autenticado");
        lblRol.setText("Sin rol asignado");
        lblTiempoSesion.setText("Sin sesión activa");
        lblEstadoSesion.setText("Inactivo");
        lblEstadoSesion.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 10px;");
        btnLogout.setDisable(true);
    }

    /**
     * Inicia la actualización automática de la información
     */
    private void iniciarActualizacionAutomatica() {
        actualizadorHilo = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(5000); // Actualizar cada 5 segundos
                    
                    Platform.runLater(() -> {
                        if (authController.estaAutenticado()) {
                            authController.actualizarActividad();
                            actualizarInformacion();
                        } else {
                            mostrarNoAutenticado();
                        }
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        
        actualizadorHilo.setDaemon(true);
        actualizadorHilo.setName("SessionInfoUpdater");
        actualizadorHilo.start();
    }

    /**
     * Detiene la actualización automática
     */
    public void detenerActualizacion() {
        if (actualizadorHilo != null && !actualizadorHilo.isInterrupted()) {
            actualizadorHilo.interrupt();
        }
    }

    /**
     * Establece la acción a ejecutar al hacer logout
     * @param onLogout Acción de logout
     */
    public void setOnLogout(Runnable onLogout) {
        this.onLogout = onLogout;
    }

    /**
     * Obtiene el botón de logout para configuración adicional
     * @return Botón de logout
     */
    public Button getLogoutButton() {
        return btnLogout;
    }

    /**
     * Establece la visibilidad del botón de logout
     * @param visible true para mostrar, false para ocultar
     */
    public void setLogoutButtonVisible(boolean visible) {
        btnLogout.setVisible(visible);
    }

    /**
     * Actualiza manualmente la información (útil después de cambios)
     */
    public void refrescar() {
        Platform.runLater(this::actualizarInformacion);
    }

    /**
     * Establece un estilo personalizado para el panel
     * @param style CSS style string
     */
    public void setCustomStyle(String style) {
        this.setStyle(style);
    }
} 