package com.example.demo.spring.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ReactorClientHttpRequestFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.json.JacksonJsonDecoder;
import org.springframework.http.codec.json.JacksonJsonEncoder;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * RestClient和WebClient配置类
 * 配置连接池，超时时间，编解码器，默认请求头，日志打印
 */
@Configuration
@Slf4j
public class HttpClientConfig {
    // 连接池连接数
    private static final int MAX_CONNECTION = 100;
    // 连接超时时间
    private static final int CONNECT_TIMEOUT_MILLIS = 2000;
    // 整个响应超时时间
    private static final int RESPONSE_TIMEOUT_MILLIS = 10_000;
    // 读取超时时间（两次读取间隔）
    private static final int READ_TIMEOUT_MILLIS = 10_000;
    // 写入超时时间（两次写入间隔）
    private static final int WRITE_TIMEOUT_MILLIS = 10_000;

    /**
     * HTTP 连接池配置
     * @return 连接池
     */
    @Bean
    public ConnectionProvider connectionProvider() {
        return ConnectionProvider
                .builder("http-client")
                // 最大连接数
                .maxConnections(MAX_CONNECTION)
                .build();
    }

    /**
     * HTTP 客户端配置
     * @param connectionProvider 连接池
     * @return HttpClient
     */
    @Bean
    public HttpClient httpClient(ConnectionProvider connectionProvider) {
        return HttpClient.create(connectionProvider)
                // 连接超时
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT_MILLIS)
                // 整个响应超时
                .responseTimeout(Duration.ofMillis(RESPONSE_TIMEOUT_MILLIS))
                // 读写超时
                .doOnConnected(connection -> connection
                        .addHandlerLast(new ReadTimeoutHandler(READ_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(WRITE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)));
    }

    /**
     * RestClient 构建器
     * @param httpClient HttpClient
     * @param jsonMapper JsonMapper
     * @return RestClient.Builder
     */
    @Bean
    public RestClient.Builder restClientBuilder(HttpClient httpClient, JsonMapper jsonMapper) {
        return RestClient.builder()
                // 请求工厂
                .requestFactory(new ReactorClientHttpRequestFactory(httpClient))
                // 编解码器
                .configureMessageConverters(converters ->
                        converters.registerDefaults().withJsonConverter(new JacksonJsonHttpMessageConverter(jsonMapper))
                )
                // 默认请求头
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.ALL_VALUE)
                // 日志拦截器
                .requestInterceptor((request, body, execution) -> {
                    log.info("Rest client: {} {}", request.getMethod(), request.getURI());
                    return execution.execute(request, body);
                });
    }

    /**
     * RestClient 实例
     * @param restClientBuilder 构建器
     * @return RestClient
     */
    @Bean
    public RestClient restClient(RestClient.Builder restClientBuilder) {
        return restClientBuilder.build();
    }

    /**
     * WebClient 构建器
     * @param httpClient HttpClient
     * @param jsonMapper JsonMapper
     * @return WebClient.Builder
     */
    @Bean
    public WebClient.Builder webClientBuilder(HttpClient httpClient, JsonMapper jsonMapper) {
        return WebClient.builder()
                // 请求工厂
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                // 编解码器
                .codecs(configurer -> configurer.defaultCodecs().jacksonJsonEncoder(new JacksonJsonEncoder(jsonMapper)))
                .codecs(configurer -> configurer.defaultCodecs().jacksonJsonDecoder(new JacksonJsonDecoder(jsonMapper)))
                // 默认请求头
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.ALL_VALUE)
                // 日志过滤器
                .filter((request, next) -> {
                    log.info("Web client: {} {}", request.method(), request.url());
                    return next.exchange(request);
                });
    }

    /**
     * WebClient 实例
     * @param webClientBuilder 构建器
     * @return WebClient
     */
    @Bean
    public WebClient webClient(WebClient.Builder webClientBuilder) {
        return webClientBuilder.build();
    }
}
