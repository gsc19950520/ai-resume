package com.aicv.airesume.utils;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder;

import java.io.*;

/**
 * 简化版HTML转PDF生成器
 * 用于测试PDF生成效果与HTML展示的一致性
 */
public class SimplePdfGenerator {
    
    public static void main(String[] args) {
        try {
            System.out.println("=== 简化版PDF生成器测试 ===");
            
            String htmlPath = "f:\\owner_project\\ai-resume\\ai-resume-service\\simple-test.html";
            String pdfPath = "f:\\owner_project\\ai-resume\\ai-resume-service\\simple-test.pdf";
            
            // 读取HTML内容
            String htmlContent = readHtmlFile(htmlPath);
            System.out.println("HTML内容长度: " + htmlContent.length());
            
            // 生成PDF
            generatePdf(htmlContent, pdfPath);
            
            // 检查生成的文件
            File pdfFile = new File(pdfPath);
            if (pdfFile.exists()) {
                System.out.println("PDF生成成功! 文件大小: " + pdfFile.length() + " 字节");
                System.out.println("PDF文件路径: " + pdfFile.getAbsolutePath());
            } else {
                System.err.println("PDF生成失败: 文件未创建");
            }
            
        } catch (Exception e) {
            System.err.println("PDF生成失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 读取HTML文件
     */
    private static String readHtmlFile(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new FileNotFoundException("HTML文件不存在: " + filePath);
        }
        
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        
        return content.toString();
    }
    
    /**
     * 生成PDF
     */
    private static void generatePdf(String htmlContent, String outputPath) throws IOException {
        System.out.println("开始生成PDF...");
        
        try (OutputStream os = new FileOutputStream(outputPath)) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            
            // 基础配置
            builder.useFastMode();
            builder.withHtmlContent(htmlContent, new File(".").toURI().toString());
            
            // 页面设置 - A4纸张
            builder.useDefaultPageSize(210, 297, BaseRendererBuilder.PageSizeUnits.MM);
            
            // 加载中文字体
            loadChineseFonts(builder);
            
            // 输出到文件
            builder.toStream(os);
            builder.run();
            
            System.out.println("PDF生成完成!");
            
        } catch (Exception e) {
            System.err.println("PDF生成错误: " + e.getMessage());
            throw new IOException("PDF生成失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 加载中文字体
     */
    private static void loadChineseFonts(PdfRendererBuilder builder) {
        String[] fontPaths = {
            "C:/Windows/Fonts/msyh.ttc",      // 微软雅黑
            "C:/Windows/Fonts/simhei.ttf",    // 黑体
            "C:/Windows/Fonts/simsun.ttc",    // 宋体
            "C:/Windows/Fonts/simkai.ttf"     // 楷体
        };
        
        for (String fontPath : fontPaths) {
            File fontFile = new File(fontPath);
            if (fontFile.exists()) {
                try {
                    String fontName = getFontName(fontPath);
                    builder.useFont(fontFile, fontName);
                    System.out.println("成功加载字体: " + fontName + " (" + fontPath + ")");
                } catch (Exception e) {
                    System.out.println("字体加载失败: " + fontPath + " - " + e.getMessage());
                }
            } else {
                System.out.println("字体文件不存在: " + fontPath);
            }
        }
    }
    
    /**
     * 获取字体名称
     */
    private static String getFontName(String fontPath) {
        String fileName = new File(fontPath).getName();
        fileName = fileName.substring(0, fileName.lastIndexOf('.'));
        
        switch (fileName.toLowerCase()) {
            case "msyh": return "Microsoft YaHei";
            case "simhei": return "SimHei";
            case "simsun": return "SimSun";
            case "simkai": return "KaiTi";
            default: return fileName;
        }
    }
}