package com.assessment.quest.part2;

import com.assessment.quest.config.DataUsaPopulationProps;
import com.assessment.quest.coreservice.S3Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
@Log4j2
public class PopulationApiService {

    private final RestTemplate restTemplate;
    private final S3Service s3Service;
    private final DataUsaPopulationProps props;
    private final ObjectMapper objectMapper;

    public PopulationApiService(@Qualifier("simpleRestTemplate") RestTemplate restTemplate, S3Service s3Service,
                                DataUsaPopulationProps props, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.s3Service = s3Service;
        this.props = props;
        this.objectMapper = objectMapper;
    }

    public String fetchAndStorePopulationData() {
        log.info("Calling DataUSA API: {}", props.getUrl());
        ResponseEntity<String> resp = restTemplate.exchange(props.getUrl(), HttpMethod.GET, HttpEntity.EMPTY, String.class);
        if (!resp.getStatusCode().is2xxSuccessful()) {
            throw new RestClientException("DataUSA API returned HTTP " + resp.getStatusCode());
        }
        String body = resp.getBody();
        if (body.isBlank()) {
            throw new RestClientException("DataUSA API returned empty body");
        }

        try {
            JsonNode root = objectMapper.readTree(body);
            if (!root.hasNonNull("data")) {
                throw new IllegalStateException("Unexpected JSON: missing 'data' field");
            }
        } catch (Exception e) {
            throw new RestClientException("Invalid JSON from DataUSA API", e);
        }

        String key = props.getS3KeyPrefix().concat("/").concat(props.getS3Filename());
        s3Service.putJson(props.getS3Bucket(), key, body);
        log.info("Uploaded DataUSA population JSON to s3://{}/{}/{}", props.getS3Bucket(), props.getS3KeyPrefix(), props.getS3Filename());
        return key;
    }

}
