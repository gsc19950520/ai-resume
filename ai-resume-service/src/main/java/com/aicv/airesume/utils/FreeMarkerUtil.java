package com.aicv.airesume.utils;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.cache.StringTemplateLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// 假设PdfUtil类在同一个包中
class PdfUtil {}
// 注意：在实际运行时，需要确保有正确的PdfUtil类实现或替换为实际的字体加载方式

public class FreeMarkerUtil {

    private static final Configuration cfg;

    static {
        cfg = new Configuration(Configuration.VERSION_2_3_32);
        cfg.setTemplateLoader(new StringTemplateLoader());
        cfg.setDefaultEncoding("UTF-8");
    }

    public static String parse(String templateString, Map<String, Object> data) {
        try {
            String templateName = "dynamicTemplate_" + System.currentTimeMillis();
            ((StringTemplateLoader) cfg.getTemplateLoader())
                    .putTemplate(templateName, templateString);

            Template template = cfg.getTemplate(templateName);
            StringWriter writer = new StringWriter();
            template.process(data, writer);

            return writer.toString();
        } catch (Exception e) {
            throw new RuntimeException("FreeMarker parse error", e);
        }
    }

    public String generatePdfFromWxml(
        String wxmlContent,
        String wxssContent,
        Map<String, Object> resumeData,
        String outputDir
    ) {
        try {

            // =============== 1. 转换 WXML -> HTML（简单适配器） ===============
            String htmlBody = wxmlContent
                .replace("<view", "<div")
                .replace("</view>", "</div>")
                .replace("<image", "<img")
                .replace("mode=\"aspectFit\"", "style=\"object-fit:contain;\"")
                .replace("/>", " />") // 保持自闭合
                .replace("wx:for", "th:each")
                .replace("wx:if", "th:if")
                .replace("{{", "${")
                .replace("}}", "}");


            // =============== 2. 将 WXSS 转 CSS（去掉小程序单位 rpx） ===============
            String css = wxssContent.replace("rpx", "px");

            // =============== 3. 构建完整 HTML ===============
            String finalHtml =
                "<!DOCTYPE html>\n" +
                    "<html>\n" +
                    "<head>\n" +
                    "<meta charset='UTF-8' />\n" +   // ← 必须自闭合
                    "<style>" +
                    "body { font-family: 'Microsoft YaHei', Arial; }" +
                    css +
                    "</style>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    htmlBody +
                    "</body>\n" +
                    "</html>";


            // =============== 4. 使用 FreeMarker 进行变量赋值 ===============
            freemarker.template.Configuration cfg = new freemarker.template.Configuration(freemarker.template.Configuration.VERSION_2_3_32);
            cfg.setTemplateLoader(new freemarker.cache.StringTemplateLoader() {{
                putTemplate("resume", finalHtml);
            }});
            cfg.setDefaultEncoding("UTF-8");

            Template template = cfg.getTemplate("resume");
            StringWriter stringWriter = new StringWriter();
            template.process(resumeData, stringWriter);
            String filledHtml = stringWriter.toString();

            // =============== 5. PDF 生成 ===============
            String fileName = UUID.randomUUID().toString() + ".pdf";
            String outputPath = outputDir + File.separator + fileName;

            try (OutputStream os = new FileOutputStream(outputPath)) {
                com.openhtmltopdf.pdfboxout.PdfRendererBuilder builder =
                        new com.openhtmltopdf.pdfboxout.PdfRendererBuilder();

                // 解决中文不显示
                builder.useFont(() -> PdfUtil.class.getResourceAsStream("/fonts/simhei.ttf"),
                        "simhei");

                builder.withHtmlContent(filledHtml, null);
                builder.toStream(os);
                builder.run();
            }

            return outputPath;

        } catch (Exception e) {
            throw new RuntimeException("PDF 生成失败", e);
        }
    }

}

