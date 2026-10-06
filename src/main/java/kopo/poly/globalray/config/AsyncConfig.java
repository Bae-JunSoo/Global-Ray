package kopo.poly.globalray.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

// 뉴스 본문 크롤링 병렬 처리용 스레드풀 (스케줄러가 돌 때마다 새로 만들지 않고 Bean 하나를 재사용)
@Configuration
public class AsyncConfig {

    @Bean(name = "crawlExecutor")
    public ThreadPoolTaskExecutor crawlExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 동시에 너무 많이 요청하면 언론사 사이트에서 봇으로 차단될 수 있어 5개로 제한
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("crawl-");
        return executor;
    }
}
