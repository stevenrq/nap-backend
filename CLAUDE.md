# CLAUDE.md

Este archivo guía a Claude Code (claude.ai/code) al trabajar con el código de este repositorio.

## Visión general

`nap-backend` es una API REST stateless en Spring Boot 4.1 / Java 25 que ofrece autenticación y un
servicio de autorización RBAC de grano fino. Se acompaña de un frontend Angular (`nap-frontend`, un
directorio de trabajo hermano). Los comentarios y el Javadoc están escritos en **español** —
mantén ese idioma al editar.

## Comandos

```bash
./mvnw spotless:apply                      # Formatea (Google Java Format). OBLIGATORIO antes de commitear.
./mvnw spotless:check                       # Lo que ejecuta el CI; falla el build si hay código sin formatear.
./mvnw verify                               # Compila + testea + empaqueta (también REST Docs + JaCoCo).
./mvnw test                                 # Ejecuta todos los tests.
./mvnw test -Dtest=RoleServiceImplTest      # Una sola clase de test.
./mvnw test -Dtest=RoleServiceImplTest#create  # Un solo método de test.
docker compose up --build                                                  # Dev local: Postgres + app.
```

Requiere JDK 25. La app necesita un Postgres en marcha (los tests de slice `@WebMvcTest` no). Copia
`.env.example` → `.env.dev` (dev) o `.env` (prod) y define `SPRING_DATASOURCE_PASSWORD`. **El default
de `application.yaml` es seguro para prod** (`ddl-auto=update`, `sql.init.mode=never`): no destruye el
esquema ni siembra datos. **Desarrollo lo anula** vía `.env.dev` con
`SPRING_JPA_HIBERNATE_DDL_AUTO=create-drop` y `SPRING_SQL_INIT_MODE=always` para tener esquema efímero
y sembrar `src/main/resources/data.sql` en cada arranque (usuarios de prueba `admin`/`admin` y
`user01`/`user01`; en prod, donde `data.sql` no corre, el usuario admin lo crea `AdminUserSeeder` —
ver RBAC). Spring enlaza esas variables por su nombre canónico (relaxed binding), sin
`${...}` en el YAML.

Solo hay un archivo Compose, `docker-compose.yml` (Postgres local + app, esquema efímero con
seeds de prueba), pensado para correr la app en tu máquina. **Producción es Render**, que no usa
docker-compose: su config (incluidos los overrides peligrosos) vive en el dashboard de Render — ver
la sección CI/CD. El endpoint `GET /actuator/health` es público (lo usa el healthcheck del contenedor
de dev y el health check de Render).

## Arquitectura

Organización por feature bajo `com.ns.nap_backend`: `auth`, `user`, `role`, `permission`, cada uno
dividido en `entity / repository / service (+impl) / controller / dto / exception`. El código
transversal vive en `config` (seguridad) y `common` (`util`, `validation`, `exception`).

### RBAC: el diseño central

La autorización se basa en **permisos de grano fino `recurso:acción`** (p. ej. `role:read`,
`user:delete`), nunca en nombres de rol.

- **`config/security/PermissionCatalog`** (enum) es la única fuente de verdad de los permisos. La
  tabla `permissions` de la BD es solo una proyección de él. **`config/RbacSeeder`** (un
  `ApplicationRunner`) reconcilia la tabla a partir del enum en cada arranque, de forma idempotente,
  y garantiza los roles bootstrap `ADMIN` (todos los permisos) y `USER` (`user:read`). Esto funciona
  igual en dev (`create-drop`) que con una BD persistente en prod — los permisos nunca salen de
  `data.sql`.
- **`config/AdminUserSeeder`** (`ApplicationRunner`, `@Order(2)`, corre tras `RbacSeeder`) garantiza
  el usuario administrador en cada arranque, de forma idempotente (no hace nada si ya existe). Toma
  las credenciales de `ADMIN_USERNAME` / `ADMIN_PASSWORD` / `ADMIN_EMAIL` (por defecto `admin`/`admin`).
  Es lo que crea el admin en prod, donde `data.sql` no se ejecuta; en dev `data.sql` ya lo siembra y
  el seeder lo respeta sin duplicarlo.
- **Los permisos son propiedad de la API y de solo lectura** por HTTP (solo `GET /api/permissions`).
  Los **roles** son agrupaciones dinámicas de permisos del catálogo con CRUD completo (`/api/roles`);
  un rol nuevo protege sus endpoints de inmediato, sin recompilar.
- Los endpoints se protegen con method security (`@EnableMethodSecurity`):
  `@PreAuthorize("hasAuthority('role:read')")`. El acceso a la propia cuenta usa
  `@PreAuthorize("... or @userSecurity.isSelf(#id, authentication)")`, de modo que un usuario puede
  editar su propia cuenta sin un permiso administrativo (los roles enviados en esa petición se
  ignoran — no hay auto-escalada).
- **Cuidado — `PreAuthorizeCatalogTest`:** este test de guardia escanea cada cadena
  `hasAuthority('...')` de los controladores y verifica que exista en `PermissionCatalog`. Si añades
  una authority en un `@PreAuthorize` sin añadirla al enum, el build falla. Añadir/quitar un permiso
  = un cambio de código en ambos sitios.

### Autenticación JWT (stateless, RSA)

- **Access token:** JWT firmado con RSA (claves en `src/main/resources/certs/*.pem`, cargadas vía
  `RsaKeyProperties`). Se devuelve en el cuerpo como `accessToken` y se reenvía como
  `Authorization: Bearer`. Lleva un claim `authorities` = la unión de los permisos de todos los roles
  del usuario, más un `jti`. Vigencia por defecto 15 min. Lo construye `JwtTokenGenerator`;
  `JwtConfig` conecta el encoder (clave privada) / decoder (clave pública) y mapea el claim
  `authorities` a `GrantedAuthority` sin prefijo.
- **Refresh token:** cadena aleatoria opaca de alta entropía; en BD solo se guarda su hash SHA-256.
  Viaja en una cookie `HttpOnly` `refresh_token` (path `/api/auth`), rotada en cada
  `/api/auth/refresh`. Vigencia por defecto 7 días. Reutilizar un token ya rotado se trata como robo:
  `RefreshTokenReuseHandler` revoca **todas** las sesiones del usuario en una transacción aparte
  (`RefreshTokenService`).
- **Logout / revocación inmediata:** `TokenDenylistService` es una denylist en memoria
  (`ConcurrentHashMap`) de los `jti` de access tokens, consultada por `DenylistJwtValidator` en cada
  petición. Es **solo de un nodo** — un despliegue multi-instancia necesita un store compartido
  (Redis).
- El registro (`/api/auth/sign-up`) crea la cuenta pero **no emite tokens**; el usuario debe iniciar
  sesión después.
- `/api/auth/**`, `/docs/**` y `/actuator/health` son públicos; todo lo demás requiere un access
  token válido. Los orígenes CORS vienen de `app.cors.allowed-origins` (por defecto
  `http://localhost:4200`).

### Entidades

Herencia JPA `JOINED`: `Person` abstracta (tabla `persons`) ← `User` (`users`, vía
`@PrimaryKeyJoinColumn`). Many-to-many, ambas `EAGER`: `User`↔`Role` (`users_roles`) y
`Role`↔`Permission` (`roles_permissions`). Lombok `@Data` en todo; OSIV está desactivado.

### Documentación de la API con Spring REST Docs (dirigida por tests)

La guía de la API se **genera a partir de los tests**, así que la doc no puede divergir del
comportamiento real. Las clases `*DocsTest` son slices `@WebMvcTest` que usan MockMvc con
`.with(jwt().authorities(...))` para simular authorities y llaman a `document(...)` para emitir
snippets. `src/docs/asciidoc/index.adoc` incluye esos snippets; el plugin asciidoctor genera el HTML
en la fase `package`, lo copia a `static/docs` y se sirve en `/docs/**`. El andamiaje de test
compartido está en `src/test/.../support/` (`RestDocsConfig`, `SecurityDocsTestConfig`,
`TestEntities`).

## Convenciones

- Comentarios/Javadoc en español; Google Java Format (impuesto por Spotless en el CI).
- Los errores son RFC 7807 `application/problem+json` vía `common/exception/ApiExceptionHandler`.
- Los controladores usan el versionado de API de Spring:
  `@RequestMapping(path = "...", version = "1.0")` (se envía como el header `X-API-Version`, por
  defecto `1.0`).
- La cobertura de JaCoCo excluye `dto`, `entity`, `config` y la clase principal de la aplicación.

## CI/CD

- `.github/workflows/ci.yml` (push/PR a `main`): `spotless:check` y luego `mvnw verify`.
- `.github/workflows/cd.yml`: tras un CI exitoso en `main` (o disparo manual), lanza un deploy en
  Render vía API y hace polling hasta que esté `live`. Render construye la imagen desde el
  `Dockerfile` (no usa docker-compose).
- Toda la config de producción se define en el **dashboard de Render** (Settings → Environment), no
  en el repo. La plantilla de referencia es `.env.example` / `.env`. Variables a definir:
  - `JWT_COOKIE_SECURE=true` y `JWT_COOKIE_SAMESITE=None` (front en Vercel y API en Render = cross-site)
  - `APP_CORS_ALLOWED_ORIGINS` = dominio(s) del frontend en Vercel
  - secretos de la BD gestionada (Neon): `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD`
  - credenciales del admin inicial: `ADMIN_USERNAME` / `ADMIN_PASSWORD` / `ADMIN_EMAIL` — **define una
    contraseña propia**; el default es `admin`/`admin` (lo crea `AdminUserSeeder`, ver RBAC)
  - `SPRING_JPA_HIBERNATE_DDL_AUTO` / `SPRING_SQL_INIT_MODE`: **opcionales** — el default del código
    ya es seguro (`update` / `never`); déjalas sin definir o explícitas, pero **no** las pongas en
    `create-drop` / `always` en prod.
- **Claves RSA para JWT** — los archivos `src/main/resources/certs/*.pem` están en `.gitignore` y
  **no** se incluyen en la imagen. En Render hay que proveerlos como _Secret Files_ y apuntar las
  propiedades a ellos vía variables de entorno:
  1. En **Settings → Secret Files** sube los dos archivos:
     - Filename: `private.pem` → contenido de `certs/private.pem`
     - Filename: `public.pem` → contenido de `certs/public.pem`
     (Render los monta en `/etc/secrets/<filename>` automáticamente)
  2. En **Settings → Environment** agrega:

     ```text
     APP_RSA_PRIVATE_KEY=file:/etc/secrets/private.pem
     APP_RSA_PUBLIC_KEY=file:/etc/secrets/public.pem
     ```

  Spring Boot mapea `APP_RSA_PRIVATE_KEY` → `app.rsa.private-key` (relaxed binding); el prefijo
  `file:` indica ruta de sistema de archivos en lugar de classpath. Si faltan estas entradas, la
  app arranca con `FileNotFoundException` y falla el deploy.
