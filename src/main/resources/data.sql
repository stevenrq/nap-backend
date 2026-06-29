--
--
--
-- Datos iniciales cargados al arrancar la aplicación (ddl-auto=create-drop).
-- Se ejecuta después de que Hibernate cree el esquema gracias a
-- spring.jpa.defer-datasource-initialization=true y spring.sql.init.mode=always.
--
--
--
-- Roles
INSERT INTO roles(id, name, description, created_at, updated_at)
    VALUES (1, 'ADMIN', 'Rol administrador con todos los permisos del catálogo', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO roles(id, name, description, created_at, updated_at)
    VALUES (2, 'USER', 'Rol usuario con permiso de lectura de usuarios', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Sincronizar la secuencia de roles con los IDs insertados explícitamente
SELECT
    setval('roles_seq',(
            SELECT
                MAX(id)
            FROM roles));

-- Usuarios de prueba
-- Contraseñas (BCrypt): admin -> "admin" | user01 -> "user01"
--
-- Direcciones
INSERT INTO addresses(street, number, city)
    VALUES ('Carrera 10', '123', 'Bogotá');

INSERT INTO addresses(street, number, city)
    VALUES ('Calle 50', '456', 'Medellín');

-- Personas (tabla base de la herencia JOINED; usa la secuencia de Hibernate)
INSERT INTO persons(id, national_id, first_name, last_name, email, phone_number, address_id, created_at, updated_at)
    VALUES (nextval('persons_seq'), 1010101010, 'Admin', 'Sistema', 'admin@sistema.nap.com', 3001234567, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO persons(id, national_id, first_name, last_name, email, phone_number, address_id, created_at, updated_at)
    VALUES (nextval('persons_seq'), 2020202020, 'Usuario', 'Sistema', 'usuario@sistema.nap.com', 3009876543, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Usuarios
INSERT INTO users(person_id, username, password, enabled, account_non_expired, account_non_locked, credentials_non_expired)
SELECT
    id,
    'admin',
    '$2a$12$6fPsAZabPnIqPO5d6SLk6efi3PNYKvBWtoyvxl5x05RA8ZMPIfBXy',
    TRUE,
    TRUE,
    TRUE,
    TRUE
FROM
    persons
WHERE
    national_id = 1010101010;

INSERT INTO users(person_id, username, password, enabled, account_non_expired, account_non_locked, credentials_non_expired)
SELECT
    id,
    'user01',
    '$2a$12$4Iqd9PFBu9kZ6I.DqHWiGOencYERWgnw9dIpVjxC.A32TDU9IpdBa',
    TRUE,
    TRUE,
    TRUE,
    TRUE
FROM
    persons
WHERE
    national_id = 2020202020;

-- Asignación de roles
INSERT INTO users_roles(user_id, role_id)
SELECT
    id,
    1
FROM
    persons
WHERE
    national_id = 1010101010;

INSERT INTO users_roles(user_id, role_id)
SELECT
    id,
    2
FROM
    persons
WHERE
    national_id = 2020202020;

