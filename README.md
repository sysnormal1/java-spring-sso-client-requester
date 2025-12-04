# Sysnormal Sso client requester

This library provides a Spring Security auto-configuration for integrating Single Sign-On (SSO) authentication into your Spring Boot application. It includes a base security configuration and a filter to validate JWT tokens against an SSO server, ensuring secure access to protected endpoints.

This library can also be used as a client implementation of the [SSO Starter](https://github.com/sysnormal1/java-spring-sso-starter), allowing other Java-based APIs or backends to easily integrate into the same authentication ecosystem.

## Features
- Provides methods for login and check token on sso

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
  base-endpoint: value
  login-endpoint: value
  default-email: value
  default-password: value
```


## 👥 Integration with SSO Starter

This client library is designed to integrate directly with the SSO Starter server, allowing seamless validation of authentication tokens and centralized access management across multiple applications.

For more details on the SSO server setup, refer to the main [SSO Starter](https://github.com/sysnormal1/java-spring-sso-starter).

---

## Contributing
For issues, feature requests, or contributions, please contact the starter maintainers or submit a pull request to the repository.

---


## 🧬 Clone the repository

To get started locally:

```bash
git clone https://github.com/sysnormal1/java-spring-sso-client-requester.git
cd java-spring-sso-client-requester
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

> 🔗 Published on [Maven Central (Sonatype)](https://central.sonatype.com/artifact/com.sysnormal.starters.security.sso.spring/client-requester)