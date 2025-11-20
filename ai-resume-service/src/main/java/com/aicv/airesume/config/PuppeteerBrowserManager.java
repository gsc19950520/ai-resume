package com.aicv.airesume.config;

import java.io.File;
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
            
            String chromiumPath = "/usr/bin/chromium";
            System.out.println("使用浏览器路径: " + chromiumPath);
            
            // 方法1：尝试设置系统属性
            System.setProperty("puppeteer_product", "chrome");
            
            // 方法2：使用不同的启动方式
            try {
                // 尝试1：不使用 executablePath，让库自动发现
                System.out.println("尝试自动发现浏览器...");
                LaunchOptions autoOptions = LaunchOptions.builder()
                        .headless(true)
                        .args(Arrays.asList(
                            "--no-sandbox",
                            "--disable-setuid-sandbox", 
                            "--disable-dev-shm-usage"
                        )).build();
                browser = Puppeteer.launch(autoOptions);
            } catch (Exception e1) {
                System.err.println("自动发现失败: " + e1.getMessage());
                
                // 尝试2：使用完整的启动选项但不指定 product
                try {
                    System.out.println("尝试指定路径但不指定product...");
                    LaunchOptions options = LaunchOptions.builder()
                            .headless(true)
                            .executablePath(chromiumPath)
                            .args(Arrays.asList(
                                "--no-sandbox",
                                "--disable-setuid-sandbox", 
                                "--disable-dev-shm-usage",
                                "--disable-gpu"
                            )).build();
                    browser = Puppeteer.launch(options);
                } catch (Exception e2) {
                    System.err.println("指定路径失败: " + e2.getMessage());
                    throw e2;
                }
            }
            
            System.out.println("Browser 已成功启动！");
            testBrowser();
        }
    }
    
    /**
     * 查找可用的浏览器路径
     */
    private String findBrowserPath() {
        // 按优先级检查可能的浏览器路径
        String[] possiblePaths = {
            System.getenv("CHROMIUM_PATH"),
            "/usr/bin/chromium",
            "/usr/bin/chromium-browser",
            "/usr/bin/google-chrome",
            "/usr/bin/google-chrome-stable",
            "/usr/bin/chrome"
        };
        
        for (String path : possiblePaths) {
            if (path != null && new File(path).exists()) {
                System.out.println("找到浏览器: " + path);
                return path;
            }
        }
        
        // 如果都找不到，尝试使用 which 命令查找
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"which", "chromium"});
            process.waitFor();
            if (process.exitValue() == 0) {
                java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()));
                String path = reader.readLine().trim();
                if (path != null && !path.isEmpty()) {
                    System.out.println("通过which找到浏览器: " + path);
                    return path;
                }
            }
        } catch (Exception e) {
            System.err.println("which命令执行失败: " + e.getMessage());
        }
        
        throw new RuntimeException("未找到可用的浏览器，请确保已安装Chromium或Chrome");
    }
    
    /**
     * 测试浏览器版本
     */
    private void testBrowserVersion(String browserPath) {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{browserPath, "--version"});
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream()));
            String version = reader.readLine();
            System.out.println("浏览器版本: " + version);
        } catch (Exception e) {
            System.err.println("无法获取浏览器版本: " + e.getMessage());
        }
    }
    
    private void testBrowser() throws Exception {
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
