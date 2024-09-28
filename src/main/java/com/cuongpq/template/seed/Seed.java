package com.cuongpq.template.seed;

import com.cuongpq.template.utils.CommonUtil;
import com.cuongpq.template.utils.DateUtil;
import com.cuongpq.template.utils.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.cuongpq.template.utils.DateUtil.FORMAT_HOUR_MINUTE_DAY_MONTH_YEAR;
import static com.cuongpq.template.utils.FileUtil.readTokens;

@Service
@Slf4j
public class Seed {

    private static final String KEY = "seed.query-id";

    private final List<String> queryIds;

    Seed() {
        queryIds = readTokens(KEY);
    }

    private Map<String, String> getHeaders(String queryId) {
        return Map.of(
                "Telegram-Data", queryId,
                "Origin", "https://cf.seeddao.org",
                "Referer", "https://cf.seeddao.org"
        );
    }

    record Data(String id, String user_id, Date timestamp, Long amount) {
    }

    record Info(String message, Data data) {
    }

    private void claim(String queryId) {
        try {
            String url = "https://elb.seeddao.org/api/v1/seed/claim";
            String res = HttpClientUtil.sendPost(url, new JSONObject(), getHeaders(queryId));
            Info info = CommonUtil.fromJson(res, Info.class);
            if (info != null && info.data != null) {
                log.info(StringUtils.hasText(info.data.id) ? "Claim seed successfully." : info.message);
            } else {
                log.error("Fail to claim seed.");
            }
        } catch (Exception e) {
            log.error("Fail to claim seed.");
            log.error(e.getMessage());
        }
    }

    record Worm(String type, Date next_worm, Boolean is_caught, String status) {
    }

    record CatchWorm(Worm data) {
    }

    private void worms(String queryId) {
        try {
            String url = "https://elb.seeddao.org/api/v1/worms";
            String res = HttpClientUtil.sendGet(url, getHeaders(queryId));
            CatchWorm catchWorm = CommonUtil.fromJson(res, CatchWorm.class);
            if (catchWorm != null && catchWorm.data != null) {
                Worm data = catchWorm.data;
                if (!data.is_caught) {
                    catchWorm(queryId);
                } else if (data.next_worm != null) {
                    log.info("You have already caught the worm. Wait till " + DateUtil.toString(data.next_worm, FORMAT_HOUR_MINUTE_DAY_MONTH_YEAR));
                    long timeLeft = data.next_worm.getTime() - DateUtil.getCurrentMillis();
                    try {
                        Thread.sleep(timeLeft + 10000);
                        worms(queryId);
                    } catch (Exception e) {
                        log.error("Fail to wait to catch worm.");
                        log.error(e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Fail to get worms info.");
            log.error(e.getMessage());
        }
    }

    private void catchWorm(String queryId) {
        try {
            String url = "https://elb.seeddao.org/api/v1/worms/catch";
            String res = HttpClientUtil.sendPost(url, new JSONObject(), getHeaders(queryId));
            CatchWorm catchWorm = CommonUtil.fromJson(res, CatchWorm.class);
            if (catchWorm != null && catchWorm.data != null) {
                Worm worm = catchWorm.data;
                if (Objects.equals(worm.status, "successful")) {
                    log.info(String.format("You caught %s worm.", worm.type));
                } else {
                    log.info(String.format("Oops. The %s worm run away.", worm.type));
                }
            }
        } catch (Exception e) {
            log.error("Fail to catch worm.");
            log.error(e.getMessage());
        }
    }

    private void claimDaily(String queryId) {
        try {
            String url = "https://elb.seeddao.org/api/v1/login-bonuses";
            String res = HttpClientUtil.sendPost(url, new JSONObject(), getHeaders(queryId));
            Info info = CommonUtil.fromJson(res, Info.class);
            log.info(info.message);
        } catch (Exception e) {
            log.error("Fail to claim daily.");
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

    @EventListener(ApplicationReadyEvent.class)
    public void catchWorm() {
        new Thread(() -> {
            log.info("================ Start Catch Worm Seed ================");
            queryIds.forEach(this::worms);
            log.info("================ End Catch Worm Seed ================");
        }).start();
    }

    @Scheduled(cron = "0 10 7 ? * *")
    @EventListener(ApplicationReadyEvent.class)
    public void claimDaily() {
        new Thread(() -> {
            log.info("================ Start Claim Daily Seed ================");
            queryIds.forEach(this::claimDaily);
            log.info("================ End Claim Daily Seed ================");
        }).start();
    }
}
