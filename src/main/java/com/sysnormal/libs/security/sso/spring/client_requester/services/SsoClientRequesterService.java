package com.sysnormal.libs.security.sso.spring.client_requester.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sysnormal.libs.commons.DefaultDataSwap;
import com.sysnormal.libs.utils.network.http.response.ClientRawResponseWrapper;
import com.sysnormal.libs.utils.network.http.response.ResponseUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

@Service
public class SsoClientRequesterService {
    private static final Logger logger = LoggerFactory.getLogger(SsoClientRequesterService.class);

    private final ObjectMapper mapper = new ObjectMapper();

    public static final int MAX_RECURSIVE_REQUEST_ATTEMPS = 10;

    private final String baseSsoEndpoint;

    private final String ssoLoginEndPoint;

    private final String ssoDefaultEmail;

    private final String ssoDefaultPassword;

    private final WebClient ssoWebClient;

    private String lastToken = null;
    private Instant tokenExpiration = null;

    public SsoClientRequesterService(
            @Value("${sso.base-endpoint}") String baseSsoEndpoint,
            @Value("${sso.login-endpoint}") String ssoLoginEndPoint,
            @Value("${sso.default-email}") String ssoDefaultEmail,
            @Value("${sso.default-password}") String ssoDefaultPassword
    ) {
        this.baseSsoEndpoint = baseSsoEndpoint;
        this.ssoLoginEndPoint = ssoLoginEndPoint;
        this.ssoWebClient = WebClient.create(baseSsoEndpoint);
        this.ssoDefaultEmail = ssoDefaultEmail;
        this.ssoDefaultPassword = ssoDefaultPassword;
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

    public DefaultDataSwap refreshToken() {
        logger.debug("INIT {}.{}",this.getClass().getSimpleName(), "refreshToken");
        DefaultDataSwap result = new DefaultDataSwap();
        try {
            logger.debug("refreshing token (re-login)");
            result = loginOnSso(ssoDefaultEmail, ssoDefaultPassword);
        } catch (Exception e) {
            result.setException(e);
        }
        logger.debug("END {}.{}",this.getClass().getSimpleName(), "refreshToken");
        return result;
    }



    public String getToken(){
        logger.debug("INIT {}.{}",this.getClass().getSimpleName(), "getToken");
        String result = null;
        if (StringUtils.hasText(lastToken) && tokenExpiration != null && Instant.now().isBefore(tokenExpiration.minusSeconds(30))) {
            result = lastToken;
        } else {
            DefaultDataSwap loginResponse = loginOnSso(ssoDefaultEmail, ssoDefaultPassword);
            if (loginResponse.success) {
                result = lastToken;
            }
        }
        logger.debug("END {}.{} {}",this.getClass().getSimpleName(), "getToken", result);
        return result;
    }

    public DefaultDataSwap loginOnSso(String email, String password) {
        logger.debug("INIT {}.{}",this.getClass().getSimpleName(), "loginOnSso");
        DefaultDataSwap result = new DefaultDataSwap();
        try {
            ClientRawResponseWrapper response = ssoWebClient.post()
                    .uri(ssoLoginEndPoint)
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}")
                    .exchangeToMono(resp ->
                            resp.bodyToMono(String.class)
                                    .defaultIfEmpty("")     // garante que nunca será null
                                    .map(body -> new ClientRawResponseWrapper(resp, body))
                    )
                    .block();
            result = ResponseUtils.handleResponse(response);
            if (result.success) {
                JsonNode jsonData = (JsonNode) result.data;
                String token = jsonData.get("token").asText();

                // split token
                String[] parts = token.split("\\.");
                String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
                // parse JSON
                Map<String, Object> payloadMap = mapper.readValue(payload, Map.class);
                // pega o exp
                long expiresIn = ((Number) payloadMap.get("exp")).longValue();
                if (StringUtils.hasText(token) && expiresIn > 0) {
                    this.lastToken = token;
                    this.tokenExpiration = Instant.now().plusSeconds(expiresIn);
                }
            }
        } catch (Exception e) {
            result.setException(e);
        }
        logger.debug("END {}.{}",this.getClass().getSimpleName(), "loginOnSso");
        return result;
    }
}
