# Documentación Técnica — Backend `spring-app`

## 1. Resumen general

API REST construida con **Spring Boot 3 + Java 17+** que gestiona **usuarios, roles y permisos**, con autenticación JWT y autorización dinámica por ruta. Persiste en PostgreSQL (Supabase) vía JPA/Hibernate.

**Paquete raíz:** `com.server.app` · **Clase principal:** `AppApplication`

### Stack técnico
- Spring Boot (Web, Security, Data JPA, Validation, Actuator)
- Spring Security con filtros personalizados
- JJWT (`io.jsonwebtoken`) para firmar/verificar JWT (HMAC)
- MapStruct para mapeo Entity ↔ DTO
- Lombok
- springdoc-openapi (Swagger UI) con esquema Bearer
- BCrypt para hash de contraseñas
- PostgreSQL vía Hibernate

---

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

---

## 3. Modelo de datos

### `BaseEntity` (`@MappedSuperclass`)
Heredado por `User` y `Role`: `createdAt`, `updatedAt` (auto-actualizado en `@PreUpdate`), `deletedAt` (soft delete).

### `User`
`id`, `username` (único), `email` (único), `name`, `surname`, `password` (hash BCrypt), `blocked` (default `false`), `role` (`@ManyToOne` LAZY).

### `Role`
`id`, `name` (único), `active` (default `true`), `permissions` (`@ManyToMany` LAZY vía tabla `role_permissions`).

### `Permission`
`path` + `method` (constraint único combinado), `title` descriptivo editable.

### `RefreshToken`
`token` (hash SHA-256 del valor real), `familyId` (agrupa tokens rotados de una misma sesión), `used`, `revoked`, `user` (`@ManyToOne` EAGER), `expiresAt`.

---

## 4. Seguridad y autenticación

### 4.1 Flujo de request
```
Request → CORS → JwtAuthenticationFilter → DynamicAuthorizationFilter → Controller
```
`SecurityConfig` desactiva CSRF, fuerza sesiones stateless y permite todas las rutas a nivel de Spring Security (`anyRequest().permitAll()`); el control de acceso real ocurre en los dos filtros personalizados.

### 4.2 `SecurityRules` — clasificación de rutas
- **`PUBLIC`**: sin token (`login`, `signup`, `refresh`, `logout`, `GET /api/public/info`)
- **`AUTH_ONLY`**: requieren estar autenticado, sin permiso específico (`/api/auth/profile`, `/api/auth/password`)
- **`IGNORED`**: infraestructura (Swagger, `/actuator/health`, `/error`)

### 4.3 `JwtAuthenticationFilter`
- Exige header `Authorization: Bearer <token>`.
- Verifica firma/expiración, extrae `userId` del claim `id`.
- Carga el perfil completo desde BD en cada request (refleja bloqueos/cambios de rol al instante).
- Rechaza si el usuario está `blocked` o su rol está `inactive`.
- Construye authorities `"METODO:/ruta"` (ej. `GET:/api/users`) + `ROLE_<NOMBRE_ROL>`.

### 4.4 `DynamicAuthorizationFilter`
Compara `method:path` del request contra las authorities del usuario usando `AntPathMatcher` (soporta wildcards `/api/users/**`). Sin match → `403 Forbidden`.

**Bypass `SUPER_ADMIN`:** si el usuario tiene la authority `ROLE_SUPER_ADMIN`, el filtro deja pasar el request sin evaluar `role_permissions` — acceso total garantizado, independiente de la tabla de permisos.

### 4.5 `JsonWebTokenProvider`
Firma HMAC con clave Base64. El JWT solo contiene el `id` del usuario; rol, permisos y estado se resuelven en cada request desde BD. `expiration-time` configurable en milisegundos.

### 4.6 Refresh tokens (`RefreshTokenService`)
Rotación con detección de reuso:
1. Al login/signup se emite un refresh token (UUID); se guarda su **hash SHA-256** junto a un `familyId`.
2. En `/refresh`, si el token ya fue usado o está revocado → se revoca **toda la familia** y se lanza `UnauthorizedException`.
3. Si es válido, se marca `used=true` y se emite uno nuevo con el mismo `familyId`.
4. `/logout` revoca la familia completa.
5. TTL de **7 días**.

### 4.7 `PermissionSeeder`
Al arrancar (`ApplicationReadyEvent`), escanea todos los endpoints reales registrados en Spring MVC, excluye `PUBLIC`/`AUTH_ONLY`/`IGNORED`, y crea en BD un `Permission` (path+method) por cada uno que no exista — el catálogo de permisos siempre refleja el código actual.

### 4.8 `RoleSeeder` (`@Order(1)`)
Crea, si no existen, los roles base: `SUPER_ADMIN`, `ADMIN`, `CLIENT` (constantes en `RoleNames`).

### 4.9 `SuperAdminSeeder` (`@Order(2)`)
Crea automáticamente el usuario `SUPER_ADMIN` al arrancar, garantizando que solo exista uno:
- Verifica `userRepository.existsByRole_Name(RoleNames.SUPER_ADMIN)`; si ya existe, no hace nada.
- Si el rol `SUPER_ADMIN` aún no existe en BD, lanza `IllegalStateException`.
- Credenciales por variables de entorno (`security.super-admin.*` → `SUPER_ADMIN_USERNAME`, `SUPER_ADMIN_EMAIL`, `SUPER_ADMIN_PASSWORD`).
- Password guardado con `PasswordEncoder` (BCrypt).

### 4.10 Protección del usuario `SUPER_ADMIN` (`UserServiceImpl`)
`update`, `delete` y `restore` verifican si el usuario objetivo tiene rol `SUPER_ADMIN`; si es así, lanzan `ConfictException` y bloquean la operación. El único `SUPER_ADMIN` del sistema queda protegido de ser modificado o eliminado vía API.

### 4.11 Listado jerárquico de roles
`GET /api/roles` filtra según la jerarquía del usuario autenticado:

| Rol autenticado | Roles visibles |
|---|---|
| `SUPER_ADMIN` | `SUPER_ADMIN`, `ADMIN`, `CLIENT` |
| `ADMIN` | `ADMIN`, `CLIENT` |
| `CLIENT` | `CLIENT` |

`RoleNames.HIERARCHY` define el orden; `RoleNames.visibleFrom(rol)` devuelve el rol dado y todo lo inferior. `RoleSpecifications.nameIn(...)` filtra por esos nombres, combinado con la specification de búsqueda/soft-delete existente. `RoleController` obtiene el rol actual vía `@AuthenticationPrincipal` y lo pasa al service.

---

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
`GET /` (paginado, filtros `search`, `deleted`, filtrado jerárquico automático), `GET /{id}`, `POST /`, `PUT /{id}`, `DELETE /{id}` (soft delete), `PATCH /{id}/restore`.

### `/api/permissions` (`PermissionController`)
`GET /` (paginado), `PUT /{id}` (solo editable el `title`).

Todas las respuestas paginadas usan `PaginationResponse<T>` (`data` + metadata: `page` 1-indexado, `pageSize`, `pageCount`, `total`).

---

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

---

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

---

## 8. Paginación y búsqueda

- `PageableConfig` fuerza que `page` sea **1-indexado** en la API.
- `RoleSpecifications` y `UserSpecifications` filtran por `deletedAt`, texto (`LIKE` case-insensitive) y, en usuarios, por `roleId`.
- `EntityGraph` en los repositorios evita el problema N+1 (ej. `UserRepository.findById` trae `role` y `role.permissions` en una sola consulta).

---

## 9. Mapeo de datos (MapStruct)

- `toEntity` ignora campos gestionados por el servidor (`id`, `password`, `blocked`, `role`, fechas de auditoría).
- `updateEntity` usa `@MappingTarget` + `NullValuePropertyMappingStrategy.IGNORE` → actualización parcial (un campo `null` no sobrescribe el valor existente).
- `RoleMapper` expone `toSummaryDto` (sin permisos) para listados, reduciendo el payload.

---

## 10. Script SQL de asignación de permisos

Complementa al `PermissionSeeder`: mientras este crea los permisos automáticamente, este script asigna permisos a roles en `role_permissions`.

```sql
-- ADMIN recibe solo los permisos de lectura (method = 'GET')
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'ADMIN' AND p.method = 'GET'
ON CONFLICT DO NOTHING;

-- Verificación / inspección
select * from roles r ;
select * from role_permissions;
select * from permissions;

-- Reset completo de asignaciones
delete from role_permissions ;
```

`SUPER_ADMIN` no necesita ningún `INSERT` — tiene acceso total vía el bypass del filtro (§4.4). `CLIENT` no recibe ningún permiso por este script; su acceso queda limitado a las rutas `AUTH_ONLY`/`PUBLIC`. El `ON CONFLICT DO NOTHING` hace el script idempotente (requiere una constraint única `(role_id, permission_id)` en la tabla).