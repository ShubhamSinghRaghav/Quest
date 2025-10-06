package com.assessment.quest.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "datausa.population")
public class DataUsaPopulationProps {
    private String url;
    private String s3Bucket;
    private String s3KeyPrefix;
    private String s3Filename;
}