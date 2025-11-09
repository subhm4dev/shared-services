# 🚀 Shared Services

Shared microservices used across all business domains (ecommerce, hospital, food delivery, cab booking, travel, etc.).

## Overview

This monorepo contains shared services that are domain-agnostic and can be reused across multiple business domains. Each service is published independently to GitHub Packages as a Maven artifact.

## Services

| Service | ArtifactId | Version | Description |
|---------|-----------|---------|-------------|
| **IAM** | `shared-iam` | 1.0.0 | Identity and Access Management - Authentication, authorization, JWT tokens |
| **User Profile** | `shared-user-profile` | 1.0.0 | User profile management - Names, avatars, contact information |
| **Address** | `shared-address` | 1.0.0 | Address management - Shipping addresses, delivery locations |
| **Payment** | `shared-payment` | 1.0.0 | Payment processing - Payment gateway integration, transactions |
| **Gateway** | `shared-gateway` | 1.0.0 | API Gateway - Routing, authentication, request/response handling |

## Structure

```
shared-services/
├── pom.xml                    # Parent POM (manages all modules)
├── shared-iam/               # IAM service
│   ├── pom.xml
│   ├── src/
│   └── README.md
├── shared-user-profile/      # User Profile service
│   ├── pom.xml
│   ├── src/
│   └── README.md
├── shared-address/           # Address service
│   ├── pom.xml
│   ├── src/
│   └── README.md
├── shared-payment/           # Payment service
│   ├── pom.xml
│   ├── src/
│   └── README.md
├── shared-gateway/           # Gateway service
│   ├── pom.xml
│   ├── src/
│   └── README.md
└── README.md                 # This file
```

## Prerequisites

- Java 25
- Maven 3.9+
- GitHub Packages access (for publishing and consuming)
- PostgreSQL (for services that require database)
- Redis (for token blacklisting, session management)
- Kafka (for event publishing, if needed)

## Building

### Build All Services

```bash
mvn clean install
```

This builds all services in the monorepo.

### Build Specific Service

```bash
cd shared-iam
mvn clean install
```

## Publishing

### Publish All Services to GitHub Packages

```bash
mvn clean deploy
```

This publishes all services to GitHub Packages at:
`https://maven.pkg.github.com/subhm4dev/shared-services`

### Publish Specific Service

```bash
cd shared-iam
mvn clean deploy
```

### GitHub Packages Authentication

You need to configure Maven `settings.xml` with GitHub Packages authentication:

```xml
<settings>
    <servers>
        <server>
            <id>github</id>
            <username>YOUR_GITHUB_USERNAME</username>
            <password>YOUR_GITHUB_TOKEN</password>
        </server>
    </servers>
</settings>
```

Generate a GitHub Personal Access Token with `read:packages` and `write:packages` permissions.

## Using Shared Services

### Add Dependency

In your service's `pom.xml`:

```xml
<dependencies>
    <!-- Shared IAM Service -->
    <dependency>
        <groupId>com.ecom</groupId>
        <artifactId>shared-iam</artifactId>
        <version>1.0.0</version>
    </dependency>
    
    <!-- Shared User Profile Service -->
    <dependency>
        <groupId>com.ecom</groupId>
        <artifactId>shared-user-profile</artifactId>
        <version>1.0.0</version>
    </dependency>
    
    <!-- Add other shared services as needed -->
</dependencies>
```

### Add Repository

```xml
<repositories>
    <repository>
        <id>github</id>
        <name>GitHub Packages</name>
        <url>https://maven.pkg.github.com/subhm4dev/shared-services</url>
    </repository>
</repositories>
```

### Authentication

Add GitHub Packages authentication to your Maven `settings.xml` (same as publishing).

## Versioning Strategy

Each service has **independent versioning**:

- Services can be versioned independently (e.g., `shared-iam:1.2.0`, `shared-user-profile:1.0.5`)
- Follow [Semantic Versioning](https://semver.org/):
  - **MAJOR**: Breaking changes
  - **MINOR**: New features (backward compatible)
  - **PATCH**: Bug fixes (backward compatible)

### Versioning Example

```xml
<!-- shared-iam/pom.xml -->
<version>1.2.0</version>  <!-- IAM at version 1.2.0 -->

<!-- shared-user-profile/pom.xml -->
<version>1.0.5</version>  <!-- User Profile at version 1.0.5 -->
```

## Service Communication

Services communicate via **HTTP/REST**:

```java
@Value("${services.shared-iam.url}")
private String iamServiceUrl;

@Autowired
private WebClient webClient;

public void validateUser(UUID userId) {
    webClient.get()
        .uri(iamServiceUrl + "/api/v1/users/{userId}", userId)
        .retrieve()
        .bodyToMono(UserResponse.class)
        .block();
}
```

## Configuration

Each service pulls configuration from Config Server (`ecom-config-repo`). Service-specific configurations are in:

- `ecom-config-repo/identity-service/application.yml` (for shared-iam)
- `ecom-config-repo/user-profile-service/application.yml` (for shared-user-profile)
- etc.

## Development

### Adding a New Shared Service

1. Create new module directory: `shared-{service-name}/`
2. Add module to parent POM: `<module>shared-{service-name}</module>`
3. Create `pom.xml` with:
   - Parent: `shared-services`
   - ArtifactId: `shared-{service-name}`
   - Version: `1.0.0`
   - Distribution management for GitHub Packages
4. Add source code in `src/` directory
5. Update this README with new service information

### Running Services Locally

Each service can be run independently:

```bash
cd shared-iam
mvn spring-boot:run
```

Default ports:
- `shared-iam`: 8081
- `shared-user-profile`: 8082
- `shared-address`: 8083
- `shared-payment`: 8089
- `shared-gateway`: 8080

## Testing

### Unit Tests

```bash
mvn test
```

### Integration Tests

Services use Testcontainers for integration tests with PostgreSQL.

```bash
mvn verify
```

## Contributing

1. Make changes to the service
2. Update version in service's `pom.xml` if needed
3. Build and test: `mvn clean install`
4. Publish: `mvn clean deploy`
5. Update consuming services to use new version

## License

[Add your license here]

## Support

For issues or questions, please open an issue in this repository.
