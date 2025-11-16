package com.aicv.airesume.model.vo;

import java.util.List;

/**
 * 推荐列表值对象
 */
public class RecommendListVO {
    
    private List<RecommendItemVO> list;
    
    public RecommendListVO() {
    }
    
    public RecommendListVO(List<RecommendItemVO> list) {
        this.list = list;
    }
    
    public List<RecommendItemVO> getList() {
        return list;
    }
    
    public void setList(List<RecommendItemVO> list) {
        this.list = list;
    }
}