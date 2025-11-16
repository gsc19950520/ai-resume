package com.aicv.airesume.model.vo;

/**
 * 推荐项值对象
 */
public class RecommendItemVO {
    
    private Integer id;
    private String title;
    private String image;
    
    public RecommendItemVO() {
    }
    
    public RecommendItemVO(Integer id, String title, String image) {
        this.id = id;
        this.title = title;
        this.image = image;
    }
    
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getImage() {
        return image;
    }
    
    public void setImage(String image) {
        this.image = image;
    }
}