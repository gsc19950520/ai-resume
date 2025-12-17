package com.aicv.airesume.controller;

import com.aicv.airesume.service.GrowthReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 成长报告控制器
 */
@RestController
@RequestMapping("/api/growth-report")
@Slf4j
public class GrowthReportController {

    @Autowired
    private GrowthReportService growthReportService;

    /**
     * 异步流式生成成长报告
     * @param userId 用户ID
     * @return SSE发射器
     */
    @GetMapping("/stream/{userId}")
    public SseEmitter generateGrowthReportStream(@PathVariable Long userId) {
        // 创建SSE发射器，设置超时时间为1小时
        SseEmitter emitter = new SseEmitter(3600000L);
        
        // 调用服务层的异步流式方法
        String reportId = growthReportService.generateGrowthReportStream(userId, emitter);
        
        log.info("开始异步生成成长报告，报告ID: {}, 用户ID: {}", reportId, userId);
        
        return emitter;
    }
}
