package com.aicv.airesume.model.dto;

import java.util.Map;

/**
 * 微信登录DTO
 */
public class WechatLoginDTO {
    
    private String code;
    private Map<String, Object> userInfo;
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public Map<String, Object> getUserInfo() {
        return userInfo;
    }
    
    public void setUserInfo(Map<String, Object> userInfo) {
        this.userInfo = userInfo;
    }
}