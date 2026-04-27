- [x] Verify that the copilot-instructions.md file in the .github directory is created.

- [x] Clarify Project Requirements (skipped - already provided)

- [x] Scaffold the Project (created basic Spring Boot structure with Gradle)

- [x] Customize the Project

- [x] Install Required Extensions (skipped - none required)

- [x] Compile the Project (installed JDK 17 and Gradle, built successfully)

- [x] Create and Run Task (created Gradle build task)

- [x] Launch the Project

- [x] Ensure Documentation is Complete (created README.md, cleaned up this file)

- [x] Test Targeted Database Reset (verified that reset preserves working data while fixing schema issues)

- [x] Validate API Functionality (confirmed authentication and pricing APIs work after targeted reset)

- [x] Implement Interactive Reset Menu (added numbered options 0-5 for module selection instead of code editing)

## Project-Specific Configuration
- **Database**: Using Docker for PostgreSQL with container name `roluun-db`
  - Access via: `docker exec -it roluun-db psql -U postgres -d deliverydb`
  - Configured in docker-compose.yml
  
- **Lombok**: Project uses Lombok annotations - **DO NOT create getter/setter methods manually**
  - Use `@Data`, `@Getter`, `@Setter` annotations instead
  - Getters/setters are auto-generated at compile time

## Extending BaseApiController Pattern
When instructed to "extend BaseApiController" for a new entity, follow this checklist to ensure proper implementation:

### 1. **Model Entity Requirements**
   - Model class MUST extend `TenantAuditableEntity` (not `AuditableEntity`)
   - `TenantAuditableEntity` provides: `companyId`, `createdAt`, `updatedAt`, `createdBy`, `updatedBy`
   - Use `@Data` and `@EqualsAndHashCode(callSuper = true)` Lombok annotations
   - Example:
     ```java
     @Entity
     @Data
     @EqualsAndHashCode(callSuper = true)
     public class Product extends TenantAuditableEntity {
         // fields here
     }
     ```

### 2. **Repository Interface Format**
   - Repository MUST extend BOTH:
     - `JpaRepository<EntityType, UUID>`
     - `JpaSpecificationExecutor<EntityType>` ← **CRITICAL for BaseServiceImpl filtering**
   - Example (see [ExchangeRateRepository.java](../src/main/java/com/delivery/deliveryapi/repo/ExchangeRateRepository.java)):
     ```java
     public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {
         // custom query methods here
     }
     ```

### 3. **Service Class Format**
   - Service MUST extend `BaseServiceImpl<EntityType, RepositoryType>`
   - Call `super(repository)` in constructor
   - Example (see [ExchangeRateService.java](../src/main/java/com/delivery/deliveryapi/service/ExchangeRateService.java)):
     ```java
     @Service
     public class ProductService extends BaseServiceImpl<Product, ProductRepository> {
         
         public ProductService(ProductRepository productRepository) {
             super(productRepository);
             // additional initialization
         }
     }
     ```

### 4. **Controller Class Format**
   - Controller MUST extend `BaseApiController<EntityType>`
   - Call `super(service, userRepository)` in constructor
   - Example (see [ExchangeRateController.java](../src/main/java/com/delivery/deliveryapi/controller/ExchangeRateController.java)):
     ```java
     @RestController
     @RequestMapping("/products")
     public class ProductController extends BaseApiController<Product> {
         
         public ProductController(ProductService productService, UserRepository userRepository) {
             super(productService, userRepository);
         }
     }
     ```

### 5. **Automatic Endpoints Provided**
   Once properly configured, BaseApiController automatically provides:
   - `GET /{resource}` - List with pagination & filters
   - `GET /{resource}/{id}` - Get by ID
   - `POST /{resource}` - Create
   - `PUT /{resource}/{id}` - Update
   - `DELETE /{resource}/{id}?hardDelete=true` - Delete
   - `PATCH /{resource}/{id}/recover` - Recover soft-deleted

### 6. **Reference Implementation**
   For a complete working example, see:
   - Model: [ExchangeRate.java](../src/main/java/com/delivery/deliveryapi/model/ExchangeRate.java)
   - Repository: [ExchangeRateRepository.java](../src/main/java/com/delivery/deliveryapi/repo/ExchangeRateRepository.java)
   - Service: [ExchangeRateService.java](../src/main/java/com/delivery/deliveryapi/service/ExchangeRateService.java)
   - Controller: [ExchangeRateController.java](../src/main/java/com/delivery/deliveryapi/controller/ExchangeRateController.java)

## Terminal Commands Best Practice
- **Always add a space at the beginning of terminal commands** to prevent the first character from being lost during command execution. This is a known issue with the terminal.
  - Example: ` cd /path/to/dir` instead of `cd /path/to/dir`
  - Example: ` ./gradlew build` instead of `./gradlew build`

- Work through each checklist item systematically.
- Keep communication concise and focused.
- Follow development best practices.