package com.sigcr.models;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Gestor centralizado de sesiones para el sistema SIGCR.
 * Implementa el patrón Singleton para mantener una única instancia
 * de sesión activa en toda la aplicación (CU-03).
 */
public class SessionManager {
    
    private static SessionManager instance;
    private User usuarioActual;
    private LocalDateTime inicioSesion;
    private LocalDateTime ultimaActividad;
    private Map<String, Object> atributosSesion;
    private boolean sesionActiva;
    
    // Configuración de sesión
    private static final int TIMEOUT_MINUTOS = 30; // 30 minutos de inactividad
    private static final int MAX_INTENTOS_LOGIN = 3;
    
    // Control de intentos fallidos por usuario
    private Map<String, Integer> intentosFallidos;
    private Map<String, LocalDateTime> tiempoBloqueo;

    /**
     * Constructor privado para patrón Singleton
     */
    private SessionManager() {
        this.atributosSesion = new HashMap<>();
        this.intentosFallidos = new HashMap<>();
        this.tiempoBloqueo = new HashMap<>();
        this.sesionActiva = false;
    }

    /**
     * Obtiene la instancia única del SessionManager
     * @return Instancia del SessionManager
     */
    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    /**
     * Inicia una nueva sesión para el usuario
     * @param usuario Usuario autenticado
     */
    public void iniciarSesion(User usuario) {
        this.usuarioActual = usuario;
        this.inicioSesion = LocalDateTime.now();
        this.ultimaActividad = LocalDateTime.now();
        this.sesionActiva = true;
        this.atributosSesion.clear();
        
        // Limpiar intentos fallidos al login exitoso
        intentosFallidos.remove(usuario.getUsername());
        tiempoBloqueo.remove(usuario.getUsername());
        
        System.out.println("Sesión iniciada para usuario: " + usuario.getUsername() + 
                          " con rol: " + usuario.getRole() + " a las " + inicioSesion);
    }

    /**
     * Cierra la sesión actual
     */
    public void cerrarSesion() {
        if (usuarioActual != null) {
            System.out.println("Sesión cerrada para usuario: " + usuarioActual.getUsername() + 
                              " duración: " + java.time.Duration.between(inicioSesion, LocalDateTime.now()).toMinutes() + " minutos");
        }
        
        this.usuarioActual = null;
        this.inicioSesion = null;
        this.ultimaActividad = null;
        this.sesionActiva = false;
        this.atributosSesion.clear();
    }

    /**
     * Actualiza la última actividad del usuario
     */
    public void actualizarActividad() {
        if (sesionActiva) {
            this.ultimaActividad = LocalDateTime.now();
        }
    }

    /**
     * Verifica si la sesión está activa y no ha expirado
     * @return true si la sesión es válida
     */
    public boolean esSesionValida() {
        if (!sesionActiva || usuarioActual == null || ultimaActividad == null) {
            return false;
        }
        
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime expiracion = ultimaActividad.plusMinutes(TIMEOUT_MINUTOS);
        
        if (ahora.isAfter(expiracion)) {
            System.out.println("Sesión expirada por inactividad para usuario: " + usuarioActual.getUsername());
            cerrarSesion();
            return false;
        }
        
        return true;
    }

    /**
     * Obtiene el usuario actualmente autenticado
     * @return Usuario actual o null si no hay sesión activa
     */
    public User getUsuarioActual() {
        if (esSesionValida()) {
            actualizarActividad();
            return usuarioActual;
        }
        return null;
    }

    /**
     * Verifica si el usuario actual tiene un rol específico
     * @param rol Rol a verificar
     * @return true si el usuario tiene el rol especificado
     */
    public boolean tieneRol(String rol) {
        User usuario = getUsuarioActual();
        return usuario != null && usuario.getRole().equals(rol);
    }

    /**
     * Verifica si el usuario actual es administrador
     * @return true si es administrador
     */
    public boolean esAdmin() {
        return tieneRol("ADMIN");
    }

    /**
     * Verifica si el usuario actual es médico
     * @return true si es médico
     */
    public boolean esMedico() {
        return tieneRol("MEDICO");
    }

    /**
     * Verifica si el usuario actual es terapeuta
     * @return true si es terapeuta
     */
    public boolean esTerapeuta() {
        return tieneRol("TERAPEUTA");
    }

    /**
     * Verifica si el usuario actual es personal de enfermería
     * @return true si es enfermería
     */
    public boolean esEnfermeria() {
        return tieneRol("ENFERMERIA");
    }

    /**
     * Registra un intento fallido de login
     * @param username Nombre de usuario
     * @return true si el usuario está bloqueado
     */
    public boolean registrarIntentoFallido(String username) {
        int intentos = intentosFallidos.getOrDefault(username, 0) + 1;
        intentosFallidos.put(username, intentos);
        
        if (intentos >= MAX_INTENTOS_LOGIN) {
            // Bloquear usuario por 15 minutos
            tiempoBloqueo.put(username, LocalDateTime.now().plusMinutes(15));
            System.out.println("Usuario bloqueado por intentos fallidos: " + username);
            return true;
        }
        
        return false;
    }

    /**
     * Verifica si un usuario está bloqueado
     * @param username Nombre de usuario
     * @return true si está bloqueado
     */
    public boolean estaUsuarioBloqueado(String username) {
        LocalDateTime bloqueo = tiempoBloqueo.get(username);
        if (bloqueo != null) {
            if (LocalDateTime.now().isBefore(bloqueo)) {
                return true;
            } else {
                // El bloqueo ha expirado, limpiar
                tiempoBloqueo.remove(username);
                intentosFallidos.remove(username);
            }
        }
        return false;
    }

    /**
     * Obtiene el número de intentos fallidos para un usuario
     * @param username Nombre de usuario
     * @return Número de intentos fallidos
     */
    public int getIntentosFallidos(String username) {
        return intentosFallidos.getOrDefault(username, 0);
    }

    /**
     * Obtiene el tiempo restante de bloqueo para un usuario
     * @param username Nombre de usuario
     * @return Minutos restantes de bloqueo, 0 si no está bloqueado
     */
    public long getMinutosBloqueoRestantes(String username) {
        LocalDateTime bloqueo = tiempoBloqueo.get(username);
        if (bloqueo != null && LocalDateTime.now().isBefore(bloqueo)) {
            return java.time.Duration.between(LocalDateTime.now(), bloqueo).toMinutes();
        }
        return 0;
    }

    /**
     * Establece un atributo de sesión
     * @param clave Clave del atributo
     * @param valor Valor del atributo
     */
    public void setAtributoSesion(String clave, Object valor) {
        if (esSesionValida()) {
            atributosSesion.put(clave, valor);
        }
    }

    /**
     * Obtiene un atributo de sesión
     * @param clave Clave del atributo
     * @return Valor del atributo o null si no existe
     */
    public Object getAtributoSesion(String clave) {
        if (esSesionValida()) {
            return atributosSesion.get(clave);
        }
        return null;
    }

    /**
     * Obtiene información de la sesión actual
     * @return String con información de la sesión
     */
    public String getInfoSesion() {
        if (!esSesionValida()) {
            return "No hay sesión activa";
        }
        
        long minutosSesion = java.time.Duration.between(inicioSesion, LocalDateTime.now()).toMinutes();
        long minutosInactividad = java.time.Duration.between(ultimaActividad, LocalDateTime.now()).toMinutes();
        
        return String.format("Usuario: %s | Rol: %s | Sesión: %d min | Inactividad: %d min", 
                           usuarioActual.getUsername(), 
                           usuarioActual.getRole(), 
                           minutosSesion, 
                           minutosInactividad);
    }

    /**
     * Desbloquea manualmente un usuario (para administradores)
     * @param username Usuario a desbloquear
     */
    public void desbloquearUsuarioAdmin(String username) {
        tiempoBloqueo.remove(username);
        intentosFallidos.remove(username);
    }

    /**
     * Obtiene el número de usuarios con intentos fallidos
     * @return Número de usuarios con intentos fallidos
     */
    public int getNumeroUsuariosConIntentosFallidos() {
        return intentosFallidos.size();
    }

    /**
     * Obtiene el número de usuarios bloqueados
     * @return Número de usuarios bloqueados
     */
    public int getNumeroUsuariosBloqueados() {
        return tiempoBloqueo.size();
    }

    /**
     * Obtiene el mapa de intentos fallidos (para administradores)
     * @return Copia del mapa de intentos fallidos
     */
    public Map<String, Integer> getMapaIntentosFallidos() {
        return new HashMap<>(intentosFallidos);
    }
} 