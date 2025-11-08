package com.aicv.airesume.common.util;

import com.aicv.airesume.common.constant.ResponseCode;
import com.aicv.airesume.common.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 业务工具类
 * 提供常用的业务处理方法
 *
 * @author AI Resume Team
 * @date 2023-07-01
 */
public class BusinessUtil {

    // 手机号正则表达式
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    // 邮箱正则表达式
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    // 用户名正则表达式（字母数字下划线，长度4-20）
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{4,20}$");
    // 文件扩展名正则（用于验证简历文件类型）
    private static final Pattern RESUME_EXT_PATTERN = Pattern.compile("\\.(pdf|doc|docx|txt|rtf)$", Pattern.CASE_INSENSITIVE);
    
    // 日期格式化
    private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT = 
            ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
    private static final ThreadLocal<SimpleDateFormat> DATE_ONLY_FORMAT = 
            ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd"));

    /**
     * 生成唯一ID
     * 使用UUID生成唯一标识符
     *
     * @return 唯一ID字符串
     */
    public static String generateUniqueId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 验证手机号格式
     *
     * @param phone 手机号
     * @return 是否有效
     */
    public static boolean isValidPhone(String phone) {
        return StringUtils.isNotEmpty(phone) && PHONE_PATTERN.matcher(phone).matches();
    }

    /**
     * 验证邮箱格式
     *
     * @param email 邮箱
     * @return 是否有效
     */
    public static boolean isValidEmail(String email) {
        return StringUtils.isNotEmpty(email) && EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * 验证用户名格式
     *
     * @param username 用户名
     * @return 是否有效
     */
    public static boolean isValidUsername(String username) {
        return StringUtils.isNotEmpty(username) && USERNAME_PATTERN.matcher(username).matches();
    }

    /**
     * 验证是否为有效的简历文件类型
     *
     * @param fileName 文件名
     * @return 是否有效
     */
    public static boolean isValidResumeFileType(String fileName) {
        return StringUtils.isNotEmpty(fileName) && RESUME_EXT_PATTERN.matcher(fileName).find();
    }

    /**
     * 获取文件扩展名
     *
     * @param fileName 文件名
     * @return 文件扩展名
     */
    public static String getFileExtension(String fileName) {
        if (StringUtils.isEmpty(fileName) || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * 安全地从Object转换为String
     *
     * @param obj 对象
     * @return 字符串表示
     */
    public static String safeToString(Object obj) {
        return obj == null ? "" : obj.toString();
    }

    /**
     * 安全地从String转换为Integer
     *
     * @param str 字符串
     * @param defaultValue 默认值
     * @return Integer值
     */
    public static Integer safeToInteger(String str, Integer defaultValue) {
        if (StringUtils.isEmpty(str)) {
            return defaultValue;
        }
        try {
            return Integer.valueOf(str);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 检查参数是否为空，如果为空则抛出异常
     *
     * @param obj 参数对象
     * @param message 错误消息
     * @param <T> 参数类型
     * @return 参数对象（用于链式调用）
     */
    public static <T> T requireNonNull(T obj, String message) {
        if (obj == null) {
            throw new BusinessException(ResponseCode.PARAMS_VALID_ERROR, message);
        }
        return obj;
    }

    /**
     * 检查字符串是否为空，如果为空则抛出异常
     *
     * @param str 字符串
     * @param message 错误消息
     * @return 非空字符串
     */
    public static String requireNotEmpty(String str, String message) {
        if (StringUtils.isEmpty(str)) {
            throw new BusinessException(ResponseCode.PARAMS_VALID_ERROR, message);
        }
        return str;
    }

    /**
     * 格式化日期时间
     *
     * @param date 日期
     * @return 格式化后的字符串
     */
    public static String formatDateTime(Date date) {
        if (date == null) {
            return "";
        }
        return DATE_FORMAT.get().format(date);
    }

    /**
     * 格式化日期
     *
     * @param date 日期
     * @return 格式化后的字符串
     */
    public static String formatDate(Date date) {
        if (date == null) {
            return "";
        }
        return DATE_ONLY_FORMAT.get().format(date);
    }

    /**
     * 转换LocalDateTime到Date
     *
     * @param localDateTime LocalDateTime对象
     * @return Date对象
     */
    public static Date toDate(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    /**
     * 转换Date到LocalDateTime
     *
     * @param date Date对象
     * @return LocalDateTime对象
     */
    public static LocalDateTime toLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    /**
     * 检查是否在有效期内
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 是否在有效期内
     */
    public static boolean isInValidPeriod(Date startTime, Date endTime) {
        Date now = new Date();
        boolean afterStart = startTime == null || now.after(startTime) || now.equals(startTime);
        boolean beforeEnd = endTime == null || now.before(endTime) || now.equals(endTime);
        return afterStart && beforeEnd;
    }

    /**
     * 计算两个日期之间的天数差
     *
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 天数差
     */
    public static long daysBetween(Date startDate, Date endDate) {
        if (startDate == null || endDate == null) {
            return 0;
        }
        long diff = endDate.getTime() - startDate.getTime();
        return diff / (1000 * 60 * 60 * 24);
    }

    /**
     * 安全地比较两个对象是否相等
     *
     * @param a 对象a
     * @param b 对象b
     * @return 是否相等
     */
    public static boolean safeEquals(Object a, Object b) {
        return Objects.equals(a, b);
    }

    /**
     * 生成文件存储路径
     * 格式：/{module}/{yyyyMMdd}/{uuid}{ext}
     *
     * @param module 模块名
     * @param originalFilename 原始文件名
     * @return 存储路径
     */
    public static String generateFilePath(String module, String originalFilename) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        String dateStr = dateFormat.format(new Date());
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String ext = getFileExtension(originalFilename);
        
        StringBuilder path = new StringBuilder();
        path.append("/").append(module)
            .append("/").append(dateStr)
            .append("/").append(uuid);
        
        if (StringUtils.isNotEmpty(ext)) {
            path.append(".").append(ext);
        }
        
        return path.toString();
    }
}