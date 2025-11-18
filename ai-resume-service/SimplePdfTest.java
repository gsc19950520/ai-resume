package com.aicv.airesume.utils;

import java.io.*;
import java.nio.file.*;

/**
 * 简化版PDF生成测试程序
 * 用于测试AdvancedHtmlToPdfGenerator的改进效果
 */
public class SimplePdfTest {
    
    public static void main(String[] args) {
        System.out.println("=== PDF生成测试开始 ===");
        
        try {
            // 读取测试HTML文件
            String htmlContent = readFile("test-super-minimal.html");
            System.out.println("HTML文件读取成功，长度: " + htmlContent.length() + " 字符");
            
            // 创建PDF生成器实例
            AdvancedHtmlToPdfGenerator generator = new AdvancedHtmlToPdfGenerator();
            System.out.println("PDF生成器实例创建成功");
            
            // 生成PDF文件
            String outputPath = "test-output-simple.pdf";
            generator.generatePdfFromHtml(htmlContent, outputPath);
            
            // 检查输出文件
            File pdfFile = new File(outputPath);
            if (pdfFile.exists()) {
                long fileSize = pdfFile.length();
                System.out.println("SUCCESS: PDF生成成功!");
                System.out.println("输出文件: " + outputPath);
                System.out.println("文件大小: " + (fileSize / 1024) + " KB");
                
                // 验证文件不是空的
                if (fileSize > 1000) {
                    System.out.println("文件大小合理，PDF生成正常");
                } else {
                    System.out.println("WARNING: 文件可能过小，请检查内容");
                }
            } else {
                System.out.println("ERROR: PDF文件未生成");
            }
            
        } catch (Exception e) {
            System.out.println("ERROR: PDF生成失败");
            System.out.println("错误信息: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("=== PDF生成测试结束 ===");
    }
    
    /**
     * 读取文件内容
     */
    private static String readFile(String fileName) throws IOException {
        Path filePath = Paths.get(fileName);
        if (!Files.exists(filePath)) {
            throw new FileNotFoundException("文件不存在: " + fileName);
        }
        
        return new String(Files.readAllBytes(filePath), "UTF-8");
    }
}