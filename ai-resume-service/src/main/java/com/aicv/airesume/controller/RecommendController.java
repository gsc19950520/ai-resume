package com.aicv.airesume.controller;

import com.aicv.airesume.annotation.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 推荐内容控制器
 */
@RestController
@RequestMapping("/api/recommend")
public class RecommendController {

    /**
     * 获取推荐列表
     * @return 推荐内容列表
     */
    @Log(description = "获取推荐列表", recordParams = true, recordResult = true)
    @GetMapping("/list")
    public Map<String, Object> getRecommendList() {
        // 创建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("message", "success");
        
        // 创建数据对象
        Map<String, Object> data = new HashMap<>();
        
        // 创建推荐列表
        List<Map<String, Object>> recommendList = new ArrayList<>();
        
        // 添加推荐项1
        Map<String, Object> item1 = new HashMap<>();
        item1.put("id", 1);
        item1.put("title", "如何打造优秀简历");
        item1.put("image", "/images/recommend1.png");
        recommendList.add(item1);
        
        // 添加推荐项2
        Map<String, Object> item2 = new HashMap<>();
        item2.put("id", 2);
        item2.put("title", "面试技巧指南");
        item2.put("image", "/images/recommend2.png");
        recommendList.add(item2);
        
        // 添加推荐项3
        Map<String, Object> item3 = new HashMap<>();
        item3.put("id", 3);
        item3.put("title", "热门行业分析");
        item3.put("image", "/images/recommend3.png");
        recommendList.add(item3);
        
        data.put("list", recommendList);
        result.put("data", data);
        
        return result;
    }
}