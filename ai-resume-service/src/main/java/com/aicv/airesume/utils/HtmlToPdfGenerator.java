package com.aicv.airesume.utils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ruiyun.jvppeteer.api.core.Browser;
import com.ruiyun.jvppeteer.api.core.Page;
import com.ruiyun.jvppeteer.cdp.core.Puppeteer;
import com.ruiyun.jvppeteer.cdp.entities.LaunchOptions;
import com.ruiyun.jvppeteer.cdp.entities.PDFOptions;
import com.ruiyun.jvppeteer.cdp.entities.WaitForOptions;
import com.ruiyun.jvppeteer.common.Product;
import com.ruiyun.jvppeteer.common.PuppeteerLifeCycle;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

public class HtmlToPdfGenerator {

    public static Map<String, Object> getData() {
        Map<String, Object> data = new HashMap<>();

        // 用户信息
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("country", null);
        userInfo.put("address", "福建省福州市仓山区");
        userInfo.put("gender", 1);
        userInfo.put("province", null);
        userInfo.put("phone", "17858555555");
        userInfo.put("avatarUrl", "cloud://prod-1gwm267i6a10e7cb.7072-prod-1gwm267i6a10e7cb-1258669146/oOS8B5fgXrjB4FiLAvy0L8ptzjGA/avatar_1763476467345.jpg");
        userInfo.put("city", "福建省福州市仓山区");
        userInfo.put("name", "高先生");
        userInfo.put("nickname", "轻盈的树叶画家");
        userInfo.put("birthDate", "1997-12-31");
        userInfo.put("email", "287458@qq.com");
        data.put("userInfo", userInfo);

        // 技能列表
        List<Map<String, Object>> skillList = new ArrayList<>();
        Map<String, Object> skill1 = new HashMap<>();
        skill1.put("id", 50);
        skill1.put("resumeId", 15);
        skill1.put("name", "java");
        skill1.put("level", 4);
        skill1.put("orderIndex", 0);
        skillList.add(skill1);

        Map<String, Object> skill2 = new HashMap<>();
        skill2.put("id", 51);
        skill2.put("resumeId", 15);
        skill2.put("name", "node");
        skill2.put("level", 3);
        skill2.put("orderIndex", 1);
        skillList.add(skill2);

        Map<String, Object> skill3 = new HashMap<>();
        skill3.put("id", 52);
        skill3.put("resumeId", 15);
        skill3.put("name", "pathon");
        skill3.put("level", 2);
        skill3.put("orderIndex", 2);
        skillList.add(skill3);
        data.put("skillList", skillList);

        // 教育经历
        List<Map<String, Object>> educationList = new ArrayList<>();
        Map<String, Object> edu = new HashMap<>();
        edu.put("id", 31);
        edu.put("resumeId", 15);
        edu.put("school", "湖北大学");
        edu.put("degree", "本科");
        edu.put("major", "计算机");
        edu.put("startDate", "2021");
        edu.put("endDate", "2025");
        edu.put("description", "");
        edu.put("orderIndex", 0);
        educationList.add(edu);
        data.put("educationList", educationList);

        // 工作经历
        List<Map<String, Object>> workExperienceList = new ArrayList<>();
        Map<String, Object> work = new HashMap<>();
        work.put("id", 31);
        work.put("resumeId", 15);
        work.put("companyName", "深圳科技");
        work.put("positionName", "java");
        work.put("startDate", "2020");
        work.put("endDate", "2024");
        work.put("description", "啊就是不能地区为哈佛岸似蹙很轻微覅u的按时缴纳父亲阿克苏加班费你求我啊可是今年对其");
        work.put("orderIndex", 0);
        workExperienceList.add(work);
        data.put("workExperienceList", workExperienceList);
        // 项目经验
        List<Map<String, Object>> projectList = new ArrayList<>();
        Map<String, Object> project = new HashMap<>();
        project.put("id", 31);
        project.put("resumeId", 15);
        project.put("projectName", "后端项目");
        project.put("startDate", "2021");
        project.put("endDate", "3024");
        project.put("description", "啊伤不起网易官方肚脐眼催芽给我的艾斯比区域为规范市场部阿思翠帮我也发给爱上不成其为约个饭2");
        project.put("orderIndex", 0);
        projectList.add(project);
        data.put("projectList", projectList);

        // 其他字段
        data.put("jobTitle", "java");
        data.put("selfEvaluation", "按时打算放弃我发发韩国vu有");
        data.put("updateTime", "2025-11-19T08:44:20.000+00:00");
        data.put("templateId", null);
        data.put("userId", 53);
        data.put("createTime", "2025-11-18T14:35:29.000+00:00");
        data.put("startTime", "一周内");
        data.put("expectedSalary", "20K");
        data.put("interests", "游泳篮球");
        data.put("jobTypeId", null);
        data.put("originalFilename", null);
        data.put("status", 0);

        return data;
    }

    // Process cloud storage URL, convert to HTTP accessible URL
    private static String convertCloudUrl(String cloudUrl) {
        if (cloudUrl == null || !cloudUrl.startsWith("cloud://")) {
            return cloudUrl;
        }
        
        // Cloud storage URL format: cloud://bucket-id.sub-domain/filename
        // Convert to HTTP URL: https://sub-domain.tcb.qcloud.la/filename
        String path = cloudUrl.substring(8); // Remove "cloud://"
        int dotIndex = path.indexOf('.');
        int firstSlashIndex = path.indexOf('/');
        
        if (dotIndex > 0 && firstSlashIndex > 0 && dotIndex < firstSlashIndex) {
            String subDomain = path.substring(dotIndex + 1, firstSlashIndex);
            String filename = path.substring(firstSlashIndex);
            return "https://" + subDomain + ".tcb.qcloud.la" + filename;
        }
        return cloudUrl;
    }
    
    // Preprocess data, convert all cloud storage URLs
    private static void preprocessData(Map<String, Object> data) {
        // Process user avatar URL
        if (data.containsKey("userInfo") && data.get("userInfo") instanceof Map) {
            Map<String, Object> userInfo = (Map<String, Object>) data.get("userInfo");
            if (userInfo.containsKey("avatarUrl")) {
                String avatarUrl = (String) userInfo.get("avatarUrl");
                userInfo.put("avatarUrl", convertCloudUrl(avatarUrl));
            }
        }
    }
    
    public static String renderHtml(Map<String, Object> data) {
        // 预处理数据，转换云存储URL
        preprocessData(data);
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");
        resolver.setTemplateMode("HTML");

        TemplateEngine engine = new TemplateEngine();
        engine.setTemplateResolver(resolver);

        Context context = new Context();
        data.forEach(context::setVariable);

        return engine.process("template-four", context); // template-four.html
    }

    /**
     * 检查WKHtmlToPdf是否可用
     * @return true表示WKHtmlToPdf可用
     */
    /**
     * 检查WKHtmlToPdf是否可用，可指定路径
     * @param wkHtmlToPdfPath 可选的WKHtmlToPdf可执行文件路径，如果为null则使用系统PATH中的wkhtmltopdf
     * @return 是否可用
     */
    public static boolean isWkHtmlToPdfAvailable(String... wkHtmlToPdfPath) {
        // 优先使用环境变量中的WKHtmlToPdf路径（云托管环境）
        String executable = System.getenv("WKHTMLTOPDF_PATH");
        
        // 如果环境变量未设置，则检查参数中的路径
        if (executable == null && wkHtmlToPdfPath != null && wkHtmlToPdfPath.length > 0 && wkHtmlToPdfPath[0] != null) {
            executable = wkHtmlToPdfPath[0];
        }
        
        // 如果以上都未设置，则使用系统PATH中的wkhtmltopdf
        if (executable == null) {
            executable = "wkhtmltopdf";
        }
        
        try {
            Process process = Runtime.getRuntime().exec(new String[]{executable, "--version"});
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("WKHtmlToPdf可用 (路径: " + executable + ")");
                return true;
            }
        } catch (Exception e) {
            System.out.println("WKHtmlToPdf不可用 (路径: " + executable + "): " + e.getMessage());
        }
        return false;
    }
    
    /**
     * 检查WKHtmlToPdf是否可用（默认方法，使用系统PATH）
     * @return 是否可用
     */
    public static boolean isWkHtmlToPdfAvailable() {
        return isWkHtmlToPdfAvailable((String[]) null);
    }

    /**
     * 使用WKHtmlToPdf将HTML转换为PDF（默认方法，使用系统PATH）
     * @param htmlContent HTML内容
     * @param outputPdfPath 输出PDF路径
     * @return 是否成功
     */
    public static boolean convertHtmlToPdfWithWkHtml(String htmlContent, String outputPdfPath) {
        return convertHtmlToPdfWithWkHtml(htmlContent, outputPdfPath, null);
    }
    
    /**
     * 使用WKHtmlToPdf将HTML转换为PDF，可指定WKHtmlToPdf路径
     * @param htmlContent HTML内容
     * @param outputPdfPath 输出PDF路径
     * @param wkHtmlToPdfPath WKHtmlToPdf可执行文件路径
     * @return 是否成功
     */
    public static boolean convertHtmlToPdfWithWkHtml(String htmlContent, String outputPdfPath, String wkHtmlToPdfPath) {
        try {
            // 创建临时HTML文件
            java.io.File tempHtmlFile = java.io.File.createTempFile("temp_", ".html");
            tempHtmlFile.deleteOnExit();
            
            // 写入HTML内容到临时文件
            Files.write(tempHtmlFile.toPath(), htmlContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            
            // 构建WKHtmlToPdf命令
            List<String> command = new ArrayList<>();
            
            // 优先使用环境变量中的WKHtmlToPdf路径（云托管环境）
            String executable = System.getenv("WKHTMLTOPDF_PATH");
            
            // 如果环境变量未设置，则检查参数中的路径
            if (executable == null && wkHtmlToPdfPath != null) {
                executable = wkHtmlToPdfPath;
            }
            
            // 如果以上都未设置，则使用系统PATH中的wkhtmltopdf
            if (executable == null) {
                executable = "wkhtmltopdf";
            }
            
            command.add(executable);
            
            // 添加高级参数以保持复杂样式
            command.add("--enable-local-file-access");
            command.add("--disable-javascript"); // 可选，某些复杂JS可能导致渲染问题
            command.add("--disable-smart-shrinking"); // 保持精确的大小控制
            command.add("--print-media-type"); // 使用打印样式表
            command.add("--margin-top");
            command.add("10mm");
            command.add("--margin-right");
            command.add("10mm");
            command.add("--margin-bottom");
            command.add("10mm");
            command.add("--margin-left");
            command.add("10mm");
            
            // 添加输入和输出文件路径
            command.add(tempHtmlFile.getAbsolutePath());
            command.add(outputPdfPath);
            
            System.out.println("执行WKHtmlToPdf命令: " + String.join(" ", command));
            
            // 执行命令
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            
            // 读取输出
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                StringBuilder output = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
                System.out.println("WKHtmlToPdf输出: " + output.toString());
            }
            
            // 等待进程完成
            int exitCode = process.waitFor();
            
            // 清理临时文件
            tempHtmlFile.delete();
            
            if (exitCode == 0) {
                System.out.println("WKHtmlToPdf执行成功，退出码: " + exitCode);
                return true;
            } else {
                System.out.println("WKHtmlToPdf执行失败，退出码: " + exitCode);
                return false;
            }
        } catch (Exception e) {
            System.out.println("WKHtmlToPdf执行异常: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public static void main(String[] args) throws Exception {
        Map<String, Object> data = HtmlToPdfGenerator.getData();
        String htmlContent = HtmlToPdfGenerator.renderHtml(data);

        System.out.println("开始生成PDF...");
        
        // 从命令行参数获取WKHtmlToPdf路径（如果提供）
        String wkHtmlToPdfPath = null;
        wkHtmlToPdfPath = "D:\\software\\wkhtmltopdf\\bin\\wkhtmltopdf.exe";
        System.out.println("使用命令行提供的WKHtmlToPdf路径: " + wkHtmlToPdfPath);
        
        // 检查WKHtmlToPdf是否可用
        boolean wkHtmlToPdfAvailable = isWkHtmlToPdfAvailable(wkHtmlToPdfPath);
        boolean pdfGenerated = false;
        
        // 优先使用WKHtmlToPdf生成PDF（本地测试模式）
        if (wkHtmlToPdfAvailable) {
            System.out.println("WKHtmlToPdf可用，优先使用WKHtmlToPdf生成PDF...");
            pdfGenerated = convertHtmlToPdfWithWkHtml(htmlContent, "output_wkhtmltopdf.pdf", wkHtmlToPdfPath);
            
            if (pdfGenerated) {
                System.out.println("WKHtmlToPdf方式PDF生成成功！");
            } else {
                System.out.println("WKHtmlToPdf方式生成PDF失败，尝试使用浏览器方式...");
            }
        } else {
            System.out.println("WKHtmlToPdf不可用，尝试使用浏览器方式生成PDF...");
        }
        
        // 如果WKHtmlToPdf生成失败或不可用，则尝试使用浏览器方式
        if (!pdfGenerated) {
            try {
                System.out.println("尝试使用浏览器方式生成PDF...");
                // 启动浏览器
                LaunchOptions options = LaunchOptions.builder()
                        .headless(true)
                        .args(Arrays.asList("--no-sandbox", "--disable-setuid-sandbox"))
                        .product(Product.Chrome)  // 指定 Chrome
                        .build();

                Browser browser = null;
                try {
                    browser = Puppeteer.launch(options);

                    // 新建页面
                    Page page = browser.newPage();

                    // 设置 HTML 内容
                    WaitForOptions waitForOptions = new WaitForOptions();
                    waitForOptions.setTimeout(30000);
                    waitForOptions.setWaitUntil(Arrays.asList(PuppeteerLifeCycle.load));

                    page.setContent(htmlContent, waitForOptions);

                    // 配置 PDF
                    PDFOptions pdfOptions = new PDFOptions();
                    pdfOptions.setWidth("210mm");  
                    pdfOptions.setHeight("297mm"); 
                    pdfOptions.setPrintBackground(true);
                    pdfOptions.setPreferCSSPageSize(true);
                    pdfOptions.setTimeout(30000);
                    pdfOptions.setPath("output_browser.pdf");
                    pdfOptions.setLandscape(false);

                    // 生成 PDF
                    byte[] pdfBytes = page.pdf(pdfOptions);

                    // 保存 PDF
                    Files.write(Paths.get("output2_browser.pdf"), pdfBytes);
                    
                    System.out.println("浏览器方式PDF生成成功！");
                    pdfGenerated = true;
                } finally {
                    // 确保关闭浏览器
                    if (browser != null) {
                        try {
                            browser.close();
                        } catch (Exception e) {
                            System.out.println("关闭浏览器时出错: " + e.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("浏览器方式生成PDF失败: " + e.getMessage());
                e.printStackTrace();
            }
        }

        if (pdfGenerated) {
            System.out.println("PDF已成功生成！");
        } else {
            System.out.println("PDF生成失败！");
            System.out.println("使用说明：");
            System.out.println("1. 确保已安装WKHtmlToPdf工具");
            System.out.println("2. 可通过命令行参数指定WKHtmlToPdf路径，例如：");
            System.out.println("   java -jar [jar文件名] [wkhtmltopdf.exe的完整路径]");
            System.out.println("   或");
            System.out.println("   mvn compile exec:java -Dexec.mainClass=com.aicv.airesume.utils.HtmlToPdfGenerator -Dexec.args=\"C:\\\\path\\\\to\\\\wkhtmltopdf.exe\"");
        }
    }
}
