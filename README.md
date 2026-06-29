# NAP Backend

API REST **stateless** de autenticación y autorización (RBAC de grano fino) construida con
**Spring Boot 4.1** y **Java 25**. Expone el registro, el inicio de sesión con JWT firmados con RSA y
la gestión de usuarios, roles y permisos. Está pensada para acompañar a un frontend Angular
(`nap-frontend`).

## Características

- **Autenticación JWT stateless** con firma asimétrica RSA (clave privada para firmar, pública para
  validar).
- **Refresh tokens opacos con rotación**: se guardan hasheados (SHA-256) en BD y viajan en una cookie
  `HttpOnly`. Detección de reuso que revoca todas las sesiones ante un posible robo.
- **Logout inmediato** mediante una denylist de access tokens en memoria, pese a ser stateless.
- **RBAC de grano fino** basado en permisos `recurso:acción` (no en nombres de rol), con un catálogo
  de permisos que es la única fuente de verdad en código.
- **Documentación de la API generada desde los tests** con Spring REST Docs.
- **Errores estandarizados** según RFC 7807 (`application/problem+json`).

## Stack

| Área            | Tecnología                                            |
| --------------- | ----------------------------------------------------- |
| Lenguaje        | Java 25                                               |
| Framework       | Spring Boot 4.1 (Web MVC, Security, Data JPA, Validation, Actuator) |
| Seguridad       | OAuth2 Resource Server (JWT), BCrypt                  |
| Base de datos   | PostgreSQL 18                                         |
| Build           | Maven (wrapper incluido)                              |
| Formato         | Spotless + Google Java Format                         |
| Docs            | Spring REST Docs + Asciidoctor                        |
| Cobertura       | JaCoCo                                                |
| Despliegue      | Docker · GitHub Actions · Render                      |

## Requisitos

- **JDK 25**
- **Docker** y **Docker Compose** (forma recomendada de levantar el entorno), o un **PostgreSQL 18**
  propio.

No es necesario instalar Maven: usa el wrapper (`./mvnw`).

## Puesta en marcha

### 1. Variables de entorno

Copia la plantilla y ajusta los valores. En desarrollo se usa `.env.dev`; producción se configura en
el dashboard de Render (ver [CI/CD](#cicd)):

```bash
cp .env.example .env.dev
```

| Variable                    | Por defecto              | Descripción                                              |
| --------------------------- | ------------------------ | -------------------------------------------------------- |
| `SPRING_DATASOURCE_PASSWORD`| —                        | Contraseña de PostgreSQL (obligatoria).                  |
| `APP_CORS_ALLOWED_ORIGINS`  | `http://localhost:4200`  | Orígenes CORS permitidos (lista separada por comas).     |
| `JWT_ACCESS_EXP_MIN`        | `15`                     | Vigencia del access token, en minutos.                   |
| `JWT_REFRESH_EXP_DAYS`      | `7`                      | Vigencia del refresh token, en días.                     |
| `JWT_COOKIE_SECURE`         | `false`                  | `true` en producción (HTTPS).                            |
| `JWT_COOKIE_SAMESITE`       | `Lax`                    | `Lax` \| `Strict` \| `None` (usa `None` + `secure` si front y API están en dominios distintos). |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `update`             | Estrategia de esquema. Dev usa `create-drop` (ya en `.env.dev`).   |
| `SPRING_SQL_INIT_MODE`      | `never`                  | Ejecuta `data.sql`. Dev usa `always` (ya en `.env.dev`). |

### 2a. Con Docker Compose (recomendado)

Levanta PostgreSQL y la aplicación juntos (solo hay un Compose, para desarrollo):

```bash
docker compose -f docker-compose.dev.yml --env-file .env.dev up --build
```

La API queda disponible en `http://localhost:8080`.

### 2b. Solo la base de datos + app local

Levanta únicamente Postgres con Docker y ejecuta la app con el wrapper. Como `./mvnw spring-boot:run`
no lee `.env.dev`, carga primero esas variables en el entorno:

```bash
docker compose -f docker-compose.dev.yml --env-file .env.dev up -d db
set -a && source .env.dev && set +a   # exporta las variables de dev al entorno
./mvnw spring-boot:run
```

> El default del código es seguro (`ddl-auto=update`, sin seed). `.env.dev` lo cambia a `create-drop`
> y `sql.init.mode=always`, de modo que en desarrollo la BD se recrea y se siembra `data.sql` en cada
> arranque. Sin esas variables (p. ej. `./mvnw spring-boot:run` a secas), la app no reinicia ni
> siembra la BD.

### Usuarios de prueba (sembrados por `data.sql`)

| Usuario   | Contraseña | Rol     |
| --------- | ---------- | ------- |
| `admin`   | `admin`    | `ADMIN` |
| `user01`  | `user01`   | `USER`  |

## API

URL base: `http://localhost:8080`. Formato JSON. Versionado mediante el header `X-API-Version`
(por defecto `1.0`).

| Método y ruta                       | Permiso requerido            | Descripción                                  |
| ----------------------------------- | ---------------------------- | -------------------------------------------- |
| `POST /api/auth/sign-up`            | público                      | Registra un usuario (no emite tokens).       |
| `POST /api/auth/log-in`             | público                      | Inicia sesión: access token + cookie refresh.|
| `POST /api/auth/refresh`            | público (cookie)             | Rota el refresh y emite un nuevo access token.|
| `POST /api/auth/logout`             | autenticado                  | Revoca tokens y borra la cookie.             |
| `GET /api/users`                    | `user:read`                  | Lista usuarios.                              |
| `GET /api/users/{id}`               | `user:read`                  | Obtiene un usuario.                          |
| `PUT /api/users/{id}`               | `user:update` o ser el dueño | Actualiza un usuario.                        |
| `DELETE /api/users/{id}`            | `user:delete`                | Elimina un usuario.                          |
| `GET /api/roles` · `/{id}` · `/search` | `role:read`               | Consulta roles.                              |
| `POST /api/roles`                   | `role:create`                | Crea un rol con permisos del catálogo.       |
| `PUT /api/roles/{id}`               | `role:update`                | Actualiza nombre y descripción del rol.      |
| `PUT /api/roles/{id}/permissions`   | `role:update`                | Reemplaza los permisos del rol.              |
| `DELETE /api/roles/{id}`            | `role:delete`                | Elimina un rol.                              |
| `GET /api/permissions` · `/{id}` · `/search` | `permission:read`   | Catálogo de permisos (solo lectura).         |

Documentación completa y verificada (generada desde los tests): **`http://localhost:8080/docs/`**.
Estado de salud: **`http://localhost:8080/actuator/health`**.

### Modelo de autenticación

- El **access token** (JWT) se devuelve en el cuerpo (`accessToken`) y debe enviarse en las
  peticiones protegidas con el header `Authorization: Bearer <token>`. Incluye en el claim
  `authorities` la unión de los permisos de todos los roles del usuario.
- El **refresh token** viaja en una cookie `HttpOnly` `refresh_token` (path `/api/auth`), se rota en
  cada `refresh` y se revoca en el logout.

### Modelo de autorización (RBAC)

El control de acceso se basa en **permisos finos** (`recurso:acción`), no en nombres de rol:

- Los **permisos** son propiedad de la API (catálogo en código, `PermissionCatalog`) y de solo
  lectura por HTTP.
- Los **roles** son agrupaciones dinámicas de permisos con CRUD completo; un rol nuevo protege sus
  endpoints de inmediato, sin recompilar.

## Desarrollo

```bash
./mvnw spotless:apply   # Formatea el código (OBLIGATORIO antes de commitear).
./mvnw verify           # Compila, ejecuta tests y empaqueta (genera docs + cobertura).
./mvnw test             # Ejecuta los tests.
./mvnw test -Dtest=RoleServiceImplTest          # Una clase de test.
./mvnw test -Dtest=RoleServiceImplTest#create   # Un método de test.
```

El informe de cobertura de JaCoCo queda en `target/site/jacoco/index.html`.

> Consulta [CLAUDE.md](CLAUDE.md) y [HELP.md](HELP.md) para más detalle de la arquitectura y la
> referencia técnica.

## Estructura del proyecto

Organización por feature bajo `com.ns.nap_backend`, cada una con
`entity / repository / service (+impl) / controller / dto / exception`:

```text
src/main/java/com/ns/nap_backend/
├── auth/          Registro, login, refresh, logout y servicios de token (JWT + refresh).
├── user/          Usuarios (herencia JOINED Person → User) y regla de acceso "self".
├── role/          Roles y su CRUD.
├── permission/    Catálogo de permisos (solo lectura).
├── config/        Seguridad (JWT, CORS, RBAC seeder) y configuración web.
└── common/        Utilidades, grupos de validación y manejador de errores RFC 7807.
```

## CI/CD

- **CI** (`.github/workflows/ci.yml`): en cada push/PR a `main` verifica el formato
  (`spotless:check`) y ejecuta `mvnw verify`.
- **CD** (`.github/workflows/cd.yml`): tras un CI exitoso en `main` (o disparo manual), lanza un
  deploy en **Render** vía API y espera a que quede `live`.

## Seguridad

> Las claves RSA de ejemplo en `src/main/resources/certs/` y las contraseñas por defecto son **solo
> para desarrollo**. En producción usa claves y credenciales propias mediante variables de entorno y
> secretos, y activa `JWT_COOKIE_SECURE=true` sobre HTTPS.
