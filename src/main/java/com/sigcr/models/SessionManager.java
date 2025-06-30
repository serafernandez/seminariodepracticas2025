package com.sigcr.models;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Gestor centralizado de sesiones para el sistema SIGCR.
 * Implementa el patron Singleton para mantener una unica instancia
 * de sesion activa en toda la aplicacion (CU-03).
 */
public class SessionManager {
    
    private static SessionManager instance;
    private User usuarioActual;
    private LocalDateTime inicioSesion;
    private LocalDateTime ultimaActividad;
    private Map<String, Object> atributosSesion;
    private boolean sesionActiva;
    
    // Configuracion de sesion
    private static final int TIMEOUT_MINUTOS = 30; // 30 minutos de inactividad
    private static final int MAX_INTENTOS_LOGIN = 3;
    
    // Control de intentos fallidos por usuario
    private Map<String, Integer> intentosFallidos;
    private Map<String, LocalDateTime> tiempoBloqueo;

    /**
     * Constructor privado para patron Singleton
     */
    private SessionManager() {
        this.atributosSesion = new HashMap<>();
        this.intentosFallidos = new HashMap<>();
        this.tiempoBloqueo = new HashMap<>();
        this.sesionActiva = false;
    }

    /**
     * Obtiene la instancia unica del SessionManager
     * @return Instancia del SessionManager
     */
    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    /**
     * Inicia una nueva sesion para el usuario
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
        
        System.out.println("Sesion iniciada para usuario: " + usuario.getUsername() + 
                          " con rol: " + usuario.getRole() + " a las " + inicioSesion);
    }

    /**
     * Cierra la sesion actual
     */
    public void cerrarSesion() {
        if (usuarioActual != null) {
            System.out.println("Sesion cerrada para usuario: " + usuarioActual.getUsername() + 
                              " duracion: " + java.time.Duration.between(inicioSesion, LocalDateTime.now()).toMinutes() + " minutos");
        }
        
        this.usuarioActual = null;
        this.inicioSesion = null;
        this.ultimaActividad = null;
        this.sesionActiva = false;
        this.atributosSesion.clear();
    }

    /**
     * Actualiza la ultima actividad del usuario
     */
    public void actualizarActividad() {
        if (sesionActiva) {
            this.ultimaActividad = LocalDateTime.now();
        }
    }

    /**
     * Verifica si la sesion esta activa y no ha expirado
     * @return true si la sesion es valida
     */
    public boolean esSesionValida() {
        if (!sesionActiva || usuarioActual == null || ultimaActividad == null) {
            return false;
        }
        
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime expiracion = ultimaActividad.plusMinutes(TIMEOUT_MINUTOS);
        
        if (ahora.isAfter(expiracion)) {
            System.out.println("Sesion expirada por inactividad para usuario: " + usuarioActual.getUsername());
            cerrarSesion();
            return false;
        }
        
        return true;
    }

    /**
     * Obtiene el usuario actualmente autenticado
     * @return Usuario actual o null si no hay sesion activa
     */
    public User getUsuarioActual() {
        if (esSesionValida()) {
            actualizarActividad();
            return usuarioActual;
        }
        return null;
    }

    /**
     * Verifica si el usuario actual tiene un rol especifico
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
     * Verifica si el usuario actual es medico
     * @return true si es medico
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
     * Verifica si el usuario actual es personal de enfermeria
     * @return true si es enfermeria
     */
    public boolean esEnfermeria() {
        return tieneRol("ENFERMERIA");
    }

    /**
     * Registra un intento fallido de login
     * @param username Nombre de usuario
     * @return true si el usuario esta bloqueado
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
     * Verifica si un usuario esta bloqueado
     * @param username Nombre de usuario
     * @return true si esta bloqueado
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
     * Obtiene el numero de intentos fallidos para un usuario
     * @param username Nombre de usuario
     * @return Numero de intentos fallidos
     */
    public int getIntentosFallidos(String username) {
        return intentosFallidos.getOrDefault(username, 0);
    }

    /**
     * Obtiene el tiempo restante de bloqueo para un usuario
     * @param username Nombre de usuario
     * @return Minutos restantes de bloqueo, 0 si no esta bloqueado
     */
    public long getMinutosBloqueoRestantes(String username) {
        LocalDateTime bloqueo = tiempoBloqueo.get(username);
        if (bloqueo != null && LocalDateTime.now().isBefore(bloqueo)) {
            return java.time.Duration.between(LocalDateTime.now(), bloqueo).toMinutes();
        }
        return 0;
    }

    /**
     * Establece un atributo de sesion
     * @param clave Clave del atributo
     * @param valor Valor del atributo
     */
    public void setAtributoSesion(String clave, Object valor) {
        if (esSesionValida()) {
            atributosSesion.put(clave, valor);
        }
    }

    /**
     * Obtiene un atributo de sesion
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
     * Obtiene informacion de la sesion actual
     * @return String con informacion de la sesion
     */
    public String getInfoSesion() {
        if (!esSesionValida()) {
            return "No hay sesion activa";
        }
        
        long minutosSesion = java.time.Duration.between(inicioSesion, LocalDateTime.now()).toMinutes();
        long minutosInactividad = java.time.Duration.between(ultimaActividad, LocalDateTime.now()).toMinutes();
        
        return String.format("Usuario: %s | Rol: %s | Sesion: %d min | Inactividad: %d min", 
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
     * Obtiene el numero de usuarios con intentos fallidos
     * @return Numero de usuarios con intentos fallidos
     */
    public int getNumeroUsuariosConIntentosFallidos() {
        return intentosFallidos.size();
    }

    /**
     * Obtiene el numero de usuarios bloqueados
     * @return Numero de usuarios bloqueados
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