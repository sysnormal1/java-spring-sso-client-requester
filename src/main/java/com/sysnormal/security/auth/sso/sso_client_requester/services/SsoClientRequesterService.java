package com.sysnormal.security.auth.sso.sso_client_requester.services;

import com.sysnormal.commons.core.DefaultDataSwap;
import com.sysnormal.commons.core.utils_core.TextUtils;
import com.sysnormal.commons.spring.spring_web_utils.response.ClientRawResponseWrapper;
import com.sysnormal.commons.spring.spring_web_utils.response.ResponseUtils;
import com.sysnormal.security.auth.sso.sso_client_requester.properties.SsoProperties;
import com.sysnormal.security.auth.sso.sso_client_requester.services.jwt.JwtSsoClientRequesterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;

@Service
public class SsoClientRequesterService {
    private static final Logger logger = LoggerFactory.getLogger(SsoClientRequesterService.class);

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    JwtSsoClientRequesterService jwtSsoClientRequesterService;

    public static final int MAX_RECURSIVE_REQUEST_ATTEMPS = 10;

    private final SsoProperties ssoProperties;


    private final WebClient ssoWebClient;

    private String lastToken = null;
    private Instant lastTokenExpirationInstant = null;
    private String lastRefreshToken = null;
    private Instant lastRefreshTokenExpirationInstant = null;

    public SsoClientRequesterService(SsoProperties ssoProperties) {
        this.ssoProperties = ssoProperties;
        this.ssoWebClient = WebClient.create(this.ssoProperties.getBaseEndpoint());
    }

    public boolean checkExpiredToken(DefaultDataSwap dataSwapResult, ClientRawResponseWrapper response) {
        logger.debug("INIT {}.{}",this.getClass().getSimpleName(), "checkExpiredToken");
        boolean result = false;
        if (!dataSwapResult.success && response.clientResponse.statusCode().is4xxClientError()) {
            String message = dataSwapResult.message;
            if ((StringUtils.hasText(message) && message.toLowerCase().contains("expired"))) {
                result = true;
            }
        }
        logger.debug("END {}.{} {}",this.getClass().getSimpleName(), "checkExpiredToken", result);
        return result;
    }

    public DefaultDataSwap refreshToken(
            String refreshToken,
            String agentIdentifier,
            String agentPassword,
            Long domainId
    ) {
        logger.debug("INIT {}.{}",this.getClass().getSimpleName(), "refreshToken");
        DefaultDataSwap result = new DefaultDataSwap();
        try {
            if (TextUtils.hasNotNullText(refreshToken)) {
                Long expirationIn = jwtSsoClientRequesterService.getExpiration(refreshToken);
                if (expirationIn != null && expirationIn * 1000 > System.currentTimeMillis()) {
                    result = refreshTokenOnSso(refreshToken);
                } else {
                    result = loginOnSso(agentIdentifier, agentPassword, domainId);
                }
            } else {
                result = loginOnSso(agentIdentifier, agentPassword, domainId);
            }
        } catch (Exception e) {
            result.setException(e);
        }
        logger.debug("END {}.{}",this.getClass().getSimpleName(), "refreshToken");
        return result;
    }



    public String getToken(String email, String password, Long domainId) throws Exception {
        logger.debug("INIT {}.{}",this.getClass().getSimpleName(), "getToken");
        logger.debug("current token {}, expires in {}, remaining seconds {}", lastToken, lastTokenExpirationInstant, lastTokenExpirationInstant != null ? Duration.between(Instant.now(), lastTokenExpirationInstant).getSeconds() : 0);
        logger.debug("current refreshToken {}, expires in {}, remaining seconds {}", lastRefreshToken, lastRefreshTokenExpirationInstant, lastRefreshTokenExpirationInstant != null ? Duration.between(Instant.now(), lastRefreshTokenExpirationInstant).getSeconds() : 0);
        String result = null;
        if (StringUtils.hasText(lastToken) && lastTokenExpirationInstant != null && Instant.now().isBefore(lastTokenExpirationInstant.minusSeconds(30))) {
            logger.debug("using corrent token");
            result = lastToken;
        } else if (StringUtils.hasText(lastRefreshToken) && lastRefreshTokenExpirationInstant != null && Instant.now().isBefore(lastRefreshTokenExpirationInstant.minusSeconds(30))) {
            logger.debug("using corrent refresh token");
            DefaultDataSwap loginResponse = refreshToken(lastRefreshToken, email, password, domainId);
            if (loginResponse.success) {
                result = lastToken;
            } else {
                throw new Exception(loginResponse.message);
            }
        } else {
            logger.debug("using new login");
            DefaultDataSwap loginResponse = loginOnSso(email, password, domainId);
            if (loginResponse.success) {
                result = lastToken;
            } else {
                throw new Exception(loginResponse.message);
            }
        }
        logger.debug("END {}.{} {}",this.getClass().getSimpleName(), "getToken", result);
        return result;
    }
    public String getToken() throws Exception {
        return getToken(this.ssoProperties.getDefaultEmail(), this.ssoProperties.getDefaultPassword(), null);
    }
    public String getToken(String email, String password) throws Exception {
        return getToken(email, password, null);
    }

    public void handleLoginResult(DefaultDataSwap result) throws Exception {
        logger.debug("INIT {}.{}",this.getClass().getSimpleName(), "handleLoginResult");
        if (result != null) {
            if (result.success) {
                JsonNode jsonData = (JsonNode) result.data;

                //get token
                String token = jsonData.get("token").asText();
                logger.debug("token {}", token);
                if (TextUtils.hasNotNullText(token)) {
                    Long expiresIn = jwtSsoClientRequesterService.getExpiration(token); //seconds
                    logger.debug("expiresIn {}, now millis {}, seconds remaining {}", expiresIn, System.currentTimeMillis(), (expiresIn != null && expiresIn > 0) ? expiresIn - System.currentTimeMillis() / 1000 : "infinit");
                    if (expiresIn == null || (expiresIn != null && expiresIn * 1000 > System.currentTimeMillis())) {
                        this.lastToken = token;
                        this.lastTokenExpirationInstant = expiresIn != null ? Instant.ofEpochSecond(expiresIn) : null;

                        //get refreshToekn
                        this.lastRefreshToken = null;
                        this.lastRefreshTokenExpirationInstant = null;
                        String refreshToken = jsonData.get("refreshToken").asText();
                        logger.debug("refreshToken {}", refreshToken);
                        if (TextUtils.hasNotNullText(refreshToken)) {
                            expiresIn = jwtSsoClientRequesterService.getExpiration(refreshToken);
                            if (expiresIn == null || (expiresIn != null && expiresIn * 1000 > System.currentTimeMillis())) {
                                this.lastRefreshToken = refreshToken;
                                this.lastRefreshTokenExpirationInstant = expiresIn != null ? Instant.ofEpochSecond(expiresIn) : null;
                            }
                        }

                    } else {
                        throw new Exception("token expired");
                    }
                } else {
                    throw new Exception("token is empty");
                }
            } else {
                logger.warn("login result is fail {] {}", result.httpStatusCode, result.message);
            }
        } else {
            logger.warn("login result is null");
        }
        logger.debug("END {}.{}",this.getClass().getSimpleName(), "handleLoginResult");
    }

    public DefaultDataSwap loginOnSso(String email, String password, Long domainId) {
        logger.debug("INIT {}.{}",this.getClass().getSimpleName(), "loginOnSso");
        DefaultDataSwap result = new DefaultDataSwap();
        try {
            String bodyValue = "{" + (domainId != null ? "\"domainId\":"+domainId+"," : "") + "\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
            logger.debug("bodyValue {}", bodyValue);
            ClientRawResponseWrapper response = ssoWebClient.post()
                    .uri(this.ssoProperties.getLoginEndpoint())
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(bodyValue)
                    .exchangeToMono(resp ->
                            resp.bodyToMono(String.class)
                                    .defaultIfEmpty("")     // garante que nunca será null
                                    .map(body -> new ClientRawResponseWrapper(resp, body))
                    )
                    .block();
            result = ResponseUtils.handleResponse(response);
            handleLoginResult(result);
        } catch (Exception e) {
            result.setException(e);
        }
        logger.debug("END {}.{}",this.getClass().getSimpleName(), "loginOnSso");
        return result;
    }
    public DefaultDataSwap loginOnSso() {
        return loginOnSso(this.ssoProperties.getDefaultEmail(), this.ssoProperties.getDefaultPassword(), null);
    }
    public DefaultDataSwap loginOnSso(String email, String password) {
        return loginOnSso(email, password, null);
    }
    public DefaultDataSwap loginOnSso(Long domainId) {
        return loginOnSso(this.ssoProperties.getDefaultEmail(), this.ssoProperties.getDefaultPassword(), domainId);
    }

    public DefaultDataSwap refreshTokenOnSso(String refreshToken) {
        logger.debug("INIT {}.{}",this.getClass().getSimpleName(), "refreshTokenOnSso");
        DefaultDataSwap result = new DefaultDataSwap();
        try {
            String bodyValue = "{\"refreshToken\":\"" + refreshToken + "\"}";
            logger.debug("bodyValue {}", bodyValue);
            ClientRawResponseWrapper response = ssoWebClient.post()
                    .uri(this.ssoProperties.getRefreshTokenEndpoint())
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(bodyValue)
                    .exchangeToMono(resp ->
                            resp.bodyToMono(String.class)
                                    .defaultIfEmpty("")     // garante que nunca será null
                                    .map(body -> new ClientRawResponseWrapper(resp, body))
                    )
                    .block();
            result = ResponseUtils.handleResponse(response);
            handleLoginResult(result);
        } catch (Exception e) {
            result.setException(e);
        }
        logger.debug("END {}.{}",this.getClass().getSimpleName(), "refreshTokenOnSso");
        return result;
    }
    public DefaultDataSwap refreshTokenOnSso() {
        return refreshTokenOnSso(this.lastRefreshToken);
    }
}
