package com.aicv.airesume.utils;

import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

/**
 * 增强版Word文档分析器，尝试全面解析文档所有元素
 */
public class EnhancedWordDocAnalyzer {

    public static void main(String[] args) throws IOException {
        String docxPath = "D:\\owner_project\\mini-program\\resume\\ai-resume-service\\template-one.docx";
        File docxFile = new File(docxPath);
        
        if (!docxFile.exists()) {
            System.out.println("文件不存在: " + docxPath);
            return;
        }
        
        System.out.println("开始分析文档: " + docxFile.getName());
        System.out.println("================================================");
        
        try (XWPFDocument document = new XWPFDocument(new FileInputStream(docxFile))) {
            // 1. 分析基本信息
            analyzeDocumentInfo(document);
            
            // 2. 分析所有段落（包括空段落）
            analyzeParagraphs(document);
            
            // 3. 分析表格
            analyzeTables(document);
            
            // 4. 分析图片
            analyzeImages(document);
            
            // 5. 分析页眉页脚
            analyzeHeaderFooter(document);
            
            // 6. 分析文本框和形状
            analyzeTextboxes(document);
            
            // 7. 分析文档结构和部分
            analyzeDocumentStructure(document);
        }
        
        System.out.println("================================================");
        System.out.println("文档分析完成");
    }
    
    private static void analyzeDocumentInfo(XWPFDocument document) {
        System.out.println("1. 文档基本信息:");
        System.out.println("   - 页数估计: " + estimatePageCount(document));
        System.out.println("   - 段落总数: " + document.getParagraphs().size());
        System.out.println("   - 表格总数: " + document.getTables().size());
        System.out.println("   - 图片总数估计: " + countImages(document));
        System.out.println();
    }
    
    private static void analyzeParagraphs(XWPFDocument document) {
        System.out.println("2. 段落详情 (全部段落，包括空段落):");
        List<XWPFParagraph> paragraphs = document.getParagraphs();
        
        for (int i = 0; i < paragraphs.size(); i++) {
            XWPFParagraph paragraph = paragraphs.get(i);
            String text = paragraph.getText();
            
            // 即使是空段落也要分析
            System.out.println("   段落 " + (i+1) + ":");
            System.out.println("     - 文本内容: " + (text.isEmpty() ? "[空段落]" : "\"" + text + "\""));
            System.out.println("     - 文本长度: " + text.length());
            System.out.println("     - 运行数(Runs): " + paragraph.getRuns().size());
            
            // 分析段落样式
            CTP ctp = paragraph.getCTP();
            if (ctp.isSetPPr()) {
                CTPPr ppr = ctp.getPPr();
                System.out.println("     - 段落样式: 已定义");
                if (ppr.isSetJc()) {
                    System.out.println("     - 对齐方式: " + ppr.getJc().getVal().toString());
                }
            }
            
            // 分析前几个运行的详细信息
            List<XWPFRun> runs = paragraph.getRuns();
            for (int j = 0; j < Math.min(runs.size(), 3); j++) { // 只显示前3个运行
                XWPFRun run = runs.get(j);
                System.out.println("     - 运行 " + (j+1) + ":");
                System.out.println("       * 文本: " + (run.text() == null ? "[null]" : "\"" + run.text() + "\""));
                System.out.println("       * 字体: " + run.getFontFamily());
                System.out.println("       * 字号: " + run.getFontSize());
                System.out.println("       * 加粗: " + run.isBold());
                System.out.println("       * 斜体: " + run.isItalic());
            }
            if (runs.size() > 3) {
                System.out.println("     - 还有 " + (runs.size() - 3) + " 个运行未显示...");
            }
            System.out.println();
        }
    }
    
    private static void analyzeTables(XWPFDocument document) {
        System.out.println("3. 表格信息:");
        List<XWPFTable> tables = document.getTables();
        
        if (tables.isEmpty()) {
            System.out.println("   - 文档中没有表格");
        } else {
            for (int i = 0; i < tables.size(); i++) {
                XWPFTable table = tables.get(i);
                System.out.println("   表格 " + (i+1) + ":");
                System.out.println("     - 行数: " + table.getNumberOfRows());
                
                List<XWPFTableRow> rows = table.getRows();
                for (int j = 0; j < rows.size(); j++) {
                    XWPFTableRow row = rows.get(j);
                    System.out.println("     - 行 " + (j+1) + " 列数: " + row.getTableCells().size());
                    
                    // 显示前几个单元格的内容
                    List<XWPFTableCell> cells = row.getTableCells();
                    for (int k = 0; k < Math.min(cells.size(), 3); k++) {
                        XWPFTableCell cell = cells.get(k);
                        String cellText = cell.getText();
                        System.out.println("       * 单元格 " + (k+1) + ": " + 
                                          (cellText.isEmpty() ? "[空]" : "\"" + cellText + "\""));
                    }
                }
            }
        }
        System.out.println();
    }
    
    private static void analyzeImages(XWPFDocument document) {
        System.out.println("4. 图片详细信息:");
        int imageCount = 0;
        
        // 在POI 3.15中，通过运行(Run)来查找图片
        for (XWPFParagraph paragraph : document.getParagraphs()) {
            for (XWPFRun run : paragraph.getRuns()) {
                if (run.getEmbeddedPictures() != null && !run.getEmbeddedPictures().isEmpty()) {
                    for (XWPFPicture picture : run.getEmbeddedPictures()) {
                        imageCount++;
                        XWPFPictureData pictureData = picture.getPictureData();
                        System.out.println("   图片 " + imageCount + ":");
                        System.out.println("     - 类型: " + pictureData.suggestFileExtension());
                        System.out.println("     - 大小: " + pictureData.getData().length + " bytes");
                        // POI 3.15不支持getWidth()和getHeight()，我们只保留基本信息
                    }
                }
            }
        }
        
        System.out.println("   总图片数量: " + imageCount);
        System.out.println();
    }
    
    private static void analyzeHeaderFooter(XWPFDocument document) {
        System.out.println("5. 页眉页脚信息:");
        
        // 分析页眉
        List<XWPFHeader> headers = document.getHeaderList();
        System.out.println("   页眉数量: " + headers.size());
        for (int i = 0; i < headers.size(); i++) {
            XWPFHeader header = headers.get(i);
            System.out.println("   页眉 " + (i+1) + " 包含 " + header.getParagraphs().size() + " 个段落");
            
            for (XWPFParagraph para : header.getParagraphs()) {
                String text = para.getText();
                if (!text.isEmpty()) {
                    System.out.println("     - 页眉文本: " + text);
                }
            }
        }
        
        // 分析页脚
        List<XWPFFooter> footers = document.getFooterList();
        System.out.println("   页脚数量: " + footers.size());
        for (int i = 0; i < footers.size(); i++) {
            XWPFFooter footer = footers.get(i);
            System.out.println("   页脚 " + (i+1) + " 包含 " + footer.getParagraphs().size() + " 个段落");
            
            for (XWPFParagraph para : footer.getParagraphs()) {
                String text = para.getText();
                if (!text.isEmpty()) {
                    System.out.println("     - 页脚文本: " + text);
                }
            }
        }
        System.out.println();
    }
    
    private static void analyzeTextboxes(XWPFDocument document) {
        System.out.println("6. 文本框和形状分析:");
        System.out.println("   注意: 文本框分析在POI 3.15中需要特殊处理");
        System.out.println("   此版本不支持直接访问CTD对象和getBody()方法");
        System.out.println();
    }
    
    private static void analyzeDocumentStructure(XWPFDocument document) {
        System.out.println("7. 文档结构分析:");
        System.out.println("   注意: 详细文档结构分析在POI 3.15中有局限性");
        
        // 尝试获取文档关系
        try {
            System.out.println("   - 文档关系数量: " + document.getPackagePart().getRelationships().size());
        } catch (Exception e) {
            System.out.println("   无法获取文档关系: " + e.getMessage());
        }
        System.out.println();
    }
    
    private static int estimatePageCount(XWPFDocument document) {
        // 简单的页数估计，基于段落数量和平均每页段落数
        int paragraphCount = document.getParagraphs().size();
        return Math.max(1, (int) Math.ceil(paragraphCount / 30.0));
    }
    
    private static int countImages(XWPFDocument document) {
        int count = 0;
        for (XWPFParagraph paragraph : document.getParagraphs()) {
            for (XWPFRun run : paragraph.getRuns()) {
                if (run.getEmbeddedPictures() != null) {
                    count += run.getEmbeddedPictures().size();
                }
            }
        }
        return count;
    }
}