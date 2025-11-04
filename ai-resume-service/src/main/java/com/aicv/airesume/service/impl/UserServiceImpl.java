package com.aicv.airesume.service.impl;

import com.aicv.airesume.entity.User;
import com.aicv.airesume.repository.UserRepository;
import com.aicv.airesume.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 用户服务实现类
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public Optional<User> getUserById(Long userId) {
        return userRepository.findById(userId);
    }

    @Override
    public Optional<User> getUserByOpenId(String openId) {
        return userRepository.findByOpenId(openId);
    }

    @Override
    public User createUser(User user) {
        return userRepository.save(user);
    }

    @Override
    public User updateUser(User user) {
        // 临时返回user对象，避免save方法调用
        return user;
    }

    // 临时方法实现
    public User updateUserInfo(User userInfo) {
        // 临时返回userInfo对象，避免任何方法调用
        return userInfo;
    }

    // 临时方法实现
    public User wechatLogin(String code, String nickname, String avatarUrl) {
        // 临时返回新的User对象，避免任何方法调用
        return new User();
    }

    // 临时方法实现
    public User updateUser(Long id, User user) {
        // 临时返回user对象，避免任何方法调用
        return user;
    }

    // 临时方法实现
    public User registerUser(User user) {
        // 临时返回user对象，避免任何方法调用
        return user;
    }

    @Override
    public void addOptimizeCount(Long userId, Integer count) {
        // 临时空实现
    }

    @Override
    public boolean checkOptimizeCount(Long userId) {
        // 直接返回默认值
        return true;
    }

    @Override
    public boolean reduceOptimizeCount(Long userId) {
        // 直接返回默认值
        return true;
    }

    @Override
    public void setVipStatus(Long userId, Integer days) {
        // 临时空实现
    }
    
    @Override
    public void addTemplateUseCount(Long userId) {
        // 临时空实现
    }

    @Override
    public boolean checkTemplateUsePermission(Long userId, Integer templateVip) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            // 移除vip检查，直接返回默认权限
            return templateVip != 1; // 假设非VIP模板可以使用
        }
        return false;
    }
}