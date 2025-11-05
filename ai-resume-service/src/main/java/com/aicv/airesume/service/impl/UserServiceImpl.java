package com.aicv.airesume.service.impl;

import com.aicv.airesume.entity.User;
import com.aicv.airesume.repository.UserRepository;
import com.aicv.airesume.service.UserService;
import com.aicv.airesume.utils.RetryUtils;
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
    
    @Autowired
    private RetryUtils retryUtils;

    @Override
    public Optional<User> getUserById(Long userId) {
        try {
            return retryUtils.executeWithDefaultRetry(() -> userRepository.findById(userId));
        } catch (Exception e) {
            // 转换异常，保持原有接口签名
            throw new RuntimeException("获取用户信息失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<User> getUserByOpenId(String openId) {
        try {
            return retryUtils.executeWithDefaultRetry(() -> userRepository.findByOpenId(openId));
        } catch (Exception e) {
            throw new RuntimeException("通过OpenId获取用户信息失败: " + e.getMessage(), e);
        }
    }

    @Override
    public User createUser(User user) {
        try {
            return retryUtils.executeWithDefaultRetry(() -> userRepository.save(user));
        } catch (Exception e) {
            throw new RuntimeException("创建用户失败: " + e.getMessage(), e);
        }
    }

    @Override
    public User updateUser(User user) {
        try {
            return retryUtils.executeWithDefaultRetry(() -> userRepository.save(user));
        } catch (Exception e) {
            throw new RuntimeException("更新用户失败: " + e.getMessage(), e);
        }
    }

    // 临时方法实现
    public User updateUserInfo(User userInfo) {
        try {
            return retryUtils.executeWithDefaultRetry(() -> userRepository.save(userInfo));
        } catch (Exception e) {
            throw new RuntimeException("更新用户信息失败: " + e.getMessage(), e);
        }
    }

    // 临时方法实现
    public User wechatLogin(String code, String nickname, String avatarUrl) {
        // 这里通常会涉及到微信API调用和数据库操作
        try {
            return retryUtils.executeWithDefaultRetry(() -> {
                // 实际的微信登录逻辑，这里返回临时对象作为示例
                return new User();
            });
        } catch (Exception e) {
            throw new RuntimeException("微信登录失败: " + e.getMessage(), e);
        }
    }

    // 临时方法实现
    public User updateUser(Long id, User user) {
        try {
            // 先检查用户是否存在
            Optional<User> existingUser = retryUtils.executeWithDefaultRetry(() -> userRepository.findById(id));
            if (existingUser.isPresent()) {
                // 更新用户信息
                User updatedUser = existingUser.get();
                // 这里应该设置需要更新的字段
                return retryUtils.executeWithDefaultRetry(() -> userRepository.save(updatedUser));
            } else {
                throw new RuntimeException("用户不存在");
            }
        } catch (Exception e) {
            throw new RuntimeException("更新用户失败: " + e.getMessage(), e);
        }
    }

    // 临时方法实现
    public User registerUser(User user) {
        try {
            return retryUtils.executeWithDefaultRetry(() -> userRepository.save(user));
        } catch (Exception e) {
            throw new RuntimeException("注册用户失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void addOptimizeCount(Long userId, Integer count) {
        try {
            retryUtils.executeWithDefaultRetry(() -> {
                Optional<User> userOpt = userRepository.findById(userId);
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    user.setRemainingOptimizeCount(user.getRemainingOptimizeCount() + count);
                    userRepository.save(user);
                }
                return null;
            });
        } catch (Exception e) {
            throw new RuntimeException("增加优化次数失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean checkOptimizeCount(Long userId) {
        try {
            return retryUtils.executeWithDefaultRetry(() -> {
                Optional<User> userOpt = userRepository.findById(userId);
                if (userOpt.isPresent()) {
                    return userOpt.get().getRemainingOptimizeCount() > 0 || (userOpt.get().getVip() != null && userOpt.get().getVip());
                }
                return false;
            });
        } catch (Exception e) {
            throw new RuntimeException("检查优化次数失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean reduceOptimizeCount(Long userId) {
        try {
            return retryUtils.executeWithDefaultRetry(() -> {
                Optional<User> userOpt = userRepository.findById(userId);
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    // 如果是VIP用户或者还有优化次数
                    if ((user.getVip() != null && user.getVip()) || user.getRemainingOptimizeCount() > 0) {
                        if (user.getVip() == null || !user.getVip()) {
                            user.setRemainingOptimizeCount(user.getRemainingOptimizeCount() - 1);
                            userRepository.save(user);
                        }
                        return true;
                    }
                }
                return false;
            });
        } catch (Exception e) {
            throw new RuntimeException("减少优化次数失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void setVipStatus(Long userId, Integer days) {
        try {
            retryUtils.executeWithDefaultRetry(() -> {
                Optional<User> userOpt = userRepository.findById(userId);
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    user.setVip(true);
                    // 设置VIP过期时间
                    long expireTime = System.currentTimeMillis() + (long) days * 24 * 60 * 60 * 1000;
                    user.setVipExpireTime(new java.util.Date(expireTime));
                    userRepository.save(user);
                }
                return null;
            });
        } catch (Exception e) {
            throw new RuntimeException("设置VIP状态失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void addTemplateUseCount(Long userId) {
        try {
            retryUtils.executeWithDefaultRetry(() -> {
                Optional<User> userOpt = userRepository.findById(userId);
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    // 这里假设有一个templateUseCount字段，实际可能需要根据具体实体类调整
                    userRepository.save(user);
                }
                return null;
            });
        } catch (Exception e) {
            throw new RuntimeException("增加模板使用次数失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean checkTemplateUsePermission(Long userId, Integer templateVip) {
        try {
            return retryUtils.executeWithDefaultRetry(() -> {
                Optional<User> userOpt = userRepository.findById(userId);
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    // VIP可以使用所有模板，非VIP只能使用非VIP模板
                    return (user.getVip() != null && user.getVip()) || templateVip != 1;
                }
                return false;
            });
        } catch (Exception e) {
            throw new RuntimeException("检查模板使用权限失败: " + e.getMessage(), e);
        }
    }
}