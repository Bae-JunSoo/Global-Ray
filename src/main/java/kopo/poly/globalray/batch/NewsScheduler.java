package kopo.poly.globalray.batch;

import kopo.poly.globalray.service.INewsCollectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NewsScheduler {

    private final INewsCollectService newsCollectService;

    // BATCH_001 : 2시간마다 뉴스 수집 + 즉시 번역/요약 후 저장
    // BATCH_002 제거 : 수집 즉시 번역/요약으로 대체되어 불필요
    @Scheduled(cron = "${scheduler.news.cron}")
    public void collectNews() {
        log.info("===== [BATCH_001] 뉴스 자동 수집 시작 =====");
        try {
            newsCollectService.collectAllCategories();
            log.info("===== [BATCH_001] 뉴스 자동 수집 완료 =====");
        } catch (Exception e) {
            log.error("[BATCH_001] 뉴스 수집 중 오류 : {}", e.getMessage());
        }
    }
}