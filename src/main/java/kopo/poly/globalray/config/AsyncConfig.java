package kopo.poly.globalray.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 뉴스 크롤링 병렬 처리용 스레드풀 Bean 설정
 *
 * [이전 문제]
 * NewsCollectServiceImpl.collectByCategory() 가 호출될 때마다
 * Executors.newFixedThreadPool(5) 로 스레드풀을 새로 생성하고 사용 후 shutdown()
 * → 2시간마다 스케줄러가 실행될 때마다 스레드 5개를 생성/소멸 반복
 * → JVM 스레드 생성 비용 낭비, GC 부담 증가
 *
 * [해결]
 * ThreadPoolTaskExecutor 를 Spring Bean 으로 등록해서 애플리케이션 시작 시 1회 생성
 * 이후 모든 스케줄러 실행에서 동일한 스레드풀을 재사용
 * → 스레드 생성/소멸 오버헤드 제거
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "crawlExecutor")
    public ThreadPoolTaskExecutor crawlExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 크롤링 병렬 스레드 수: 너무 많으면 대상 사이트에서 봇 차단 당할 수 있어 5개로 제한
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(5);

        // 스레드가 모두 사용 중일 때 최대 100개 요청까지 대기 허용
        executor.setQueueCapacity(100);

        // 스레드 이름 접두사: 로그에서 크롤링 스레드 구분용
        executor.setThreadNamePrefix("crawl-");

        // Bean 등록 완료 후 initialize() 호출 필수 (Spring 컨테이너 외부에서 생성 시)
        executor.initialize();

        return executor;
    }
}
