package com.assessment.quest.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class WebConfig {

    @Value("${bls.http.user-agent}")
    private String userAgent;

    @Value("${bls.http.referer}")
    private String referer;

    @Value("${bls.http.accept}")
    private String accept;

    @Value("${bls.http.accept-encoding}")
    private String acceptEncoding;

    private final BlsHttpProperties props;

    public WebConfig(BlsHttpProperties props) {
        this.props = props;
    }

    @Bean
    public RestTemplate restTemplate() {
        HttpClient jdkClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        RestTemplate rt = new RestTemplate(new JdkClientHttpRequestFactory(jdkClient));

        rt.getInterceptors().add((req, body, exec) -> {
            req.getHeaders().set("User-Agent", props.getUserAgent());
            req.getHeaders().set("Referer", props.getReferer());
            req.getHeaders().set("Accept", props.getAccept());
            return exec.execute(req, body);
        });

        return rt;
    }

    @Bean("simpleRestTemplate")
    public RestTemplate simpleRestTemplate() {
        return new RestTemplate();
    }
}