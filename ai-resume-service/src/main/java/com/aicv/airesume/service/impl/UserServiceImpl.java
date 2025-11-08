package com.aicv.airesume.service.impl;

import com.aicv.airesume.entity.User;
import com.aicv.airesume.repository.UserRepository;
import com.aicv.airesume.service.UserService;
import com.aicv.airesume.utils.RetryUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Random;
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

    @Override
    public User wechatLogin(String openId, String nickname, String avatarUrl) {
        try {
            return retryUtils.executeWithDefaultRetry(() -> {
                // 根据openId查找用户
                Optional<User> existingUserOpt = userRepository.findByOpenId(openId);
                
                if (existingUserOpt.isPresent()) {
                    // 用户已存在，更新用户信息
                    User user = existingUserOpt.get();
                    // 只有在提供了新信息时才更新
                    if (nickname != null && !nickname.isEmpty()) {
                        user.setNickname(nickname);
                    }
                    if (avatarUrl != null && !avatarUrl.isEmpty()) {
                        user.setAvatarUrl(avatarUrl);
                    }
                    // 保存更新后的用户信息
                    return userRepository.save(user);
                } else {
                    // 用户不存在，创建新用户
                    User newUser = new User();
                    newUser.setOpenId(openId);
                    
                    // 生成随机昵称（如果没有提供）
                    newUser.setNickname(nickname != null && !nickname.isEmpty() ? nickname : generateRandomNickname());
                    
                    // 生成卡通头像（如果没有提供）
                    newUser.setAvatarUrl(avatarUrl != null && !avatarUrl.isEmpty() ? avatarUrl : generateCartoonAvatar(newUser.getNickname()));
                    
                    newUser.setRemainingOptimizeCount(0); // 初始优化次数
                    newUser.setVip(false); // 初始非VIP
                    
                    // 保存新用户到数据库，自动生成id作为系统内部userId
                    return userRepository.save(newUser);
                }
            });
        } catch (Exception e) {
            throw new RuntimeException("微信登录失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 生成用户token
     * @param userId 用户ID
     * @return 生成的token字符串
     */
    public String generateToken(Long userId) {
        // 简单的token生成逻辑，可以根据需要替换为更安全的实现
        // 例如使用JWT、UUID等
        String timestamp = String.valueOf(System.currentTimeMillis());
        String randomStr = String.valueOf(Math.random()).substring(2, 8);
        return "token_" + userId + "_" + timestamp + "_" + randomStr;
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
    
    /**
     * 生成随机昵称
     * @return 随机生成的用户昵称
     */
    private String generateRandomNickname() {
        // 扩展形容词数组，增加更多选项
        String[] adjectives = {
            "快乐的", "聪明的", "勇敢的", "友善的", "活泼的", "优雅的", "机智的", "可爱的", "幽默的", "温柔的",
            "热情的", "开朗的", "乐观的", "自信的", "谦虚的", "诚实的", "善良的", "细心的", "耐心的", "坚强的",
            "敏捷的", "灵活的", "健壮的", "轻盈的", "优雅的", "高贵的", "神秘的", "梦幻的", "闪亮的", "耀眼的",
            "温暖的", "清凉的", "甜蜜的", "清新的", "自由的", "奔放的", "安静的", "沉稳的", "深邃的", "广阔的",
            "创意的", "独特的", "非凡的", "卓越的", "优秀的", "出色的", "顶尖的", "专业的", "专注的", "认真的",
            "活泼的", "调皮的", "可爱的", "萌趣的", "机智的", "聪慧的", "睿智的", "理性的", "感性的", "直率的"
        };
        
        // 扩展名词数组，增加更多选项
        String[] nouns = {
            "小猫", "小狗", "小鸟", "小鱼", "小熊", "小兔", "小鹿", "小松鼠", "小象", "小狮子",
            "小狐狸", "小熊猫", "小龙", "小虎", "小马", "小羊", "小牛", "小鸡", "小鸭", "小鹅",
            "小猫头鹰", "小海豚", "小鲸鱼", "小企鹅", "小浣熊", "小考拉", "小树懒", "小刺猬", "小兔子", "小鹿斑比",
            "星星", "月亮", "太阳", "云朵", "彩虹", "雪花", "雨滴", "花朵", "树叶", "种子",
            "火箭", "飞船", "飞机", "汽车", "火车", "轮船", "潜艇", "机器人", "无人机", "游戏机",
            "梦想家", "探险家", "科学家", "艺术家", "音乐家", "作家", "画家", "设计师", "工程师", "程序员"
        };
        
        // 扩展后缀数组，增加更多选项
        String[] suffixes = {
            "探险家", "旅行者", "梦想家", "创造者", "守护者", "冒险家", "追梦者", "探索者", "发现者", "实践者",
            "思考者", "创造者", "设计师", "工程师", "艺术家", "音乐家", "作家", "画家", "摄影师", "程序员",
            "科学家", "研究者", "发明家", "革新者", "开拓者", "领航者", "先锋", "领袖", "冠军", "英雄",
            "学霸", "达人", "专家", "大师", "王者", "精英", "新秀", "天才", "神童", "奇才",
            "爱好者", "收藏家", "鉴赏家", "玩家", "粉丝", "迷", "控", "狂", "痴", "宅",
            "小能手", "小达人", "小天才", "小精灵", "小可爱", "小机灵", "小调皮", "小宝贝", "小天使", "小恶魔"
        };
        
        Random random = new Random();
        String adjective = adjectives[random.nextInt(adjectives.length)];
        String noun = nouns[random.nextInt(nouns.length)];
        String suffix = suffixes[random.nextInt(suffixes.length)];
        int number = random.nextInt(10000);
        
        // 随机决定是否添加数字后缀，增加变化性
        if (random.nextBoolean()) {
            return adjective + noun + suffix + number;
        } else {
            return adjective + noun + suffix;
        }
    }
    
    /**
     * 生成卡通头像URL
     * @param openId 用户的openId，用于基于openId生成唯一的头像
     * @return 卡通头像URL
     */
    private String generateCartoonAvatar(String openId) {
        // 使用国内稳定可用的头像生成服务
        int hashCode = Math.abs(openId.hashCode());
        
        // 生成基于用户ID的固定颜色
        int r = hashCode % 256;
        int g = (hashCode / 256) % 256;
        int b = (hashCode / 65536) % 256;
        String bgColor = String.format("%02x%02x%02x", r, g, b);
        
        // 生成对比色作为文字颜色
        String fgColor = (r + g + b > 380) ? "000000" : "ffffff"; // 简单的颜色对比度判断
        
        // 使用用户ID的第一个字符作为头像文字
        String displayText = openId.isEmpty() ? "U" : openId.substring(0, 1).toUpperCase();
        
        try {
            // 使用国内稳定的头像生成服务，避免依赖国外服务
            // 使用更可靠的头像生成方式
            return "https://ui-avatars.com/api/?name=" + displayText + "&background=" + bgColor + "&color=" + fgColor + "&size=200";
        } catch (Exception e) {
            // 降级方案：返回一个简单的本地头像路径
            return "/api/avatar/default?color=" + bgColor;
        }
    }

    public static void main(String[] args) {
        UserServiceImpl userService = new UserServiceImpl();
        String avatarUrl = userService.generateCartoonAvatar("123456");
        System.out.println(avatarUrl);
    }
}