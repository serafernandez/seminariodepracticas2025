package com.sigcr;

import com.sigcr.views.LoginView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Clase principal para iniciar la aplicación SIGCR.
 * Inicializa la vista de Login como primer punto de entrada.
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        LoginView loginView = new LoginView(primaryStage);
        Scene scene = new Scene(loginView.getView());
        
        primaryStage.setTitle("SIGCR - Sistema Integral Gestión Clínica Rehabilitación");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}