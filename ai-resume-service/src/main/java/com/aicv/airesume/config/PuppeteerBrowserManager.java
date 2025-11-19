package com.aicv.airesume.config;

import java.util.Arrays;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.stereotype.Component;

import com.ruiyun.jvppeteer.api.core.Browser;
import com.ruiyun.jvppeteer.cdp.core.Puppeteer;
import com.ruiyun.jvppeteer.cdp.entities.LaunchOptions;
import com.ruiyun.jvppeteer.common.Product;

@Component
public class PuppeteerBrowserManager {
    
    private Browser browser;

    @PostConstruct
    public void init() throws Exception {
        LaunchOptions options = LaunchOptions.builder()
                .headless(true)
                .executablePath(System.getenv("PUPPETEER_EXECUTABLE_PATH")) // 关键点：用容器里安装的 Chromium
                .args(Arrays.asList("--no-sandbox", "--disable-setuid-sandbox", "--disable-dev-shm-usage"))
                .product(Product.Chromium)
                .build();
        browser = Puppeteer.launch(options);

        System.out.println("Browser 已启动！");
    }

    public Browser getBrowser() {
        return browser;
    }

    @PreDestroy
    public void close() throws Exception {
        if (browser != null) {
            browser.close();
            System.out.println("Browser 已关闭！");
        }
    }
}
