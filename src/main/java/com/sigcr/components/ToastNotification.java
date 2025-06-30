package com.sigcr.components;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

/**
 * Componente de notificaciones toast modernas para mejorar el feedback visual.
 * Proporciona notificaciones no intrusivas con animaciones suaves.
 */
public class ToastNotification extends StackPane {
    
    public enum ToastType {
        SUCCESS("linear-gradient(135deg, #4ade80 0%, #22c55e 100%)", "✅", "#ffffff", "#dcfce7"),
        ERROR("linear-gradient(135deg, #f87171 0%, #ef4444 100%)", "❌", "#ffffff", "#fef2f2"),
        WARNING("linear-gradient(135deg, #fbbf24 0%, #f59e0b 100%)", "⚠️", "#ffffff", "#fefbeb"),
        INFO("linear-gradient(135deg, #60a5fa 0%, #3b82f6 100%)", "ℹ️", "#ffffff", "#eff6ff");
        
        private final String backgroundColor;
        private final String icon;
        private final String textColor;
        private final String borderColor;
        
        ToastType(String backgroundColor, String icon, String textColor, String borderColor) {
            this.backgroundColor = backgroundColor;
            this.icon = icon;
            this.textColor = textColor;
            this.borderColor = borderColor;
        }
        
        public String getBackgroundColor() { return backgroundColor; }
        public String getIcon() { return icon; }
        public String getTextColor() { return textColor; }
        public String getBorderColor() { return borderColor; }
    }
    
    private Label iconLabel;
    private Label messageLabel;
    private HBox contentBox;
    private FadeTransition fadeIn;
    private FadeTransition fadeOut;
    private TranslateTransition slideIn;
    private TranslateTransition slideOut;

    public ToastNotification(String message, ToastType type) {
        inicializarComponentes(message, type);
        configurarAnimaciones();
        aplicarEstilos(type);
    }

    /**
     * Inicializa los componentes del toast con diseño moderno
     */
    private void inicializarComponentes(String message, ToastType type) {
        // icono con mejor tamaño y estilo
        iconLabel = new Label(type.getIcon());
        iconLabel.setFont(Font.font("System", FontWeight.BOLD, 20));
        iconLabel.setStyle(String.format("-fx-text-fill: %s; -fx-font-family: 'Segoe UI Emoji', 'Apple Color Emoji', 'Noto Color Emoji';", type.getTextColor()));
        iconLabel.setPrefWidth(35);
        iconLabel.setAlignment(Pos.CENTER);
        
        // Mensaje con mejor tipografia
        messageLabel = new Label(message);
        messageLabel.setFont(Font.font("Segoe UI", FontWeight.MEDIUM, 15));
        messageLabel.setStyle(String.format("-fx-text-fill: %s; -fx-font-smoothing-type: lcd;", type.getTextColor()));
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(280);
        
        // Contenedor principal con mejor espaciado
        contentBox = new HBox(15);
        contentBox.setAlignment(Pos.CENTER_LEFT);
        contentBox.setPadding(new Insets(16, 24, 16, 24));
        contentBox.getChildren().addAll(iconLabel, messageLabel);
        
        this.getChildren().add(contentBox);
        this.setMaxWidth(400);
        this.setMinHeight(60);
        this.setPrefHeight(Region.USE_COMPUTED_SIZE);
    }

    /**
     * Aplica estilos modernos segun el tipo de toast
     */
    private void aplicarEstilos(ToastType type) {
        // Estilo base moderno con gradiente y sombra mejorada
        String baseStyle = String.format(
            "-fx-background-color: %s; " +
            "-fx-background-radius: 12; " +
            "-fx-border-color: %s; " +
            "-fx-border-width: 1; " +
            "-fx-border-radius: 12; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 12, 0.4, 0, 4); " +
            "-fx-cursor: hand;",
            type.getBackgroundColor(),
            type.getBorderColor()
        );
        
        this.setStyle(baseStyle);
        
        // Animacion de hover mas suave
        this.setOnMouseEntered(e -> {
            this.setStyle(baseStyle + " -fx-scale-x: 1.02; -fx-scale-y: 1.02;");
            // Animacion suave de escala
            javafx.animation.ScaleTransition scaleIn = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(150), this);
            scaleIn.setFromX(1.0);
            scaleIn.setFromY(1.0);
            scaleIn.setToX(1.02);
            scaleIn.setToY(1.02);
            scaleIn.play();
        });
        
        this.setOnMouseExited(e -> {
            this.setStyle(baseStyle);
            // Animacion suave de vuelta
            javafx.animation.ScaleTransition scaleOut = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(150), this);
            scaleOut.setFromX(1.02);
            scaleOut.setFromY(1.02);
            scaleOut.setToX(1.0);
            scaleOut.setToY(1.0);
            scaleOut.play();
        });
        
        // Click para cerrar con feedback visual
        this.setOnMouseClicked(e -> {
            // Pequeña animacion de click
            javafx.animation.ScaleTransition clickScale = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(100), this);
            clickScale.setFromX(1.0);
            clickScale.setFromY(1.0);
            clickScale.setToX(0.95);
            clickScale.setToY(0.95);
            clickScale.setAutoReverse(true);
            clickScale.setCycleCount(2);
            clickScale.setOnFinished(event -> hide());
            clickScale.play();
        });
    }

    /**
     * Configura animaciones suaves y modernas
     */
    private void configurarAnimaciones() {
        // Animacion de entrada mas suave con curva ease-out
        fadeIn = new FadeTransition(Duration.millis(400), this);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.setInterpolator(javafx.animation.Interpolator.EASE_OUT);
        
        // Slide in mejorado con menos distancia y mejor timing
        slideIn = new TranslateTransition(Duration.millis(400), this);
        slideIn.setFromY(-60);
        slideIn.setToY(0);
        slideIn.setInterpolator(javafx.animation.Interpolator.EASE_OUT);
        
        // Animacion de salida mas rapida pero suave
        fadeOut = new FadeTransition(Duration.millis(300), this);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setInterpolator(javafx.animation.Interpolator.EASE_IN);
        
        // Slide out mejorado
        slideOut = new TranslateTransition(Duration.millis(300), this);
        slideOut.setFromY(0);
        slideOut.setToY(-60);
        slideOut.setInterpolator(javafx.animation.Interpolator.EASE_IN);
        
        // Al completar fade out, remover del parent
        fadeOut.setOnFinished(e -> {
            if (this.getParent() != null && this.getParent() instanceof StackPane) {
                ((StackPane) this.getParent()).getChildren().remove(this);
            }
        });
    }

    /**
     * Muestra el toast con animacion
     */
    public void show() {
        Platform.runLater(() -> {
            this.setOpacity(0);
            this.setTranslateY(-100);
            
            fadeIn.play();
            slideIn.play();
        });
    }

    /**
     * Oculta el toast con animacion
     */
    public void hide() {
        Platform.runLater(() -> {
            fadeOut.play();
            slideOut.play();
        });
    }

    /**
     * Muestra el toast y lo oculta automaticamente despues del tiempo especificado
     */
    public void showAndHide(Duration duration) {
        show();
        
        // Auto-hide despues del tiempo especificado
        new Thread(() -> {
            try {
                Thread.sleep((long) duration.toMillis());
                Platform.runLater(this::hide);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    /**
     * Metodos de conveniencia para crear toasts rapidamente
     */
    public static ToastNotification success(String message) {
        return new ToastNotification(message, ToastType.SUCCESS);
    }

    public static ToastNotification error(String message) {
        return new ToastNotification(message, ToastType.ERROR);
    }

    public static ToastNotification warning(String message) {
        return new ToastNotification(message, ToastType.WARNING);
    }

    public static ToastNotification info(String message) {
        return new ToastNotification(message, ToastType.INFO);
    }

    /**
     * Muestra un toast en un contenedor especifico
     */
    public static void showToast(StackPane container, String message, ToastType type) {
        showToast(container, message, type, Duration.seconds(3));
    }

    public static void showToast(StackPane container, String message, ToastType type, Duration duration) {
        Platform.runLater(() -> {
            ToastNotification toast = new ToastNotification(message, type);
            
            // Calcular posicion para apilar toasts
            long existingToasts = container.getChildren().stream()
                .filter(node -> node instanceof ToastNotification)
                .count();
            
            double topMargin = 20 + (existingToasts * 80); // 80px de separacion entre toasts
            
            // Posicionar en la parte superior derecha para mejor visibilidad
            StackPane.setAlignment(toast, Pos.TOP_RIGHT);
            StackPane.setMargin(toast, new Insets(topMargin, 20, 0, 20));
            
            container.getChildren().add(toast);
            toast.showAndHide(duration);
        });
    }

    /**
     * Metodos de conveniencia para mostrar directamente en un contenedor
     */
    public static void showSuccess(StackPane container, String message) {
        showToast(container, message, ToastType.SUCCESS);
    }

    public static void showError(StackPane container, String message) {
        showToast(container, message, ToastType.ERROR);
    }

    public static void showWarning(StackPane container, String message) {
        showToast(container, message, ToastType.WARNING);
    }

    public static void showInfo(StackPane container, String message) {
        showToast(container, message, ToastType.INFO);
    }

    /**
     * Limpia todos los toasts del contenedor
     */
    public static void clearAllToasts(StackPane container) {
        Platform.runLater(() -> {
            container.getChildren().removeIf(node -> node instanceof ToastNotification);
        });
    }
} 