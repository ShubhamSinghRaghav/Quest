package com.assessment.quest.part1.service;

import com.assessment.quest.part1.model.BLSFileMetadata;
import com.assessment.quest.part1.utils.HtmlParser;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.util.*;

@Log4j2
@Component
public class ComputeExternalSiteMetaService {

    private final String blsBaseUrl;
    private final RestTemplate restTemplate;

    public ComputeExternalSiteMetaService(@Value("${bls.base.url}") String blsBaseUrl,
                                          RestTemplate restTemplate) {
        this.blsBaseUrl = blsBaseUrl;
        this.restTemplate = restTemplate;
    }

    @Retryable(
            value = { HttpClientErrorException.Forbidden.class, RestClientException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 60000, multiplier = 2.0)
    )
    public List<BLSFileMetadata> buildMeta() {
        log.info("Fetching BLS listing: {}", blsBaseUrl);

        try {
            ResponseEntity<byte[]> resp = restTemplate.exchange(
                    blsBaseUrl, HttpMethod.GET, HttpEntity.EMPTY, byte[].class
            );

            if (!resp.getStatusCode().is2xxSuccessful()) {
                throw new RestClientException("Unexpected HTTP status: " + resp.getStatusCode());
            }

            byte[] body = Optional.of(resp.getBody()).orElse(new byte[0]);
            String html = new String(body, java.nio.charset.StandardCharsets.UTF_8);
            return HtmlParser.parse(html);
        } catch (HttpClientErrorException.Forbidden e) {
            log.error("BLS blocked (403). Respect 30–60s between automated requests.");
            throw e;
        } catch (Exception e) {
            log.error("Fetch failed: {}", e.getMessage(), e);
            throw e;
        }
    }

}
