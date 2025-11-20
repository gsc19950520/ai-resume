package com.aicv.airesume.config;

import java.util.Arrays;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.stereotype.Component;

import com.ruiyun.jvppeteer.api.core.Browser;
import com.ruiyun.jvppeteer.api.core.Page;
import com.ruiyun.jvppeteer.cdp.core.Puppeteer;
import com.ruiyun.jvppeteer.cdp.entities.LaunchOptions;
import com.ruiyun.jvppeteer.common.Product;

@Component
public class PuppeteerBrowserManager {
    
    private Browser browser;
    private final Object lock = new Object();

    @PostConstruct
    public void init() throws Exception {
        synchronized (lock) {
            if (browser != null) {
                return;
            }
            
            String chromiumPath = System.getenv().getOrDefault("CHROMIUM_PATH", "/usr/bin/chromium");
            System.out.println("使用Chromium路径: " + chromiumPath);
            
            LaunchOptions options = LaunchOptions.builder()
                    .headless(true)
                    .executablePath(chromiumPath)
                    .product(Product.Chromium)
                    .args(Arrays.asList(
                        "--no-sandbox",
                        "--disable-setuid-sandbox", 
                        "--disable-dev-shm-usage",
                        "--disable-gpu",
                        "--disable-software-rasterizer",
                        "--disable-web-security",
                        "--disable-features=VizDisplayCompositor",
                        "--disable-background-timer-throttling",
                        "--disable-backgrounding-occluded-windows",
                        "--disable-renderer-backgrounding",
                        "--disable-field-trial-config",
                        "--disable-ipc-flooding-protection",
                        "--no-first-run",
                        "--no-default-browser-check",
                        "--disable-default-apps",
                        "--disable-translate",
                        "--disable-extensions",
                        "--remote-debugging-port=0",
                        "--user-data-dir=/tmp/chrome-user-data",
                        "--single-process",
                        "--memory-pressure-off"
                    )).build();

            try {
                browser = Puppeteer.launch(options);
                System.out.println("Browser 已成功启动！");
                
                // 测试浏览器是否正常工作
                testBrowser();
                
            } catch (Exception e) {
                System.err.println("Browser 启动失败: " + e.getMessage());
                e.printStackTrace();
                throw e;
            }
        }
    }
    
    private void testBrowser() throws Exception {
        // 创建一个临时页面测试浏览器是否正常工作
        Page page = browser.newPage();
        try {
            page.goTo("data:text/html,<h1>Test Page</h1>");
            System.out.println("Browser 测试通过");
        } finally {
            page.close();
        }
    }

    public Browser getBrowser() {
        if (browser == null) {
            throw new IllegalStateException("Browser 未初始化");
        }
        return browser;
    }
    
    public boolean isBrowserReady() {
        return browser != null;
    }

    @PreDestroy
    public void close() throws Exception {
        synchronized (lock) {
            if (browser != null) {
                try {
                    browser.close();
                    System.out.println("Browser 已关闭！");
                } catch (Exception e) {
                    System.err.println("关闭Browser时出错: " + e.getMessage());
                } finally {
                    browser = null;
                }
            }
        }
    }
}
