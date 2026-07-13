package com.mybill.MyBill_Backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class HttpClientConfig {

    @Bean(destroyMethod = "shutdown")
    public ExecutorService outboundHttpExecutor(
            @Value("${app.http-client.threads:2}") int threads
    ) {
        return Executors.newFixedThreadPool(Math.max(1, threads));
    }

    @Bean
    public HttpClient outboundHttpClient(ExecutorService outboundHttpExecutor) {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .executor(outboundHttpExecutor)
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }
}
