package com.cuongpq.template.template;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import static com.cuongpq.template.utils.FileUtil.readTokens;

@Service
@Slf4j
public class Template {

    private static final String KEY = "";

    private final List<String> queryIds;

    Template() {
        queryIds = readTokens(KEY);
    }

    private Map<String, String> getHeaders(String queryId) {
        return Map.of(
                "Telegram-Data", queryId,
                "Origin", "https://cf.seeddao.org",
                "Referer", "https://cf.seeddao.org"
        );
    }

    private void claim(String queryId) {
        try {

        } catch (Exception e) {
            log.error("Fail to claim.");
            log.error(e.getMessage());
        }
    }

    @Scheduled(cron = "0 0 0/4 ? * *")
    @EventListener(ApplicationReadyEvent.class)
    public void claim() {
        new Thread(() -> {
            log.info("================ Start Claim Seed ================");
            queryIds.forEach(this::claim);
            log.info("================ End Claim Seed ================");
        }).start();
    }
}
