package com.aicv.airesume.utils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * PDF生成器测试类
 */
public class TestPdfGenerator {
    
    public static void main(String[] args) {
        try {
            // 读取测试HTML文件
            String htmlContent = new String(Files.readAllBytes(
                Paths.get("f:\\owner_project\\ai-resume\\ai-resume-service\\test-resume.html")
            ), "UTF-8");
            
            // 创建PDF生成器
            AdvancedHtmlToPdfGenerator generator = new AdvancedHtmlToPdfGenerator();
            
            // 配置选项
            Map<String, Object> config = new HashMap<>();
            config.put("pageSize", "A4");
            config.put("margin", "20mm");
            config.put("enableSvg", true);
            config.put("enableBookmarks", true);
            config.put("compress", true);
            
            // 生成PDF
            String outputPath = "f:\\owner_project\\ai-resume\\ai-resume-service\\test-output-improved.pdf";
            generator.generatePdf(htmlContent, outputPath, config);
            
            System.out.println("SUCCESS: PDF生成成功: " + outputPath);
            System.out.println("文件大小: " + new File(outputPath).length() + " bytes");
            
        } catch (Exception e) {
            System.err.println("ERROR: PDF生成失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}