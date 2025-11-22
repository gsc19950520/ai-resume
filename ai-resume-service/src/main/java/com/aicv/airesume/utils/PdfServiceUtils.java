package com.aicv.airesume.utils;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.context.Context;

/**
 * PDF生成服务工具类
 * 仅支持WKHtmlToPdf方式生成PDF
 */
@Service
public class PdfServiceUtils {

    private TemplateEngine templateEngine;

    /**
     * 构造函数
     */
    public PdfServiceUtils() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");
        resolver.setTemplateMode("HTML");
        // 重要：设置缓存为false，确保每次都使用最新的模板
        resolver.setCacheable(false);
        // 增强HTML解析能力，确保样式完整性
        resolver.setCheckExistence(true);
        resolver.setOrder(1);

        templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(resolver);
    }

    /**
     * 生成PDF文件
     * @param data 模板数据
     * @param templateName 模板名称
     * @return PDF文件字节数组
     * @throws Exception 生成过程中出现的异常
     */
    public byte[] generatePdf(Map<String, Object> data, String templateName) throws Exception {
        // 1️⃣ 渲染 HTML
        Context context = new Context();
        // 设置上下文参数，确保UTF-8编码
        context.setVariable("encoding", "UTF-8");
        // 添加数据到上下文
        data.forEach(context::setVariable);
        
        // 处理模板名称，确保不含.html后缀
        String processedTemplateName = templateName;
        if (templateName.endsWith(".html")) {
            processedTemplateName = templateName.substring(0, templateName.lastIndexOf(".html"));
        }
        
        // 生成完整的HTML内容
        String htmlContent = templateEngine.process(processedTemplateName, context);
        
        // 确保HTML内容包含必要的DOCTYPE声明和字符集设置
        if (!htmlContent.trim().startsWith("<!DOCTYPE")) {
            htmlContent = "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"></head><body>" + htmlContent + "</body></html>";
        }

        // 2️⃣ 检查WKHtmlToPdf是否可用
        if (!HtmlToPdfGenerator.isWkHtmlToPdfAvailable()) {
            String wkHtmlToPdfPath = System.getenv("WKHTMLTOPDF_PATH");
            String errorMsg = "无法生成PDF：WKHtmlToPdf不可用";
            if (wkHtmlToPdfPath != null) {
                errorMsg += " (环境变量配置路径: " + wkHtmlToPdfPath + ")";
            }
            throw new RuntimeException(errorMsg);
        }

        // 3️⃣ 使用WKHtmlToPdf生成PDF
        return generatePdfWithWkHtml(htmlContent);
    }

    /**
     * 使用WKHtmlToPdf生成PDF
     * @param htmlContent HTML内容
     * @return PDF文件字节数组
     * @throws Exception 生成过程中出现的异常
     */
    private byte[] generatePdfWithWkHtml(String htmlContent) throws Exception {
        // 创建临时PDF文件
        java.io.File tempPdfFile = java.io.File.createTempFile("resume_", ".pdf");
        try {
            // 使用HtmlToPdfGenerator的WKHtmlToPdf功能生成PDF
            // 注意：HtmlToPdfGenerator内部会自动从环境变量WKHTMLTOPDF_PATH获取路径，
            // 在Docker环境中该路径为/usr/bin/wkhtmltopdf
            boolean success = HtmlToPdfGenerator.convertHtmlToPdfWithWkHtml(htmlContent, tempPdfFile.getAbsolutePath());
            if (!success) {
                throw new RuntimeException("WKHtmlToPdf生成PDF失败");
            }
            // 读取PDF内容
            return Files.readAllBytes(tempPdfFile.toPath());
        } finally {
            // 清理临时文件
            tempPdfFile.deleteOnExit();
        }
    }

    /**
     * 保存PDF文件到指定路径
     * @param pdfBytes PDF字节数组
     * @param path 保存路径
     * @throws Exception 保存过程中出现的异常
     */
    public void savePdf(byte[] pdfBytes, String path) throws Exception {
        Files.write(Paths.get(path), pdfBytes);
    }

}
