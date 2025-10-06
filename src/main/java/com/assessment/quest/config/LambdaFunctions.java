package com.assessment.quest.config;

import com.assessment.quest.coreservice.S3Service;
import com.assessment.quest.part1.service.ComputeExternalSiteMetaService;
import com.assessment.quest.part2.PopulationApiService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.function.Function;

@Configuration
public class LambdaFunctions {

    private final S3Service s3;
    private final ComputeExternalSiteMetaService blsMeta;
    private final PopulationApiService population;

    public LambdaFunctions(S3Service s3,
                           ComputeExternalSiteMetaService blsMeta,
                           PopulationApiService population) {
        this.s3 = s3;
        this.blsMeta = blsMeta;
        this.population = population;
    }


    @Bean
    public Function<Map<String,Object>, String> ingest() {
        return event -> {
            var files = blsMeta.buildMeta();
            s3.syncToS3(files);
            population.fetchAndStorePopulationData();
            return "Ingest OK: bls=" + files.size();
        };
    }
}

