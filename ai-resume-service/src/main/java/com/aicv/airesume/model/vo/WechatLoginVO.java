package com.aicv.airesume.model.vo;

import java.util.Map;

/**
 * 微信登录响应VO
 */
public class WechatLoginVO {
    
    private String token;
    private Long userId;
    private String openId;
    private UserInfoVO userInfo;
    
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getOpenId() {
        return openId;
    }
    
    public void setOpenId(String openId) {
        this.openId = openId;
    }
    
    public UserInfoVO getUserInfo() {
        return userInfo;
    }
    
    public void setUserInfo(UserInfoVO userInfo) {
        this.userInfo = userInfo;
    }
    
    public static class UserInfoVO {
        private Long id;
        private Long userId;
        private String openId;
        private String nickName;
        private String avatarUrl;
        private Integer gender;
        private String city;
        private String province;
        private String country;
        private Boolean vip;
        private Integer remainingOptimizeCount;
        
        public Long getId() {
            return id;
        }
        
        public void setId(Long id) {
            this.id = id;
        }
        
        public Long getUserId() {
            return userId;
        }
        
        public void setUserId(Long userId) {
            this.userId = userId;
        }
        
        public String getOpenId() {
            return openId;
        }
        
        public void setOpenId(String openId) {
            this.openId = openId;
        }
        
        public String getNickName() {
            return nickName;
        }
        
        public void setNickName(String nickName) {
            this.nickName = nickName;
        }
        
        public String getAvatarUrl() {
            return avatarUrl;
        }
        
        public void setAvatarUrl(String avatarUrl) {
            this.avatarUrl = avatarUrl;
        }
        
        public Integer getGender() {
            return gender;
        }
        
        public void setGender(Integer gender) {
            this.gender = gender;
        }
        
        public String getCity() {
            return city;
        }
        
        public void setCity(String city) {
            this.city = city;
        }
        
        public String getProvince() {
            return province;
        }
        
        public void setProvince(String province) {
            this.province = province;
        }
        
        public String getCountry() {
            return country;
        }
        
        public void setCountry(String country) {
            this.country = country;
        }
        
        public Boolean getVip() {
            return vip;
        }
        
        public void setVip(Boolean vip) {
            this.vip = vip;
        }
        
        public Integer getRemainingOptimizeCount() {
            return remainingOptimizeCount;
        }
        
        public void setRemainingOptimizeCount(Integer remainingOptimizeCount) {
            this.remainingOptimizeCount = remainingOptimizeCount;
        }
    }
}