package com.aicv.airesume.controller;

import com.aicv.airesume.service.StatisticsService;
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
     * @return 统计信息
     */
    @GetMapping("/user/{userId}")
    public Map<String, Object> getUserStatistics(@PathVariable Long userId) {
        return statisticsService.getUserStatistics(userId);
    }

    /**
     * 获取系统统计信息
     * @return 系统统计
     */
    @GetMapping("/system")
    public Map<String, Object> getSystemStatistics() {
        return statisticsService.getSystemStatistics();
    }

    /**
     * 记录用户行为
     * @param userId 用户ID
     * @param action 行为类型
     * @param details 详细信息
     */
    @PostMapping("/action/log")
    public void recordUserAction(@RequestParam Long userId, @RequestParam String action, @RequestParam(required = false) String details) {
        statisticsService.recordUserAction(userId, action, details);
    }

    /**
     * 获取用户行为统计
     * @param userId 用户ID
     * @param actionType 行为类型
     * @return 行为统计
     */
    @GetMapping("/action/user/{userId}/type/{actionType}")
    public Map<String, Object> getUserActionStatistics(
            @PathVariable Long userId, 
            @PathVariable String actionType) {
        // 临时返回空map，接口中未定义此方法
        return java.util.Collections.emptyMap();
    }

    /**
     * 获取系统行为统计
     * @param actionType 行为类型
     * @return 系统行为统计
     */
    @GetMapping("/action/system/type/{actionType}")
    public Map<String, Object> getSystemActionStatistics(@PathVariable String actionType) {
        // 临时返回空map，接口中未定义此方法
        return java.util.Collections.emptyMap();
    }
}