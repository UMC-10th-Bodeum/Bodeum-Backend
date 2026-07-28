package com.bodeum.domain.news.collector.selenium;

import com.bodeum.domain.news.collector.NewsCandidate;
import com.bodeum.domain.news.collector.NewsCollector;
import com.bodeum.domain.news.entity.NewsSourceType;
import com.bodeum.domain.news.entity.NewsSource;
import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
public class SeleniumNewsCollector implements NewsCollector {

    private static final long PAGE_LOAD_TIMEOUT_SECONDS = 10;

    @Override
    public NewsSourceType getSourceType() {
        return NewsSourceType.DYNAMIC_CRAWLING;
    }

    @Override
    public List<NewsCandidate> collect(NewsSource newsSource) {
        String listUrl = newsSource.getListUrl();

        if (listUrl == null || listUrl.isBlank()) {
            log.warn("동적 뉴스 크롤링 URL이 비어 있습니다. newsSourceId={}", newsSource.getId());
            return List.of();
        }

        WebDriverManager.chromedriver().setup();

        ChromeOptions options = createChromeOptions();
        WebDriver driver = new ChromeDriver(options);

        try {
            driver.manage()
                    .timeouts()
                    .pageLoadTimeout(Duration.ofSeconds(PAGE_LOAD_TIMEOUT_SECONDS));

            driver.get(listUrl);

            return parse(driver);
        } catch (RuntimeException e) {
            log.warn("동적 뉴스 크롤링에 실패했습니다. newsSourceId={}, listUrl={}", newsSource.getId(), listUrl, e);
            return List.of();
        } finally {
            driver.quit();
        }
    }

    private ChromeOptions createChromeOptions() {
        ChromeOptions options = new ChromeOptions();

        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");

        return options;
    }

    private List<NewsCandidate> parse(WebDriver driver) {
        // TODO: 동적 렌더링 완료 대기 후 사이트별 selector 전략 적용 예정
        // 예: driver.findElements(By.cssSelector(".notice-list li"))
        return List.of();
    }
}
