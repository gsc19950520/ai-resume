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

    public static String renderHtml(Map<String, Object> data) {
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

    public static void main(String[] args) throws Exception {
        Map<String, Object> data = HtmlToPdfGenerator.getData();
        String htmlContent = HtmlToPdfGenerator.renderHtml(data);

        System.out.println("htmlContent: " + htmlContent);
        // 2️⃣ 启动浏览器
        LaunchOptions options = LaunchOptions.builder()
                .headless(true)
                .args(Arrays.asList("--no-sandbox", "--disable-setuid-sandbox"))
                .product(Product.Chrome)  // 指定 Chrome
                .build();

        Browser browser = Puppeteer.launch(options);

        // 3️⃣ 新建页面
        Page page = browser.newPage();

        // 4️⃣ 设置 HTML 内容
        WaitForOptions waitForOptions = new WaitForOptions();
        waitForOptions.setTimeout(30000);
        waitForOptions.setWaitUntil(Arrays.asList(PuppeteerLifeCycle.load));

        page.setContent(htmlContent, waitForOptions);

        // 5️⃣ 配置 PDF
        PDFOptions pdfOptions = new PDFOptions();
        pdfOptions.setWidth("210mm");  
        pdfOptions.setHeight("297mm"); 
        pdfOptions.setPrintBackground(true);
        pdfOptions.setPreferCSSPageSize(true);
        pdfOptions.setTimeout(30000);
        pdfOptions.setPath("output.pdf");
        pdfOptions.setLandscape(false);

        // 6️⃣ 生成 PDF
        byte[] pdfBytes = page.pdf(pdfOptions);

        // 7️⃣ 保存 PDF
        Files.write(Paths.get("output2.pdf"), pdfBytes);

        // 8️⃣ 关闭浏览器
        browser.close();

        System.out.println("PDF 已生成！");
    }
}
