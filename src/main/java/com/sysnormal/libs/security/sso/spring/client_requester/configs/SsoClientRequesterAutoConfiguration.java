package com.sysnormal.libs.security.sso.spring.client_requester.configs;

import com.sysnormal.libs.security.sso.spring.client_requester.services.SsoClientRequesterService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@ConditionalOnClass(name = "org.springframework.boot.SpringApplication")
@Import({
        SsoClientRequesterService.class
})
public class SsoClientRequesterAutoConfiguration {}
