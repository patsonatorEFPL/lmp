# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Project Overview

**LMP Digital Services** is a comprehensive Spring Boot web application for digital services management with integrated Stripe payments, secure authentication, and administrative dashboard. The application handles service bookings, user management, payment processing, email notifications, and PDF invoice generation.

### Key Technologies
- **Backend**: Spring Boot 3.5.4, Java 17
- **Database**: MySQL 8.0+ with Flyway migrations
- **Security**: Spring Security with BCrypt encryption
- **Payments**: Stripe API with webhook handling
- **Frontend**: Thymeleaf templates with HTML5/CSS3/JavaScript
- **Build Tool**: Maven 3.9+
- **Deployment**: Docker with Coolify platform

## Common Development Commands

### Build and Run
```bash
# Clean and build project
./mvnw clean package

# Run application in development mode
./mvnw spring-boot:run

# Run with specific profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Build Docker image
docker build -t lmp-app .

# Run with Docker Compose (Coolify)
docker-compose -f docker-compose.coolify.yml up
```

### Testing
```bash
# Run all tests
./mvnw test

# Run integration tests
./mvnw verify

# Run specific test class
./mvnw test -Dtest=LmpApplicationTests

# Run tests with coverage
./mvnw test jacoco:report
```

### Database Operations
```bash
# Run Flyway migrations
./mvnw flyway:migrate

# Clean database (development only)
./mvnw flyway:clean

# Validate migrations
./mvnw flyway:validate

# Show migration info
./mvnw flyway:info
```

### Database Connection (Production)
```bash
# Connect to production MySQL database (Coolify)
mysql -h 179.61.246.91 -P 5432 -u mysql -p default

# Quick database stats
mysql -h 179.61.246.91 -P 5432 -u mysql -p default -e "SELECT 'Users' as entity, COUNT(*) as count FROM users UNION SELECT 'Orders', COUNT(*) FROM orders UNION SELECT 'Services', COUNT(*) FROM services;"

# Check order status distribution
mysql -h 179.61.246.91 -P 5432 -u mysql -p default -e "SELECT status, COUNT(*) as count FROM orders GROUP BY status;"

# View recent orders
mysql -h 179.61.246.91 -P 5432 -u mysql -p default -e "SELECT id, service_name, status, total_amount, created_at FROM orders ORDER BY created_at DESC LIMIT 10;"
```

### Development Utilities
```bash
# Start with live reload (if devtools enabled)
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.devtools.restart.enabled=true"

# Generate sources and compile
./mvnw generate-sources compile

# Dependency tree analysis
./mvnw dependency:tree

# Check for dependency updates
./mvnw versions:display-dependency-updates
```

## Architecture Overview

### Package Structure
```
com.lmp/
├── config/              # Spring configuration classes
│   ├── SecurityConfig.java      # Security rules and authentication
│   ├── StripeConfig.java        # Stripe payment configuration
│   ├── MailConfig.java          # Email service configuration
│   └── DataInitializer.java     # Database initialization
├── controller/          # Public web controllers
│   ├── HomeController.java      # Landing pages
│   ├── ServicesController.java  # Service catalog
│   └── ContactController.java   # Contact forms
├── web/controller/      # API and specialized controllers
│   ├── admin/                   # Admin dashboard controllers
│   ├── auth/                    # Authentication controllers
│   ├── payment/                 # Payment processing controllers
│   └── user/                    # User profile controllers
├── service/             # Business logic layer
│   ├── auth/                    # Authentication services
│   ├── payment/                 # Payment processing services
│   ├── email/                   # Email notification services
│   ├── admin/                   # Administrative services
│   └── user/                    # User management services
├── domain/              # Data models
│   ├── entity/                  # JPA entities
│   ├── dto/                     # Data transfer objects
│   └── enums/                   # Enumeration types
├── repository/          # Data access layer (JPA repositories)
└── util/               # Utility classes
```

### Key Architecture Patterns

#### Security Architecture
- **Multi-layer authorization**: Public pages, authenticated users, admin-only sections
- **Role-based access control**: USER and ADMIN roles with method-level security
- **Session management**: Configurable session limits with registry tracking
- **CSRF protection**: Enabled for forms, disabled for API endpoints and webhooks
- **CORS configuration**: Supports localhost development and production domain

#### Payment System Architecture
- **Payment abstraction**: `PaymentService` interface with multiple processor implementations
- **Stripe integration**: Checkout sessions, webhook handling, and transaction tracking
- **Transaction persistence**: Full audit trail with `PaymentTransaction` entities
- **Webhook security**: Signature verification for Stripe events
- **Error handling**: Comprehensive exception hierarchy for payment failures

#### Email System Architecture
- **Multi-provider support**: Mailtrap for production, configurable SMTP
- **Template-based emails**: Thymeleaf integration for HTML email templates  
- **Notification services**: Async email processing with WebSocket real-time updates
- **Domain authentication**: Cloudflare email routing with branded sender addresses

#### Data Architecture
- **Entity relationships**: User → Orders → OrderItems → Services with full referential integrity
- **Audit trail**: Order status history, payment transactions, and user activity tracking
- **Soft delete support**: User status enum (ACTIVE/INACTIVE/DELETED) maintains data integrity
- **Migration management**: Flyway for database versioning and schema evolution

##### Database Schema Overview
**Core Tables (14 total):**
- `users` - User accounts with profile information, status tracking, and security fields
- `roles` & `user_roles` - Role-based access control (USER/ADMIN)
- `orders` - Comprehensive order management with 28 fields including Stripe integration
- `order_items` & `order_status_history` - Order composition and status audit trail
- `services` - Service catalog with pricing, categories, and benefits
- `service_categories` & `service_benefits` - Service organization and feature lists
- `payment_transactions` - Payment processing audit with status tracking
- `refunds` - Refund management and tracking
- `appointments` - Service appointment scheduling
- `invoices` - Generated invoice management
- `reviews` - Customer feedback system

**Key Schema Features:**
- **Stripe Integration**: Full Stripe session, payment intent, and charge ID tracking in orders
- **Comprehensive Order Status**: 11 status types (PENDING → COMPLETED workflow)
- **Payment Status Tracking**: 4 transaction states (PENDING/COMPLETED/FAILED/REFUNDED)
- **User Status Management**: ACTIVE/INACTIVE/DELETED with account locking support
- **Audit Fields**: Created/updated timestamps across all entities
- **Billing Information**: Complete billing address capture in orders
- **Admin Features**: Admin notes, processing notes, priority levels, and tags

## Development Guidelines

### Environment Configuration
The application uses profile-based configuration:
- **production**: Default profile for deployed environments
- **dev**: Local development with debug logging
- **test**: Testing profile with H2 in-memory database

Key environment variables for deployment:
```bash
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=jdbc:mysql://host:port/database
DATABASE_USERNAME=user
DATABASE_PASSWORD=password
STRIPE_SECRET_KEY=sk_live_xxx
STRIPE_PUBLISHABLE_KEY=pk_live_xxx
STRIPE_WEBHOOK_SECRET=whsec_xxx
APP_BASE_URL=https://lmp-services.ca
MAIL_USERNAME=api
MAIL_PASSWORD=xxx
```

### Common Issues and Solutions

#### LazyInitializationException with User Roles
The application uses specific repository methods to prevent Hibernate LazyInitializationException:
- Use `findByIdWithRoles(Long id)` instead of `findById(Long id)` when accessing user roles
- Use `findByEmailWithRoles(String email)` instead of `findByEmail(String email)` when roles are needed
- These methods use `LEFT JOIN FETCH` to eagerly load the roles collection

**Fixed Components:**
- `AdminDashboardController`: Uses `findByEmailWithRoles()` for admin user loading
- `CustomUserDetailsService`: Uses `findByEmailWithRoles()` and `findByIdWithRoles()` for authentication
- `SecurityConfig`: Added `/.well-known/**` permit rule for Chrome DevTools

#### Chrome DevTools 404 Errors
The security configuration includes a rule to prevent Chrome DevTools from generating 404 errors:
```java
.requestMatchers("/.well-known/**").permitAll()
```

### Security Best Practices
- All admin operations require `@PreAuthorize("hasRole('ADMIN')")`
- Sensitive data stored in environment variables, not application.properties
- Webhook endpoints validate signatures before processing
- User passwords encrypted with BCrypt (strength 10)
- Session security with timeout and concurrency controls

### Payment Integration Notes
- **Stripe webhooks**: Must be configured to point to `/api/stripe/webhook`
- **Currency handling**: Default CAD, configurable per service
- **Amount validation**: Min $1.00, Max $10,000.00 enforced
- **Transaction tracking**: All payment events logged with full audit trail
- **Refund support**: Partial and full refunds through admin interface

### Email System Notes
- **Template location**: `src/main/resources/templates/emails/`
- **Static assets**: Email images must be accessible via public URLs
- **Async processing**: Email sending is non-blocking for better performance
- **Delivery tracking**: Integration with Mailtrap for delivery analytics

### Database Migration Guidelines
- All schema changes must be in Flyway migration files (`src/main/resources/db/migration/`)
- Use descriptive naming: `V{version}__{description}.sql`
- Test migrations on dev database before production deployment
- Never modify existing migration files after deployment

### Testing Approach
- **Unit tests**: Service layer logic with mocked dependencies
- **Integration tests**: Full stack tests with test database
- **Security tests**: Authentication and authorization verification
- **Payment tests**: Stripe webhook simulation and transaction validation

### Deployment Considerations
- **Docker multi-stage build**: Optimized for production deployment
- **Health checks**: Spring Boot Actuator endpoints for monitoring
- **Resource limits**: Memory and CPU constraints defined in docker-compose
- **Volume persistence**: Logs and generated invoices stored in named volumes
- **Reverse proxy support**: Headers configured for Coolify/nginx integration

## Monitoring and Maintenance

### Health Check Endpoints
- `GET /actuator/health` - Application health status
- `GET /actuator/info` - Application information and version
- `GET /actuator/metrics` - Performance metrics (if enabled)

### Logging Configuration
- **Root level**: INFO for production, DEBUG for development
- **Security events**: Dedicated AUDIT logger for admin actions
- **Payment events**: Detailed logging for transaction tracking
- **Email delivery**: Comprehensive SMTP debugging when needed

### Performance Considerations
- **Connection pooling**: HikariCP configured with optimal pool sizes
- **Thymeleaf caching**: Enabled in production for better performance
- **Static resources**: Served directly from classpath with appropriate caching headers
- **Database queries**: JPA query optimization with show-sql disabled in production

### Production Database Status
**Current Data (as of connection):**
- **Users**: 2 registered users in system
- **Orders**: 22 orders processed (indicates active usage)
- **Services**: 0 services (catalog may need initialization)
- **Tables**: 14 core tables fully deployed and functional

**Key Insights:**
- System is live with real user orders
- Payment processing is operational (22 orders indicate Stripe integration working)
- User management system active with role-based access
- Database schema fully migrated and operational on Coolify platform

### Backup and Recovery
- Database backups managed by hosting platform (Coolify)
- Application logs retained in persistent volumes
- Generated invoices stored in persistent storage
- Configuration values backed up in environment variable management
