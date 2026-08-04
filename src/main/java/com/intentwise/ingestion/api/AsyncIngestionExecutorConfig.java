package com.intentwise.ingestion.api;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Bounded thread pool that runs triggered ingestions off the HTTP request thread. */
@Configuration
public class AsyncIngestionExecutorConfig {

    @Bean(destroyMethod = "shutdown")
    public ExecutorService ingestionExecutor() {
        return new ThreadPoolExecutor(2, 4, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(50));
    }
}
