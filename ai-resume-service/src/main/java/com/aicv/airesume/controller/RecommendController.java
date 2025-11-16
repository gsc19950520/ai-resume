package com.aicv.airesume.controller;

import com.aicv.airesume.annotation.Log;
import com.aicv.airesume.model.vo.BaseResponseVO;
import com.aicv.airesume.model.vo.RecommendItemVO;
import com.aicv.airesume.model.vo.RecommendListVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

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
    public BaseResponseVO getRecommendList() {
        // 创建推荐项列表
        List<RecommendItemVO> recommendList = new ArrayList<>();
        
        // 添加推荐项1
        RecommendItemVO item1 = new RecommendItemVO();
        item1.setId(1);
        item1.setTitle("如何打造优秀简历");
        item1.setImage("/images/recommend1.png");
        recommendList.add(item1);
        
        // 添加推荐项2
        RecommendItemVO item2 = new RecommendItemVO();
        item2.setId(2);
        item2.setTitle("面试技巧指南");
        item2.setImage("/images/recommend2.png");
        recommendList.add(item2);
        
        // 添加推荐项3
        RecommendItemVO item3 = new RecommendItemVO();
        item3.setId(3);
        item3.setTitle("热门行业分析");
        item3.setImage("/images/recommend3.png");
        recommendList.add(item3);
        
        // 创建返回对象
        RecommendListVO recommendListVO = new RecommendListVO(recommendList);
        
        return BaseResponseVO.success(recommendListVO);
    }
}