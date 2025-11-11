package com.aicv.airesume.service;

import com.aicv.airesume.entity.Resume;
import com.aicv.airesume.entity.Template;

import java.io.InputStream;
import java.util.Map;

/**
 * 模板渲染服务接口
 * 用于处理Word模板转换为HTML、HTML渲染和PDF生成
 */
public interface TemplateRendererService {

    /**
     * 从Word模板生成HTML模板内容
     * @param wordTemplateStream Word模板文件流
     * @return HTML模板内容
     * @throws Exception 转换过程中可能出现的异常
     */
    String generateHtmlTemplateFromWord(InputStream wordTemplateStream) throws Exception;

    /**
     * 从本地resources下的Word模板生成HTML
     * @param template 模板对象，包含模板路径信息
     * @return HTML模板内容
     * @throws Exception 转换过程中可能出现的异常
     */
    String generateHtmlFromLocalWordTemplate(Template template) throws Exception;

    /**
     * 渲染HTML模板
     * @param htmlTemplate HTML模板内容
     * @param resumeData 简历数据
     * @return 渲染后的HTML内容
     * @throws Exception 渲染过程中可能出现的异常
     */
    String renderHtmlTemplate(String htmlTemplate, Map<String, Object> resumeData) throws Exception;

    /**
     * 将HTML转换为PDF
     * @param htmlContent HTML内容
     * @return PDF字节数组
     * @throws Exception 转换过程中可能出现的异常
     */
    byte[] convertHtmlToPdf(String htmlContent) throws Exception;

    /**
     * 渲染简历并生成PDF
     * @param template 模板对象
     * @param resume 简历对象
     * @return PDF字节数组
     * @throws Exception 渲染过程中可能出现的异常
     */
    byte[] renderResumeToPdf(Template template, Resume resume) throws Exception;

    /**
     * 渲染简历并生成Word
     * @param template 模板对象
     * @param resume 简历对象
     * @return Word字节数组
     * @throws Exception 渲染过程中可能出现的异常
     */
    byte[] renderResumeToWord(Template template, Resume resume) throws Exception;

    /**
     * 将简历数据转换为模板渲染所需的数据格式
     * @param resume 简历对象
     * @return 格式化后的简历数据
     */
    Map<String, Object> formatResumeData(Resume resume);
}