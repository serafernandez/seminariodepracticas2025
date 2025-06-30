package com.sigcr.controllers;

import com.sigcr.models.SessionManager;
import com.sigcr.models.User;
import com.sigcr.repositories.UserRepository;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Controlador mejorado que maneja la autenticacion y autorizacion de usuarios (CU-03).
 * Implementa seguridad robusta con gestion de sesiones, control de intentos fallidos
 * y verificacion de permisos en tiempo real.
 */
public class AuthController {

    private UserRepository userRepository;
    private SessionManager sessionManager;

    public AuthController() {
        userRepository = new UserRepository();
        sessionManager = SessionManager.getInstance();
    }

    /**
     * Autentica un usuario con validaciones de seguridad completas
     * @param username Nombre de usuario
     * @param password Contraseña
     * @return Resultado de la autenticacion con detalles
     */
    public ResultadoAutenticacion autenticar(String username, String password) {
        // Validacion basica de entrada
        if (username == null || username.trim().isEmpty() || 
            password == null || password.trim().isEmpty()) {
            return new ResultadoAutenticacion(false, "Debe completar todos los campos");
        }

        username = username.trim().toLowerCase();

        // Verificar si el usuario esta bloqueado
        if (sessionManager.estaUsuarioBloqueado(username)) {
            long minutosRestantes = sessionManager.getMinutosBloqueoRestantes(username);
            return new ResultadoAutenticacion(false, 
                String.format("Usuario bloqueado. Tiempo restante: %d minutos", minutosRestantes));
        }

        try {
            // Buscar usuario en la base de datos
            User user = userRepository.findByUsername(username);
            if (user == null) {
                // Usuario no existe - registrar intento fallido
                sessionManager.registrarIntentoFallido(username);
                return new ResultadoAutenticacion(false, "Credenciales incorrectas");
            }

            // Verificar contraseña
            boolean passwordValida;
            if (esPasswordHasheada(user.getPasswordHash())) {
                // Contraseña hasheada
                passwordValida = verificarPasswordHash(password, user.getPasswordHash());
            } else {
                // Contraseña en texto plano (para compatibilidad con datos de prueba)
                passwordValida = password.equals(user.getPasswordHash());
            }

            if (!passwordValida) {
                // Contraseña incorrecta - registrar intento fallido
                boolean bloqueado = sessionManager.registrarIntentoFallido(username);
                int intentos = sessionManager.getIntentosFallidos(username);
                
                if (bloqueado) {
                    return new ResultadoAutenticacion(false, 
                        "Demasiados intentos fallidos. Usuario bloqueado por 15 minutos");
                } else {
                    return new ResultadoAutenticacion(false, 
                        String.format("Credenciales incorrectas. Intentos restantes: %d", 3 - intentos));
                }
            }

            // Autenticacion exitosa - iniciar sesion
            sessionManager.iniciarSesion(user);
            return new ResultadoAutenticacion(true, "Autenticacion exitosa", user);

        } catch (Exception e) {
            System.err.println("Error en autenticacion: " + e.getMessage());
            return new ResultadoAutenticacion(false, "Error interno del sistema");
        }
    }

    /**
     * Cierra la sesion del usuario actual
     * @return true si se cerro exitosamente
     */
    public boolean cerrarSesion() {
        sessionManager.cerrarSesion();
        return true;
    }

    /**
     * Obtiene el usuario actualmente autenticado
     * @return Usuario actual o null si no hay sesion activa
     */
    public User getUsuarioActual() {
        return sessionManager.getUsuarioActual();
    }

    /**
     * Verifica si hay un usuario autenticado con sesion valida
     * @return true si hay usuario autenticado
     */
    public boolean estaAutenticado() {
        return sessionManager.esSesionValida();
    }

    /**
     * Verifica si el usuario actual tiene un rol especifico
     * @param rol Rol a verificar
     * @return true si el usuario tiene el rol especificado
     */
    public boolean tieneRol(String rol) {
        return sessionManager.tieneRol(rol);
    }

    /**
     * Verifica si el usuario actual es administrador
     * @return true si es administrador
     */
    public boolean esAdmin() {
        return sessionManager.esAdmin();
    }

    /**
     * Verifica si el usuario actual es medico
     * @return true si es medico
     */
    public boolean esMedico() {
        return sessionManager.esMedico();
    }

    /**
     * Verifica si el usuario actual es terapeuta
     * @return true si es terapeuta
     */
    public boolean esTerapeuta() {
        return sessionManager.esTerapeuta();
    }

    /**
     * Verifica si el usuario actual es personal de enfermeria
     * @return true si es enfermeria
     */
    public boolean esEnfermeria() {
        return sessionManager.esEnfermeria();
    }

    /**
     * Requiere que el usuario tenga un rol especifico
     * @param rol Rol requerido
     * @throws SecurityException si el usuario no tiene el rol
     */
    public void requireRol(String rol) throws SecurityException {
        if (!tieneRol(rol)) {
            User usuario = getUsuarioActual();
            String rolActual = usuario != null ? usuario.getRole() : "Sin autenticar";
            throw new SecurityException(String.format(
                "Acceso denegado. Se requiere rol '%s', usuario actual tiene rol '%s'", 
                rol, rolActual));
        }
    }

    /**
     * Requiere que el usuario sea administrador
     * @throws SecurityException si no es administrador
     */
    public void requireAdmin() throws SecurityException {
        requireRol("ADMIN");
    }

    /**
     * Requiere que el usuario sea medico
     * @throws SecurityException si no es medico
     */
    public void requireMedico() throws SecurityException {
        requireRol("MEDICO");
    }

    /**
     * Actualiza la actividad del usuario (para control de timeout)
     */
    public void actualizarActividad() {
        sessionManager.actualizarActividad();
    }

    /**
     * Obtiene informacion detallada de la sesion actual
     * @return String con informacion de la sesion
     */
    public String getInfoSesion() {
        return sessionManager.getInfoSesion();
    }

    /**
     * Genera hash SHA-256 con salt para una contraseña
     * @param password Contraseña en texto plano
     * @return Hash con salt en formato "salt:hash"
     */
    public String hashPassword(String password) {
        try {
            // Generar salt aleatorio
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[16];
            random.nextBytes(salt);
            
            // Generar hash con salt
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hash = md.digest(password.getBytes("UTF-8"));
            
            // Combinar salt y hash
            String saltStr = Base64.getEncoder().encodeToString(salt);
            String hashStr = Base64.getEncoder().encodeToString(hash);
            
            return saltStr + ":" + hashStr;
        } catch (Exception e) {
            throw new RuntimeException("Error al generar hash de contraseña", e);
        }
    }

    /**
     * Verifica una contraseña contra un hash con salt
     * @param password Contraseña en texto plano
     * @param hashedPassword Hash almacenado en formato "salt:hash"
     * @return true si la contraseña es correcta
     */
    private boolean verificarPasswordHash(String password, String hashedPassword) {
        try {
            String[] parts = hashedPassword.split(":");
            if (parts.length != 2) {
                return false;
            }
            
            byte[] salt = Base64.getDecoder().decode(parts[0]);
            byte[] storedHash = Base64.getDecoder().decode(parts[1]);
            
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] computedHash = md.digest(password.getBytes("UTF-8"));
            
            return MessageDigest.isEqual(storedHash, computedHash);
        } catch (Exception e) {
            System.err.println("Error al verificar hash de contraseña: " + e.getMessage());
            return false;
        }
    }

    /**
     * Verifica si una contraseña esta hasheada (contiene ":")
     * @param password Contraseña a verificar
     * @return true si esta hasheada
     */
    private boolean esPasswordHasheada(String password) {
        return password != null && password.contains(":");
    }

    /**
     * Desbloquea manualmente un usuario (solo para administradores)
     * @param username Usuario a desbloquear
     * @throws SecurityException si no es administrador
     */
    public void desbloquearUsuario(String username) throws SecurityException {
        requireAdmin();
        sessionManager.desbloquearUsuarioAdmin(username);
        System.out.println("Usuario desbloqueado por administrador: " + username);
    }

    /**
     * Obtiene estadisticas de intentos de login
     * @return String con estadisticas
     * @throws SecurityException si no es administrador
     */
    public String getEstadisticasLogin() throws SecurityException {
        requireAdmin();
        
        StringBuilder stats = new StringBuilder();
        stats.append("=== Estadisticas de Login ===\n");
        stats.append("Usuarios con intentos fallidos: ").append(sessionManager.getNumeroUsuariosConIntentosFallidos()).append("\n");
        stats.append("Usuarios bloqueados: ").append(sessionManager.getNumeroUsuariosBloqueados()).append("\n");
        
        if (sessionManager.getNumeroUsuariosConIntentosFallidos() > 0) {
            stats.append("\nIntentos fallidos por usuario:\n");
            sessionManager.getMapaIntentosFallidos().forEach((user, intentos) -> 
                stats.append("- ").append(user).append(": ").append(intentos).append(" intentos\n"));
        }
        
        return stats.toString();
    }

    /**
     * Clase interna para encapsular el resultado de la autenticacion
     */
    public static class ResultadoAutenticacion {
        private boolean exitoso;
        private String mensaje;
        private User usuario;

        public ResultadoAutenticacion(boolean exitoso, String mensaje) {
            this.exitoso = exitoso;
            this.mensaje = mensaje;
        }

        public ResultadoAutenticacion(boolean exitoso, String mensaje, User usuario) {
            this.exitoso = exitoso;
            this.mensaje = mensaje;
            this.usuario = usuario;
        }

        public boolean isExitoso() { return exitoso; }
        public String getMensaje() { return mensaje; }
        public User getUsuario() { return usuario; }
    }
}