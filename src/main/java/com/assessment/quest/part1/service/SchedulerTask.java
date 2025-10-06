package com.assessment.quest.part1.service;

import com.assessment.quest.coreservice.S3Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SchedulerTask {

    private final ComputeExternalSiteMetaService metaService;
    private final S3Service s3Service;

    public SchedulerTask(ComputeExternalSiteMetaService metaService, S3Service s3Service) {
        this.metaService = metaService;
        this.s3Service = s3Service;
    }


//    @Scheduled(cron = "${scheduler.cron.expression}")
    public void scheduledMethod() {
        s3Service.syncToS3(metaService.buildMeta());
    }
}
