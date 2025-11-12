package com.aicv.airesume.utils;

import org.apache.poi.xwpf.usermodel.*;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

/**
 * Word文档结构分析器，用于分析Word模板文件的内容和结构
 */
public class WordDocAnalyzer {

    public static void main(String[] args) {
        try {
            String filePath = "D:\\owner_project\\mini-program\\resume\\ai-resume-service\\template-one.docx";
            File file = new File(filePath);
            
            if (!file.exists()) {
                System.out.println("文件不存在: " + filePath);
                return;
            }
            
            System.out.println("开始分析Word文档: " + filePath);
            
            try (XWPFDocument document = new XWPFDocument(new FileInputStream(file))) {
                // 分析段落
                List<XWPFParagraph> paragraphs = document.getParagraphs();
                System.out.println("\n1. 段落信息 (共" + paragraphs.size() + "个段落)");
                
                for (int i = 0; i < paragraphs.size(); i++) {
                    XWPFParagraph paragraph = paragraphs.get(i);
                    String text = paragraph.getText();
                    System.out.println("\n段落 " + (i + 1) + ":");
                    System.out.println("文本内容: \"" + text + "\"");
                    System.out.println("样式ID: " + paragraph.getStyleID());
                    System.out.println("对齐方式: " + paragraph.getAlignment());
                    
                    // 分析段落中的运行元素
                    List<XWPFRun> runs = paragraph.getRuns();
                    System.out.println("运行元素数量: " + runs.size());
                    
                    for (XWPFRun run : runs) {
                        System.out.println("  - 运行文本: \"" + run.getText(0) + "\"");
                        System.out.println("    字体: " + run.getFontFamily());
                        System.out.println("    字号: " + run.getFontSize());
                        System.out.println("    加粗: " + run.isBold());
                        System.out.println("    斜体: " + run.isItalic());
                    }
                }
                
                // 分析表格
                List<XWPFTable> tables = document.getTables();
                System.out.println("\n2. 表格信息 (共" + tables.size() + "个表格)");
                
                for (int i = 0; i < tables.size(); i++) {
                    XWPFTable table = tables.get(i);
                    System.out.println("\n表格 " + (i + 1) + ":");
                    
                    List<XWPFTableRow> rows = table.getRows();
                    System.out.println("行数: " + rows.size());
                    
                    for (int j = 0; j < rows.size(); j++) {
                        XWPFTableRow row = rows.get(j);
                        List<XWPFTableCell> cells = row.getTableCells();
                        System.out.println("  行 " + (j + 1) + " 单元格数量: " + cells.size());
                        
                        for (int k = 0; k < cells.size(); k++) {
                            XWPFTableCell cell = cells.get(k);
                            String cellText = cell.getText();
                            System.out.println("    单元格 " + (k + 1) + " 内容: \"" + cellText + "\"");
                        }
                    }
                }
                
                // 分析图片
                System.out.println("\n3. 图片信息:");
                int imageCount = 0;
                
                for (XWPFParagraph paragraph : paragraphs) {
                    for (XWPFRun run : paragraph.getRuns()) {
                        if (run.getEmbeddedPictures() != null && !run.getEmbeddedPictures().isEmpty()) {
                            for (XWPFPicture picture : run.getEmbeddedPictures()) {
                                imageCount++;
                                System.out.println("图片 " + imageCount + ":");
                                System.out.println("  图片类型: " + picture.getPictureData().suggestFileExtension());
                                System.out.println("  图片大小: " + picture.getPictureData().getData().length + " bytes");
                                System.out.println("  图片宽度: " + picture.getCTPicture().getSpPr().getXfrm().getExt().getCx());
                                System.out.println("  图片高度: " + picture.getCTPicture().getSpPr().getXfrm().getExt().getCy());
                            }
                        }
                    }
                }
                
                System.out.println("\n总图片数量: " + imageCount);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
