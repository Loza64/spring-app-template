# Documentación Técnica — Backend `spring-app`

## 1. Resumen general

API REST construida con **Spring Boot 3.5.8 + Java 21** que gestiona **usuarios, roles y permisos**, con autenticación JWT y autorización basada en permisos. Persiste en PostgreSQL vía JPA/Hibernate.

**Paquete raíz:** `com.server.app` · **Clase principal:** `AppApplication`

### Stack técnico
- Java 21
- Spring Boot 3.5.8 (Web, Security, Data JPA, Validation y Actuator)
- PostgreSQL con Hibernate
- JJWT 0.12.6 para JWT HMAC
- BCrypt para contraseñas
- MapStruct 1.6.3 y Lombok
- Springdoc OpenAPI 2.8.5
- Maven Wrapper y Docker con Eclipse Temurin 21


## 2. Arquitectura por capas

```
controller/   → Endpoints REST
service/      → Lógica de negocio
repository/   → Acceso a datos (Spring Data JPA)
domain/
  model/      → Entidades JPA
  dto/        → DTOs de entrada/salida por módulo (auth, user, role, permission, base)
  mapper/     → Interfaces MapStruct (Entity ↔ DTO)
config/       → Seguridad, CORS, JWT, OpenAPI, paginación, Jackson
filter/       → Filtros de autenticación/autorización
seeders/      → Poblado automático al arrancar (roles, permisos, super admin)
common/
  exceptions/ → Excepciones de negocio + manejador global
  pagination/ → Wrapper estándar de respuestas paginadas
```

Los módulos `User` y `Role` implementan una interfaz genérica `IBaseService<ID, CreateDto, UpdateDto, ResponseDto>` con los métodos CRUD estándar (`create`, `update`, `findById`, `delete`, `restore`).


## 3. Modelo de datos

### `BaseEntity` (`@MappedSuperclass`)
Heredado por `User` y `Role`: `createdAt`, `updatedAt` (auto-actualizado en `@PreUpdate`), `deletedAt` (soft delete).

### `User`
`id`, `username` (único), `email` (único), `name`, `surname`, `password` (hash BCrypt), `blocked` (default `false`), `role` (`@ManyToOne` LAZY).

### `Role`
`id`, `name` (único), `active` (default `true`), `permissions` (`@ManyToMany` LAZY vía tabla `role_permissions`).

### `Permission`
`id`, `name` (único) y `title` descriptivo editable. Los nombres de permisos se definen en `PermissionNames`.

### `RefreshToken`
`token` (hash SHA-256 del valor real), `familyId` (agrupa tokens rotados de una misma sesión), `used`, `revoked`, `user` (`@ManyToOne` EAGER), `expiresAt`.


## 4. Seguridad y autenticación

### 4.1 Flujo de request
```
Request → CORS → SecurityFilterChain → JwtAuthenticationFilter → Controller
```
`SecurityConfig` desactiva CSRF, fuerza sesiones stateless, permite las operaciones públicas de autenticación y exige autenticación para el resto. El control de permisos se aplica con `@PreAuthorize` y las authorities cargadas por `JwtAuthenticationFilter`.

### 4.2 Rutas públicas y autenticadas
- Públicas en `SecurityConfig`: `POST /api/auth/login`, `POST /api/auth/signup`, `POST /api/auth/refresh` y `POST /api/auth/logout`.
- Autenticadas: perfil, cambio de contraseña y todos los endpoints de usuarios, roles y permisos.
- OpenAPI: `/v3/api-docs` y `/swagger-ui.html`.

### 4.3 `JwtAuthenticationFilter`
- Exige el header `Authorization: Bearer <token>`.
- Verifica la firma y expiración, y extrae el id desde el claim `id`.
- Carga el perfil actual desde la base de datos en cada request.
- Rechaza usuarios bloqueados o con roles inactivos.
- Registra como authorities los nombres de los permisos y `ROLE_<NOMBRE_ROL>`.

Aunque `SecurityConfig` declara públicas las rutas de autenticación, el filtro actual responde `401` si no reciben un header Bearer. Este comportamiento debe considerarse al probar login, signup, refresh y logout.

### 4.4 Autorización por permisos
Los controladores de usuarios, roles y permisos usan `@PreAuthorize("hasAuthority('PERMISO')")`. Las authorities se obtienen desde `role_permissions` y se agregan al contexto de Spring Security por cada request.

El usuario con role `SUPER_ADMIN` recibe todos los permisos disponibles durante el `RoleSeeder`. Además, sus operaciones de edición de role están protegidas en `RoleServiceImpl` y el usuario super administrador no puede modificarse, eliminarse ni restaurarse desde la API.

### 4.5 `JsonWebTokenProvider`
Firma HMAC con clave Base64. El JWT solo contiene el `id` del usuario; rol, permisos y estado se resuelven en cada request desde BD. `expiration-time` configurable en milisegundos.

### 4.6 Refresh tokens (`RefreshTokenService`)
Rotación con detección de reuso:
1. Al login/signup se emite un refresh token (UUID); se guarda su **hash SHA-256** junto a un `familyId`.
2. En `/refresh`, si el token ya fue usado o está revocado → se revoca **toda la familia** y se lanza `UnauthorizedException`.
3. Si es válido, se marca `used=true` y se emite uno nuevo con el mismo `familyId`.
4. `/logout` revoca la familia completa.
5. TTL de **7 días**.

### 4.7 `PermissionSeeder` (`@Order(0)`)
Al arrancar (`ApplicationReadyEvent`), crea en BD los permisos definidos en `PermissionNames.HIERARCHY` que todavía no existan. No elimina permisos existentes.

### 4.8 `RoleSeeder` (`@Order(1)`)
Crea, si no existen, los roles base `SUPER_ADMIN`, `ADMIN` y `CLIENT` (constantes en `RoleNames`). Después asigna a `SUPER_ADMIN` todos los registros de la tabla `permissions`, por lo que también actualiza instalaciones existentes cuando se agregan nuevos permisos.

### 4.9 `SuperAdminSeeder` (`@Order(2)`)
Crea automáticamente el usuario `SUPER_ADMIN` al arrancar, garantizando que solo exista uno:
- Verifica `userRepository.existsByRole_Name(RoleNames.SUPER_ADMIN)`; si ya existe, no hace nada.
- Si el rol `SUPER_ADMIN` aún no existe, lanza `IllegalStateException`.
- Usa `SUPER_ADMIN_USERNAME`, `SUPER_ADMIN_EMAIL` y `SUPER_ADMIN_PASSWORD`.
- Guarda la contraseña mediante `BCryptPasswordEncoder`.

### 4.10 Protección del usuario `SUPER_ADMIN` (`UserServiceImpl`)
`update`, `delete` y `restore` verifican si el usuario objetivo tiene rol `SUPER_ADMIN`; si es así, lanzan `ConfictException` y bloquean la operación. El único `SUPER_ADMIN` del sistema queda protegido de ser modificado o eliminado vía API.

### 4.11 Catálogo de permisos

Los permisos actuales son:

| Permiso | Operaciones protegidas |
|---|---|
| `USER_CREATE` | Crear usuarios |
| `USER_READ` | Listar y consultar usuarios |
| `USER_UPDATE` | Actualizar usuarios |
| `USER_DELETE` | Eliminar y restaurar usuarios |
| `ROLE_CREATE` | Crear roles |
| `ROLE_READ` | Listar y consultar roles |
| `ROLE_UPDATE` | Actualizar roles |
| `ROLE_DELETE` | Eliminar y restaurar roles |
| `PERMISSION_READ` | Listar permisos |
| `PERMISSION_UPDATE` | Actualizar el título de un permiso |

El seeder asigna los diez permisos a `SUPER_ADMIN`. Los permisos de `ADMIN` y `CLIENT` se administran mediante la relación `role_permissions` o mediante la creación/actualización de roles. Los endpoints de autenticación y perfil tienen reglas propias y no usan estos permisos CRUD.


## 5. Endpoints expuestos

### `/api/auth` (`AuthController`)
| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| POST | `/login` | público | Retorna `AuthResponseDto` (JWT + refresh + perfil) |
| POST | `/signup` | público | Crea usuario con rol `ADMIN` por defecto, 201 + tokens |
| POST | `/refresh` | público | Rota el refresh token |
| POST | `/logout` | público | Revoca la familia del refresh token |
| GET | `/profile` | autenticado | Perfil del usuario actual |
| PUT | `/profile` | autenticado | Actualiza username/name/surname/email |
| PATCH | `/password` | autenticado | Cambia contraseña |

### `/api/users` (`UserController`) — requiere permiso explícito
`GET /` (paginado, filtros `search`, `role`, `deleted`), `GET /{id}`, `POST /`, `PUT /{id}`, `DELETE /{id}` (soft delete), `PATCH /{id}/restore`.

### `/api/roles` (`RoleController`)
`GET /` (paginado, filtros `search` y `deleted`), `GET /{id}`, `POST /`, `PUT /{id}`, `DELETE /{id}` (soft delete), `PATCH /{id}/restore`.

### `/api/permissions` (`PermissionController`)
`GET /` (paginado), `PUT /{id}` (solo editable el `title`).

Todas las respuestas paginadas usan `PaginationResponse<T>` (`data` + metadata: `page` 1-indexado, `pageSize`, `pageCount`, `total`).


## 6. Manejo de errores (`AdviceController`)

`@RestControllerAdvice` global que traduce excepciones a `ExceptionResponse { status, message }`:

| Excepción | HTTP |
|---|---|
| `BadCredentialsException` / `UnauthorizedException` | 401 |
| `AccessDeniedException` / `ForbiddenException` | 403 |
| `NotFoundException` | 404 |
| `ConfictException` | 409 |
| `BadRequestException` | 400 |
| `ServerException` | 500 |
| `MethodArgumentNotValidException` / `MissingServletRequestParameterException` / `MethodArgumentTypeMismatchException` | 400 |
| `RuntimeException` genérica | 400 |
| `Exception` genérica | 500 |


## 7. Configuración (`application.properties`)

Todo alimentado por variables de entorno:
```
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

springdoc.api-docs.path=/v3/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.operationsSorter=method
spring.jackson.serialization.write-dates-as-timestamps=false

management.endpoints.web.exposure.include=health
management.endpoint.health.show-details=never

security.super-admin.username=${SUPER_ADMIN_USERNAME}
security.super-admin.email=${SUPER_ADMIN_EMAIL}
security.super-admin.password=${SUPER_ADMIN_PASSWORD}
```
`spring.jpa.open-in-view=false`, Swagger en `/v3/api-docs` y `/swagger-ui.html`, solo se expone el endpoint `health` de Actuator sin detalles.


## 8. Paginación y búsqueda
- `PageableConfig` fuerza que `page` sea 1-indexado.
- `UserSpecifications` filtra por texto, `roleId` y `deleted`.
- `RoleSpecifications` filtra por texto y `deleted`.
- Los repositorios usan `EntityGraph` para cargar roles y permisos cuando es necesario y evitar N+1.
- Las respuestas paginadas usan `PaginationResponse<T>` con `data`, `page`, `pageSize`, `pageCount` y `total`.

## 9. Mapeo de datos (MapStruct)
- Los mappers ignoran ids, contraseñas, relaciones gestionadas por el servicio y fechas de auditoría cuando corresponde.
- Las actualizaciones usan `NullValuePropertyMappingStrategy.IGNORE`, por lo que los campos `null` no sobrescriben valores existentes.
- `RoleMapper.toSummaryDto` omite permisos en listados para reducir el payload.

## 10. DTOs principales

### Autenticación
- `LoginDto`: `username` y `password`, ambos obligatorios.
- `SignUpDto`: `username`, `name`, `surname`, `email` y `password`; la contraseña requiere mínimo 8 caracteres.
- `UpdateProfileDto`: username, nombre, apellido y email obligatorios.
- `UpdatePasswordDto`: contraseña actual, nueva contraseña y confirmación; la nueva requiere mínimo 8 caracteres.
- `AuthResponseDto`: JWT, refresh token y perfil.

### Usuarios, roles y permisos
- `UserCreateDto`: username, name, surname, email, password y role opcional mediante `IdDto`.
- `UserUpdateDto`: username, name, surname, blocked, email y role opcional.
- `RoleCreateDto` y `RoleUpdateDto`: `name`, `active` y permisos opcionales mediante ids.
- `PermissionUpdateDto`: `title` obligatorio, entre 3 y 100 caracteres.
- Las respuestas incluyen datos de auditoría (`createdAt`, `updatedAt` y `deletedAt`) cuando corresponde.

## 11. Ejecución local

Requisitos: Java 21, PostgreSQL y las variables de entorno descritas en la sección de configuración.

```powershell
.\mvnw.cmd -B test
.\mvnw.cmd -B verify
.\mvnw.cmd spring-boot:run
```

## 12. Docker

El `Dockerfile` compila con Maven y Temurin 21, ejecuta la imagen final con `eclipse-temurin:21-jre-alpine`, usa el usuario no root `springboot`, expone el puerto 3000 y comprueba `/actuator/health`.

`docker-compose.yml` construye el servicio `api`, carga `.env`, publica `${SERVER_PORT}` y usa la red externa `postgres_network`.

```powershell
docker network create postgres_network
docker compose up --build
```

## 13. Consultas SQL de permisos

`PermissionSeeder` y `RoleSeeder` se encargan de crear los permisos y asignarlos todos a `SUPER_ADMIN`. Para inspeccionar las asignaciones actuales:

```sql
select * from roles r ;
select * from role_permissions;
select * from permissions;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'ADMIN' AND p.name = 'USER_READ'
ON CONFLICT DO NOTHING;
```

La tabla `permissions` usa `name`, no `path` ni `method`. `SUPER_ADMIN` recibe todos los permisos automáticamente durante el arranque; no es necesario insertar sus relaciones manualmente. `ON CONFLICT DO NOTHING` mantiene idempotentes las inserciones si existe la restricción única de la tabla intermedia.

El archivo `script.sql` incluido en el repositorio contiene una versión anterior de la inserción para `ADMIN` que filtra por `permissions.method` y también incluye una limpieza de `role_permissions`. Esa consulta no coincide con el modelo actual, que usa `permissions.name`, por lo que debe actualizarse antes de ejecutarse.