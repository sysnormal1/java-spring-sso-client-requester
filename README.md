# Sysnormal Sso client requester

This library provides a Spring Security auto-configuration for integrating Single Sign-On (SSO) authentication into your Spring Boot application. It includes a base security configuration and a filter to validate JWT tokens against an SSO server, ensuring secure access to protected endpoints.

This library can also be used as a client implementation of the [SSO Starter](https://github.com/aalencarvz1/sso-starter), allowing other Java-based APIs or backends to easily integrate into the same authentication ecosystem.

## Features
- Configures Spring Security with a custom SSO authentication filter.
- Supports public endpoints that bypass authentication.
- Validates JWT tokens via an external SSO server.
- Configurable CORS settings for cross-origin requests.
- Disables CSRF protection for stateless API usage.

## Prerequisites
- Spring Boot 4+
- Java 21+
- An SSO server providing token validation endpoints
- Maven or Gradle for dependency management

## Installation

Add the following dependency to your `pom.xml` (Maven) or `build.gradle` (Gradle):

### Maven
```xml
<dependency>
    <groupId>com.sysnormal.libs.security.sso.spring</groupId>
    <artifactId>client-requester</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

### Gradle
```groovy
implementation 'com.sysnormal.libs.security.sso.spring:client-requester:0.0.1-SNAPSHOT'
```

## Configuration and usage

This library is auto-configuration, but is necessary inject this at here necessary:

````java
import com.sysnormal.libs.security.sso.spring.client_requester.services.SsoClientRequesterService;
import org.springframework.beans.factory.annotation.Autowired;

@Autowired
private SsoClientRequesterService ssoClientRequesterService;
...
DefaultDataSwap loginResponse = ssoClientRequesterService.loginOnSso(email,pasword);
````

### Required Configuration Properties

You need to configure the following properties in your `application.yml` or `application.properties` file:

#### application.yml
```yaml
sso:
  base-endpoint: "http://your-sso-server.com"  # Base URL of the SSO server
  check-token-endpoint: "/api/check-token"     # Endpoint for token validation
app:
  security:
    public-endpoints:                         # List of public endpoints that don't require authentication
      - "/public/**"
      - "/health"
      - "/info"
```

#### application.properties
```properties
sso.base-endpoint=http://your-sso-server.com
sso.check-token-endpoint=/api/check-token
app.security.public-endpoints=/public/**,/health,/info
```

#### Property Descriptions
- **`sso.base-endpoint`**: The base URL of the SSO server (e.g., `http://your-sso-server.com`). This is used to initialize the `WebClient` for token validation.
- **`sso.check-token-endpoint`**: The specific endpoint on the SSO server to validate JWT tokens (e.g., `/api/check-token`).
- **`app.security.public-endpoints`**: A comma-separated list (in `application.properties`) or a YAML list (in `application.yml`) of endpoints that do not require authentication. These endpoints are accessible without a valid JWT token.

### CORS Configuration
The starter includes a default CORS configuration that allows all origins, methods, and headers with credentials disabled. You can customize this by overriding the `corsConfigurationSource` bean in your `SecurityConfig` class if needed.

Example customization:
```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.addAllowedOrigin("https://your-frontend.com");
    configuration.addAllowedMethod("GET");
    configuration.addAllowedMethod("POST");
    configuration.addAllowedHeader("Authorization");
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}
```

## How It Works
1. **Security Filter Chain**: The `WebSecurityAutoConfiguration` class configures a Spring Security filter chain that:
    - Disables CSRF (suitable for stateless APIs).
    - Enables CORS with the provided configuration.
    - Allows unauthenticated access to public endpoints specified in `app.security.public-endpoints`.
    - Requires authentication for all other endpoints using a JWT token.

2. **SSO Authentication Filter**: The `SsoClientProtectorService` filter:
    - Checks if the requested endpoint is public. If it is, the request proceeds without authentication.
    - For non-public endpoints, extracts the JWT token from the `Authorization` header (expected format: `Bearer <token>`).
    - Sends the token to the SSO server's `check-token-endpoint` for validation.
    - If the token is valid, sets up Spring Security's authentication context with the user details.
    - If the token is invalid or missing, returns a `401 Unauthorized` response with an error message.

3. **Token Validation**: The filter expects the SSO server to return a JSON response with at least `token` and `user_id` fields in the `data` object. If these fields are present and valid, the user is authenticated; otherwise, the request is rejected.

## Example Request
To access a protected endpoint, include the JWT token in the `Authorization` header:

```bash
curl -H "Authorization: Bearer <your-jwt-token>" http://your-app.com/api/protected
```

For public endpoints (e.g., `/health`), no token is required:

```bash
curl http://your-app.com/health
```
---

## Logging
The starter uses SLF4J for logging. Key events (e.g., token validation, endpoint access) are logged at the `DEBUG` level. Ensure your application's logging configuration is set up to capture these logs if needed.

---

## 👥 Integration with SSO Starter

This client starter is designed to integrate directly with the SSO Starter server, allowing seamless validation of authentication tokens and centralized access management across multiple applications.

For more details on the SSO server setup, refer to the main [SSO Starter](https://github.com/aalencarvz1/sso-starter).

---

## Troubleshooting
- **401 Unauthorized**: Ensure the `Authorization` header contains a valid `Bearer <token>`. Verify that the SSO server is reachable and the `sso.check-token-endpoint` is correct.
- **CORS Issues**: Check the `corsConfigurationSource` bean configuration and ensure the frontend origin is allowed if you have customized CORS.
- **Missing Properties**: Ensure all required properties (`sso.base-endpoint`, `sso.check-token-endpoint`, `app.security.public-endpoints`) are defined in your configuration file.
---
## Contributing
For issues, feature requests, or contributions, please contact the starter maintainers or submit a pull request to the repository.

---


## 🧬 Clone the repository

To get started locally:

```bash
git clone https://github.com/sysnormal1/java-spring-sso-client-protector.git
cd java-spring-sso-client-protector
mvn install
```

## 🔧 Build and Local Test

```bash
mvn clean install
```

---

## ⚖️ License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.

---

## 👤 Author

**Alencar Velozo**  
GitHub: [@aalencarvz1](https://github.com/aalencarvz1)

---

> 🔗 Published on [Maven Central (Sonatype)](https://central.sonatype.com/artifact/com.sysnormal.starters.security.sso.spring/client-protector)