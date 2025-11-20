package com.aicv.airesume.config;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.ruiyun.jvppeteer.api.core.Browser;
import com.ruiyun.jvppeteer.api.core.Page;
import com.ruiyun.jvppeteer.cdp.core.Puppeteer;
import com.ruiyun.jvppeteer.cdp.entities.LaunchOptions;
import com.ruiyun.jvppeteer.common.Product;

@Component
public class PuppeteerBrowserManager {
    private static final Logger logger = LoggerFactory.getLogger(PuppeteerBrowserManager.class);
    
    private Browser browser;
    private final Object lock = new Object();
    private boolean isCloudEnv;

    private static final int MAX_RETRY_COUNT = 3;
    private static final int RETRY_DELAY_MS = 1000;
    
    @PostConstruct
    public void init() throws Exception {
        synchronized (lock) {
            if (browser != null) {
                return;
            }
            
            // 检测当前环境是否为云环境
            this.isCloudEnv = isCloudEnvironment();
            logger.info("当前运行环境: {}", isCloudEnv ? "云环境" : "本地环境");
            
            // 方法1：尝试设置系统属性
            System.setProperty("puppeteer_product", "chrome");
            
            // 初始化浏览器实例
            initializeBrowser();
            
            logger.info("Browser 已成功启动！");
            testBrowser();
        }
    }
    
    /**
     * 初始化浏览器实例
     */
    private void initializeBrowser() {
        for (int retry = 0; retry < MAX_RETRY_COUNT; retry++) {
            try {
                logger.info("开始初始化浏览器，第 {} 次尝试", retry + 1);
                
                // 构建基础配置 - 使用builder模式
                LaunchOptions.Builder optionsBuilder = LaunchOptions.builder()
                    .product(Product.Chrome)
                    .headless(true)
                    .args(Arrays.asList(getBrowserArgs()));
                    
                // 本地环境下可以考虑设置超时
                if (!isCloudEnv) {
                    optionsBuilder.timeout(30000); // 本地环境设置30秒超时
                }
                
                LaunchOptions options = optionsBuilder.build();
                
                // 尝试自动发现浏览器
                try {
                    logger.info("尝试自动发现浏览器");
                    this.browser = Puppeteer.launch(options);
                    logger.info("自动发现浏览器成功");
                    break;
                } catch (Exception e) {
                    logger.warn("自动发现浏览器失败: {}, 将尝试使用指定路径", e.getMessage());
                }
                
                // 如果自动发现失败，尝试使用指定路径
                String browserPath = findBrowserPath();
                logger.info("使用指定浏览器路径: {}", browserPath);
                
                // 使用指定路径启动 - 创建新的builder并设置executablePath
                LaunchOptions.Builder pathOptionsBuilder = LaunchOptions.builder()
                    .product(Product.Chrome)
                    .headless(true)
                    .args(Arrays.asList(getBrowserArgs()))
                    .executablePath(browserPath);
                    
                // 本地环境下设置超时
                if (!isCloudEnv) {
                    pathOptionsBuilder.timeout(30000);
                }
                
                this.browser = Puppeteer.launch(pathOptionsBuilder.build());
                
                // 浏览器已成功启动，更新日志状态
                logger.info("浏览器初始化成功，正在验证版本信息");
                // 测试浏览器版本
                testBrowserVersion(browserPath);
                // 标记浏览器初始化任务完成
                logger.info("浏览器初始化任务完成");
                
                logger.info("浏览器初始化成功");
                break;
            } catch (Exception e) {
                logger.error("浏览器初始化失败，第 {} 次尝试: {}", retry + 1, e.getMessage(), e);
                
                if (retry == MAX_RETRY_COUNT - 1) {
                    logger.error("达到最大重试次数，浏览器初始化失败");
                    throw new RuntimeException("浏览器初始化失败: " + e.getMessage(), e);
                }
                
                try {
                    logger.info("等待 {}ms 后重试", RETRY_DELAY_MS);
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("浏览器初始化中断", ie);
                }
            }
        }
    }
    
    /**
     * 检测当前环境是否为云环境
     * @return true表示云环境，false表示本地环境
     */
    private boolean isCloudEnvironment() {
        // 通过以下方式判断是否为云环境
        // 1. 检查是否存在特定的环境变量
        if (System.getenv("CLOUD_ENVIRONMENT") != null || 
            System.getenv("KUBERNETES_SERVICE_HOST") != null || 
            System.getenv("APP_ENV") != null && (System.getenv("APP_ENV").equalsIgnoreCase("prod") || System.getenv("APP_ENV").equalsIgnoreCase("production"))) {
            return true;
        }
        
        // 2. 检查操作系统路径特征
        if (new File("/usr/bin/chromium").exists() || new File("/usr/bin/google-chrome").exists()) {
            return true;
        }
        
        // 3. 默认视为本地环境
        return false;
    }
    
    /**
     * 获取适合当前环境的浏览器启动参数
     */
    private String[] getBrowserArgs() {
        if (isCloudEnv) {
            // 云环境参数 - 安全且资源受限环境的配置
            return new String[]{
                "--no-sandbox",
                "--disable-setuid-sandbox",
                "--disable-dev-shm-usage",
                "--disable-gpu",
                "--disable-software-rasterizer",
                "--disable-features=site-per-process",
                "--disable-features=TranslateUI",
                "--disable-features=IsolateOrigins,site-per-process",
                "--enable-features=NetworkServiceInProcess",
                "--window-size=1920,1080"
            };
        } else {
            // 本地环境参数 - 更加灵活的配置，优化开发体验
            List<String> args = new ArrayList<>();
            
            // 基础参数
            args.add("--disable-dev-shm-usage");
            args.add("--disable-gpu");
            args.add("--window-size=1920,1080");
            
            // 本地环境性能优化参数
            args.add("--enable-automation");
            args.add("--no-first-run");
            args.add("--disable-background-networking");
            args.add("--disable-background-timer-throttling");
            args.add("--disable-backgrounding-occluded-windows");
            args.add("--disable-breakpad");
            args.add("--disable-client-side-phishing-detection");
            args.add("--disable-default-apps");
            args.add("--disable-extensions");
            args.add("--disable-features=site-per-process");
            args.add("--disable-hang-monitor");
            args.add("--disable-ipc-flooding-protection");
            args.add("--disable-prompt-on-repost");
            args.add("--disable-renderer-backgrounding");
            args.add("--disable-sync");
            args.add("--metrics-recording-only");
            args.add("--safebrowsing-disable-auto-update");
            
            // 非Windows环境可以考虑禁用沙盒以提高兼容性
            String os = System.getProperty("os.name").toLowerCase();
            if (!os.contains("win")) {
                args.add("--no-sandbox");
            }
            
            return args.toArray(new String[0]);
        }
    }
    
    /**
     * 查找可用的浏览器路径
     */
    private String findBrowserPath() {
        // 获取操作系统类型
        String os = System.getProperty("os.name").toLowerCase();
        logger.info("当前操作系统: {}", os);
        
        // 按优先级检查可能的浏览器路径
        String[] possiblePaths;
        
        // 根据操作系统类型设置不同的浏览器路径
        if (os.contains("win")) {
            // Windows环境路径
            possiblePaths = new String[]{
                // 从环境变量获取
                System.getenv("CHROMIUM_PATH"),
                // 项目中的本地浏览器路径
                System.getProperty("user.dir") + "\\..\\..\\.local-browser\\win64-131.0.6778.87\\chrome-win\\chrome.exe",
                // Windows系统常见Chrome安装路径
                System.getenv("ProgramFiles") + "\\Google\\Chrome\\Application\\chrome.exe",
                System.getenv("ProgramFiles(x86)") + "\\Google\\Chrome\\Application\\chrome.exe",
                System.getenv("LOCALAPPDATA") + "\\Google\\Chrome\\Application\\chrome.exe",
                // Edge浏览器
                System.getenv("ProgramFiles") + "\\Microsoft Edge\\Application\\msedge.exe",
                System.getenv("ProgramFiles(x86)") + "\\Microsoft Edge\\Application\\msedge.exe"
            };
        } else {
            // Linux/Mac环境路径
            possiblePaths = new String[]{
                System.getenv("CHROMIUM_PATH"),
                "/usr/bin/chromium",
                "/usr/bin/chromium-browser",
                "/usr/bin/google-chrome",
                "/usr/bin/google-chrome-stable",
                "/usr/bin/chrome",
                "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
                "/Applications/Microsoft Edge.app/Contents/MacOS/Microsoft Edge"
            };
        }
        
        // 检查所有可能的路径
        for (String path : possiblePaths) {
            if (path != null && new File(path).exists()) {
                logger.info("找到浏览器: {}", path);
                return path;
            }
        }
        
        // 如果都找不到，尝试使用系统命令查找
        if (!os.contains("win")) {
            try {
                Process process = Runtime.getRuntime().exec(new String[]{"which", "chromium"});
                process.waitFor();
                if (process.exitValue() == 0) {
                    java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(process.getInputStream()));
                    String path = reader.readLine().trim();
                    if (path != null && !path.isEmpty() && new File(path).exists()) {
                        logger.info("通过which命令找到浏览器: {}", path);
                        return path;
                    }
                }
                
                // 尝试查找chrome
                process = Runtime.getRuntime().exec(new String[]{"which", "google-chrome"});
                process.waitFor();
                if (process.exitValue() == 0) {
                    java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(process.getInputStream()));
                    String path = reader.readLine().trim();
                    if (path != null && !path.isEmpty() && new File(path).exists()) {
                        logger.info("通过which命令找到浏览器: {}", path);
                        return path;
                    }
                }
            } catch (Exception e) {
                logger.warn("系统命令查找浏览器失败: {}", e.getMessage());
            }
        }
        
        logger.error("未找到可用的浏览器，请确保已安装Chromium或Chrome");
        throw new RuntimeException("未找到可用的浏览器，请确保已安装Chromium或Chrome或设置正确的CHROMIUM_PATH环境变量");
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
            logger.info("浏览器版本: {}", version);
        } catch (Exception e) {
            logger.warn("无法获取浏览器版本: {}", e.getMessage());
        }
    }
    
    private void testBrowser() throws Exception {
        Page page = browser.newPage();
        try {
            page.goTo("data:text/html,<h1>Test Page</h1>");
            logger.info("Browser 测试通过");
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
                    logger.error("关闭Browser时出错: {}", e.getMessage(), e);
                } finally {
                    browser = null;
                }
            }
        }
    }
}
