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
            
            // 1. 首先检查浏览器是否存在
            String chromiumPath = findBrowserPath();
            System.out.println("最终使用浏览器路径: " + chromiumPath);
            
            // 2. 验证浏览器可执行文件
            File browserFile = new File(chromiumPath);
            if (!browserFile.exists()) {
                throw new RuntimeException("浏览器文件不存在: " + chromiumPath);
            }
            if (!browserFile.canExecute()) {
                throw new RuntimeException("浏览器文件不可执行: " + chromiumPath);
            }
            
            // 3. 测试浏览器版本
            testBrowserVersion(chromiumPath);
            
            // 4. 启动浏览器
            LaunchOptions options = LaunchOptions.builder()
                    .headless(true)
                    .executablePath(chromiumPath)
                    .args(Arrays.asList(
                        "--no-sandbox",
                        "--disable-setuid-sandbox", 
                        "--disable-dev-shm-usage",
                        "--disable-gpu",
                        "--disable-software-rasterizer",
                        "--disable-web-security",
                        "--no-first-run",
                        "--no-default-browser-check",
                        "--disable-default-apps",
                        "--disable-translate",
                        "--disable-extensions",
                        "--remote-debugging-port=0",
                        "--user-data-dir=/tmp/chrome-user-data"
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
