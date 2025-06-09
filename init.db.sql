    -- Crear base de datos
    CREATE DATABASE IF NOT EXISTS sigcr_db;

    USE sigcr_db;

    -- Tabla de usuarios (para login y control de acceso)
    CREATE TABLE IF NOT EXISTS usuario (
        id INT AUTO_INCREMENT PRIMARY KEY,
        username VARCHAR(40) NOT NULL UNIQUE,
        password VARCHAR(64) NOT NULL,
        rol ENUM('ADMIN', 'MEDICO', 'TERAPEUTA', 'ENFERMERIA') NOT NULL
    );

    -- Tabla de pacientes (actualizada para CU-01 completo)
    CREATE TABLE IF NOT EXISTS paciente (
        id INT AUTO_INCREMENT PRIMARY KEY,
        nombre VARCHAR(80) NOT NULL,
        documento VARCHAR(20) NOT NULL UNIQUE,
        fecha_nacimiento DATE,
        diagnostico TEXT NOT NULL,
        habitacion VARCHAR(10),
        estado ENUM('Activo', 'Alta', 'Baja') DEFAULT 'Activo',
        fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        fecha_modificacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    );

    -- Tabla de sesiones terapéuticas
    CREATE TABLE IF NOT EXISTS sesion (
        id INT AUTO_INCREMENT PRIMARY KEY,
        paciente_id INT NOT NULL,
        terapeuta VARCHAR(80) NOT NULL,
        tipo_terapia VARCHAR(40) NOT NULL,
        fecha_hora DATETIME NOT NULL,
        duracion INT NOT NULL,
        observaciones TEXT,
        estado ENUM('Programada', 'Realizada', 'Cancelada') DEFAULT 'Programada',
        fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (paciente_id) REFERENCES paciente(id) ON DELETE CASCADE
    );

    -- Tabla de planes de tratamiento (para CU-02)
    CREATE TABLE IF NOT EXISTS plan_tratamiento (
        id INT AUTO_INCREMENT PRIMARY KEY,
        paciente_id INT NOT NULL,
        fecha_inicio DATE NOT NULL,
        fecha_fin DATE NOT NULL,
        estado ENUM('Activo', 'Completado', 'Suspendido') DEFAULT 'Activo',
        observaciones TEXT,
        fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        fecha_modificacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        FOREIGN KEY (paciente_id) REFERENCES paciente(id) ON DELETE CASCADE
    );

    -- Tabla de detalles del plan (horas semanales por tipo de terapia)
    CREATE TABLE IF NOT EXISTS plan_detalle (
        id INT AUTO_INCREMENT PRIMARY KEY,
        plan_id INT NOT NULL,
        tipo_terapia VARCHAR(50) NOT NULL,
        horas_semanales INT NOT NULL DEFAULT 0,
        FOREIGN KEY (plan_id) REFERENCES plan_tratamiento(id) ON DELETE CASCADE,
        UNIQUE KEY unique_plan_tipo (plan_id, tipo_terapia)
    );

    -- Tabla de notificaciones (actualizada para CU-04)
    CREATE TABLE IF NOT EXISTS notificacion (
        id INT AUTO_INCREMENT PRIMARY KEY,
        paciente_id INT,
        mensaje TEXT NOT NULL,
        fecha_hora DATETIME DEFAULT CURRENT_TIMESTAMP,
        leida BOOLEAN DEFAULT FALSE,
        tipo ENUM('PACIENTE_CREADO', 'PACIENTE_ACTUALIZADO', 'PACIENTE_BAJA', 'CRONOGRAMA_CAMBIO', 'PLAN_CREADO', 'PLAN_ACTUALIZADO', 'GENERAL') DEFAULT 'GENERAL',
        destinatario_rol ENUM('ADMIN', 'MEDICO', 'TERAPEUTA', 'ENFERMERIA', 'TODOS') DEFAULT 'TODOS',
        FOREIGN KEY (paciente_id) REFERENCES paciente(id) ON DELETE CASCADE
    );

    -- INSERTS INICIALES
    -- Usuarios (contraseñas en texto plano para facilitar testing - en producción deberían estar hasheadas)
    INSERT INTO
        usuario (username, password, rol)
    VALUES
        ('admin', 'admin123', 'ADMIN'),
        ('dr.juarez', 'medico123', 'MEDICO'),
        ('luz.terapeuta', 'terapia123', 'TERAPEUTA'),
        ('pablo.enfermero', 'enfermeria123', 'ENFERMERIA');

    -- Pacientes de ejemplo (con todos los campos requeridos)
    INSERT INTO
        paciente (
            nombre,
            documento,
            fecha_nacimiento,
            diagnostico,
            habitacion,
            estado
        )
    VALUES
        (
            'Maria Rodriguez',
            '12345678',
            '1980-06-10',
            'ACV isquémico con hemiparesia derecha. Requiere fisioterapia y terapia ocupacional intensiva.',
            '101',
            'Activo'
        ),
        (
            'Juan Perez',
            '23456789',
            '1975-09-15',
            'Lesión medular incompleta T12. Programa de rehabilitación integral.',
            '102',
            'Activo'
        ),
        (
            'Carmen Sánchez',
            '34567890',
            '1992-12-01',
            'Fractura de cadera con complicaciones. Requiere fortalecimiento muscular.',
            '201',
            'Activo'
        ),
        (
            'Roberto Martinez',
            '45678901',
            '1985-03-20',
            'Traumatismo craneoencefálico. Rehabilitación cognitiva y motora.',
            '203',
            'Alta'
        );

    -- Sesiones de ejemplo con fechas dinámicas
    INSERT INTO
        sesion (
            paciente_id,
            terapeuta,
            tipo_terapia,
            fecha_hora,
            duracion,
            observaciones,
            estado
        )
    VALUES
        (
            1,
            'luz.terapeuta',
            'Fisioterapia',
            CONCAT(CURDATE(), ' 09:00:00'),
            60,
            'Ejercicios de fortalecimiento miembro superior derecho',
            'Programada'
        ),
        (
            1,
            'luz.terapeuta',
            'Terapia Ocupacional',
            CONCAT(DATE_ADD(CURDATE(), INTERVAL 1 DAY), ' 11:00:00'),
            45,
            'Actividades de vida diaria, coordinación motora fina',
            'Programada'
        ),
        (
            2,
            'luz.terapeuta',
            'Psicología',
            CONCAT(CURDATE(), ' 14:00:00'),
            30,
            'Sesión de adaptación psicológica',
            'Programada'
        ),
        (
            3,
            'luz.terapeuta',
            'Fisioterapia',
            CONCAT(DATE_ADD(CURDATE(), INTERVAL 2 DAY), ' 09:00:00'),
            60,
            'Fortalecimiento de miembros inferiores',
            'Programada'
        ),
        (
            2,
            'pablo.enfermero',
            'Enfermería',
            CONCAT(CURDATE(), ' 16:00:00'),
            30,
            'Control de signos vitales y medicación',
            'Programada'
        ),
        (
            3,
            'pablo.enfermero',
            'Enfermería',
            CONCAT(DATE_ADD(CURDATE(), INTERVAL 1 DAY), ' 15:30:00'),
            30,
            'Curaciones y cuidados generales',
            'Programada'
        ),
        (
            1,
            'luz.terapeuta',
            'Hidroterapia',
            CONCAT(DATE_ADD(CURDATE(), INTERVAL 3 DAY), ' 10:00:00'),
            45,
            'Ejercicios en piscina terapéutica',
            'Programada'
        ),
        (
            2,
            'luz.terapeuta',
            'Terapia Ocupacional',
            CONCAT(DATE_SUB(CURDATE(), INTERVAL 1 DAY), ' 10:00:00'),
            45,
            'Sesión completada exitosamente',
            'Realizada'
        );

    -- Planes de tratamiento de ejemplo (para CU-02)
    INSERT INTO
        plan_tratamiento (paciente_id, fecha_inicio, fecha_fin, estado, observaciones)
    VALUES
        (
            1,
            '2025-01-01',
            '2025-04-01',
            'Activo',
            'Plan integral para recuperación post-ACV. Enfoque en movilidad y autonomía.'
        ),
        (
            2,
            '2025-01-15',
            '2025-07-15',
            'Activo',
            'Rehabilitación de lesión medular. Trabajo intensivo de fortalecimiento.'
        ),
        (
            3,
            '2025-02-01',
            '2025-05-01',
            'Activo',
            'Recuperación post-fractura de cadera. Progresión gradual de carga.'
        );

    -- Detalles de planes (horas semanales por tipo de terapia)
    INSERT INTO
        plan_detalle (plan_id, tipo_terapia, horas_semanales)
    VALUES
        -- Plan de Maria Rodriguez (ACV)
        (1, 'Fisioterapia', 4),
        (1, 'Terapia Ocupacional', 3),
        (1, 'Psicología', 2),
        
        -- Plan de Juan Perez (Lesión medular)
        (2, 'Fisioterapia', 5),
        (2, 'Terapia Ocupacional', 3),
        (2, 'Psicología', 2),
        (2, 'Hidroterapia', 2),
        
        -- Plan de Carmen Sánchez (Fractura cadera)
        (3, 'Fisioterapia', 3),
        (3, 'Terapia Ocupacional', 2);

    -- Notificaciones de ejemplo (para demostrar CU-04)
    INSERT INTO
        notificacion (paciente_id, mensaje, tipo, destinatario_rol, leida)
    VALUES
        (
            1,
            'Nuevo paciente Maria Rodriguez registrado. Requiere asignación de plan terapéutico.',
            'PACIENTE_CREADO',
            'MEDICO',
            FALSE
        ),
        (
            1,
            'Plan de tratamiento creado para Maria Rodriguez. Total: 9 horas semanales. Tipos: Fisioterapia, Terapia Ocupacional, Psicología',
            'PLAN_CREADO',
            'TERAPEUTA',
            FALSE
        ),
        (
            2,
            'El Dr. Juarez modificó el cronograma de Juan Perez para la próxima semana.',
            'CRONOGRAMA_CAMBIO',
            'TERAPEUTA',
            FALSE
        ),
        (
            3,
            'Paciente Carmen Sánchez: Diagnóstico actualizado. Revisar plan de tratamiento.',
            'PACIENTE_ACTUALIZADO',
            'MEDICO',
            TRUE
        ),
        (
            4,
            'Paciente Roberto Martinez dado de alta. Cancelar sesiones pendientes.',
            'PACIENTE_BAJA',
            'TODOS',
            FALSE
        );

    -- Crear índices para mejorar rendimiento
    CREATE INDEX idx_paciente_documento ON paciente(documento);
    CREATE INDEX idx_paciente_estado ON paciente(estado);
    CREATE INDEX idx_sesion_fecha ON sesion(fecha_hora);
    CREATE INDEX idx_sesion_terapeuta ON sesion(terapeuta);
    CREATE INDEX idx_sesion_paciente_fecha ON sesion(paciente_id, fecha_hora);
    CREATE INDEX idx_plan_paciente_estado ON plan_tratamiento(paciente_id, estado);
    CREATE INDEX idx_plan_detalle_plan ON plan_detalle(plan_id);
    CREATE INDEX idx_notificacion_fecha ON notificacion(fecha_hora);
    CREATE INDEX idx_notificacion_leida ON notificacion(leida);
    CREATE INDEX idx_notificacion_tipo ON notificacion(tipo);

    -- Consultas de verificación para testing
    -- SELECT 'Pacientes activos:', COUNT(*) FROM paciente WHERE estado = 'Activo';
    -- SELECT 'Sesiones programadas:', COUNT(*) FROM sesion WHERE estado = 'Programada';
    -- SELECT 'Planes activos:', COUNT(*) FROM plan_tratamiento WHERE estado = 'Activo';
    -- SELECT 'Notificaciones sin leer:', COUNT(*) FROM notificacion WHERE leida = FALSE;

    -- Consulta para verificar planes completos con detalles
    -- SELECT pt.id, p.nombre, pt.estado, SUM(pd.horas_semanales) as total_horas
    -- FROM plan_tratamiento pt 
    -- JOIN paciente p ON pt.paciente_id = p.id 
    -- JOIN plan_detalle pd ON pt.id = pd.plan_id 
    -- WHERE pt.estado = 'Activo' 
    -- GROUP BY pt.id, p.nombre, pt.estado;