package com.aicv.airesume.utils;
import org.apache.poi.xwpf.converter.core.FileImageExtractor;
import org.apache.poi.xwpf.converter.core.FileURIResolver;
import org.apache.poi.xwpf.converter.xhtml.XHTMLConverter;
import org.apache.poi.xwpf.converter.xhtml.XHTMLOptions;
import org.apache.poi.xwpf.usermodel.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 🔹 直接将 Word 模板转为高保真 HTML
 * 🔹 自动修复 {{变量}} 占位符
 * 🔹 保留字体、颜色、段落、表格等样式
 * 使用方式：
 *     java com.example.docx.DocxToHtmlConverter input.docx
 * 结果输出：
 *     input.html
 */
public class DocxToHtmlConverter {

    private static final Pattern VAR_PATTERN = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_\\.]+)\\s*\\}\\}");

    public static void main(String[] args) throws Exception {
        String inputPath = "D:\\owner_project\\mini-program\\resume\\ai-resume-service\\template-one.docx";
        File inputFile = new File(inputPath);
        if (!inputFile.exists()) {
            System.out.println("❗ 找不到文件: " + inputPath);
            return;
        }

        String outHtmlPath = inputFile.getAbsolutePath().replaceAll("\\.docx$", ".html");

        System.out.println("🚀 正在转换: " + inputFile.getName());
        convertDocxToHtml(inputFile, new File(outHtmlPath));

        System.out.println("✅ 转换完成: " + outHtmlPath);
    }

    /** 主函数：将 docx 转成 HTML 并修复变量格式 */
    public static void convertDocxToHtml(File docxFile, File outHtmlFile) throws Exception {
        // 1️⃣ 使用 Apache POI 转 HTML
        try (XWPFDocument document = new XWPFDocument(new FileInputStream(docxFile));
             OutputStream outputStream = new FileOutputStream(outHtmlFile)) {
            
            // 配置图片处理
            File imageDir = new File(outHtmlFile.getParentFile(), "images");
            if (!imageDir.exists()) imageDir.mkdirs();
            
            XHTMLOptions options = XHTMLOptions.create();
            // 设置图片提取器
            options.setExtractor(new FileImageExtractor(imageDir));
            // 设置URI解析器 - 使用相对路径而非绝对路径
            options.URIResolver(new FileURIResolver(imageDir));
            
            // 优化样式保留设置
            options.setIgnoreStylesIfUnused(false); // 保留所有样式
            options.setFragment(false); // 生成完整的HTML文档
            
            // 启用更完整的文本提取
            options.setOmitHeaderFooterPages(false);
            
            // 注意：POI 3.15可能不支持setIgnoreImageGraphics和setExtractCSS
            // 所以我们不添加这些可能导致编译错误的方法调用
            
            // 转换为HTML
            XHTMLConverter.getInstance().convert(document, outputStream, options);
        }

        // 2️⃣ 修复被转义的 {{变量}}
        restorePlaceholders(outHtmlFile);
        
        // 3️⃣ 增强HTML样式和内容完整性
        postProcessHtml(outHtmlFile);
    }

    /** 修复 docx4j 转换后 {{变量}} 被转义为 &#123; 的问题 */
    private static void restorePlaceholders(File htmlFile) throws IOException {
        Document doc = Jsoup.parse(htmlFile, "UTF-8");
        String html = doc.outerHtml();

        // 恢复 {{ }} 占位符
        html = html
                .replaceAll("&#123;\\s*#?\\s*([a-zA-Z0-9_\\.]+)\\s*&#125;", "\\{\\{$1\\}\\}")
                .replaceAll("\\{\\s*#?\\s*([a-zA-Z0-9_\\.]+)\\s*\\}", "\\{\\{$1\\}\\}")
                .replaceAll("(&#123;|\\{)\\s*#?\\s*([a-zA-Z0-9_\\.]+)\\s*(&#125;|\\})", "{{$2}}");

        try (FileWriter fw = new FileWriter(htmlFile, false)) {
            fw.write(html);
        }
    }

    /** （可选）扫描 Word 文件中所有变量，用于检查模板 */
    public static void scanVariables(File docxFile) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(new FileInputStream(docxFile))) {
            System.out.println("🔍 扫描模板变量：");
            for (XWPFParagraph p : doc.getParagraphs()) {
                String text = p.getText();
                Matcher m = VAR_PATTERN.matcher(text);
                while (m.find()) {
                    System.out.println(" - " + m.group(1));
                }
            }
        }
    }
    
    /**
     * 后处理HTML文件以增强样式保留和内容完整性
     * 
     * @param htmlFile HTML文件对象
     */
    private static void postProcessHtml(File htmlFile) throws IOException {
        // 使用Jsoup解析和处理HTML
        Document doc = Jsoup.parse(htmlFile, "UTF-8");
        
        // 1. 确保HTML文档结构完整
        if (!doc.html().startsWith("<!DOCTYPE html>") && !doc.html().contains("<!DOCTYPE html>")) {
            doc.prepend("<!DOCTYPE html>");
        }
        
        // 2. 添加基础CSS样式以改善显示效果
        org.jsoup.nodes.Element styleElement = doc.head().selectFirst("style");
        if (styleElement == null) {
            styleElement = doc.head().appendElement("style");
        }
        
        // 添加增强的CSS样式，更接近Word文档的默认样式
        String enhancedStyles = "\n" +
                "body { font-family: 'Times New Roman', serif; line-height: 1.15; color: #000000; margin: 0; padding: 20px; background-color: #ffffff; }\n" +
                "p { margin: 12pt 0; text-indent: 0; }\n" +
                "img { max-width: 100%; height: auto; display: block; margin: 12pt 0; }\n" +
                ".docx-paragraph { margin: 12pt 0; line-height: 1.15; }\n" +
                "table { border-collapse: collapse; width: auto; margin: 12pt 0; border: 1pt solid windowtext; }\n" +
                "table, th, td { border: 1pt solid windowtext; }\n" +
                "th, td { padding: 5.4pt; text-align: left; vertical-align: top; }\n" +
                "h1, h2, h3, h4, h5, h6 { margin-top: 18pt; margin-bottom: 12pt; font-weight: bold; }\n" +
                "h1 { font-size: 24pt; }\n" +
                "h2 { font-size: 18pt; }\n" +
                "h3 { font-size: 14pt; }\n" +
                "span { color: #000000; }\n" +
                "/* 保留空段落的垂直间距 */\n" +
                "p:empty { height: 12pt; min-height: 12pt; }\n" +
                "/* 确保所有元素的默认颜色为黑色 */\n" +
                "* { color: #000000 !important; }\n";
        
        styleElement.append(enhancedStyles);
        
        // 3. 修复图片路径
        for (org.jsoup.nodes.Element img : doc.select("img")) {
            String src = img.attr("src");
            
            // 处理不同格式的图片路径
            if (src.startsWith("word/media/")) {
                // 正确处理从word/media/开始的路径
                String imgName = src.substring(src.lastIndexOf("/") + 1);
                img.attr("src", "images/word/media/" + imgName);
            } else if (src.contains("media/")) {
                // 处理包含media/的路径
                String imgName = src.substring(src.lastIndexOf("/") + 1);
                img.attr("src", "images/word/media/" + imgName);
            }
            
            // 确保图片有alt属性
            if (!img.hasAttr("alt")) {
                img.attr("alt", "文档图片");
            }
        }
        
        // 4. 保留所有段落，包括空段落
        // 注意：不再移除空段落，因为它们在原始文档中可能具有重要的布局意义
        
        // 为所有段落添加类标识
        for (org.jsoup.nodes.Element p : doc.select("p")) {
            if (!p.hasClass("docx-paragraph")) {
                p.addClass("docx-paragraph");
            }
        }
        
        // 5. 为表格添加边框和样式
        for (org.jsoup.nodes.Element table : doc.select("table")) {
            if (!table.hasAttr("border")) {
                table.attr("border", "1");
                table.attr("cellpadding", "5.4");
                table.attr("cellspacing", "0");
                table.attr("style", "border-collapse: collapse; width: auto; margin: 12pt 0;");
            }
            
            // 为表格单元格添加样式
            for (org.jsoup.nodes.Element cell : table.select("td, th")) {
                if (!cell.hasAttr("style")) {
                    cell.attr("style", "border: 1pt solid windowtext; padding: 5.4pt; vertical-align: top;");
                }
            }
        }
        
        // 6. 处理文本格式，确保粗体、斜体等格式正确显示
        for (org.jsoup.nodes.Element b : doc.select("b, strong")) {
            b.attr("style", "font-weight: bold;");
        }
        for (org.jsoup.nodes.Element i : doc.select("i, em")) {
            i.attr("style", "font-style: italic;");
        }
        
        // 7. 确保文档结构完整
        if (doc.body() == null) {
            doc.body(); // 确保body元素存在
        }
        
        // 8. 为所有span元素添加样式，确保文本颜色正确
        for (org.jsoup.nodes.Element span : doc.select("span")) {
            if (!span.hasAttr("style")) {
                span.attr("style", "color: #000000;");
            }
        }
        
        // 9. 创建文档元数据
        org.jsoup.nodes.Element metaCharset = doc.head().selectFirst("meta[charset]");
        if (metaCharset == null) {
            doc.head().appendElement("meta").attr("charset", "UTF-8");
        }
        
        org.jsoup.nodes.Element metaViewport = doc.head().selectFirst("meta[name=viewport]");
        if (metaViewport == null) {
            doc.head().appendElement("meta")
                .attr("name", "viewport")
                .attr("content", "width=device-width, initial-scale=1.0");
        }
        
        // 保存处理后的HTML
        try (FileWriter fw = new FileWriter(htmlFile, false)) {
            fw.write(doc.outerHtml());
        }
    }
}

