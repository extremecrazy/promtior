# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Documento de referencia sobre la estructura, convenciones y decisiones de diseño de este proyecto.

---

## Comandos

Usar siempre el wrapper de Maven (`./mvnw`, Maven 3.9.16). Requiere JDK 25.

```bash
./mvnw clean package                 # compilar + jar
./mvnw spring-boot:run               # levantar la app (perfil dev; necesita Postgres accesible, ver Configuración)
```

---

## Stack

| Capa | Tecnología |
|------|-----------|
| Lenguaje | Java 25 |
| Framework | Spring Boot 4.1 (`spring-boot-starter-webmvc`) |
| Seguridad | Spring Security stateless + JWT propio (`io.jsonwebtoken:jjwt-*` 0.12.6, no OAuth2 resource server) |
| Persistencia | Spring Data JPA + Hibernate |
| Base de datos | PostgreSQL |
| Boilerplate | Lombok (annotation processor configurado en `maven-compiler-plugin`) |
| Migraciones | Flyway (`spring-boot-starter-flyway` + `flyway-database-postgresql`) |
| Mapeo DTOs | MapStruct 1.6.3 |
| Validación | Bean Validation (`spring-boot-starter-validation`) |
| UI | Thymeleaf (`spring-boot-starter-thymeleaf`) — `/login`, `/home`, `/chat`, sesión + form login |
| Chatbot | Spring AI (`spring-ai-starter-model-openai`, BOM `spring-ai-bom`) contra OpenAI |

Los annotation processors (Lombok, `mapstruct-processor`, `lombok-mapstruct-binding`) están declarados por duplicado en las ejecuciones `default-compile` y `default-testCompile` del `maven-compiler-plugin`; cualquier processor nuevo va en **ambas**. Las versiones de MapStruct no las gestiona Spring Boot: se fijan en las properties `mapstruct.version` y `lombok-mapstruct-binding.version`.

**Trampa de Jackson**: Spring Boot 4 usa Jackson 3, cuyas clases viven en el paquete `tools.jackson.*` (`tools.jackson.databind.ObjectMapper`), no en el clásico `com.fasterxml.jackson.databind`. `com.fasterxml.jackson.databind:jackson-databind` (Jackson 2) sigue en el classpath, pero solo transitivamente vía `jjwt-jackson`, en scope `runtime` — no compila si se importa desde código propio. Si hace falta un `ObjectMapper` inyectado o instanciado a mano, usar `tools.jackson.databind.ObjectMapper` (ver `security/CustomAuthenticationEntryPoint.java`).

---

## Estructura de paquetes

Paquete raíz: `promtior.booking.backend`. La estructura es **horizontal por tipo de archivo**: todos los archivos del mismo tipo van juntos, sin importar a qué dominio pertenecen.

```
promtior.booking.backend
├── controller/      # @RestController — un archivo por recurso
├── service/         # @Service — lógica de negocio
├── repository/      # interfaces JPA
├── entity/          # @Entity — entidades JPA
├── dto/             # Records de request y response
├── mapper/          # Interfaces MapStruct
├── enums/           # Enums del dominio
├── exception/       # Excepciones propias + GlobalExceptionHandler
├── config/          # Beans de configuración (@Configuration)
├── security/        # SecurityConfig, JwtUtil, JwtAuthenticationFilter, UserDetailsServiceImpl
└── shared/          # Auditable, utils transversales
```

Un nuevo módulo (ej: reservas) se distribuye en los paquetes correspondientes: `entity/Booking.java`, `repository/BookingRepository.java`, `service/BookingService.java`, `controller/BookingController.java`, `dto/booking/BookingRequest.java`, `mapper/BookingMapper.java`. **No crear paquetes por dominio.**

---

## Convenciones de código

### Entidades JPA
- Extender `shared.Auditable` (`@MappedSuperclass`: aporta `createdDate`/`lastModifiedDate` automáticos vía `AuditingEntityListener`; no aporta `id` — cada entidad sigue declarando su propia PK).
- Anotar con `@Table(name = "nombre_en_snake_case")`.
- No usar `@Data` de Lombok en entidades — genera `hashCode` sobre colecciones lazy. Usar `@Getter @Setter`.
- Relaciones: `fetch = FetchType.LAZY` por defecto.
- Nunca exponer entidades desde los endpoints — siempre DTOs.
- **Los campos que un `{Recurso}Request` valida llevan la misma anotación de Bean Validation en el campo de la entidad** (`@NotBlank`, `@Size`, `@Pattern`, `@Min`/`@Max`, `@NotNull` en relaciones `@ManyToOne`) — defensa en profundidad: Hibernate valida automáticamente antes de `persist`/`update` (`spring-boot-starter-validation` en el classpath activa el modo `AUTO` de Bean Validation de JPA, sin config extra). Ver `entity.Room`/`entity.User`/`entity.Booking`/`entity.BookingSettings`. Si además se agrega `@Column(nullable = false)` para que coincida con un `@NotBlank`/`@NotNull` del DTO, la columna real en la DB **tiene que ser `NOT NULL` también** (`ddl-auto=validate` rompe el arranque si no) — si la tabla ya está creada sin esa constraint, agregar una migración nueva que la agregue (caso de `booking.name`, ya squasheado dentro de `V1__create_initial_schema.sql` — ver **Migraciones existentes**).

### DTOs
- Java records, cada uno en su propio archivo, en subpaquetes por dominio dentro de `dto/` (`dto/booking/`, …).
- Nombrar `{Recurso}Request.java` / `{Recurso}Response.java`.
- DTOs transversales (`EnumResource`, `ErrorResponse`) viven directamente en `dto/`.
- Validar requests con Bean Validation (`@NotBlank`, `@NotNull`, `@Min`, `@Max`, `@Size`, `@Pattern`, …) — ver la entidad correspondiente para la misma regla espejada (arriba).
- Mapear siempre con MapStruct, nunca a mano.

### Repositorios
- Extender `JpaRepository<Entidad, UUID>`.
- Queries complejas con `@Query` JPQL; evitar SQL nativo salvo necesidad justificada.

### Servicios
- Solo `@Service` a nivel de clase — **nunca** `@Transactional` a nivel de clase.
- Solo los métodos que requieren atomicidad llevan `@Transactional(rollbackFor = Exception.class)`; los de lectura no.
- Lanzar `ResourceNotFoundException` o `BusinessException`.

### Controllers
- `@RestController @RequestMapping("/ruta")`.
- Retornar el tipo directamente — sin `ResponseEntity` ni `@ResponseStatus`.
- Sin lógica de negocio — delegar al servicio.

### Mappers
- Interfaces `@Mapper(componentModel = "spring")`, una por recurso principal. Cálculos simples permitidos en métodos `default`.

### Excepciones
Manejadas centralmente en `GlobalExceptionHandler` con `ProblemDetail` (RFC 9457):
- `ResourceNotFoundException` → 404
- `BusinessException` → 422
- Validación de request → 400 con detalle por campo

---


## Migraciones Flyway

- Ubicación: `src/main/resources/db/migration/`, nombre `V{version}__{descripcion_en_snake_case}.sql`.
- **Nunca modificar una migración ya aplicada**; crear una nueva.
- PKs UUID con `gen_random_uuid()` (default de columna, no `@GeneratedValue` de JPA — las entidades usan `GenerationType.UUID`, que genera el UUID en el JVM, así que el default de la columna solo importa para INSERTs de seed/SQL directo). Requiere PostgreSQL 13+ (tiene `gen_random_uuid()` nativo, sin extensión `pgcrypto`).
- Solo DDL estructural (tablas, columnas, índices, PK, FK, UNIQUE). **Sin CHECK constraints de negocio** — esas reglas viven en el backend.
- **Excepción al DDL-puro**: datos de referencia/semilla (seed data) sí se cargan por migración, en un archivo aparte (`V2__seed_...`), nunca mezclados con el DDL de `V1`.
- No usar `spring.jpa.hibernate.ddl-auto=create-drop`/`update`: Flyway gestiona el schema (`ddl-auto=validate` en `application.properties`, para detectar drift entre entidades y schema real).


---

## Configuración

- `spring.profiles.active=dev` por defecto en `application.properties` (activa `application-dev.properties`); se puede pisar con la variable de entorno `SPRING_PROFILES_ACTIVE` (tiene más prioridad que la property), p. ej. `SPRING_PROFILES_ACTIVE=prod` en despliegues.
- Perfil `dev` (`application-dev.properties`) usa defaults para la conexión a la base si las variables no están seteadas; perfil `prod` (`application-prod.properties`) las referencia sin default — si falta alguna, el arranque falla con un error claro de placeholder no resuelto.
- **No hay secretos committeados en el repo** (es público): `JWT_SECRET` y `OPENAI_API_KEY` no tienen default en ningún perfil, así que también en dev hay que setearlas como variables de entorno para levantar la app. Nunca volver a poner un valor real como default en un `.properties`.
- `server.port=${PORT:8080}` en `application.properties`: usa la variable `PORT` si la plataforma la inyecta (Railway lo hace), si no 8080 — no hace falta configurar el target port a mano al desplegar.

| Variable | Dev default | Prod |
|----------|-------------|------|
| `DB_HOST` | `localhost` | requerida |
| `DB_PORT` | `5432` | requerida |
| `DB_NAME` | `booking_db` | requerida |
| `DB_USER` | `booking` | requerida |
| `DB_PASSWORD` | `booking_pass` | requerida |
| `JWT_SECRET` | requerida | requerida |
| `OPENAI_API_KEY` | requerida| requerida |



