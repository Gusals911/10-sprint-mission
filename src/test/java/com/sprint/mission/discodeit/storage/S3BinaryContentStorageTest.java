package com.sprint.mission.discodeit.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sprint.mission.discodeit.storage.s3.S3BinaryContentStorage;
import com.sprint.mission.discodeit.dto.data.BinaryContentDto;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

public class S3BinaryContentStorageTest {

    private S3BinaryContentStorage storage;

    @BeforeEach
    void setUp() {
        Properties props = EnvPropertiesLoader.loadEnv();

        String accessKey = props.getProperty("AWS_S3_ACCESS_KEY");
        String secretKey = props.getProperty("AWS_S3_SECRET_KEY");
        String region = props.getProperty("AWS_S3_REGION");
        String bucket = props.getProperty("AWS_S3_BUCKET");
        long presignedUrlExpirationSeconds = Long.parseLong(
                props.getProperty("AWS_S3_PRESIGNED_URL_EXPIRATION", "600")
        );

        storage = new S3BinaryContentStorage(
                accessKey,
                secretKey,
                region,
                bucket,
                presignedUrlExpirationSeconds
        );
    }

    @Test
    void put_test() {
        UUID id = UUID.randomUUID();
        byte[] content = "hello s3 storage".getBytes(StandardCharsets.UTF_8);

        UUID savedId = storage.put(id, content);

        assertEquals(id, savedId);
    }

    @Test
    void get_test() throws Exception {
        UUID id = UUID.randomUUID();
        String content = "s3 get test content";
        storage.put(id, content.getBytes(StandardCharsets.UTF_8));

        try (InputStream inputStream = storage.get(id)) {
            String loaded = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(content, loaded);
        }
    }

    @Test
    void download_test() {
        UUID id = UUID.randomUUID();
        String content = "download content";
        storage.put(id, content.getBytes(StandardCharsets.UTF_8));

        BinaryContentDto metaData = new BinaryContentDto(
                id,
                "test-file.txt",
                (long) content.getBytes(StandardCharsets.UTF_8).length,
                "text/plain"
        );

        ResponseEntity<?> response = storage.download(metaData);

        assertNotNull(response);
        assertEquals(HttpStatusCode.valueOf(302), response.getStatusCode());
        assertTrue(response.getHeaders().containsKey(HttpHeaders.LOCATION));
        assertNotNull(response.getHeaders().getFirst(HttpHeaders.LOCATION));
    }
}
