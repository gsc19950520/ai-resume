package com.aicv.airesume.utils;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

/**
 * 文件处理工具类
 */
public class FileUtils {

    /**
     * 从PDF文件中提取文本
     */
    public static String extractTextFromPdf(MultipartFile file) throws Exception {
        try (InputStream inputStream = file.getInputStream();
             PDDocument document = PDDocument.load(inputStream)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    /**
     * 从Word文件中提取文本
     */
    public static String extractTextFromWord(MultipartFile file) throws Exception {
        try (InputStream inputStream = file.getInputStream();
             XWPFDocument document = new XWPFDocument(inputStream)) {
            StringBuilder textBuilder = new StringBuilder();
            List<XWPFParagraph> paragraphs = document.getParagraphs();
            for (XWPFParagraph paragraph : paragraphs) {
                textBuilder.append(paragraph.getText()).append("\n");
            }
            return textBuilder.toString();
        }
    }

    /**
     * 验证文件类型
     */
    public static boolean isValidFileType(String fileName) {
        String[] validTypes = {"pdf", "doc", "docx"};
        String fileType = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        for (String type : validTypes) {
            if (type.equals(fileType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 生成唯一文件名
     */
    public static String generateUniqueFileName(String originalFilename) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        return timestamp + "_" + extension;
    }
}