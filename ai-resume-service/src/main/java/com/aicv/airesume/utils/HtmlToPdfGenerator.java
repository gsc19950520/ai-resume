package com.aicv.airesume.utils;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HtmlToPdfGenerator {
    
    private static final Logger logger = Logger.getLogger(HtmlToPdfGenerator.class.getName());
    
    // 支持的字体列表
    private static final String[] FONT_PATHS = {
        "C:/Windows/Fonts/msyh.ttc",      // 微软雅黑
        "C:/Windows/Fonts/msyhbd.ttc",    // 微软雅黑粗体
        "C:/Windows/Fonts/simhei.ttf",    // 黑体
        "C:/Windows/Fonts/simsun.ttc",    // 宋体
        "C:/Windows/Fonts/simkai.ttf",    // 楷体
        "/System/Library/Fonts/PingFang.ttc", // 苹方 (macOS)
        "/System/Library/Fonts/STHeiti Light.ttc", // 华文黑体
        "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf" // Linux
    };

    public static void main(String[] args) {
        // 测试简单HTML
        String simpleHtmlFilePath = "F:\\owner_project\\ai-resume\\ai-resume-service\\test-simple.html";
        String simplePdfFilePath = "F:/test-simple.pdf";
        
        // 测试原始简历HTML
        String resumeHtmlFilePath = "F:\\owner_project\\ai-resume\\ai-resume-service\\test-resume.html";
        String resumePdfFilePath = "F:/resume-output.pdf";
        
        try {
            System.out.println("=== 测试简单HTML ===");
            System.out.println("HTML文件: " + simpleHtmlFilePath);
            System.out.println("输出文件: " + simplePdfFilePath);
            
            generatePdfFromFile(simpleHtmlFilePath, simplePdfFilePath);
            System.out.println("简单HTML PDF生成成功：" + simplePdfFilePath);
            
            File simplePdfFile = new File(simplePdfFilePath);
            if (simplePdfFile.exists() && simplePdfFile.length() > 0) {
                System.out.println("简单HTML PDF文件大小：" + (simplePdfFile.length() / 1024) + " KB");
            }
            
            System.out.println("\n=== 测试简历HTML ===");
            System.out.println("HTML文件: " + resumeHtmlFilePath);
            System.out.println("输出文件: " + resumePdfFilePath);
            
            generatePdfFromFile(resumeHtmlFilePath, resumePdfFilePath);
            System.out.println("简历PDF生成成功：" + resumePdfFilePath);
            
            File resumePdfFile = new File(resumePdfFilePath);
            if (resumePdfFile.exists() && resumePdfFile.length() > 0) {
                System.out.println("简历PDF文件大小：" + (resumePdfFile.length() / 1024) + " KB");
            } else {
                System.out.println("警告：PDF文件不存在或大小为0");
            }
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "PDF生成失败", e);
            System.err.println("错误详情: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 从HTML文件生成PDF
     */
    public static void generatePdfFromFile(String htmlFilePath, String outputPath) throws IOException {
        String htmlContent = readHtmlFile(htmlFilePath);
        generatePdfFromHtml(htmlContent, outputPath);
    }
    
    /**
     * 从HTML内容生成PDF
     */
    public static void generatePdfFromHtml(String htmlContent, String outputPath) throws IOException {
        logger.info("开始生成PDF...");
        
        // 预处理HTML内容
        String processedHtml = preprocessHtml(htmlContent);
        
        try (OutputStream os = new FileOutputStream(outputPath)) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            
            // 基础配置
            builder.useFastMode();
            builder.withHtmlContent(processedHtml, new File(".").toURI().toString());
            
            // 设置页面大小和边距 (A4: 210mm x 297mm)
            builder.useDefaultPageSize(210, 297, PdfRendererBuilder.PageSizeUnits.MM);
            
            // 加载所有可用字体
            loadFonts(builder);
            
            // 输出到流
            builder.toStream(os);
            
            // 运行生成
            builder.run();
            
            logger.info("PDF生成完成");
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "PDF生成失败", e);
            throw new IOException("PDF生成失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 预处理HTML内容，确保兼容性
     */
    private static String preprocessHtml(String htmlContent) {
        logger.info("预处理HTML内容...");
        
        // 1. 清理DOCTYPE - 完全移除DOCTYPE声明
        htmlContent = htmlContent.replaceAll("(?i)<!doctype[^>]*>", "");
        htmlContent = htmlContent.replaceAll("(?i)<\\?xml[^>]*>\\?", ""); // 移除XML声明
        
        // 2. 修复自闭合标签 - 确保meta标签正确关闭
        htmlContent = htmlContent.replaceAll("<meta([^>]*)>", "<meta$1/>");
        htmlContent = htmlContent.replaceAll("<link([^>]*)>", "<link$1/>");
        htmlContent = htmlContent.replaceAll("<br([^>]*)>", "<br$1/>");
        htmlContent = htmlContent.replaceAll("<hr([^>]*)>", "<hr$1/>");
        htmlContent = htmlContent.replaceAll("<img([^>]*)>", "<img$1/>");
        
        // 3. 确保基本的HTML结构
        if (!htmlContent.toLowerCase().contains("<html")) {
            htmlContent = "<html>" + htmlContent + "</html>";
        }
        
        // 4. 添加基础CSS样式
        String baseStyles = "<style>\n" +
            "* { box-sizing: border-box; }\n" +
            "body { margin: 0; padding: 0; font-family: Arial, sans-serif; }\n" +
            "table { border-collapse: collapse; }\n" +
            "img { max-width: 100%; height: auto; }\n" +
            "</style>\n";
        
        // 添加样式到head标签
        if (htmlContent.toLowerCase().contains("<head>")) {
            htmlContent = htmlContent.replaceAll("(?i)<head>", "<head>\n" + baseStyles);
        } else {
            // 如果没有head标签，在body之前添加
            htmlContent = htmlContent.replaceAll("(?i)<body>", "<head>\n" + baseStyles + "</head>\n<body>");
        }
        
        // 5. 处理图片路径
        htmlContent = processImagePaths(htmlContent);
        
        // 6. 处理CSS兼容性
        htmlContent = processCssCompatibility(htmlContent);
        
        return htmlContent;
    }
    
    /**
     * 处理图片路径
     */
    private static String processImagePaths(String htmlContent) {
        // 将本地文件路径转换为data URI或处理为相对路径
        return htmlContent.replaceAll("src=\"([A-Za-z]:\\\\[^\"]+)\"", "src=\"data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNzAiIGhlaWdodD0iNzAiIHZpZXdCb3g9IjAgMCA3MCA3MCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48Y2lyY2xlIGN4PSIzNSIgY3k9IjM1IiByPSIzNSIgZmlsbD0iI0RERCIvPjxzdmcgeD0iMTUiIHk9IjE1IiB3aWR0aD0iNDAiIGhlaWdodD0iNDAiIHZpZXdCb3g9IjAgMCAyNCAyNCIgZmlsbD0ibm9uZSI+PHBhdGggZD0iTTEyIDEyQzE0LjIwOTEgMTIgMTYgMTAuMjA5MSAxNiA4QzE2IDUuNzkwODYgMTQuMjA5MSA0IDEyIDRDOS43OTA4NiA0IDggNS43OTA4NiA4IDhDOCAxMC4yMDkxIDkuNzkwODYgMTIgMTIgMTJaIiBmaWxsPSIjOTk5Ii8+PHBhdGggZD0iTTEyIDE0QzcuNTgxNzIgMTQgNCAxNy41ODE3IDQgMjJIMjBDMjAgMTcuNTgxNyAxNi40MTgzIDE0IDEyIDE0WiIgZmlsbD0iIzk5OSIvPjwvc3ZnPjwvc3ZnPg==\"");
    }
    
    /**
     * 处理CSS兼容性
     */
    private static String processCssCompatibility(String htmlContent) {
        // 1. 处理RGBA颜色
        htmlContent = htmlContent.replaceAll("rgba\\((\\d+),\\s*(\\d+),\\s*(\\d+),\\s*([\\d.]+)\\)", 
            rgbaToRgb(htmlContent));
        
        // 2. 处理CSS变量（如果有）
        htmlContent = htmlContent.replaceAll("var\\(--[^)]+\\)", "#333333");
        
        // 3. 处理现代CSS特性
        htmlContent = htmlContent.replaceAll("display:\\s*flex", "display: block");
        htmlContent = htmlContent.replaceAll("display:\\s*grid", "display: block");
        
        return htmlContent;
    }
    
    /**
     * 将RGBA转换为RGB
     */
    private static String rgbaToRgb(String htmlContent) {
        // 简化处理：将RGBA转换为RGB，忽略透明度
        return htmlContent.replaceAll("rgba\\((\\d+),\\s*(\\d+),\\s*(\\d+),\\s*[\\d.]+\\)", "rgb($1, $2, $3)");
    }
    
    /**
     * 加载所有可用字体
     */
    private static void loadFonts(PdfRendererBuilder builder) throws IOException {
        logger.info("加载字体...");
        
        boolean fontLoaded = false;
        
        for (String fontPath : FONT_PATHS) {
            File fontFile = new File(fontPath);
            if (fontFile.exists()) {
                try {
                    String fontName = getFontNameFromPath(fontPath);
                    builder.useFont(fontFile, fontName);
                    logger.info("加载字体成功: " + fontName + " (" + fontPath + ")");
                    fontLoaded = true;
                } catch (Exception e) {
                    logger.warning("加载字体失败: " + fontPath + " - " + e.getMessage());
                }
            }
        }
        
        if (!fontLoaded) {
            logger.warning("未找到任何可用字体，将使用默认字体");
        }
    }
    
    /**
     * 从字体路径获取字体名称
     */
    private static String getFontNameFromPath(String fontPath) {
        String fileName = new File(fontPath).getName();
        fileName = fileName.substring(0, fileName.lastIndexOf('.'));
        
        // 映射常见字体名称
        switch (fileName.toLowerCase()) {
            case "msyh": return "Microsoft YaHei";
            case "msyhbd": return "Microsoft YaHei Bold";
            case "simhei": return "SimHei";
            case "simsun": return "SimSun";
            case "simkai": return "SimKai";
            case "pingfang": return "PingFang SC";
            case "stheiti": return "STHeiti";
            case "liberationsans-regular": return "Liberation Sans";
            default: return fileName;
        }
    }
    
    /**
     * 读取HTML文件
     */
    private static String readHtmlFile(String filePath) throws IOException {
        logger.info("读取HTML文件: " + filePath);
        
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new FileNotFoundException("HTML文件不存在: " + filePath);
        }
        
        return new String(Files.readAllBytes(path), "UTF-8");
    }
}
