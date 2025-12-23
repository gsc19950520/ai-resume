package com.aicv.airesume.controller;

import com.aicv.airesume.annotation.Log;
import com.aicv.airesume.entity.Resume;
import com.aicv.airesume.exception.BusinessException;
import com.aicv.airesume.model.dto.ResumeDataDTO;
import com.aicv.airesume.model.vo.BaseResponseVO;
import com.aicv.airesume.service.ResumeService;
import com.aicv.airesume.utils.GlobalContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletOutputStream;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


/**
 * 简历控制器
 * 优化后的控制器，合并了冗余接口，提供统一的API入口
 */
@RestController
@RequestMapping("/api/resume")
public class ResumeController {

    @Autowired
    private ResumeService resumeService;

    /**
     * 上传简历
     * @param name 简历名称
     * @param file 简历文件
     * @return 简历信息
     */
    @Log(description = "用户上传简历文件", recordParams = true, recordResult = true)
    @PostMapping("/upload")
    public Resume uploadResume(@RequestParam String name, @RequestParam MultipartFile file) {
        Long userId = GlobalContextUtil.getUserId();
        return resumeService.uploadResume(userId, name, file);
    }

    /**
     * 获取用户简历列表
     * @return 简历列表，统一包装在BaseResponseVO中返回
     */
    @Log(description = "获取用户简历列表", recordParams = true, recordResult = false)
    @GetMapping("/user")
    public BaseResponseVO getUserResumeList() {
        try {
            Long userId = GlobalContextUtil.getUserId();
            List<Resume> resumeList = resumeService.getResumeListByUserId(userId);
            return BaseResponseVO.success(resumeList);
        } catch (BusinessException e) {
            return BaseResponseVO.error(e.getMessage());
        } catch (Exception e) {
            return BaseResponseVO.error("获取用户简历列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取简历详情
     * 统一接口，合并了基础信息和完整数据的获取
     * @param resumeId 简历ID
     * @param fullData 是否返回完整数据（默认false）
     * @return 简历信息
     */
    @Log(description = "获取简历详情", recordParams = true, recordResult = true)
    @GetMapping("/{resumeId}")
    public Object getResume(@PathVariable Long resumeId, @RequestParam(required = false, defaultValue = "false") boolean fullData) {
        if (fullData) {
            // 返回完整数据，包含所有关联信息
            return resumeService.getResumeFullData(resumeId);
        } else {
            // 返回基础简历信息
            return resumeService.getResumeById(resumeId);
        }
    }

    /**
     * 删除简历
     * @param resumeId 简历ID
     */
    @Log(description = "删除简历", recordParams = true, recordResult = true)
    @DeleteMapping("/{resumeId}")
    public boolean deleteResume(@PathVariable Long resumeId) {
        Long userId = GlobalContextUtil.getUserId();
        return resumeService.deleteResume(userId, resumeId);
    }

    /**
     * 导出为PDF
     * 统一接口，合并了默认模板和指定模板的导出功能
     * @param resumeId 简历ID
     * @param templateId 模板ID
     * @param response HTTP响应对象
     */
    @Log(description = "导出简历为PDF", recordParams = true, recordResult = false, recordExecutionTime = true)
    @GetMapping("/export/pdf")
    public void exportToPdf(@RequestParam Long resumeId, 
                           @RequestParam String templateId, 
                           HttpServletResponse response) {
        try {
            byte[] pdfBytes;
            String fileName;
            
            // 根据是否提供模板ID选择不同的导出方法
            if (templateId != null && !templateId.isEmpty()) {
                pdfBytes = resumeService.exportResumeToPdf(resumeId, templateId);
                fileName = "resume_" + resumeId + "_" + templateId + ".pdf";
            } else {
                throw new RuntimeException("templateId不可为空");
            }
            
            // 设置响应头
            response.setContentType("application/pdf");
            response.setContentLength(pdfBytes.length);
            response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");

            // 写入响应体
            ServletOutputStream outputStream = response.getOutputStream();
            outputStream.write(pdfBytes);
            outputStream.flush();
            outputStream.close();
        } catch (Exception e) {
            throw new RuntimeException("导出PDF失败", e);
        }
    }
    
    /**
     * 创建简历
     */
    @PostMapping
    public BaseResponseVO createResume(@RequestBody ResumeDataDTO resumeDataDTO) {
        Long userId = GlobalContextUtil.getUserId();
        Resume resume = resumeService.createResumeWithFullData(userId, resumeDataDTO);
        Map<String, Object> result = new HashMap<>();
        result.put("resumeId", resume.getId());
        result.put("message", "简历创建成功");
        return BaseResponseVO.success(result);
    }
    
    /**
     * 更新简历
     */
    @PutMapping("/{resumeId}")
    public BaseResponseVO updateResume(@PathVariable Long resumeId,
                                           @RequestBody ResumeDataDTO resumeDataDTO) {
        Resume resume = resumeService.updateResumeWithFullData(resumeId, resumeDataDTO);
        
        Map<String, Object> result = new HashMap<>();
        result.put("resumeId", resume.getId());
        result.put("message", "简历更新成功");
        return BaseResponseVO.success(result);
    }
    
    /**
     * 获取用户最新的简历数据
     * @return 最新简历的完整数据
     */
    @GetMapping("/getLatest")
    public BaseResponseVO getLatestResumeData() {
        Long userId = GlobalContextUtil.getUserId();
        
        Map<String, Object> result = resumeService.getLatestResumeData(userId);
        return BaseResponseVO.success(result);
    }
}