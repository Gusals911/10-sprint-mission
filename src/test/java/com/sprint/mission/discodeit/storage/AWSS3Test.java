package com.sprint.mission.discodeit.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Properties;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

public class AWSS3Test {

    private final Properties props = EnvPropertiesLoader.loadEnv();

    private final String accessKey = props.getProperty("AWS_S3_ACCESS_KEY");
    private final String secretKey = props.getProperty("AWS_S3_SECRET_KEY");
    private final String region = props.getProperty("AWS_S3_REGION");
    private final String bucket = props.getProperty("AWS_S3_BUCKET");
    private final long presignedUrlExpirationSeconds = Long.parseLong(
            props.getProperty("AWS_S3_PRESIGNED_URL_EXPIRATION", "600")
    );

    private S3Client s3Client;
    private S3Presigner s3Presigner;

    @BeforeEach
    void setUp() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        Region awsRegion = Region.of(region);

        s3Client = S3Client.builder()
                .region(awsRegion)
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();

        s3Presigner = S3Presigner.builder()
                .region(awsRegion)
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }

    @Test
    void upload_test() {
        String key = "test-upload-" + UUID.randomUUID() + ".txt";
        String content = "hello s3";

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType("text/plain")
                .build();

        PutObjectResponse response = s3Client.putObject(
                request,
                RequestBody.fromString(content, StandardCharsets.UTF_8)
        );

        assertNotNull(response);
        assertEquals(HttpStatusCode.OK, response.sdkHttpResponse().statusCode());
        assertNotNull(response.eTag());
    }

    @Test
    void download_test() {
        String key = "test-download-" + UUID.randomUUID() + ".txt";
        String content = "download test content";

        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType("text/plain")
                        .build(),
                RequestBody.fromString(content, StandardCharsets.UTF_8)
        );

        ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(
                GetObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .build()
        );

        String downloaded = response.asString(StandardCharsets.UTF_8);

        assertEquals(content, downloaded);
    }

    @Test
    void PresignedUrl_create_test() {
        String key = "test-presigned-" + UUID.randomUUID() + ".txt";
        String content = "presigned url test";

        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType("text/plain")
                        .build(),
                RequestBody.fromString(content, StandardCharsets.UTF_8)
        );

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(presignedUrlExpirationSeconds))
                .getObjectRequest(getObjectRequest)
                .build();

        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
        URL url = presignedRequest.url();

        assertNotNull(url);
        assertTrue(url.toString().contains(bucket));
        assertFalse(url.toString().isBlank());
    }
}
