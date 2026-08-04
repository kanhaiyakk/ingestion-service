package com.intentwise.ingestion.sink;

/**
 * Puts a blob at a bucket+key location. The seam between {@link S3Sink} and
 * wherever the bytes actually land — a real S3 client is just another
 * implementation of this interface; neither {@link S3Sink} nor the engine
 * change to support it.
 */
public interface ObjectStoreClient {

    void put(String bucket, String key, byte[] content);
}
