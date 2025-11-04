package com.aicv.airesume.service;

import com.aicv.airesume.entity.User;

import java.util.Optional;

/**
 * 用户服务接口
 */
public interface UserService {
    
    /**
     * 根据openId获取用户
     */
    Optional<User> getUserByOpenId(String openId);
    
    /**
     * 根据ID获取用户
     */
    Optional<User> getUserById(Long id);
    
    /**
     * 创建用户
     */
    User createUser(User user);
    
    /**
     * 更新用户信息
     */
    User updateUser(User user);
    
    /**
     * 微信登录
     */
    User wechatLogin(String openId, String nickname, String avatarUrl);
    
    /**
     * 增加优化次数
     */
    void addOptimizeCount(Long userId, Integer count);
    
    /**
     * 减少优化次数
     */
    boolean reduceOptimizeCount(Long userId);
    
    /**
     * 设置VIP状态
     */
    void setVipStatus(Long userId, Integer durationDays);
    
    /**
     * 检查用户是否有优化次数
     */
    boolean checkOptimizeCount(Long userId);
    
    /**
     * 增加模板使用次数
     */
    void addTemplateUseCount(Long userId);
    
    /**
     * 检查模板使用权限
     */
    boolean checkTemplateUsePermission(Long userId, Integer templateVip);
}