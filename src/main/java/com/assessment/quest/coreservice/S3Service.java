package com.assessment.quest.coreservice;

import com.assessment.quest.part1.model.BLSFileMetadata;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Log4j2
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner presigner;
    private final String blsManifestKey;
    private final String blsS3Bucket;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final String blsBaseUrl;
    private final String blsPrefixKey;

    public S3Service(S3Client s3Client,
                     S3Presigner presigner, @Value("${bls.bucket.manifest.key}") String blsManifestKey,
                     @Value("${bls.bucket.name}") String blsS3Bucket,
                     ObjectMapper objectMapper, RestTemplate restTemplate,
                     @Value("${bls.base.url}") String blsBaseUrl, @Value("${bls.part1.prefix}") String blsPrefixKey) {
        this.s3Client = s3Client;
        this.presigner = presigner;
        this.blsManifestKey = blsManifestKey;
        this.blsS3Bucket = blsS3Bucket;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
        this.blsBaseUrl = blsBaseUrl;
        this.blsPrefixKey = blsPrefixKey;
    }

    public void syncToS3(List<BLSFileMetadata> fileMetadataFromSource) {
        log.info("Started Sync ... ");
        Map<String, BLSFileMetadata> manifestCache = loadManifestFromS3();
        Map<String, BLSFileMetadata> remoteMap = fileMetadataFromSource.stream()
                .collect(Collectors.toMap(BLSFileMetadata::getName, f -> f));

        for (BLSFileMetadata file : fileMetadataFromSource) {
            if (shouldUpload(manifestCache.get(file.getName()), file)) {
                try {
                    byte[] bytes = downloadFile(file.getName());
                    uploadObject(file.getName(), bytes);
                    manifestCache.put(file.getName(),
                            new BLSFileMetadata(file.getName(), file.getSize(), file.getLastModified()));
                } catch (Exception e) {
                    log.error("Upload failed for " + file.getName() + ": " + e.getMessage());
                }
            } else {
                log.info("Skipping unchanged: " + file.getName());
            }
        }

        Set<String> remoteKeys = remoteMap.keySet();
        manifestCache.keySet().removeIf(key -> {
            if (!remoteKeys.contains(key)) {
                deleteObject(key);
                return true;
            }
            return false;
        });

        uploadManifestToS3(manifestCache);
    }

    private void uploadManifestToS3(Map<String, BLSFileMetadata> manifestCache) {
        try {
            String json = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(manifestCache);
            uploadObject(blsManifestKey, json.getBytes());
            log.info("Manifest updated successfully");
        } catch (Exception e) {
            log.error("Failed to upload manifest: " + e.getMessage());
        }
    }


    private boolean shouldUpload(BLSFileMetadata existingMetadata, BLSFileMetadata metadataFromSource) {
        if (existingMetadata == null) {
            log.info("New file: " + metadataFromSource.getName());
            return true;
        }

        boolean sizeChanged = existingMetadata.getSize() != metadataFromSource.getSize();
        boolean dateChanged = existingMetadata.getLastModified() != (metadataFromSource.getLastModified());

        if (sizeChanged || dateChanged) {
            log.info("Changed file detected: " + metadataFromSource.getName());
            return true;
        }
        return false;
    }

    public byte[] downloadFile(String fileName) {
        HttpHeaders headers = new HttpHeaders();
        buildDownloadHeaders(headers);
        String url = blsBaseUrl + fileName;
        ResponseEntity<byte[]> resp = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), byte[].class);
        if (!resp.getStatusCode().is2xxSuccessful()) {
            throw new RestClientException("Download failed " + resp.getStatusCode() + " for " + url);
        }
        return Optional.of(resp.getBody()).orElse(new byte[0]);
    }

    public void uploadObject(String key, byte[] content) {
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(blsS3Bucket).key(getBaseKey(key)).build(),
                RequestBody.fromBytes(content)
        );
    }

    public void deleteObject(String key) {
        log.info("Deleting Object :: key " + key);
        s3Client.deleteObject(DeleteObjectRequest.builder().bucket(blsS3Bucket).key(key).build());
    }

    public void putJson(String bucket, String key, String json) {
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType("application/json")
                            .cacheControl("no-cache")
                            .build(),
                    RequestBody.fromBytes(json.getBytes(StandardCharsets.UTF_8))
            );
            log.info("Uploaded JSON to s3://{}/{}", bucket, key);
        } catch (S3Exception e) {
            log.error("Failed to upload JSON to s3://{}/{}: {}", bucket, key, e.awsErrorDetails().errorMessage(), e);
            throw e;
        }
    }

    public Map<String, String> presignAllUnderPrefixAsMap(String prefix, Duration ttl) {
        ListObjectsV2Response list = s3Client.listObjectsV2(
                ListObjectsV2Request.builder()
                        .bucket(blsS3Bucket)
                        .prefix(prefix)
                        .build());

        return list.contents().stream()
                .map(S3Object::key)
                .filter(k -> !k.endsWith("/"))                                  // skip folder keys
                .collect(Collectors.toMap(
                        k -> k.substring(k.lastIndexOf('/') + 1),
                        k -> presignDownload(blsS3Bucket, k, ttl),
                        (u1, u2) -> u1
                ));
    }


    public String presignDownload(String bucket, String key, Duration ttl) {
        GetObjectRequest get = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .responseContentDisposition("attachment; filename=\"" + key.substring(key.lastIndexOf('/')+1) + "\"")
                .build();

        GetObjectPresignRequest req = GetObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .getObjectRequest(get)
                .build();

        return presigner.presignGetObject(req).url().toString();
    }

    private Map<String, BLSFileMetadata> loadManifestFromS3() {
        try {
            return s3Client.getObject(
                    GetObjectRequest.builder()
                            .bucket(blsS3Bucket)
                            .key(getBaseKey(blsManifestKey))
                            .build(),
                    (resp, in) -> {
                        try {
                            return objectMapper.readValue(in, new TypeReference<>() {});
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to parse manifest", e);
                        }
                    });
        } catch (NoSuchKeyException e) {
            log.warn("No manifest found, starting fresh");
            return new HashMap<>();
        } catch (Exception e) {
            log.error("Error reading manifest: " + e.getMessage());
            return new HashMap<>();
        }
    }

    private void buildDownloadHeaders(HttpHeaders headers) {
        headers.set(HttpHeaders.REFERER, blsBaseUrl);
        headers.set(HttpHeaders.ACCEPT, "*/*");
        headers.set(HttpHeaders.CONNECTION, "keep-alive");
    }

    private String getBaseKey(String key) {
        return blsPrefixKey.concat("/").concat(key);
    }
}
