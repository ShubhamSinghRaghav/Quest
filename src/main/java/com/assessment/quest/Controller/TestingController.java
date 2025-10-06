package com.assessment.quest.Controller;

import com.assessment.quest.coreservice.S3Service;
import com.assessment.quest.part1.service.SchedulerTask;
import com.assessment.quest.part2.PopulationApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;


@Log4j2
@RestController
@RequestMapping("/testing")
@RequiredArgsConstructor
public class TestingController {

    private final SchedulerTask schedulerTask;
    private final PopulationApiService populationApiService;
    private final S3Service s3Service;

    @PostMapping("/run-sync")
    public ResponseEntity<String> runSyncNow() {
        log.info("Manual sync trigger received.");
        schedulerTask.scheduledMethod();
        return ResponseEntity.ok("Sync completed (manual trigger).");
    }

    @PostMapping("/ingest/usa-population-data")
    public ResponseEntity<?> ingestDataToS3() {
        log.info("Manually ingesting data");
        return ResponseEntity.ok(Map.of("key", populationApiService.fetchAndStorePopulationData()));
    }

    @PostMapping("/presigned-urls")
    public ResponseEntity<?> getPreSignedURLs(@RequestParam String key) {
        return ResponseEntity.ok(s3Service.presignAllUnderPrefixAsMap(key, Duration.ofSeconds(60)));
    }

}
