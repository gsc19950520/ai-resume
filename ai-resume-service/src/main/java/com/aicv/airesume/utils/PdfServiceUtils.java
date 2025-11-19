package com.aicv.airesume.utils;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import com.aicv.airesume.config.PuppeteerBrowserManager;
import com.ruiyun.jvppeteer.api.core.Browser;
import com.ruiyun.jvppeteer.cdp.entities.PDFOptions;
import com.ruiyun.jvppeteer.common.PuppeteerLifeCycle;
import com.ruiyun.jvppeteer.api.core.Page;
import com.ruiyun.jvppeteer.cdp.entities.WaitForOptions;
import org.thymeleaf.context.Context;

@Service
public class PdfServiceUtils {
    
    @Autowired
    private PuppeteerBrowserManager browserManager;

    private TemplateEngine templateEngine;

    public PdfServiceUtils() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");
        resolver.setTemplateMode("HTML");

        templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(resolver);
    }

    public byte[] generatePdf(Map<String, Object> data, String templateName) throws Exception {
        // 1️⃣ 渲染 HTML
        Context context = new Context();
        data.forEach(context::setVariable);
        String htmlContent = templateEngine.process(templateName, context);

        // 2️⃣ 创建新页面
        Browser browser = browserManager.getBrowser();
        Page page = browser.newPage();

        WaitForOptions waitForOptions = new WaitForOptions();
        waitForOptions.setTimeout(30000);
        waitForOptions.setWaitUntil(Arrays.asList(PuppeteerLifeCycle.load));

        page.setContent(htmlContent, waitForOptions);

        // 3️⃣ 配置 PDF
        PDFOptions pdfOptions = new PDFOptions();
        pdfOptions.setWidth("210mm");
        pdfOptions.setHeight("297mm");
        pdfOptions.setPrintBackground(true);
        pdfOptions.setPreferCSSPageSize(true);
        pdfOptions.setTimeout(30000);
        pdfOptions.setLandscape(false);

        // 4️⃣ 生成 PDF
        byte[] pdfBytes = page.pdf(pdfOptions);

        // 5️⃣ 关闭页面（不关闭浏览器）
        page.close();

        return pdfBytes;
    }

    public void savePdf(byte[] pdfBytes, String path) throws Exception {
        Files.write(Paths.get(path), pdfBytes);
    }

}
