package com.aicv.airesume.utils;


/**
 * 文件处理工具类
 */
public class FileUtils {

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