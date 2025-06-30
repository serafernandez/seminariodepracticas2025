package com.sigcr.views;

import com.sigcr.controllers.AuthController;
import com.sigcr.models.User;
import com.sigcr.repositories.UserRepository;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Vista completa para la gestion de usuarios del sistema SIGCR.
 * Permite a los administradores crear, editar, desbloquear y gestionar
 * cuentas de usuario, implementando funcionalidades avanzadas del CU-03.
 */
public class GestionUsuariosView {
    
    private AuthController authController;
    private UserRepository userRepository;
    private Connection conn;
    private VBox mainLayout;
    
    // Componentes principales
    private TableView<User> tablaUsuarios;
    private ObservableList<User> listaUsuarios;
    private TextField nombreUsuarioField;
    private PasswordField passwordField;
    private PasswordField confirmPasswordField;
    private ComboBox<String> rolCombo;
    private TextArea areaEstadisticas;
    private Label lblInfoSesion;
    
    private User usuarioSeleccionado = null;

    /**
     * Constructor de la vista de gestion de usuarios
     */
    public GestionUsuariosView(Connection conn, AuthController authController) {
        this.conn = conn;
        this.authController = authController;
        this.userRepository = new UserRepository();
        this.listaUsuarios = FXCollections.observableArrayList();
        
        // Verificar permisos de administrador
        if (!authController.esAdmin()) {
            throw new SecurityException("Solo los administradores pueden acceder a la gestion de usuarios");
        }
        
        inicializarVista();
        cargarUsuarios();
        cargarEstadisticas();
        iniciarActualizacionPeriodica();
    }

    /**
     * Inicializa todos los componentes de la vista
     */
    private void inicializarVista() {
        mainLayout = new VBox(15);
        mainLayout.setPadding(new Insets(20));

        // Titulo y informacion de sesion
        VBox headerPanel = crearPanelHeader();
        
        // Panel de estadisticas de seguridad
        VBox panelEstadisticas = crearPanelEstadisticas();
        
        // Panel de gestion de usuarios
        HBox panelPrincipal = new HBox(20);
        
        // Panel izquierdo - Lista de usuarios
        VBox panelLista = crearPanelListaUsuarios();
        
        // Panel derecho - Formulario de usuario
        VBox panelFormulario = crearPanelFormulario();
        
        panelPrincipal.getChildren().addAll(panelLista, panelFormulario);
        
        // Panel de acciones administrativas
        VBox panelAcciones = crearPanelAccionesAdmin();

        mainLayout.getChildren().addAll(headerPanel, panelEstadisticas, panelPrincipal, panelAcciones);
    }

    /**
     * Crea el panel de header con titulo e informacion de sesion
     */
    private VBox crearPanelHeader() {
        VBox panel = new VBox(10);
        panel.setAlignment(Pos.CENTER);

        Label titulo = new Label("Gestion de Usuarios del Sistema");
        titulo.setFont(Font.font("System", FontWeight.BOLD, 20));
        titulo.setStyle("-fx-text-fill: #2c3e50;");

        lblInfoSesion = new Label(authController.getInfoSesion());
        lblInfoSesion.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");

        Button btnCerrarSesion = new Button("Cerrar Sesion");
        btnCerrarSesion.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        btnCerrarSesion.setOnAction(e -> cerrarSesion());

        HBox headerInfo = new HBox(20);
        headerInfo.setAlignment(Pos.CENTER);
        headerInfo.getChildren().addAll(lblInfoSesion, btnCerrarSesion);

        panel.getChildren().addAll(titulo, headerInfo);
        return panel;
    }

    /**
     * Crea el panel de estadisticas de seguridad
     */
    private VBox crearPanelEstadisticas() {
        VBox panel = new VBox(10);
        panel.setStyle("-fx-background-color: #ecf0f1; -fx-padding: 15; -fx-background-radius: 5;");

        Label lblTitulo = new Label("Estadisticas de Seguridad");
        lblTitulo.setFont(Font.font("System", FontWeight.BOLD, 14));

        areaEstadisticas = new TextArea();
        areaEstadisticas.setPrefRowCount(4);
        areaEstadisticas.setEditable(false);
        areaEstadisticas.setStyle("-fx-background-color: white;");

        Button btnActualizar = new Button("Actualizar Estadisticas");
        btnActualizar.setOnAction(e -> cargarEstadisticas());

        panel.getChildren().addAll(lblTitulo, areaEstadisticas, btnActualizar);
        return panel;
    }

    /**
     * Crea el panel con la lista de usuarios
     */
    private VBox crearPanelListaUsuarios() {
        VBox panel = new VBox(10);
        panel.setPrefWidth(400);

        Label lblTitulo = new Label("Usuarios del Sistema");
        lblTitulo.setFont(Font.font("System", FontWeight.BOLD, 14));

        // Tabla de usuarios
        tablaUsuarios = new TableView<>();
        tablaUsuarios.setItems(listaUsuarios);
        tablaUsuarios.setPrefHeight(300);

        TableColumn<User, String> colUsername = new TableColumn<>("Usuario");
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colUsername.setPrefWidth(120);

        TableColumn<User, String> colRol = new TableColumn<>("Rol");
        colRol.setCellValueFactory(new PropertyValueFactory<>("role"));
        colRol.setPrefWidth(100);

        TableColumn<User, Integer> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setPrefWidth(50);

        tablaUsuarios.getColumns().addAll(colId, colUsername, colRol);

        // Listener para seleccion
        tablaUsuarios.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                cargarUsuarioEnFormulario(newSelection);
                usuarioSeleccionado = newSelection;
            }
        });

        Button btnRefrescar = new Button("Refrescar Lista");
        btnRefrescar.setOnAction(e -> cargarUsuarios());

        panel.getChildren().addAll(lblTitulo, tablaUsuarios, btnRefrescar);
        return panel;
    }

    /**
     * Crea el panel del formulario de usuario
     */
    private VBox crearPanelFormulario() {
        VBox panel = new VBox(15);
        panel.setPrefWidth(350);
        panel.setStyle("-fx-background-color: white; -fx-padding: 20; -fx-border-color: #bdc3c7; -fx-border-radius: 5;");

        Label lblTitulo = new Label("Datos del Usuario");
        lblTitulo.setFont(Font.font("System", FontWeight.BOLD, 14));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(15);

        // Nombre de usuario
        grid.add(new Label("Usuario:"), 0, 0);
        nombreUsuarioField = new TextField();
        nombreUsuarioField.setPromptText("Nombre de usuario");
        grid.add(nombreUsuarioField, 1, 0);

        // Contraseña
        grid.add(new Label("Contraseña:"), 0, 1);
        passwordField = new PasswordField();
        passwordField.setPromptText("Nueva contraseña");
        grid.add(passwordField, 1, 1);

        // Confirmar contraseña
        grid.add(new Label("Confirmar:"), 0, 2);
        confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirmar contraseña");
        grid.add(confirmPasswordField, 1, 2);

        // Rol
        grid.add(new Label("Rol:"), 0, 3);
        rolCombo = new ComboBox<>();
        rolCombo.getItems().addAll("ADMIN", "MEDICO", "TERAPEUTA", "ENFERMERIA");
        rolCombo.setValue("TERAPEUTA");
        grid.add(rolCombo, 1, 3);

        // Botones
        HBox panelBotones = new HBox(10);
        
        Button btnNuevo = new Button("Nuevo Usuario");
        btnNuevo.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        btnNuevo.setOnAction(e -> limpiarFormulario());

        Button btnGuardar = new Button("Guardar");
        btnGuardar.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
        btnGuardar.setOnAction(e -> guardarUsuario());

        Button btnActualizar = new Button("Actualizar");
        btnActualizar.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white;");
        btnActualizar.setOnAction(e -> actualizarUsuario());

        Button btnEliminar = new Button("Eliminar");
        btnEliminar.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        btnEliminar.setOnAction(e -> eliminarUsuario());

        panelBotones.getChildren().addAll(btnNuevo, btnGuardar, btnActualizar, btnEliminar);

        panel.getChildren().addAll(lblTitulo, grid, panelBotones);
        return panel;
    }

    /**
     * Crea el panel de acciones administrativas
     */
    private VBox crearPanelAccionesAdmin() {
        VBox panel = new VBox(10);
        panel.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 15; -fx-border-color: #dee2e6; -fx-border-radius: 5;");

        Label lblTitulo = new Label("Acciones Administrativas");
        lblTitulo.setFont(Font.font("System", FontWeight.BOLD, 14));

        HBox panelBotones = new HBox(15);

        Button btnDesbloquearUsuario = new Button("Desbloquear Usuario");
        btnDesbloquearUsuario.setStyle("-fx-background-color: #17a2b8; -fx-text-fill: white;");
        btnDesbloquearUsuario.setOnAction(e -> desbloquearUsuario());

        Button btnHashearPasswords = new Button("Hashear Contraseñas");
        btnHashearPasswords.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white;");
        btnHashearPasswords.setOnAction(e -> hashearTodasLasPasswords());

        panelBotones.getChildren().addAll(btnDesbloquearUsuario, btnHashearPasswords);

        panel.getChildren().addAll(lblTitulo, panelBotones);
        return panel;
    }

    /**
     * Carga todos los usuarios del sistema
     */
    private void cargarUsuarios() {
        try {
            List<User> usuarios = obtenerTodosLosUsuarios();
            listaUsuarios.clear();
            listaUsuarios.addAll(usuarios);
        } catch (Exception e) {
            mostrarError("Error al cargar usuarios", e.getMessage());
        }
    }

    /**
     * Obtiene todos los usuarios de la base de datos
     */
    private List<User> obtenerTodosLosUsuarios() throws Exception {
        List<User> usuarios = new ArrayList<>();
        String sql = "SELECT * FROM usuario ORDER BY username";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                User usuario = new User(
                    rs.getInt("id"),
                    rs.getString("username"),
                    rs.getString("password"),
                    rs.getString("rol")
                );
                usuarios.add(usuario);
            }
        }
        return usuarios;
    }

    /**
     * Carga un usuario seleccionado en el formulario
     */
    private void cargarUsuarioEnFormulario(User usuario) {
        nombreUsuarioField.setText(usuario.getUsername());
        rolCombo.setValue(usuario.getRole());
        passwordField.clear();
        confirmPasswordField.clear();
    }

    /**
     * Limpia el formulario para un nuevo usuario
     */
    private void limpiarFormulario() {
        nombreUsuarioField.clear();
        passwordField.clear();
        confirmPasswordField.clear();
        rolCombo.setValue("TERAPEUTA");
        usuarioSeleccionado = null;
        tablaUsuarios.getSelectionModel().clearSelection();
    }

    /**
     * Guarda un nuevo usuario
     */
    private void guardarUsuario() {
        try {
            String username = nombreUsuarioField.getText().trim();
            String password = passwordField.getText();
            String confirmPassword = confirmPasswordField.getText();
            String rol = rolCombo.getValue();

            // Validaciones
            if (username.isEmpty() || password.isEmpty() || rol == null) {
                mostrarAdvertencia("Campos incompletos", "Complete todos los campos obligatorios");
                return;
            }

            if (!password.equals(confirmPassword)) {
                mostrarAdvertencia("Contraseñas no coinciden", "La confirmacion de contraseña no coincide");
                return;
            }

            if (password.length() < 6) {
                mostrarAdvertencia("Contraseña muy corta", "La contraseña debe tener al menos 6 caracteres");
                return;
            }

            // Hashear contraseña
            String hashedPassword = authController.hashPassword(password);

            // Crear usuario en BD
            String sql = "INSERT INTO usuario (username, password, rol) VALUES (?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, username.toLowerCase());
                stmt.setString(2, hashedPassword);
                stmt.setString(3, rol);
                stmt.executeUpdate();
            }

            mostrarInformacion("Exito", "Usuario creado exitosamente");
            limpiarFormulario();
            cargarUsuarios();

        } catch (Exception e) {
            mostrarError("Error al crear usuario", e.getMessage());
        }
    }

    /**
     * Actualiza un usuario existente
     */
    private void actualizarUsuario() {
        if (usuarioSeleccionado == null) {
            mostrarAdvertencia("Sin seleccion", "Seleccione un usuario para actualizar");
            return;
        }

        try {
            String username = nombreUsuarioField.getText().trim();
            String rol = rolCombo.getValue();

            if (username.isEmpty() || rol == null) {
                mostrarAdvertencia("Campos incompletos", "Complete todos los campos obligatorios");
                return;
            }

            String sql;
            String password = passwordField.getText();
            
            if (!password.isEmpty()) {
                // Actualizar con nueva contraseña
                if (!password.equals(confirmPasswordField.getText())) {
                    mostrarAdvertencia("Contraseñas no coinciden", "La confirmacion de contraseña no coincide");
                    return;
                }
                
                String hashedPassword = authController.hashPassword(password);
                sql = "UPDATE usuario SET username=?, password=?, rol=? WHERE id=?";
                
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, username.toLowerCase());
                    stmt.setString(2, hashedPassword);
                    stmt.setString(3, rol);
                    stmt.setInt(4, usuarioSeleccionado.getId());
                    stmt.executeUpdate();
                }
            } else {
                // Actualizar sin cambiar contraseña
                sql = "UPDATE usuario SET username=?, rol=? WHERE id=?";
                
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, username.toLowerCase());
                    stmt.setString(2, rol);
                    stmt.setInt(3, usuarioSeleccionado.getId());
                    stmt.executeUpdate();
                }
            }

            mostrarInformacion("Exito", "Usuario actualizado exitosamente");
            limpiarFormulario();
            cargarUsuarios();

        } catch (Exception e) {
            mostrarError("Error al actualizar usuario", e.getMessage());
        }
    }

    /**
     * Elimina un usuario (con confirmacion)
     */
    private void eliminarUsuario() {
        if (usuarioSeleccionado == null) {
            mostrarAdvertencia("Sin seleccion", "Seleccione un usuario para eliminar");
            return;
        }

        // No permitir eliminar al usuario actual
        if (usuarioSeleccionado.getId() == authController.getUsuarioActual().getId()) {
            mostrarAdvertencia("Operacion no permitida", "No puede eliminar su propia cuenta");
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar eliminacion");
        confirmacion.setHeaderText("¿Esta seguro de eliminar este usuario?");
        confirmacion.setContentText("Esta accion no se puede deshacer");

        Optional<ButtonType> resultado = confirmacion.showAndWait();
        if (resultado.isPresent() && resultado.get() == ButtonType.OK) {
            try {
                String sql = "DELETE FROM usuario WHERE id=?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, usuarioSeleccionado.getId());
                    stmt.executeUpdate();
                }

                mostrarInformacion("Exito", "Usuario eliminado exitosamente");
                limpiarFormulario();
                cargarUsuarios();

            } catch (Exception e) {
                mostrarError("Error al eliminar usuario", e.getMessage());
            }
        }
    }

    /**
     * Carga estadisticas de seguridad
     */
    private void cargarEstadisticas() {
        try {
            String estadisticas = authController.getEstadisticasLogin();
            areaEstadisticas.setText(estadisticas);
        } catch (Exception e) {
            areaEstadisticas.setText("Error al cargar estadisticas: " + e.getMessage());
        }
    }

    /**
     * Desbloquea un usuario
     */
    private void desbloquearUsuario() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Desbloquear Usuario");
        dialog.setHeaderText("Ingrese el nombre de usuario a desbloquear:");
        dialog.setContentText("Usuario:");

        Optional<String> resultado = dialog.showAndWait();
        resultado.ifPresent(username -> {
            try {
                authController.desbloquearUsuario(username.trim().toLowerCase());
                mostrarInformacion("Exito", "Usuario desbloqueado: " + username);
                cargarEstadisticas();
            } catch (Exception e) {
                mostrarError("Error al desbloquear usuario", e.getMessage());
            }
        });
    }

    /**
     * Hashea todas las contraseñas en texto plano
     */
    private void hashearTodasLasPasswords() {
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Hashear contraseñas");
        confirmacion.setHeaderText("¿Hashear todas las contraseñas en texto plano?");
        confirmacion.setContentText("Esta operacion es irreversible");

        Optional<ButtonType> resultado = confirmacion.showAndWait();
        if (resultado.isPresent() && resultado.get() == ButtonType.OK) {
            try {
                int actualizadas = 0;
                String selectSql = "SELECT id, username, password FROM usuario WHERE password NOT LIKE '%:%'";
                String updateSql = "UPDATE usuario SET password=? WHERE id=?";
                
                try (PreparedStatement selectStmt = conn.prepareStatement(selectSql);
                     PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    
                    ResultSet rs = selectStmt.executeQuery();
                    while (rs.next()) {
                        int id = rs.getInt("id");
                        String plainPassword = rs.getString("password");
                        String hashedPassword = authController.hashPassword(plainPassword);
                        
                        updateStmt.setString(1, hashedPassword);
                        updateStmt.setInt(2, id);
                        updateStmt.executeUpdate();
                        actualizadas++;
                    }
                }

                mostrarInformacion("Completado", "Se hashearon " + actualizadas + " contraseñas");
                
            } catch (Exception e) {
                mostrarError("Error al hashear contraseñas", e.getMessage());
            }
        }
    }

    /**
     * Inicia actualizacion periodica de informacion de sesion
     */
    private void iniciarActualizacionPeriodica() {
        Thread actualizador = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(10000); // Actualizar cada 10 segundos
                    javafx.application.Platform.runLater(() -> {
                        if (authController.estaAutenticado()) {
                            lblInfoSesion.setText(authController.getInfoSesion());
                            authController.actualizarActividad();
                        }
                    });
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        actualizador.setDaemon(true);
        actualizador.start();
    }

    /**
     * Cierra la sesion y vuelve al login
     */
    private void cerrarSesion() {
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Cerrar Sesion");
        confirmacion.setHeaderText("¿Esta seguro de cerrar la sesion?");

        Optional<ButtonType> resultado = confirmacion.showAndWait();
        if (resultado.isPresent() && resultado.get() == ButtonType.OK) {
            authController.cerrarSesion();
            // Aqui deberiamos volver a la vista de login
            // Por simplicidad, solo cerramos la aplicacion
            javafx.application.Platform.exit();
        }
    }

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

    public VBox getView() {
        return mainLayout;
    }
} 