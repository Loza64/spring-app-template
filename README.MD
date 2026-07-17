# Documentación Técnica — App (Spring Boot Backend)

## 1. Resumen del proyecto

API REST desarrollada en **Spring Boot 3.5.8 / Java 21**, con autenticación JWT, autorización dinámica basada en permisos (RBAC), soft delete y documentación OpenAPI/Swagger. Persiste en **PostgreSQL** vía JPA/Hibernate.

**Paquete base:** `com.server.app`

---

## 2. Stack tecnológico

| Categoría | Tecnología |
|---|---|
| Lenguaje / Runtime | Java 21 |
| Framework | Spring Boot 3.5.8 (starter-parent) |
| Persistencia | Spring Data JPA + Hibernate + PostgreSQL Driver |
| Seguridad | Spring Security (stateless) + JJWT 0.12.6 |
| Validación | spring-boot-starter-validation (Jakarta Validation) |
| Mapeo Entity↔DTO | MapStruct 1.6.3 (+ lombok-mapstruct-binding) |
| Documentación API | springdoc-openapi-starter-webmvc-ui 2.8.5 (Swagger UI) |
| Utilidades | Lombok, jackson-datatype-jsr310, spring-dotenv |
| Tests | spring-boot-starter-test, spring-security-test, H2 (in-memory) |
| Build | Maven (`mvnw`) |
| Contenedor | Docker multi-stage (Maven build → JRE Alpine runtime) |

`spring-dotenv` permite que Spring lea variables directamente del archivo `.env` incluso fuera de Docker (útil al correr con `./mvnw spring-boot:run` localmente).

---

## 3. Arquitectura y estructura de paquetes

```
com.server.app
├── AppApplication.java            # Entry point (@SpringBootApplication)
├── common/
│   ├── constants/RoleNames.java   # SUPER_ADMIN, ADMIN, CLIENT
│   ├── exceptions/                # BadRequest, Conflict, Forbidden, NotFound, Unauthorized, Server
│   └── pagination/                # PaginationResponse<T>, PaginationMapper
├── config/
│   ├── SecurityConfig.java        # SecurityFilterChain (stateless, CORS, filtros JWT/RBAC)
│   ├── SecurityRules.java         # Rutas públicas / solo-autenticado / ignoradas
│   ├── JsonWebTokenProvider.java  # Emisión y verificación de JWT (HS-SHA, claim "id")
│   ├── CorsConfig.java            # CORS_ALLOWED_ORIGINS
│   ├── WebConfig.java             # ObjectMapper, BCryptPasswordEncoder
│   ├── PageableConfig.java        # Paginación 1-indexada
│   └── OpenApiConfig.java         # Swagger + esquema Bearer
├── controller/                    # Auth, User, Role, Permission, Advice (manejo global de errores)
├── domain/
│   ├── dto/                       # DTOs por módulo (auth, user, role, permission, base)
│   ├── mapper/                    # MapStruct: AuthMapper, UserMapper, RoleMapper, PermissionMapper
│   └── model/                     # User, Role, Permission (+ BaseEntity)
├── filter/
│   ├── JwtAuthenticationFilter.java     # Valida Bearer token, carga usuario, setea Authorities
│   └── DynamicAuthorizationFilter.java  # Autoriza por permisos dinámicos (method:path)
├── repository/                    # JPA repos + Specifications (User, Role)
├── seeders/
│   ├── permission/PermissionSeeder.java  # Escanea endpoints reales y crea permisos faltantes
│   └── role/RoleSeeder.java              # Crea roles base al arrancar
└── service/                       # AuthService, UserServiceImpl, RoleServiceImpl, PermissionService
```

### Modelo de datos (entidades)

- **BaseEntity** (`@MappedSuperclass`): `createdAt`, `updatedAt` (auto en `@PreUpdate`), `deletedAt` (soft delete).
- **User**: `id, username (unique), name, surname, email (unique), password, blocked, role (ManyToOne → Role)`.
- **Role**: `id, name (unique), active, permissions (ManyToMany → Permission vía tabla role_permissions)`.
- **Permission**: `id, path, method, title`, único por `(path, method)`.

### Flujo de seguridad

1. **`JwtAuthenticationFilter`**: rutas públicas (`SecurityRules.PUBLIC`, ej. `POST /api/auth/login`, `POST /api/auth/signup`) e ignoradas (Swagger, `/error`) no requieren token. El resto exige header `Authorization: Bearer <token>`. Extrae el `id` del claim, busca el usuario, valida que no esté `blocked` y que su rol esté `active`, y construye las `GrantedAuthority` como `"METHOD:/path"` (uno por cada permiso del rol) más `ROLE_<nombre>`.
2. **`DynamicAuthorizationFilter`**: para rutas que no son públicas ni "solo-autenticado" (`SecurityRules.AUTH_ONLY`), verifica que el usuario autenticado tenga una authority cuyo método y patrón (`AntPathMatcher`) coincidan con la ruta solicitada; si no, responde `403`.
3. **`PermissionSeeder`** (al arrancar la app, `ApplicationReadyEvent`): inspecciona todos los `@RequestMapping` reales registrados en Spring MVC y crea en BD los permisos (`method:path`) que no sean públicos/ignorados/auth-only y aún no existan — así los permisos siempre reflejan los endpoints reales del código.
4. **`RoleSeeder`**: crea los roles `SUPER_ADMIN`, `ADMIN`, `CLIENT` si no existen.
5. `script.sql` es un script manual de ejemplo para asignar todos los permisos a `SUPER_ADMIN` y los `GET` a `ADMIN`.

### Endpoints principales

| Método | Ruta | Auth | Descripción |
|---|---|---|---|
| POST | `/api/auth/login` | Pública | Login, retorna `{token, data: UserResponseDto}` |
| POST | `/api/auth/signup` | Pública | Registro |
| GET | `/api/auth/profile` | Solo autenticado | Perfil del usuario actual |
| PUT | `/api/auth/profile` | Solo autenticado | Actualizar perfil |
| PATCH | `/api/auth/password` | Solo autenticado | Cambiar contraseña |
| GET/POST | `/api/users` | Por permiso | Listado paginado (`search`, `role`, `deleted`) / creación |
| GET/PUT/DELETE | `/api/users/{id}` | Por permiso | CRUD + soft delete |
| PATCH | `/api/users/{id}/restore` | Por permiso | Restaurar soft-deleted |
| GET/POST/PUT/DELETE | `/api/roles` (y `/{id}`) | Por permiso | CRUD de roles + restore |
| GET/PUT | `/api/permissions` (y `/{id}`) | Por permiso | Listado y edición de permisos (títulos) |

Paginación: `PaginationResponse<T>` con `{ data, pagination: { page, pageSize, pageCount, total } }`, páginas 1-indexadas (`PageableConfig`).

### Documentación OpenAPI

- Swagger UI: `swagger-ui.html` → `/swagger-ui/index.html`
- JSON OpenAPI: `/v3/api-docs`
- Autenticación en Swagger vía esquema Bearer (`OpenApiConfig`).

---

## 4. Configuración vía variables de entorno

Archivo `.env` (no versionado; existe `.env.example` como plantilla) inyectado en `application.properties`:

```properties
server.port=${SERVER_PORT}
spring.application.name=${SPRING_APPLICATION_NAME}

spring.datasource.url=${SPRING_DATASOURCE_URL}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD}
spring.datasource.driver-class-name=${SPRING_DATASOURCE_DRIVER_CLASS_NAME}

spring.jpa.properties.hibernate.dialect=${SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT}
spring.jpa.hibernate.ddl-auto=${SPRING_JPA_HIBERNATE_DDL_AUTO}
spring.jpa.show-sql=${SPRING_JPA_SHOW_SQL}
spring.jpa.open-in-view=false

spring.cors.allowed.origins=${CORS_ALLOWED_ORIGINS}

security.jwt.secret-key=${SECURITY_JWT_SECRET_KEY}
security.jwt.expiration-time=${SECURITY_JWT_EXPIRATION_TIME}
```

| Variable | Ejemplo local (`.env`) | Notas |
|---|---|---|
| `SERVER_PORT` | `3000` | Puerto expuesto por la app y mapeado en `docker-compose.yml` |
| `SPRING_APPLICATION_NAME` | `app` | |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/spring-app` | **Cambia según el entorno** (ver sección 5) |
| `SPRING_DATASOURCE_USERNAME` | `postgres` | |
| `SPRING_DATASOURCE_PASSWORD` | `-change_me` | |
| `SPRING_DATASOURCE_DRIVER_CLASS_NAME` | `org.postgresql.Driver` | |
| `SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT` | `org.hibernate.dialect.PostgreSQLDialect` | |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `update` | Hibernate crea/actualiza el esquema automáticamente |
| `SPRING_JPA_SHOW_SQL` | `false` | |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Frontend permitido |
| `SECURITY_JWT_SECRET_KEY` | (Base64, 512 bits) | **Regenerar por entorno**, nunca reusar la de ejemplo en producción |
| `SECURITY_JWT_EXPIRATION_TIME` | `86400000` | ms (24h) |
---

## 5. Docker y conexión a la Base de Datos (foco principal)

### 5.1 Piezas involucradas

1. **`Dockerfile`** (multi-stage): compila con `maven:3.9.8-eclipse-temurin-21`, corre en `eclipse-temurin:21-jre-alpine` como usuario no-root `springboot`. Expone el puerto **3000** (nota: el `HEALTHCHECK` interno apunta a `http://localhost:4000/actuator/health`, un puerto distinto — ver advertencia abajo).
2. **`docker-compose.yml`** (este proyecto, la API):

```yaml
services:
  api:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: spring-app
    restart: unless-stopped
    ports:
      - "${SERVER_PORT}:${SERVER_PORT}"
    env_file:
      - .env
    networks:
      - postgres_network

networks:
  postgres_network:
    external: true
```

   La API **no levanta su propio Postgres**: se conecta a una red Docker **externa** llamada `postgres_network`, que debe existir previamente (creada por otro `docker-compose`).

3. **`docker-compose.yaml` del contenedor de Postgres** (repo `Loza64/docker-containers`, ruta `database/postgres/`):

```yaml
services:
  postgres:
    image: postgres:latest
    container_name: some-postgres
    ports:
      - "5432:5432"
    environment:
      POSTGRES_USER: ${POSTGRES_USER:-postgres}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-change_me}
      POSTGRES_DB: ${POSTGRES_DB:-app_db}
    volumes:
      - data:/var/lib/postgresql
      - ./sql:/docker-entrypoint-initdb.d:ro
    networks:
      - network
    restart: unless-stopped
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U $${POSTGRES_USER} -d $${POSTGRES_DB}"]
      interval: 10s
      timeout: 5s
      retries: 5
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"

networks:
  network:
    driver: bridge

volumes:
  data:
```

### 5.2 Cómo encajan las dos redes

Docker Compose nombra las redes definidas dentro de un `docker-compose.yaml` como `<nombre_del_proyecto>_<clave_red>`. El nombre del proyecto por defecto es el **nombre de la carpeta** donde se ejecuta el compose (o el que se pase con `-p`).

- El compose de Postgres vive en la carpeta `postgres/` y define la red con clave `network`.
- Al levantarlo desde esa carpeta (`docker compose up -d`), Docker crea la red real **`postgres_network`** (`postgres` + `_` + `network`).
- Ese es exactamente el nombre que el `docker-compose.yml` de esta API espera como red **externa**: `postgres_network`.

**Por eso el orden de arranque importa:**

```bash
# 1) Levantar primero el contenedor de Postgres (crea la red externa)
cd docker-containers/database/postgres
docker compose up -d

# 2) Verificar que la red se llame exactamente "postgres_network"
docker network ls | grep postgres_network

# 3) Levantar la API, que se conecta a esa red externa
cd /ruta/al/proyecto/app
docker compose up -d --build
```

Si el compose de Postgres se ejecuta desde una carpeta con otro nombre (o con `-p otro_nombre`), la red resultante no se llamará `postgres_network` y el `docker compose up` de la API fallará con un error como:
`network postgres_network declared as external, but could not be found`.

Solución en ese caso: crear la red manualmente antes de levantar Postgres, fijando el nombre:

```bash
docker network create postgres_network
```

y añadir en el compose de Postgres:

```yaml
networks:
  network:
    external: true
    name: postgres_network
```

(o simplemente forzar el nombre del proyecto con `docker compose -p postgres up -d` desde cualquier carpeta).

### 5.3 Host y puerto de conexión desde la API

Como ambos contenedores (`spring-app` y `some-postgres`) comparten la misma red Docker (`postgres_network`), Docker resuelve por nombre de contenedor. Esto significa que **dentro de Docker**, `SPRING_DATASOURCE_URL` **no debe usar `localhost`** (eso apuntaría al propio contenedor de la API), sino el nombre del contenedor de Postgres:

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://some-postgres:5432/app_db
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=change_me
```

(los valores de usuario/contraseña/DB deben coincidir con `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_DB` definidos al levantar el contenedor de Postgres).

El `.env` actual del proyecto tiene `jdbc:postgresql://localhost:5432/spring-app`, que **solo funciona si la API corre fuera de Docker** (ej. `./mvnw spring-boot:run` en tu máquina) apuntando a un Postgres publicado en el puerto `5432` del host. Si vas a correr la API **dentro** del contenedor, cambia el host a `some-postgres` como se explicó arriba.

### 5.4 Resumen rápido: dos escenarios de conexión

| Escenario | Dónde corre la API | Host en `SPRING_DATASOURCE_URL` | Requiere |
|---|---|---|---|
| Desarrollo local (sin Docker para la API) | `./mvnw spring-boot:run` en el host | `localhost` | Postgres publicado en `5432:5432` (el compose ya lo hace) |
| API en contenedor, misma red que Postgres | `docker compose up` (API) | `some-postgres` (nombre del contenedor) | Red externa `postgres_network` ya creada por el compose de Postgres |

### 5.5 Advertencias detectadas en la configuración Docker actual

- **Puerto de healthcheck inconsistente**: el `Dockerfile` expone `3000` y hace `HEALTHCHECK` contra `http://localhost:4000/actuator/health`, pero no hay dependencia `spring-boot-starter-actuator` expuesta en `4000` — el puerto de la app es `${SERVER_PORT}` (3000 según `.env`). Conviene alinear el healthcheck al mismo puerto que `SERVER_PORT`, o exponer explícitamente `/actuator/health` (ya está el starter incluido en `pom.xml`) en el puerto correcto.
- **Nombre de base de datos distinto entre archivos**: `.env` usa `spring-app` como nombre de BD, mientras el compose de Postgres usa por defecto `app_db`. Deben coincidir para que la conexión funcione.
- El `Dockerfile` espera un directorio `.mvn` (`COPY .mvn .mvn`) que no aparece en el zip subido; si falta en el repo real, el build de Docker fallará. Confirmar que exista `.mvn/wrapper/maven-wrapper.properties` junto al `mvnw`.

---

## 6. Cómo ejecutar el proyecto

### 6.1 Localmente (sin Docker para la API)

```bash
# 1. Levantar solo Postgres (contenedor externo)
cd docker-containers/database/postgres
docker compose up -d

# 2. Clonar el proyecto y crear el .env a partir de la plantilla
#    (.env no está en el repo por estar en .gitignore)
cd /ruta/al/proyecto/app
cp .env.example .env
# Editar .env: host localhost y el mismo usuario/db/password que el contenedor de Postgres

# 3. Ejecutar la app
./mvnw spring-boot:run
```

### 6.2 Todo en contenedores

```bash
cd docker-containers/database/postgres
docker compose up -d          # crea la red externa "postgres_network"

cd /ruta/al/proyecto/app
cp .env.example .env
# Editar .env: SPRING_DATASOURCE_URL=jdbc:postgresql://some-postgres:5432/<POSTGRES_DB>
#              usuario/password/DB deben coincidir con los del contenedor de Postgres
docker compose up -d --build
```

Verificar:

```bash
docker ps                                   # spring-app y some-postgres corriendo
docker logs spring-app                      # revisar arranque / conexión a BD
curl http://localhost:3000/swagger-ui.html  # Swagger disponible
```

---

## 7. Manejo de errores

`AdviceController` (junto con `common/exceptions/*`) centraliza excepciones de negocio (`BadRequestException`, `ConfictException`, `ForbiddenException`, `NotFoundException`, `UnauthorizedException`, `ServerException`), devueltas como `ExceptionResponse { status, message }` en JSON — el mismo formato que generan los filtros de seguridad ante fallos de autenticación/autorización.

---