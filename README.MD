# SPRING APP TEMPLATE

Plantilla backend en **Spring Boot 3.5** con JWT, roles/permiso dinámico y gestión de usuarios.

## 📌 Resumen del proyecto

Este proyecto es una API REST segura para autenticación y administración de usuarios, roles y permisos. Está diseñado para funcionar con PostgreSQL y utiliza filtros personalizados para el control de acceso basado en JWT y permisos almacenados en la base de datos.

## 🧩 Tecnologías principales

- Java 21
- Spring Boot 3.5.8
- Spring Data JPA
- Spring Security
- Spring Web
- Spring Validation
- PostgreSQL
- JWT con `io.jsonwebtoken` (JJWT)
- Lombok
- Docker / Docker Compose

## 📁 Estructura importante

- `src/main/java/com/server/app/AppApplication.java` - punto de entrada de Spring Boot.
- `src/main/java/com/server/app/config/` - configuración de seguridad, CORS y JWT.
- `src/main/java/com/server/app/filters/` - filtros de autenticación y autorización dinámica.
- `src/main/java/com/server/app/components/SaveEndpoints.java` - registra las rutas de la app en la tabla `permissions`.
- `src/main/java/com/server/app/controllers/` - controladores REST para auth, usuarios, roles y permisos.
- `src/main/java/com/server/app/services/` - lógica de negocio para usuarios, roles y permisos.
- `src/main/java/com/server/app/entities/` - modelo de datos JPA.
- `src/main/java/com/server/app/repositories/` - acceso a datos con Spring Data JPA.
- `src/main/resources/application.properties` - configuración del entorno.
- `script.sql` - inicialización de roles y asignación de permisos.
- `Dockerfile` / `docker-compose.yml` - despliegue con contenedores.

## 🔒 Diseño de seguridad

### Seguridad general

- `SecurityConfig` habilita seguridad sin estado (`STATELESS`) y desactiva CSRF.
- Todos los endpoints se permiten inicialmente en Spring Security, porque la autorización se realiza en los filtros personalizados.
- Se aplican dos filtros en esta orden:
  1. `JwtAuthenticationFilter` - valida el token y carga el usuario.
  2. `DynamicAuthorizationFilter` - verifica permisos por método y ruta.

### Autenticación JWT

- `JsonWebToken` crea y valida tokens JWT.
- El token guarda el `id` del usuario en sus claims.
- `JwtAuthenticationFilter`:
  - extrae el header `Authorization: Bearer <token>`.
  - valida expiración y contenido.
  - carga el `User` desde la base de datos.
  - construye authorities a partir de permisos: `METHOD:/ruta`.
  - agrega también la autoridad `ROLE_<ROL>`.

### Autorización dinámica

- `DynamicAuthorizationFilter` compara la ruta y el método HTTP contra las autoridades del usuario.
- Si no existe autorización exacta, devuelve `403 Forbidden`.
- Hay rutas públicas y rutas que solo requieren autenticación, definidas en `SecurityRules`.

## 🧠 Registro automático de permisos

`SaveEndpoints` detecta todas las rutas expuestas por Spring al arrancar la aplicación y guarda en la tabla `permissions` los endpoints que no son públicos ni ignorados.

Esto permite registrar automáticamente nuevas rutas en la base de datos sin tener que insertarlas manualmente.

## 🗄️ Modelo de datos

### User

- `id` - entero auto generado.
- `username` - único, no nulo.
- `email` - único, no nulo.
- `password` - cifrado con BCrypt antes de persistir.
- `blocked` - booleano de bloqueo de cuenta.
- `role` - relación `ManyToOne` con `Role`.

### Role

- `id` - entero auto generado.
- `name` - único, no nulo.
- `permissions` - relación `ManyToMany` con `Permission` a través de la tabla intermedia `role_permissions`.

### Permission

- `id` - entero auto generado.
- `path` - ruta HTTP.
- `method` - método HTTP.
- `title` - nombre descriptivo opcional.

## 📜 SQL de inicialización (`script.sql`)

Este archivo realiza:

- Inserción de roles `ADMIN` y `USER`.
- Asignación de todos los permisos a `ADMIN`.
- Asignación de permisos `GET` y el permiso de cambio de contraseña a `USER`.

### Contenido principal

- `INSERT INTO roles (name) VALUES ('ADMIN'), ('USER')`
- `role_permissions` se llena con permisos existentes según método y rol.
- Se agrega manualmente el permiso `id = 13` al rol `USER` para actualización de contraseña.

> Nota: `script.sql` asume que ya existen permisos registrados en la tabla `permissions`. El componente `SaveEndpoints` los crea automáticamente en el arranque.

## 📦 Endpoints disponibles

### Auth

- `POST /api/auth/login`
  - body: `{ "username": "...", "password": "..." }`
  - devuelve token JWT y datos de usuario.

- `POST /api/auth/signup`
  - body: `{ "username", "name", "surname", "email", "password" }`
  - crea usuario con rol por defecto `USER` y retorna token.

- `GET /api/auth/profile`
  - requiere token.
  - devuelve datos del usuario autenticado.

- `PUT /api/auth/update/profile`
  - requiere token.
  - body: `{ "username", "name", "surname", "email" }`
  - actualiza perfil del usuario actual.

- `PUT /api/auth/update/password`
  - requiere token.
  - body: `{ "oldpassword", "newpassword", "confirmpassword" }`
  - cambia contraseña del usuario autenticado.

### Usuarios

- `POST /api/users`
  - crea un usuario nuevo.
  - body similar a signup más `role` opcional.

- `PUT /api/users/{id}`
  - actualiza usuario existente.
  - puede cambiar `username`, `name`, `surname`, `email`, `blocked`, `role`.

- `GET /api/users`
  - paginación: `page`, `size`, `search`.

- `GET /api/users/{id}`
  - obtiene usuario por ID.

### Roles

- `POST /api/roles`
  - crea rol con permisos.
  - body: `{ "name", "permissions": [{ "id" }, ...] }`

- `PUT /api/roles/{id}`
  - actualiza nombre y permisos de rol.

- `GET /api/roles`
  - paginación: `page`, `size`.

- `GET /api/roles/{id}`
  - obtiene rol por ID.

- `DELETE /api/roles/{id}`
  - elimina rol.

### Permisos

- `GET /api/permissions`
  - lista permisos con paginación.

- `GET /api/permissions/{id}`
  - obtiene permiso por ID.

- `PUT /api/permissions/{id}`
  - actualiza el `title` de un permiso.

## 🧾 DTOs y respuestas

- `LoginDto` - login básico.
- `UpdatePasswordDto` - cambio de contraseña.
- `UserCreateDto` - creación de usuario con validaciones.
- `UserUpdateDto` - actualización parcial de usuario.
- `UpdateProfileDto` - edición de perfil de usuario.
- `RoleDto` - nombre de rol y lista de permisos.
- `PermissionDto` - título descriptivo del permiso.
- `AuthResponse` - token JWT y datos del usuario.
- `Pagination<T>` / `PaginationMeta` - paginación estándar.

## ⚙️ Variables de entorno usadas

- `SPRING_APPLICATION_NAME`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `SPRING_DATASOURCE_DRIVER_CLASS_NAME`
- `SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT`
- `SPRING_JPA_HIBERNATE_DDL_AUTO`
- `SPRING_JPA_SHOW_SQL`
- `SPRING_DATA_WEB_PAGEABLE_ONE_INDEXED_PARAMETERS`
- `SERVER_PORT`
- `CORS_ALLOWED_ORIGINS`
- `SECURITY_JWT_SECRET_KEY`
- `SECURITY_JWT_EXPIRATION_TIME`

## 🚀 Ejecución local

```bash
./mvnw clean package
./mvnw spring-boot:run
```

O con Maven instalado:

```bash
mvn clean package
mvn spring-boot:run
```

## 🐳 Ejecución con Docker

```bash
docker compose build
docker compose up -d
docker compose down -v
```

También puedes construir la imagen directamente:

```bash
docker build -t spring-app .
```

## 🔧 Observaciones del código

- `SecurityConfig` delega la autorización a filtros personalizados.
- `JwtAuthenticationFilter` audita token y carga usuario.
- `DynamicAuthorizationFilter` protege rutas con permisos por método y patrón.
- `SaveEndpoints` registra rutas en la tabla `permissions` para mantener control dinámico.
- `User` cifra el password automáticamente con BCrypt en `@PrePersist`/`@PreUpdate`.

## ✅ Recomendaciones

1. Crea `.env` con tus variables.
2. Inicializa la base de datos y ejecuta `script.sql`.
3. Ejecuta la app y prueba login/signup.
4. Usa `POST /api/roles` para crear roles y asignar permisos.
5. Ajusta `SecurityRules` si agregas rutas públicas o auth-only.




