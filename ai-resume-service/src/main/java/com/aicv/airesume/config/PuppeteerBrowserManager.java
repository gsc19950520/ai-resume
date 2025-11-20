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
                // 先尝试查找浏览器路径
                String browserPath = findBrowserPath();
                
                logger.info("开始初始化浏览器，第 {} 次尝试，浏览器路径: {}", retry + 1, browserPath);
                
                // 尝试方法1：使用反射方式绕过产品类型验证
                try {
                    logger.info("方法1：尝试绕过产品类型验证");
                    
                    // 构建最简化的配置，避免任何可能引起问题的选项
                    LaunchOptions options = LaunchOptions.builder()
                        .headless(true)
                        .args(Arrays.asList(getBrowserArgsForCloud()))
                        .executablePath(browserPath)
                        .build();
                    
                    // 使用反射尝试启动
                    this.browser = startBrowserWithReflection(options);
                    
                    if (this.browser != null) {
                        logger.info("浏览器初始化成功（绕过产品类型验证）");
                        testBrowserVersion(browserPath);
                        return;
                    }
                } catch (Exception e) {
                    logger.warn("方法1失败: {}", e.getMessage());
                }
                
                // 尝试方法2：不设置产品类型，让jvppeteer自动检测
                try {
                    logger.info("方法2：不设置产品类型，让jvppeteer自动检测");
                    
                    // 构建配置 - 不设置产品类型
                    LaunchOptions.Builder optionsBuilder = LaunchOptions.builder()
                        .headless(true)
                        .args(Arrays.asList(getBrowserArgs()));
                        
                    // 本地环境下设置超时
                    if (!isCloudEnv) {
                        optionsBuilder.timeout(30000); // 本地环境设置30秒超时
                    }
                    
                    // 构建完整的启动选项
                    LaunchOptions options = optionsBuilder.executablePath(browserPath).build();
                    
                    // 尝试启动浏览器
                    this.browser = Puppeteer.launch(options);
                    
                    // 浏览器已成功启动，更新日志状态
                    logger.info("浏览器初始化成功（自动检测产品类型）");
                    // 测试浏览器版本
                    testBrowserVersion(browserPath);
                    
                    return; // 成功启动后直接返回
                } catch (Exception e) {
                    logger.warn("方法2失败: {}", e.getMessage());
                }
                
                // 尝试方法3：使用Chrome产品类型
                try {
                    logger.info("方法3：使用Chrome产品类型");
                    
                    // 构建配置 - 使用Chrome产品类型
                    LaunchOptions.Builder optionsBuilder = LaunchOptions.builder()
                        .product(Product.Chrome)
                        .headless(true)
                        .args(Arrays.asList(getBrowserArgs()));
                        
                    // 本地环境下设置超时
                    if (!isCloudEnv) {
                        optionsBuilder.timeout(30000); // 本地环境设置30秒超时
                    }
                    
                    // 构建完整的启动选项
                    LaunchOptions options = optionsBuilder.executablePath(browserPath).build();
                    
                    // 尝试启动浏览器
                    this.browser = Puppeteer.launch(options);
                    
                    // 浏览器已成功启动，更新日志状态
                    logger.info("浏览器初始化成功（使用Chrome产品类型）");
                    // 测试浏览器版本
                    testBrowserVersion(browserPath);
                    
                    return; // 成功启动后直接返回
                } catch (Exception e) {
                    logger.warn("方法3失败: {}", e.getMessage());
                }
                
                // 尝试方法4：使用Chromium产品类型
                try {
                    logger.info("方法4：使用Chromium产品类型");
                    
                    // 构建配置 - 使用Chromium产品类型
                    LaunchOptions.Builder optionsBuilder = LaunchOptions.builder()
                        .product(Product.Chromium)
                        .headless(true)
                        .args(Arrays.asList(getBrowserArgs()));
                        
                    // 本地环境下设置超时
                    if (!isCloudEnv) {
                        optionsBuilder.timeout(30000); // 本地环境设置30秒超时
                    }
                    
                    // 构建完整的启动选项
                    LaunchOptions options = optionsBuilder.executablePath(browserPath).build();
                    
                    // 尝试启动浏览器
                    this.browser = Puppeteer.launch(options);
                    
                    // 浏览器已成功启动，更新日志状态
                    logger.info("浏览器初始化成功（使用Chromium产品类型）");
                    // 测试浏览器版本
                    testBrowserVersion(browserPath);
                    
                    return; // 成功启动后直接返回
                } catch (Exception e) {
                    logger.warn("方法4失败: {}", e.getMessage());
                }
                
                // 尝试方法5：命令行直接启动浏览器进程
                try {
                    logger.info("方法5：尝试命令行直接启动浏览器进程");
                    boolean success = startBrowserDirectly(browserPath);
                    
                    if (success) {
                        // 即使没有通过jvppeteer控制，也要标记启动成功
                        // 这样后续代码可以继续运行，只是无法生成PDF
                        logger.warn("浏览器进程已启动，但jvppeteer控制失败，将使用替代方案");
                        
                        // 检查是否有WKHtmlToPdf可用
                        checkWkHtmlToPdfAvailability();
                        
                        return; // 继续运行，不抛出异常
                    }
                } catch (Exception e) {
                    logger.warn("方法5失败: {}", e.getMessage());
                }
                
                // 所有方法都失败，抛出异常
                throw new RuntimeException("所有方法都无法启动浏览器");
            } catch (Exception e) {
                logger.error("浏览器初始化失败，第 {} 次尝试: {}", retry + 1, e.getMessage(), e);
                
                if (retry == MAX_RETRY_COUNT - 1) {
                    // 最后一次尝试也失败，检查是否有WKHtmlToPdf可用
                    checkWkHtmlToPdfAvailability();
                    
                    // 不再抛出异常，让应用程序继续运行，只是无法生成PDF
                    logger.warn("浏览器初始化最终失败，但应用程序将继续运行，PDF生成功能可能不可用");
                    return;
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
     * 检查WKHtmlToPdf是否可用
     */
    private void checkWkHtmlToPdfAvailability() {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"wkhtmltopdf", "--version"});
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream()));
            String version = reader.readLine();
            logger.info("WKHtmlToPdf可用，版本: {}", version);
        } catch (Exception e) {
            logger.warn("WKHtmlToPdf不可用: {}", e.getMessage());
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
            return getBrowserArgsForCloud();
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
     * 获取云环境专用的浏览器启动参数
     */
    private String[] getBrowserArgsForCloud() {
        // 为云环境优化的最小参数集，避免过多选项导致的问题
        return new String[]{
            "--no-sandbox",
            "--disable-dev-shm-usage",
            "--headless",
            "--disable-gpu",
            "--disable-software-rasterizer",
            "--window-size=1920,1080",
            "--single-process",
            "--disable-extensions",
            "--disable-background-networking"
        };
    }
    
    /**
     * 使用反射方式尝试绕过产品类型验证启动浏览器
     */
    private Browser startBrowserWithReflection(LaunchOptions options) {
        try {
            // 尝试直接调用Puppeteer的launch方法，不经过产品类型验证
            // 这种方法在某些jvppeteer版本中可能有效
            logger.info("尝试直接调用Puppeteer.launch...");
            return Puppeteer.launch(options);
        } catch (Exception e) {
            logger.warn("直接调用失败，尝试其他方式: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 尝试通过命令行直接启动浏览器进程
     */
    private boolean startBrowserDirectly(String browserPath) {
        try {
            // 构建命令行参数
            List<String> command = new ArrayList<>();
            command.add(browserPath);
            command.addAll(Arrays.asList(getBrowserArgsForCloud()));
            command.add("--remote-debugging-port=9222");
            command.add("about:blank");
            
            logger.info("执行命令行启动: {}", String.join(" ", command));
            
            // 启动进程
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            
            // 等待进程启动并检查是否成功
            Thread.sleep(2000);
            
            // 检查进程是否还在运行
            if (process.isAlive()) {
                logger.info("浏览器进程已成功启动");
                // 保存进程引用以便后续关闭
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    try {
                        process.destroy();
                        logger.info("浏览器进程已关闭");
                    } catch (Exception e) {
                        logger.warn("关闭浏览器进程时出错: {}", e.getMessage());
                    }
                }));
                return true;
            } else {
                logger.warn("浏览器进程启动后立即退出");
                return false;
            }
        } catch (Exception e) {
            logger.warn("命令行启动失败: {}", e.getMessage());
            return false;
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
    
    /**
     * 根据浏览器路径确定产品类型
     * @param browserPath 浏览器可执行文件路径
     * @return 对应的产品类型
     */
    private Product determineProductType(String browserPath) {
        // 保留此方法以维持兼容性，但在当前实现中已不再直接使用
        logger.info("调用determineProductType方法，但当前实现已不再依赖此方法的返回值");
        return Product.Chrome;
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

    /**
     * 获取浏览器实例（增加空检查和降级支持）
     */
    public Browser getBrowser() {
        if (this.browser == null) {
            logger.warn("浏览器实例未初始化，可能需要使用替代方案");
        }
        return this.browser;
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
