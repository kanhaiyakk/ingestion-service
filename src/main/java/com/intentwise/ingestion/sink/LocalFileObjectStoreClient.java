package com.intentwise.ingestion.sink;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Local-disk stand-in for a real object store: writes each put under a root
 * directory, mirroring an S3 bucket/key layout as a plain file tree. Meant
 * for local development and this demo, not production.
 */
@Component
public class LocalFileObjectStoreClient implements ObjectStoreClient {

    private final Path rootDir;

    public LocalFileObjectStoreClient(@Value("${ingestion.object-store.root:object-store}") String rootDir) {
        this.rootDir = Path.of(rootDir);
    }

    @Override
    public void put(String bucket, String key, byte[] content) {
        try {
            Path target = rootDir.resolve(bucket).resolve(key);
            Files.createDirectories(target.getParent());
            Files.write(target, content);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write object store entry " + bucket + "/" + key, e);
        }
    }
}
