package com.assessment.quest.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "bls.http")
public class BlsHttpProperties {
    private String userAgent;
    private String referer;
    private String accept;
    private String acceptEncoding;
    private int connectTimeout;
    private int readTimeout;
}
