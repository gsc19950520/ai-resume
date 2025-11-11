package com.aicv.airesume.service.impl;

import com.aicv.airesume.entity.Resume;
import com.aicv.airesume.entity.Template;
import com.aicv.airesume.service.TemplateRendererService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.templateresolver.StringTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;

import org.xhtmlrenderer.pdf.ITextRenderer;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.BaseFont;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 模板渲染服务实现类
 */
@Service
public class TemplateRendererServiceImpl implements TemplateRendererService {

    private static final Logger logger = LoggerFactory.getLogger(TemplateRendererServiceImpl.class);



    @Override
    public String generateHtmlTemplateFromWord(InputStream wordTemplateStream) throws Exception {
        logger.info("开始从Word模板生成HTML模板");
        
        // 使用Apache POI读取Word文档
        XWPFDocument document = new XWPFDocument(wordTemplateStream);
        StringBuilder htmlBuilder = new StringBuilder();
        
        // 创建HTML头部，添加更完整的内联样式以保留Word格式
        htmlBuilder.append("<!DOCTYPE html>")
                .append("<html lang=\"zh-CN\">")
                .append("<head>")
                .append("<meta charset=\"UTF-8\">")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">")
                .append("<title>简历模板</title>")
                .append("<style>")
                .append("body { font-family: 'Microsoft YaHei', SimSun, Arial, sans-serif; margin: 20px; line-height: 1.6; color: #333; }")
                .append("h1, h2, h3, h4, h5, h6 { color: #2c3e50; margin-top: 16px; margin-bottom: 8px; }")
                .append("h1 { font-size: 24pt; }")
                .append("h2 { font-size: 18pt; }")
                .append("h3 { font-size: 16pt; }")
                .append("h4 { font-size: 14pt; }")
                .append(".section { margin-bottom: 24px; padding-bottom: 16px; border-bottom: 1px solid #eee; }")
                .append(".info-item { margin-bottom: 10px; display: flex; }")
                .append(".info-label { font-weight: bold; margin-right: 8px; min-width: 80px; }")
                .append(".info-value { flex: 1; }")
                .append(".table-container { margin: 16px 0; overflow-x: auto; }")
                .append("table { border-collapse: collapse; width: 100%; }")
                .append("th, td { border: 1px solid #ddd; padding: 8px 12px; text-align: left; }")
                .append("th { background-color: #f5f5f5; font-weight: bold; }")
                .append("tr:nth-child(even) { background-color: #f9f9f9; }")
                .append(".experience-item { margin-bottom: 16px; }")
                .append(".experience-header { font-weight: bold; }")
                .append(".experience-date { color: #666; font-size: 0.9em; }")
                .append(".experience-description { margin-top: 8px; }")
                .append(".skill-tag { display: inline-block; background-color: #e8f4fc; border: 1px solid #b8e0f8; border-radius: 4px; padding: 2px 8px; margin-right: 8px; margin-bottom: 8px; }")
                .append(".objective { font-style: italic; color: #555; margin-bottom: 16px; }")
                .append(".profile { margin-bottom: 20px; line-height: 1.8; }")
                .append(".highlight { background-color: #fff9c4; }")
                .append(".subtitle { font-size: 14pt; color: #555; margin-bottom: 12px; }")
                .append("ul, ol { padding-left: 24px; }")
                .append("li { margin-bottom: 4px; }")
                .append("hr { border: 0; height: 1px; background-color: #eee; margin: 20px 0; }")
                .append("@media print { body { font-size: 11pt; } }")
                .append("</style>")
                .append("</head>")
                .append("<body>");
        
        // 处理文档中的段落
        int paragraphCount = 0;
        for (XWPFParagraph paragraph : document.getParagraphs()) {
            paragraphCount++;
            
            try {
                String text = paragraph.getText();
                
                if (text.isEmpty()) {
                    // 添加空行
                    htmlBuilder.append("<br/>");
                    continue;
                }
                
                logger.debug("处理段落 {}: {}", paragraphCount, text.length() > 50 ? text.substring(0, 50) + "..." : text);
                
                // 处理段落样式和替换变量占位符
                String processedText = processParagraph(text);
                
                // 构建样式字符串
                StringBuilder styleBuilder = new StringBuilder();
                
                // 检查段落对齐方式
                if (paragraph.getAlignment() != null) {
                    switch (paragraph.getAlignment()) {
                        case CENTER:
                            styleBuilder.append("text-align: center; ");
                            break;
                        case RIGHT:
                            styleBuilder.append("text-align: right; ");
                            break;
                        case BOTH:
                            styleBuilder.append("text-align: justify; ");
                            break;
                        default:
                            styleBuilder.append("text-align: left; ");
                    }
                }
                
                // 获取段落缩进 - 只对普通段落应用缩进
                if (paragraph.getIndentationFirstLine() > 0 && !isSpecialElement(text)) {
                    styleBuilder.append("text-indent: " + (paragraph.getIndentationFirstLine() / 20) + "em; ");
                }
                
                // 获取段落间距
                if (paragraph.getSpacingBefore() > 0) {
                    styleBuilder.append("margin-top: " + (paragraph.getSpacingBefore() / 240) + "em; ");
                }
                if (paragraph.getSpacingAfter() > 0) {
                    styleBuilder.append("margin-bottom: " + (paragraph.getSpacingAfter() / 240) + "em; ");
                }
                
                // 从段落中提取字体样式
                boolean hasBold = false;
                boolean hasItalic = false;
                boolean hasUnderline = false;
                Integer fontSize = null;
                String fontFamily = null;
                
                // 分析所有Run，找出最常见的样式
                for (XWPFRun run : paragraph.getRuns()) {
                    if (run.isBold()) hasBold = true;
                    if (run.isItalic()) hasItalic = true;
                    // 检查是否有下划线样式（非NONE状态）
                    if (run.getUnderline() != null && run.getUnderline() != org.apache.poi.xwpf.usermodel.UnderlinePatterns.NONE) hasUnderline = true;
                    if (run.getFontSize() != -1 && fontSize == null) fontSize = run.getFontSize();
                    if (fontFamily == null && run.getFontFamily() != null && !run.getFontFamily().isEmpty()) {
                        fontFamily = run.getFontFamily();
                    }
                }
                
                // 应用字体样式
                if (fontFamily != null) {
                    styleBuilder.append("font-family: '" + fontFamily + "', 'Microsoft YaHei', SimSun, Arial, sans-serif; ");
                }
                if (fontSize != null) {
                    styleBuilder.append("font-size: " + fontSize + "pt; ");
                }
                if (hasBold) {
                    styleBuilder.append("font-weight: bold; ");
                }
                if (hasItalic) {
                    styleBuilder.append("font-style: italic; ");
                }
                if (hasUnderline) {
                    styleBuilder.append("text-decoration: underline; ");
                }
            
                String style = styleBuilder.toString();
                
                // 根据文本特征判断段落类型并添加相应的HTML标签
                try {
                    // 标题识别（支持1-4级标题）
                    if (text.startsWith("#### ")) {
                        // 四级标题
                        htmlBuilder.append("<h4").append(style.isEmpty() ? "" : " style=\"" + style + "\"").append(">").append(processedText).append("</h4>");
                    }
                    else if (text.startsWith("### ")) {
                        // 三级标题
                        htmlBuilder.append("<h3").append(style.isEmpty() ? "" : " style=\"" + style + "\"").append(">").append(processedText).append("</h3>");
                    }
                    else if (text.startsWith("## ")) {
                        // 二级标题
                        htmlBuilder.append("<h2").append(style.isEmpty() ? "" : " style=\"" + style + "\"").append(">").append(processedText).append("</h2>");
                    }
                    else if (text.startsWith("# ")) {
                        // 一级标题
                        htmlBuilder.append("<h1").append(style.isEmpty() ? "" : " style=\"" + style + "\"").append(">").append(processedText).append("</h1>");
                    }
                    // 使用辅助方法判断求职意向
                    else if (isObjectiveSection(text, hasBold)) {
                        htmlBuilder.append("<div class=\"objective\"").append(style.isEmpty() ? "" : " style=\"" + style + "\"").append(">").append(processedText).append("</div>");
                    }
                    // 使用辅助方法判断信息项（支持中英文冒号）
                    else if (isInfoItem(text)) {
                        processInfoItem(htmlBuilder, text, style);
                        continue;
                    }
                    // 使用辅助方法判断副标题
                    else if (isSubtitle(text, hasBold, fontSize)) {
                        htmlBuilder.append("<div class=\"subtitle\"").append(style.isEmpty() ? "" : " style=\"" + style + "\"").append(">").append(processedText).append("</div>");
                    }
                    // 使用辅助方法判断个人简介
                    else if (isProfileSection(text, paragraphCount)) {
                        htmlBuilder.append("<div class=\"profile\"").append(style.isEmpty() ? "" : " style=\"" + style + "\"").append(">").append(processedText).append("</div>");
                    }
                    // 使用辅助方法判断技能列表
                    else if (isSkillsSection(text)) {
                        processSkillsSection(htmlBuilder, text);
                    }
                    // 普通段落
                    else {
                        htmlBuilder.append("<p").append(style.isEmpty() ? "" : " style=\"" + style + "\"").append(">").append(processedText).append("</p>");
                    }
                } catch (Exception e) {
                    logger.error("处理段落类型识别时发生错误: {}, 段落文本: {}", e.getMessage(), text);
                    // 出错时作为普通段落处理
                    htmlBuilder.append("<p").append(style.isEmpty() ? "" : " style=\"" + style + "\"").append(">").append(processedText).append("</p>");
                }
            } catch (Exception e) {
                logger.error("处理段落时发生异常: {}", e.getMessage());
                // 出错时继续处理下一段落
            }
        }
        
        // 处理文档中的表格
        for (int i = 0; i < document.getTables().size(); i++) {
            org.apache.poi.xwpf.usermodel.XWPFTable table = document.getTables().get(i);
            htmlBuilder.append("<div class=\"table-container\">");
            htmlBuilder.append("<table>");
            
            // 处理表头行和数据行
            boolean isFirstRow = true;
            for (org.apache.poi.xwpf.usermodel.XWPFTableRow row : table.getRows()) {
                StringBuilder rowStyle = new StringBuilder();
                
                // 为偶数行添加斑马纹样式，增强可读性
                if (!isFirstRow && i % 2 == 0) {
                    rowStyle.append("background-color: #f8f9fa; ");
                }
                
                htmlBuilder.append("<tr");
                if (rowStyle.length() > 0) {
                    htmlBuilder.append(" style=\"").append(rowStyle.toString()).append("\"");
                }
                htmlBuilder.append(">\n");

                for (org.apache.poi.xwpf.usermodel.XWPFTableCell cell : row.getTableCells()) {
                    String cellText = cell.getText();
                    // 处理变量占位符
                    String processedText = processParagraph(cellText);
                    
                    // 为单元格构建样式
                    StringBuilder cellStyle = new StringBuilder();
                    
                    // 为变量占位符添加高亮样式，便于识别
                    if (!processedText.isEmpty() && (processedText.startsWith("$") || processedText.contains("{{"))) {
                        cellStyle.append("color: #0288d1; font-weight: 500; background-color: #e3f2fd; ");
                    }
                    
                    String cellTag = isFirstRow ? "th" : "td";
                    if (cellStyle.length() > 0) {
                        htmlBuilder.append("<").append(cellTag).append(" style=\"").append(cellStyle.toString()).append("\">");
                    } else {
                        htmlBuilder.append("<").append(cellTag).append(">");
                    }
                    htmlBuilder.append(processedText);
                    htmlBuilder.append("</").append(cellTag).append(">");
                }
                
                htmlBuilder.append("</tr>");
                isFirstRow = false;
            }
            
            htmlBuilder.append("</table>");
            htmlBuilder.append("</div>");
        }
        
        htmlBuilder.append("</body>")
                .append("</html>");
        
        // 清理资源
        try {
            document.close();
        } catch (IOException e) {
            logger.warn("关闭Word文档时发生异常: {}", e.getMessage());
        }
        
        logger.info("Word转HTML完成，共处理 {} 个段落，生成的HTML长度: {} 字符", paragraphCount, htmlBuilder.length());
        return htmlBuilder.toString();
    }
    
    /**
     * 判断是否为特殊元素（不需要默认缩进的元素）
     */
    private boolean isSpecialElement(String text) {
        // 检查是否包含特殊区块的关键词
        String[] specialKeywords = {"求职意向", "求职目标", "职业目标", "个人简介", "技能", "Skill", "Skills", ":", "："};
        for (String keyword : specialKeywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 判断是否为求职意向区块
     */
    private boolean isObjectiveSection(String text, boolean hasBold) {
        String[] objectiveKeywords = {"求职意向", "求职目标", "职业目标", "期望职位", "意向岗位"};
        
        // 检查是否包含求职意向关键词
        for (String keyword : objectiveKeywords) {
            if (text.contains(keyword)) {
                // 如果文本中只包含关键词（可能是标题）或者包含关键词且是粗体，则认为是求职意向
                if (text.trim().equals(keyword) || hasBold) {
                    return true;
                }
                // 即使不是粗体，如果内容明显是求职意向描述，也返回true
                if (text.length() > keyword.length() + 10) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * 判断是否为信息项（如姓名: 张三）
     */
    private boolean isInfoItem(String text) {
        // 支持中英文冒号
        if ((text.contains(":") || text.contains("：")) && text.length() < 100) { // 长度限制避免误判
            String[] parts = text.split("[:：]", 2);
            if (parts.length == 2) {
                String label = parts[0].trim();
                String value = parts[1].trim();
                // 确保标签不为空且值也不为空，并且标签长度合理
                return !label.isEmpty() && !value.isEmpty() && label.length() <= 20;
            }
        }
        return false;
    }
    
    /**
     * 处理信息项，生成对应的HTML
     */
    private void processInfoItem(StringBuilder htmlBuilder, String text, String style) {
        String delimiter = text.contains(":") ? ":" : "：";
        String[] parts = text.split(delimiter, 2);
        if (parts.length == 2) {
            String label = parts[0].trim();
            String value = parts[1].trim();
            
            htmlBuilder.append("<div class=\"info-item\"")
                      .append(style.isEmpty() ? "" : " style=\"" + style + "\"")
                      .append(">")
                      .append("<span class=\"info-label\">").append(label).append(delimiter).append("</span>")
                      .append("<span class=\"info-value\">").append(value).append("</span>")
                      .append("</div>");
        }
    }
    
    /**
     * 判断是否为副标题
     */
    private boolean isSubtitle(String text, boolean hasBold, Integer fontSize) {
        // 基于多个特征判断：粗体、适当的字体大小、合理的长度
        return hasBold && 
               fontSize != null && fontSize >= 14 && fontSize <= 18 && 
               !text.contains(":") && !text.contains("：") &&
               text.length() > 2 && text.length() < 50;
    }
    
    /**
     * 判断是否为个人简介区块
     */
    private boolean isProfileSection(String text, int paragraphCount) {
        // 检查是否包含简介相关关键词和合理的长度
        String[] profileKeywords = {"简介", "概述", "总结", "经历", "背景", "自评"};
        
        for (String keyword : profileKeywords) {
            if (text.contains(keyword) && text.length() > 50) {
                return true;
            }
        }
        
        // 如果是文档前几个段落且长度较长，也可能是个人简介
        return paragraphCount <= 5 && text.length() > 100;
    }
    
    /**
     * 判断是否为技能列表区块
     */
    private boolean isSkillsSection(String text) {
        String[] skillKeywords = {"技能", "Skill", "Skills", "技术栈", "专长", "能力"};
        String[] separators = {",", "、", "；", ";", "，"};
        
        // 检查是否包含技能相关关键词
        boolean hasSkillKeyword = false;
        for (String keyword : skillKeywords) {
            if (text.contains(keyword)) {
                hasSkillKeyword = true;
                break;
            }
        }
        
        // 检查是否包含分隔符
        boolean hasSeparator = false;
        for (String separator : separators) {
            if (text.contains(separator)) {
                hasSeparator = true;
                break;
            }
        }
        
        return hasSkillKeyword && hasSeparator;
    }
    
    /**
     * 处理技能列表，生成标签样式的HTML
     */
    private void processSkillsSection(StringBuilder htmlBuilder, String text) {
        htmlBuilder.append("<div class=\"skills-section\">");
        
        // 使用正则表达式分割技能，支持多种分隔符
        String[] skills = text.split("[,，；;]");
        
        for (String skill : skills) {
            String trimmedSkill = skill.trim();
            if (!trimmedSkill.isEmpty()) {
                // 过滤掉可能的标签前缀
                if (trimmedSkill.contains(":") || trimmedSkill.contains("：")) {
                    String delimiter = trimmedSkill.contains(":") ? ":" : "：";
                    String[] skillParts = trimmedSkill.split(delimiter, 2);
                    if (skillParts.length == 2) {
                        trimmedSkill = skillParts[1].trim();
                    }
                }
                
                // 过滤掉技能关键词本身
                String[] skillKeywords = {"技能", "Skill", "Skills", "技术栈", "专长", "能力"};
                for (String keyword : skillKeywords) {
                    if (trimmedSkill.contains(keyword)) {
                        trimmedSkill = trimmedSkill.replace(keyword, "").trim();
                        break;
                    }
                }
                
                if (!trimmedSkill.isEmpty()) {
                    htmlBuilder.append("<span class=\"skill-tag\">").append(trimmedSkill).append("</span>");
                }
            }
        }
        
        htmlBuilder.append("</div>");
    }
    
    /**
     * 处理段落文本，主要处理变量占位符
     */
    private String processParagraph(String text) {
        // 这里可以添加更多的文本处理逻辑，目前主要是保持变量占位符原样
        // 例如：${variable} 或 {{variable}} 等格式的占位符
        return text;
    }

    /**
     * 将HTML模板转换为Thymeleaf模板格式
     */
    private String convertToThymeleafTemplate(String htmlTemplate) {
        // 使用Jsoup解析HTML
        Document doc = Jsoup.parse(htmlTemplate);
        
        // 确保body标签存在
        Element body = doc.body();
        
        // 这里可以添加额外的Thymeleaf命名空间等
        // 简化实现，实际项目中可能需要更复杂的转换
        
        return doc.html();
    }

    /**
     * 生成默认的HTML模板
     */
    private String generateDefaultHtmlTemplate(Map<String, Object> resumeData) {
        StringBuilder htmlBuilder = new StringBuilder();
        
        htmlBuilder.append("<!DOCTYPE html>")
                .append("<html lang=\"zh-CN\">")
                .append("<head>")
                .append("<meta charset=\"UTF-8\">")
                .append("<title>简历</title>")
                .append("<style>")
                .append("body { font-family: 'Microsoft YaHei', Arial, sans-serif; margin: 0; padding: 0; }")
                .append(".container { max-width: 800px; margin: 0 auto; padding: 20px; }")
                .append("h1 { color: #2c3e50; margin-bottom: 10px; }")
                .append("h2 { color: #34495e; border-bottom: 1px solid #eee; padding-bottom: 5px; }")
                .append(".info { margin-bottom: 20px; }")
                .append(".section { margin-bottom: 30px; }")
                .append("</style>")
                .append("</head>")
                .append("<body>")
                .append("<div class=\"container\">")
                .append("<h1>").append(resumeData.getOrDefault("name", "")).append("</h1>")
                .append("<div class=\"info\">")
                .append("<p>邮箱: " + resumeData.getOrDefault("email", "") + "</p>")
                .append("<p>电话: " + resumeData.getOrDefault("phone", "") + "</p>")
                .append("<p>地址: " + resumeData.getOrDefault("address", "") + "</p>")
                .append(resumeData.getOrDefault("birthDate", "").toString().isEmpty() ? "" : "<p>出生日期: " + resumeData.getOrDefault("birthDate", "") + "</p>")
                .append("<p>应聘职位: " + resumeData.getOrDefault("jobType", "") + "</p>")
                .append("</div>");
        
        // 添加其他部分
        htmlBuilder.append("</div>")
                .append("</body>")
                .append("</html>");
        
        return htmlBuilder.toString();
    }

    /**
     * 向Word文档添加带文本的段落
     */
    private void addParagraphWithText(XWPFDocument document, String text) {
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setText(text);
    }

    @Override
    public String generateHtmlFromLocalWordTemplate(Template template) throws Exception {
        logger.info("开始从本地Word模板生成HTML，模板ID: {}", template.getId());
        
        // 这里简化实现，实际项目中需要从文件系统或数据库读取模板文件
        // 然后调用generateHtmlTemplateFromWord方法生成HTML
        
        // 暂时返回一个简单的HTML模板
        return generateDefaultHtmlTemplate(Collections.emptyMap());
    }
    
    /**
     * 渲染HTML模板
     * @param htmlTemplate HTML模板内容
     * @param resumeData 简历数据
     * @return 渲染后的HTML内容
     * @throws Exception 渲染过程中可能出现的异常
     */
    @Autowired
    private SpringTemplateEngine springTemplateEngine;
    
    @Override
    public String renderHtmlTemplate(String htmlTemplate, Map<String, Object> resumeData) throws Exception {
        logger.info("开始渲染HTML模板");
        
        try {
            // 将HTML转换为Thymeleaf模板格式
            String thymeleafTemplate = convertToThymeleafTemplate(htmlTemplate);
            
            // 创建Thymeleaf上下文并设置数据
            Context context = new Context();
            context.setVariables(resumeData);
            
            // 使用Spring自动配置的TemplateEngine，避免手动创建带来的依赖问题
            String renderedHtml = springTemplateEngine.process(thymeleafTemplate, context);
            
            logger.info("HTML模板渲染完成，生成的HTML长度: {} 字符", renderedHtml.length());
            return renderedHtml;
        } catch (Exception e) {
            logger.error("渲染HTML模板时发生异常: {}", e.getMessage(), e);
            throw new RuntimeException("HTML模板渲染失败", e);
        }
    }

    /**
     * 将HTML转换为PDF
     * @param htmlContent HTML内容
     * @return PDF字节数组
     * @throws Exception 转换过程中可能出现的异常
     */
    @Override
    public byte[] convertHtmlToPdf(String htmlContent) throws Exception {
        logger.info("开始将HTML转换为PDF");
        
        ByteArrayOutputStream outputStream = null;
        ITextRenderer renderer = null;
        
        try {
            // 创建输出流
            outputStream = new ByteArrayOutputStream();
            
            // 创建ITextRenderer实例
            renderer = new ITextRenderer();
            
            // 设置中文字体支持
            renderer.getFontResolver().addFont("C:/Windows/Fonts/simsun.ttc", BaseFont.IDENTITY_H, BaseFont.NOT_EMBEDDED);
            renderer.getFontResolver().addFont("C:/Windows/Fonts/msyh.ttc", BaseFont.IDENTITY_H, BaseFont.NOT_EMBEDDED);
            
            // 设置PDF文档大小为A4
            renderer.setDocumentFromString(htmlContent);
            renderer.layout();
            
            // 渲染为PDF
            renderer.createPDF(outputStream);
            
            byte[] pdfBytes = outputStream.toByteArray();
            logger.info("HTML转PDF完成，生成的PDF大小: {} KB", pdfBytes.length / 1024.0);
            
            return pdfBytes;
        } catch (Exception e) {
            logger.error("HTML转PDF时发生异常: {}", e.getMessage(), e);
            throw new RuntimeException("HTML转PDF失败", e);
        } finally {
            // 清理资源
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    logger.warn("关闭输出流时发生异常: {}", e.getMessage());
                }
            }
            if (renderer != null) {
                renderer.finishPDF();
            }
        }
    }
    
    /**
     * 渲染简历并生成PDF
     * @param template 模板对象
     * @param resume 简历对象
     * @return PDF字节数组
     * @throws Exception 渲染过程中可能出现的异常
     */
    @Override
    public byte[] renderResumeToPdf(Template template, Resume resume) throws Exception {
        logger.info("开始渲染简历为PDF，模板ID: {}, 简历ID: {}", template.getId(), resume.getId());
        
        try {
            // 格式化简历数据
            Map<String, Object> formattedData = formatResumeData(resume);
            
            // 从模板生成HTML
            String htmlTemplate = generateHtmlFromLocalWordTemplate(template);
            
            // 渲染HTML
            String renderedHtml = renderHtmlTemplate(htmlTemplate, formattedData);
            
            // 转换为PDF
            return convertHtmlToPdf(renderedHtml);
        } catch (Exception e) {
            logger.error("渲染简历为PDF时发生异常: {}", e.getMessage(), e);
            throw new RuntimeException("简历渲染为PDF失败", e);
        }
    }
    
    /**
     * 渲染简历并生成Word
     * @param template 模板对象
     * @param resume 简历对象
     * @return Word字节数组
     * @throws Exception 渲染过程中可能出现的异常
     */
    @Override
    public byte[] renderResumeToWord(Template template, Resume resume) throws Exception {
        logger.info("开始渲染简历为Word，模板ID: {}, 简历ID: {}", template.getId(), resume.getId());
        
        try {
            // 格式化简历数据
            Map<String, Object> formattedData = formatResumeData(resume);
            
            // 这里简化实现，实际项目中可能需要更复杂的Word文档生成逻辑
            // 可以使用Apache POI的XWPFDocument来生成Word文档
            
            // 暂时返回空字节数组，实际项目中需要实现具体的Word生成逻辑
            return new byte[0];
        } catch (Exception e) {
            logger.error("渲染简历为Word时发生异常: {}", e.getMessage(), e);
            throw new RuntimeException("简历渲染为Word失败", e);
        }
    }
    
    /**
     * 将简历数据转换为模板渲染所需的数据格式
     * @param resume 简历对象
     * @return 格式化后的简历数据
     */
    @Override
    public Map<String, Object> formatResumeData(Resume resume) {
        logger.info("开始格式化简历数据，简历ID: {}", resume.getId());
        
        Map<String, Object> formattedData = new HashMap<>();
        
        try {
            // 将简历对象的字段映射到模板所需的数据格式
            formattedData.put("id", resume.getId());
            formattedData.put("name", resume.getName());
            formattedData.put("email", resume.getEmail());
            formattedData.put("phone", resume.getPhone());
            formattedData.put("address", resume.getAddress());
            formattedData.put("birthDate", resume.getBirthDate());
            formattedData.put("jobType", resume.getJobType());
            formattedData.put("education", resume.getEducation());
            formattedData.put("workExperience", resume.getWorkExperience());
            formattedData.put("skills", resume.getSkills());
            formattedData.put("projects", resume.getProjects());
            formattedData.put("objective", resume.getObjective());
            formattedData.put("profile", resume.getProfile());
            
            // 解析JSON字段为对象或列表
            if (resume.getEducation() != null) {
                formattedData.put("educationList", JSONArray.parseArray(resume.getEducation(), JSONObject.class));
            }
            
            if (resume.getWorkExperience() != null) {
                formattedData.put("workExperienceList", JSONArray.parseArray(resume.getWorkExperience(), JSONObject.class));
            }
            
            if (resume.getSkills() != null) {
                formattedData.put("skillList", JSONArray.parseArray(resume.getSkills(), String.class));
            }
            
            if (resume.getProjects() != null) {
                formattedData.put("projectList", JSONArray.parseArray(resume.getProjects(), JSONObject.class));
            }
            
            logger.info("简历数据格式化完成");
            return formattedData;
        } catch (Exception e) {
            logger.error("格式化简历数据时发生异常: {}", e.getMessage(), e);
            throw new RuntimeException("简历数据格式化失败", e);
        }
    }
}