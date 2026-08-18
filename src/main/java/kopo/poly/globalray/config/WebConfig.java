package kopo.poly.globalray.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
                .codecs(config -> config.defaultCodecs()
                        .maxInMemorySize(5 * 1024 * 1024)); // 5MB
    }

    /**
     * Spring Boot 의 JacksonAutoConfiguration 이 ObjectMapper Bean 을 자동 등록하지만
     * 환경(EC2, 특정 Spring Boot 버전)에 따라 감지되지 않는 경우가 있어 명시적으로 선언
     * NewsCollectServiceImpl 생성자 4번째 파라미터로 주입됨
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}