package com.aicv.airesume.controller;

import com.aicv.airesume.model.dto.RecordUserActionDTO;
import com.aicv.airesume.model.vo.BaseResponseVO;
import com.aicv.airesume.service.StatisticsService;
import com.aicv.airesume.utils.TokenUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 统计服务控制器
 */
@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    @Autowired
    private StatisticsService statisticsService;

    /**
     * 获取用户统计信息
     * @param userId 用户ID
/**
     * 获取用户统计信息
     */
    @GetMapping("/user/{userId}")
    public BaseResponseVO getUserStatistics(@PathVariable Long userId) {
        Map<String, Object> result = statisticsService.getUserStatistics(userId);
        return BaseResponseVO.success(result);
    }

    /**
     * 获取系统统计信息
/**
     * 获取系统统计信息
     */
    @GetMapping("/system")
    public BaseResponseVO getSystemStatistics() {
        Map<String, Object> result = statisticsService.getSystemStatistics();
        return BaseResponseVO.success(result);
    }

    /**
     * 记录用户行为
     */
    @PostMapping("/user-action")
    public BaseResponseVO recordUserAction(@RequestBody RecordUserActionDTO recordUserActionDTO) {
        Long userId = recordUserActionDTO.getUserId();
        String action = recordUserActionDTO.getAction();
        String details = recordUserActionDTO.getDetails();
        statisticsService.recordUserAction(userId, action, details);
        return BaseResponseVO.success(null);
    }

    /**
     * 获取用户行为统计
     */
    @GetMapping("/user-action-statistics")
    public BaseResponseVO getUserActionStatistics() {
        Map<String, Object> result = statisticsService.getUserActionStatistics();
        return BaseResponseVO.success(result);
    }

    /**
     * 获取系统行为统计
     */
    @GetMapping("/system-action-statistics")
    public BaseResponseVO getSystemActionStatistics() {
        Map<String, Object> result = statisticsService.getSystemActionStatistics();
        return BaseResponseVO.success(result);
    }
}