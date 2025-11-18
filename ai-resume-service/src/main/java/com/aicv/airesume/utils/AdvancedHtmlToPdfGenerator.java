package com.aicv.airesume.utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities;
import org.jsoup.select.Elements;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder;

import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.Base64;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * 高级HTML到PDF生成器
 * 确保生成的PDF与HTML展示完全一致
 */
public class AdvancedHtmlToPdfGenerator {
    
    private static final Logger logger = Logger.getLogger(AdvancedHtmlToPdfGenerator.class.getName());
    
    // 字体配置
    private static final Map<String, String[]> FONT_CONFIGS = new HashMap<>();
    static {
        FONT_CONFIGS.put("windows", new String[]{
            "C:/Windows/Fonts/msyh.ttc",      // 微软雅黑
            "C:/Windows/Fonts/msyhbd.ttc",    // 微软雅黑粗体
            "C:/Windows/Fonts/simhei.ttf",    // 黑体
            "C:/Windows/Fonts/simsun.ttc",    // 宋体
            "C:/Windows/Fonts/simkai.ttf",    // 楷体
            "C:/Windows/Fonts/arial.ttf",     // Arial
            "C:/Windows/Fonts/arialbd.ttf"  // Arial Bold
        });
        
        FONT_CONFIGS.put("macos", new String[]{
            "/System/Library/Fonts/PingFang.ttc",
            "/System/Library/Fonts/STHeiti Light.ttc",
            "/System/Library/Fonts/Helvetica.ttc"
        });
        
        FONT_CONFIGS.put("linux", new String[]{
            "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf",
            "/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf",
            "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
        });
    }
    
    // CSS兼容性映射
    private static final Map<String, String> CSS_COMPATIBILITY = new HashMap<>();
    static {
        // 现代CSS特性映射
        CSS_COMPATIBILITY.put("display: flex", "display: block");
        CSS_COMPATIBILITY.put("display: grid", "display: table");
        CSS_COMPATIBILITY.put("justify-content", "text-align");
        CSS_COMPATIBILITY.put("align-items", "vertical-align");
        CSS_COMPATIBILITY.put("flex-direction", "display");
        CSS_COMPATIBILITY.put("flex-wrap", "white-space");
        CSS_COMPATIBILITY.put("gap", "margin");
        CSS_COMPATIBILITY.put("place-items", "text-align");
        CSS_COMPATIBILITY.put("place-content", "text-align");
    }
    
    private final PdfRendererBuilder builder;
    private final Map<String, Object> config;
    
    public AdvancedHtmlToPdfGenerator() {
        this.builder = new PdfRendererBuilder();
        this.config = new HashMap<>();
        initializeDefaultConfig();
    }
    
    /**
     * 主方法 - 用于测试
     */
    public static void main(String[] args) {
        try {
            String htmlFilePath = "F:\\owner_project\\ai-resume\\ai-resume-service\\src\\main\\resources\\templates\\resume.html";
            String pdfFilePath = "F:/resume_advanced.pdf";
            
            AdvancedHtmlToPdfGenerator generator = new AdvancedHtmlToPdfGenerator();
            
            // 配置选项
            generator.configure("pageSize", "A4");
            generator.configure("margin", "20mm");
            generator.configure("enableSvg", true);
            generator.configure("enableMathML", false);
            generator.configure("enableForms", false);
            
            // 生成PDF
            generator.generatePdfFromFile(htmlFilePath, pdfFilePath);
            
            // 验证结果
            File pdfFile = new File(pdfFilePath);
            if (pdfFile.exists() && pdfFile.length() > 0) {
                System.out.println("PDF生成成功：" + pdfFilePath);
                System.out.println("文件大小：" + (pdfFile.length() / 1024) + " KB");
            }
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "PDF生成失败", e);
        }
    }
    
    /**
     * 初始化默认配置
     */
    private void initializeDefaultConfig() {
        config.put("pageSize", "A4");
        config.put("margin", "20mm");
        config.put("enableSvg", true);
        config.put("enableMathML", false);
        config.put("enableForms", false);
        config.put("enableBookmarks", true);
        config.put("compress", true);
        config.put("colorProfile", "sRGB");
    }
    
    /**
     * 设置配置选项
     */
    public void configure(String key, Object value) {
        config.put(key, value);
    }
    
    /**
     * 从HTML文件生成PDF
     */
    public void generatePdfFromFile(String htmlFilePath, String outputPath) throws IOException {
        logger.info("从文件生成PDF: " + htmlFilePath);
        
        String htmlContent = readHtmlFile(htmlFilePath);
        generatePdfFromHtml(htmlContent, outputPath);
    }
    
    /**
     * 从HTML内容生成PDF
     */
    public void generatePdfFromHtml(String htmlContent, String outputPath) throws IOException {
        logger.info("开始生成PDF...");
        
        // 1. 预处理HTML内容
        String processedHtml = preprocessHtml(htmlContent);
        
        // 调试：保存预处理后的HTML到文件
        try {
            Files.write(Paths.get("F:/debug_processed.html"), processedHtml.getBytes("UTF-8"));
            logger.info("预处理后的HTML已保存到: F:/debug_processed.html");
        } catch (Exception e) {
            logger.warning("保存调试文件失败: " + e.getMessage());
        }
        
        // 2. 配置PDF生成器
        configurePdfBuilder();
        
        // 3. 生成PDF - 使用更robust的方式
        try (OutputStream os = new FileOutputStream(outputPath)) {
            // 使用文件URI作为基础路径
            String baseUri = new File(".").toURI().toString();
            logger.info("使用基础URI: " + baseUri);
            
            // 确保HTML内容是有效的XML格式
            String xmlCompatibleHtml = makeXmlCompatible(processedHtml);
            
            builder.withHtmlContent(xmlCompatibleHtml, baseUri);
            builder.toStream(os);
            builder.run();
            
            logger.info("PDF生成完成: " + outputPath);
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "PDF生成失败", e);
            throw new IOException("PDF生成失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 预处理HTML内容 - 优化样式保真度
     */
    private String preprocessHtml(String htmlContent) {
        logger.info("预处理HTML内容...");

        try {
            // 1. 首先验证HTML内容是否有效
        if (htmlContent == null || htmlContent.trim().isEmpty()) {
            logger.warning("HTML内容为空");
            return "";
        }

        // 2. 检查是否存在XML解析问题
        if (htmlContent.trim().startsWith("<?xml")) {
            logger.warning("检测到XML声明，可能导致解析问题");
            // 移除XML声明
            htmlContent = htmlContent.replaceFirst("<\\?xml[^>]*>\\s*", "");
        }

        // 3. 保留DOCTYPE声明，确保标准模式渲染
        String doctype = "";
        Pattern doctypePattern = Pattern.compile("<!DOCTYPE[^>]*>", Pattern.CASE_INSENSITIVE);
        Matcher doctypeMatcher = doctypePattern.matcher(htmlContent);
        if (doctypeMatcher.find()) {
            doctype = doctypeMatcher.group(0) + "\n";
        }

        // 4. 使用Jsoup解析，保留更多原始结构
        Document doc;
        try {
            doc = Jsoup.parse(htmlContent);
            logger.info("Jsoup解析成功");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Jsoup解析失败，尝试清理HTML", e);
            // 清理HTML内容
            String cleanedHtml = htmlContent.replaceAll("<\\?xml[^>]*>", "")
                                              .replaceAll("<\\!--.*?-->", "")
                                              .replaceAll("&\\s+", "&amp; ");
            doc = Jsoup.parse(cleanedHtml);
        }
        
        // 5. 设置输出格式 - 使用HTML语法而不是XML，避免过度转义
        doc.outputSettings()
            .syntax(Document.OutputSettings.Syntax.html)  // 使用HTML语法
            .charset("UTF-8")
            .prettyPrint(true)  // 启用美化，便于调试
            .escapeMode(Entities.EscapeMode.base);  // 基础转义模式

        // 6. 确保基本结构完整
        ensureBasicStructure(doc);
        
        // 7. 处理CSS兼容性（改进版）
        processCssCompatibility(doc);
        
        // 8. 处理图片路径
        processImages(doc);
        
        // 9. 处理字体
        processFonts(doc);
        
        // 10. 优化表格布局
        optimizeTableLayout(doc);
        
        // 11. 处理颜色
        processColors(doc);
        
        // 12. 添加PDF优化样式
        addPdfOptimizationStyles(doc);

        String processedHtml = doc.outerHtml();
        
        // 重新添加DOCTYPE - 修复重复的DOCTYPE问题
           // 首先移除所有可能存在的DOCTYPE（不区分大小写）
           processedHtml = processedHtml.replaceAll("(?i)<!doctype[^>]*>\\s*", "");
           // 然后只添加一个标准的DOCTYPE到开头
           processedHtml = "<!DOCTYPE html>\\n" + processedHtml;
           
           // 确保只有一个DOCTYPE - 再次检查
           processedHtml = processedHtml.replaceAll("(?i)(<!doctype[^>]*>\\s*)+", "$1");
           
           // 保存调试文件用于分析
           try {
               Files.write(Paths.get("F:/debug_processed.html"), processedHtml.getBytes(StandardCharsets.UTF_8));
               logger.info("调试HTML文件已保存到: F:/debug_processed.html");
           } catch (IOException e) {
               logger.warning("保存调试文件失败: " + e.getMessage());
           }

        return processedHtml;

        } catch (Exception e) {
            logger.log(Level.SEVERE, "HTML预处理失败", e);
            return htmlContent; // 返回原始内容作为后备
        }
    }
    
    /**
     * 确保HTML基本结构
     */
    private void ensureBasicStructure(Document doc) {
        // 确保head和body
        if (doc.head() == null) {
            doc.prepend("<head></head>");
        }
        if (doc.body() == null) {
            doc.append("<body></body>");
        }
    }
    
    /**
     * 确保HTML内容符合XML格式要求
     */
    private String makeXmlCompatible(String html) {
        try {
            logger.info("转换HTML为XML兼容格式...");
            
            // 1. 移除可能导致XML解析问题的字符
            String cleaned = html.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", ""); // 移除控制字符
            
            // 2. 移除XML声明（如果存在），我们将重新创建
            cleaned = cleaned.replaceFirst("<\\?xml[^>]*>\\s*", "");
            
            // 3. 确保只有DOCTYPE声明，没有XML声明
            // openhtmltopdf需要DOCTYPE而不是XML声明
            if (!cleaned.trim().startsWith("<!DOCTYPE")) {
                // 如果没有DOCTYPE，添加标准的HTML5 DOCTYPE
                cleaned = "<!DOCTYPE html>\n" + cleaned;
            }
            
            // 4. 使用Jsoup重新解析为XML格式，但保持HTML兼容性
            Document doc = Jsoup.parse(cleaned);
            doc.outputSettings()
                .syntax(Document.OutputSettings.Syntax.html)  // 使用HTML语法而不是XML
                .charset("UTF-8")
                .prettyPrint(false)  // 禁用美化，避免格式问题
                .escapeMode(Entities.EscapeMode.base);
            
            String xmlContent = doc.outerHtml();
            logger.info("XML兼容转换完成，长度: " + xmlContent.length());
            
            // 5. 验证转换后的内容
            if (xmlContent.trim().isEmpty()) {
                logger.warning("转换后的内容为空，返回原始HTML");
                return html;
            }
            
            return xmlContent;
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "XML兼容转换失败，返回原始HTML", e);
            return html;
        }
    }
    
    /**
     * 处理CSS兼容性 - 改进版本，更好地支持现代CSS
     */
    private void processCssCompatibility(Document doc) {
        // 处理内联样式
        Elements elementsWithStyle = doc.select("[style]");
        for (Element element : elementsWithStyle) {
            String style = element.attr("style");
            String processedStyle = processCssProperties(style);
            if (!style.equals(processedStyle)) {
                element.attr("style", processedStyle);
                logger.fine("处理内联样式: " + style + " -> " + processedStyle);
            }
        }
        
        // 处理样式标签
        Elements styleTags = doc.select("style");
        for (Element styleTag : styleTags) {
            String css = styleTag.html();
            String processedCss = processCssProperties(css);
            if (!css.equals(processedCss)) {
                styleTag.text(processedCss);
                logger.fine("处理样式标签内容");
            }
        }
        
        // 处理link标签引入的外部样式
        Elements linkTags = doc.select("link[rel=stylesheet]");
        for (Element linkTag : linkTags) {
            String href = linkTag.attr("href");
            logger.info("发现外部样式表: " + href);
            // 尝试内联外部CSS
            inlineExternalCss(doc, linkTag);
        }
    }
    
    /**
     * 处理CSS属性 - 改进版本，更智能的CSS处理
     */
    private String processCssProperties(String css) {
        if (css == null || css.trim().isEmpty()) {
            return css;
        }
        
        String processedCss = css;
        
        // 1. 处理RGBA颜色 - 转换为RGB，保持透明度为1的RGBA
        processedCss = processedCss.replaceAll(
            "rgba\\((\\d+),\\s*(\\d+),\\s*(\\d+),\\s*1(\\.0)?\\)", 
            "rgb($1, $2, $3)"
        );
        
        // 2. 处理其他RGBA颜色（保持透明度）
        processedCss = processedCss.replaceAll(
            "rgba\\((\\d+),\\s*(\\d+),\\s*(\\d+),\\s*([\\d.]+)\\)", 
            "rgb($1, $2, $3)"  // PDF不支持透明度，转换为RGB
        );
        
        // 3. 使用智能CSS变量替换
        processedCss = smartReplaceCssVariables(processedCss, null);
        
        // 4. 处理box-shadow - 保留简单的阴影效果
        processedCss = processedCss.replaceAll(
            "box-shadow:\\s*([\\d]+)px\\s+([\\d]+)px\\s+([\\d]+)px\\s+([\\d]+)px\\s+([#\\w]+)",
            "/* box-shadow: $1px $2px $3px $4px $5 */ border: 1px solid $5"
        );
        
        // 5. 处理backdrop-filter - 移除（PDF不支持）
        processedCss = processedCss.replaceAll("backdrop-filter:\\s*[^;]+", "/* backdrop-filter removed */");
        
        // 6. 处理现代布局属性 - 提供更智能的回退
        processedCss = processedCss.replaceAll(
            "display:\\s*flex",
            "display: block; /* flex fallback */"
        );
        
        processedCss = processedCss.replaceAll(
            "display:\\s*grid",
            "display: table; /* grid fallback */"
        );
        
        // 7. 处理flex布局属性
        processedCss = processedCss.replaceAll("justify-content:\\s*[^;]+", "text-align:center;");
        processedCss = processedCss.replaceAll("align-items:\\s*[^;]+", "vertical-align:middle;");
        processedCss = processedCss.replaceAll("align-content:\\s*[^;]+", "");
        
        // 8. 处理grid布局属性
        processedCss = processedCss.replaceAll("grid-template-columns:\\s*[^;]+", "");
        processedCss = processedCss.replaceAll("grid-template-rows:\\s*[^;]+", "");
        processedCss = processedCss.replaceAll("grid-gap:\\s*[^;]+", "margin:10px;");
        processedCss = processedCss.replaceAll("gap:\\s*[^;]+", "margin:10px;");
        
        // 9. 处理渐变背景 - 保留第一个颜色作为背景色
        processedCss = processedCss.replaceAll(
            "background:\\s*linear-gradient\\([^)]+,\\s*([#\\w]+)[^)]*\\)",
            "background: $1 /* gradient fallback */"
        );
        
        processedCss = processedCss.replaceAll(
            "background:\\s*linear-gradient\\([^)]+\\)",
            "background: #f0f0f0 /* gradient fallback */"
        );
        
        // 10. 处理transform - 移除复杂的变换
        processedCss = processedCss.replaceAll("transform:\\s*[^;]+", "/* transform removed */");
        
        // 11. 处理现代CSS特性
        processedCss = processedCss.replaceAll("position:\\s*sticky\\s*;", "position:relative;");
        processedCss = processedCss.replaceAll("mix-blend-mode:\\s*[^;]+", "");
        processedCss = processedCss.replaceAll("filter:\\s*[^;]+", "");
        
        // 12. 处理现代边框属性（保留有用的）
        processedCss = processedCss.replaceAll("border-radius:\\s*([^;]+);", "border-radius:$1;"); // 保留圆角
        processedCss = processedCss.replaceAll("box-sizing:\\s*[^;]+", "box-sizing:border-box;"); // 保留box-sizing
        
        return processedCss;
    }
    
    /**
     * 内联外部CSS文件
     */
    private void inlineExternalCss(Document doc, Element linkTag) {
        String href = linkTag.attr("href");
        
        try {
            // 尝试读取CSS文件内容
            String cssContent = null;
            
            // 处理相对路径
            if (href.startsWith("http")) {
                // 网络资源 - 暂时跳过
                logger.info("跳过网络CSS资源: " + href);
                return;
            } else if (href.startsWith("/")) {
                // 绝对路径
                cssContent = readFileContent(href);
            } else {
                // 相对路径 - 尝试多个位置
                String[] possiblePaths = {
                    "src/main/resources/static" + (href.startsWith("/") ? "" : "/") + href,
                    "src/main/resources/templates" + (href.startsWith("/") ? "" : "/") + href,
                    "static" + (href.startsWith("/") ? "" : "/") + href,
                    href
                };
                
                for (String path : possiblePaths) {
                    try {
                        cssContent = readFileContent(path);
                        if (cssContent != null) {
                            logger.info("成功读取CSS文件: " + path);
                            break;
                        }
                    } catch (Exception e) {
                        logger.fine("尝试路径失败: " + path + " - " + e.getMessage());
                    }
                }
            }
            
            if (cssContent != null) {
                // 处理CSS内容
                String processedCss = processCssProperties(cssContent);
                
                // 创建style标签并插入到head中
                Element styleElement = doc.createElement("style");
                styleElement.text(processedCss);
                styleElement.attr("data-source", href);  // 标记来源
                
                doc.head().appendChild(styleElement);
                logger.info("成功内联CSS: " + href);
                
                // 移除原始的link标签
                linkTag.remove();
            } else {
                logger.warning("无法找到CSS文件: " + href);
            }
            
        } catch (Exception e) {
            logger.warning("内联CSS失败: " + href + " - " + e.getMessage());
        }
    }
    
    /**
     * 读取文件内容
     */
    private String readFileContent(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        if (Files.exists(path)) {
            return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        }
        return null;
    }
    
    /**
     * 处理图片 - 改进版本
     */
    private void processImages(Document doc) {
        Elements images = doc.select("img");
        for (Element img : images) {
            String src = img.attr("src");
            
            // 处理本地文件路径
            if (src.matches("[A-Za-z]:\\\\.*") || src.startsWith("file:")) {
                // 尝试转换为data URI
                String dataUri = convertLocalImageToDataUri(src);
                if (dataUri != null) {
                    img.attr("src", dataUri);
                    logger.info("转换本地图片为data URI: " + src);
                } else {
                    // 转换失败，使用占位图片
                    img.attr("src", createPlaceholderImage());
                    logger.warning("转换本地图片失败，使用占位图片: " + src);
                }
            }
            
            // 处理网络图片 - 保持原样，PDF生成器会尝试加载
            else if (src.startsWith("http")) {
                logger.info("保留网络图片: " + src);
            }
            
            // 处理base64图片 - 保持原样
            else if (src.startsWith("data:")) {
                logger.info("保留data URI图片");
            }
            
            // 确保图片有尺寸（PDF需要明确的尺寸）
            if (!img.hasAttr("width") && !img.hasAttr("height")) {
                img.attr("width", "70");
                img.attr("height", "70");
            }
            
            // 确保图片有alt属性
            if (!img.hasAttr("alt")) {
                img.attr("alt", "Image");
            }
        }
    }
    
    /**
     * 转换本地图片为data URI
     */
    private String convertLocalImageToDataUri(String imagePath) {
        try {
            // 处理file://协议
            if (imagePath.startsWith("file:")) {
                imagePath = imagePath.substring(5);
            }
            
            // 转换为Path
            Path path = Paths.get(imagePath);
            if (!Files.exists(path)) {
                logger.warning("图片文件不存在: " + imagePath);
                return null;
            }
            
            // 检测文件类型
            String contentType = detectImageContentType(path);
            if (contentType == null) {
                logger.warning("无法检测图片类型: " + imagePath);
                return null;
            }
            
            // 读取文件内容
            byte[] imageBytes = Files.readAllBytes(path);
            String base64 = Base64.getEncoder().encodeToString(imageBytes);
            
            return "data:" + contentType + ";base64," + base64;
            
        } catch (Exception e) {
            logger.warning("转换图片到data URI失败: " + imagePath + " - " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 检测图片内容类型
     */
    private String detectImageContentType(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (fileName.endsWith(".png")) {
            return "image/png";
        } else if (fileName.endsWith(".gif")) {
            return "image/gif";
        } else if (fileName.endsWith(".svg")) {
            return "image/svg+xml";
        } else if (fileName.endsWith(".bmp")) {
            return "image/bmp";
        } else if (fileName.endsWith(".webp")) {
            return "image/webp";
        }
        
        // 尝试通过文件头检测
        try {
            byte[] header = Files.readAllBytes(path);
            if (header.length >= 4) {
                // JPEG
                if (header[0] == (byte)0xFF && header[1] == (byte)0xD8) {
                    return "image/jpeg";
                }
                // PNG
                if (header[0] == (byte)0x89 && header[1] == (byte)0x50) {
                    return "image/png";
                }
                // GIF
                if (header[0] == (byte)0x47 && header[1] == (byte)0x49) {
                    return "image/gif";
                }
            }
        } catch (Exception e) {
            logger.warning("检测图片类型失败: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * 智能替换CSS变量
     */
    private String smartReplaceCssVariables(String css, Document doc) {
        if (!css.contains("var(")) {
            return css;
        }
        
        // 提取CSS中的变量定义
        Map<String, String> cssVariables = extractCssVariables(css);
        
        // 提取HTML中的变量定义（如果有）
        Map<String, String> htmlVariables = extractHtmlCssVariables(doc);
        
        // 合并变量定义（CSS中的优先级更高）
        Map<String, String> allVariables = new HashMap<>(htmlVariables);
        allVariables.putAll(cssVariables);
        
        // 替换var()函数
        Pattern varPattern = Pattern.compile("var\\s*\\(\\s*--([a-zA-Z0-9_-]+)\\s*(?:,\\s*([^)]*))?\\s*\\)");
        Matcher matcher = varPattern.matcher(css);
        
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String varName = "--" + matcher.group(1);
            String fallback = matcher.group(2);
            String replacement = allVariables.getOrDefault(varName, fallback);
            
            if (replacement == null) {
                logger.warning("CSS变量未找到: " + varName);
                matcher.appendReplacement(result, "inherit");
            } else {
                matcher.appendReplacement(result, replacement);
            }
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * 提取CSS中的变量定义
     */
    private Map<String, String> extractCssVariables(String css) {
        Map<String, String> variables = new HashMap<>();
        
        Pattern varDefPattern = Pattern.compile("--([a-zA-Z0-9_-]+)\\s*:\\s*([^;]+);");
        Matcher matcher = varDefPattern.matcher(css);
        
        while (matcher.find()) {
            String varName = "--" + matcher.group(1);
            String varValue = matcher.group(2).trim();
            variables.put(varName, varValue);
        }
        
        return variables;
    }
    
    /**
     * 提取HTML中的CSS变量定义
     */
    private Map<String, String> extractHtmlCssVariables(Document doc) {
        Map<String, String> variables = new HashMap<>();
        
        // 从style标签中提取
        Elements styleElements = doc.select("style");
        for (Element style : styleElements) {
            variables.putAll(extractCssVariables(style.data()));
        }
        
        // 从行内样式中提取
        Elements elementsWithStyle = doc.select("[style]");
        for (Element element : elementsWithStyle) {
            variables.putAll(extractCssVariables(element.attr("style")));
        }
        
        return variables;
    }
    
    /**
     * 创建占位图片
     */
    private String createPlaceholderImage() {
        return "data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNzAiIGhlaWdodD0iNzAiIHZpZXdCb3g9IjAgMCA3MCA3MCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48Y2lyY2xlIGN4PSIzNSIgY3k9IjM1IiByPSIzNSIgZmlsbD0iI0RERCIvPjxzdmcgeD0iMTUiIHk9IjE1IiB3aWR0aD0iNDAiIGhlaWdodD0iNDAiIHZpZXdCb3g9IjAgMCAyNCAyNCIgZmlsbD0ibm9uZSI+PHBhdGggZD0iTTEyIDEyQzE0LjIwOTEgMTIgMTYgMTAuMjA5MSAxNiA4QzE2IDUuNzkwODYgMTQuMjA5MSA0IDEyIDRDOS43OTA4NiA0IDggNS43OTA4NiA4IDhDOCAxMC4yMDkxIDkuNzkwODYgMTIgMTIgMTJaIiBmaWxsPSIjOTk5Ii8+PHBhdGggZD0iTTEyIDE0QzcuNTgxNzIgMTQgNCAxNy41ODE3IDQgMjJIMjBDMjAgMTcuNTgxNyAxNi40MTgzIDE0IDEyIDE0WiIgZmlsbD0iIzk5OSIvPjwvc3ZnPjwvc3ZnPg==";
    }
    
    /**
     * 处理字体 - 改进版本
     */
    private void processFonts(Document doc) {
        // 添加字体样式 - 更智能的字体处理
        String fontStyles = "<style>\n" +
            "/* PDF优化字体样式 */\n" +
            "body { \n" +
            "  font-family: 'Microsoft YaHei', 'SimSun', 'SimHei', 'Arial Unicode MS', 'Noto Sans CJK SC', sans-serif; \n" +
            "  font-size: 12px; \n" +
            "  line-height: 1.4; \n" +
            "}\n" +
            ".name { \n" +
            "  font-family: 'Microsoft YaHei', 'SimHei', 'Arial Unicode MS', 'Noto Sans CJK SC', sans-serif; \n" +
            "  font-weight: bold; \n" +
            "  font-size: 18px; \n" +
            "}\n" +
            ".section-title { \n" +
            "  font-family: 'Microsoft YaHei', 'SimHei', 'Arial Unicode MS', 'Noto Sans CJK SC', sans-serif; \n" +
            "  font-weight: 600; \n" +
            "  font-size: 14px; \n" +
            "  margin: 10px 0; \n" +
            "}\n" +
            "h1, h2, h3, h4, h5, h6 { \n" +
            "  font-family: 'Microsoft YaHei', 'SimHei', 'Arial Unicode MS', 'Noto Sans CJK SC', sans-serif; \n" +
            "  font-weight: bold; \n" +
            "  margin: 8px 0; \n" +
            "}\n" +
            "p { margin: 4px 0; }\n" +
            "ul, ol { margin: 4px 0; padding-left: 20px; }\n" +
            "</style>\n";
        
        doc.head().prepend(fontStyles);
        
        // 确保body有字体设置
        Element body = doc.body();
        if (body != null && !body.hasAttr("style")) {
            body.attr("style", "font-family: 'Microsoft YaHei', 'SimSun', 'SimHei', 'Arial Unicode MS', 'Noto Sans CJK SC', sans-serif;");
        }
    }
    
    /**
     * 优化表格布局
     */
    private void optimizeTableLayout(Document doc) {
        // 处理display: table布局
        Elements tableContainers = doc.select(".template-four-container");
        for (Element container : tableContainers) {
            // 添加表格样式优化
            container.attr("style", "width: 800px; margin: 20px auto; background: #fff;");
        }
        
        // 处理表格单元格
        Elements tableCells = doc.select(".left-panel, .right-panel");
        for (Element cell : tableCells) {
            if (!cell.hasAttr("valign")) {
                cell.attr("valign", "top");
            }
        }
    }
    
    /**
     * 处理颜色
     */
    private void processColors(Document doc) {
        // 确保颜色值格式正确
        Elements coloredElements = doc.select("[style*=color], [style*=background]");
        for (Element element : coloredElements) {
            String style = element.attr("style");
            style = style.replaceAll("rgba\\((\\d+),\\s*(\\d+),\\s*(\\d+),\\s*([\\d.]+)\\)", "rgb($1, $2, $3)");
            element.attr("style", style);
        }
    }
    
    /**
     * 添加PDF优化样式
     */
    private void addPdfOptimizationStyles(Document doc) {
        String pdfStyles = "<style>\n" +
            "/* PDF优化样式 */\n" +
            "@page { margin: 20mm; size: A4; }\n" +
            "body { font-size: 12px; line-height: 1.4; }\n" +
            ".template-four-container { page-break-inside: avoid; }\n" +
            ".section { page-break-inside: avoid; margin-bottom: 15px; }\n" +
            "img { image-rendering: crisp-edges; }\n" +
            "table { border-collapse: collapse; }\n" +
            "td, th { padding: 2px; }\n" +
            "</style>\n";
        
        doc.head().append(pdfStyles);
    }
    
    /**
     * 配置PDF生成器
     */
    private void configurePdfBuilder() {
        // 基础配置
        builder.useFastMode();
        
        // 页面设置
        String pageSize = (String) config.getOrDefault("pageSize", "A4");
        String margin = (String) config.getOrDefault("margin", "20mm");
        
        // 设置页面大小
        switch (pageSize.toUpperCase()) {
            case "A3":
                builder.useDefaultPageSize(297, 420, BaseRendererBuilder.PageSizeUnits.MM);
                break;
            case "A4":
                builder.useDefaultPageSize(210, 297, BaseRendererBuilder.PageSizeUnits.MM);
                break;
            case "A5":
                builder.useDefaultPageSize(148, 210, BaseRendererBuilder.PageSizeUnits.MM);
                break;
            case "LETTER":
                builder.useDefaultPageSize(8.5f, 11f, BaseRendererBuilder.PageSizeUnits.INCHES);
                break;
            default:
                builder.useDefaultPageSize(210, 297, BaseRendererBuilder.PageSizeUnits.MM);
        }
        
        // 加载字体
        loadSystemFonts();
        
        // 启用SVG支持 (注释掉，因为BatikSVGDrawer不可用)
        // if ((Boolean) config.getOrDefault("enableSvg", true)) {
        //     builder.useSVGDrawer(new com.openhtmltopdf.svgsupport.BatikSVGDrawer());
        // }
        
        // 启用MathML支持 (注释掉，因为MathMLDrawer不可用)
        // if ((Boolean) config.getOrDefault("enableMathML", false)) {
        //     builder.useMathMLDrawer(new com.openhtmltopdf.mathmlsupport.MathMLDrawer());
        // }
        
        // 其他配置 (注释掉PDF/A和PDF版本配置)
        // if ((Boolean) config.getOrDefault("enableBookmarks", true)) {
        //     builder.usePdfAConformance(BaseRendererBuilder.PdfAConformance.PDFA_3_A);
        // }
        
        // if ((Boolean) config.getOrDefault("compress", true)) {
        //     builder.usePdfVersion(BaseRendererBuilder.PdfVersion.PDF_1_7);
        // }
    }
    
    /**
     * 加载系统字体 - 改进版本
     */
    private void loadSystemFonts() {
        logger.info("加载系统字体...");
        
        String osName = System.getProperty("os.name").toLowerCase();
        String[] fontPaths;
        
        if (osName.contains("win")) {
            fontPaths = FONT_CONFIGS.get("windows");
        } else if (osName.contains("mac")) {
            fontPaths = FONT_CONFIGS.get("macos");
        } else {
            fontPaths = FONT_CONFIGS.get("linux");
        }
        
        boolean fontLoaded = false;
        Set<String> loadedFonts = new HashSet<>();
        
        for (String fontPath : fontPaths) {
            File fontFile = new File(fontPath);
            if (fontFile.exists()) {
                try {
                    String fontName = getFontName(fontPath);
                    
                    // 避免重复加载相同字体
                    if (!loadedFonts.contains(fontName)) {
                        builder.useFont(fontFile, fontName);
                        loadedFonts.add(fontName);
                        logger.info("加载字体: " + fontName + " from " + fontPath);
                        fontLoaded = true;
                    }
                } catch (Exception e) {
                    logger.warning("字体加载失败: " + fontPath + " - " + e.getMessage());
                }
            } else {
                logger.fine("字体文件不存在: " + fontPath);
            }
        }
        
        // 如果系统字体加载失败，尝试加载通用字体
        if (!fontLoaded) {
            logger.info("尝试加载通用字体...");
            try {
                // 添加一些基本的字体族支持
                builder.useFont(new File("/System/Library/Fonts/Arial Unicode.ttf"), "Arial Unicode MS");
                logger.info("加载通用字体: Arial Unicode MS");
                fontLoaded = true;
            } catch (Exception e) {
                logger.fine("通用字体加载失败: " + e.getMessage());
            }
        }
        
        if (!fontLoaded) {
            logger.warning("未找到任何可用字体，PDF可能显示异常");
        } else {
            logger.info("成功加载 " + loadedFonts.size() + " 个字体");
        }
    }
    
    /**
     * 获取字体名称
     */
    private String getFontName(String fontPath) {
        String fileName = new File(fontPath).getName();
        fileName = fileName.substring(0, fileName.lastIndexOf('.'));
        
        Map<String, String> fontNameMap = new HashMap<>();
        fontNameMap.put("msyh", "Microsoft YaHei");
        fontNameMap.put("msyhbd", "Microsoft YaHei Bold");
        fontNameMap.put("simhei", "SimHei");
        fontNameMap.put("simsun", "SimSun");
        fontNameMap.put("simkai", "SimKai");
        fontNameMap.put("arial", "Arial");
        fontNameMap.put("arialbd", "Arial Bold");
        fontNameMap.put("pingfang", "PingFang SC");
        fontNameMap.put("stheiti", "STHeiti");
        fontNameMap.put("liberationsans-regular", "Liberation Sans");
        
        return fontNameMap.getOrDefault(fileName.toLowerCase(), fileName);
    }
    
    /**
     * 读取HTML文件
     */
    private String readHtmlFile(String filePath) throws IOException {
        logger.info("读取HTML文件: " + filePath);
        
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new FileNotFoundException("HTML文件不存在: " + filePath);
        }
        
        return new String(Files.readAllBytes(path), "UTF-8");
    }
}