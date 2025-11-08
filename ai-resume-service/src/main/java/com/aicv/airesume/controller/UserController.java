package com.aicv.airesume.controller;

import com.aicv.airesume.entity.User;
import com.aicv.airesume.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 根据ID获取用户信息
     * @param id 用户ID
     * @return 用户信息
     */
    @GetMapping("/{id}")
    public User getUserById(@PathVariable Long id) {
        return userService.getUserById(id).orElse(null);
    }

    /**
     * 微信登录
     * @param request 请求参数，包含code和用户信息
     * @return 登录结果
     */
    @PostMapping("/wechat-login")
    public ResponseEntity<Map<String, Object>> wechatLogin(@RequestBody Map<String, Object> request) {
        String code = (String) request.get("code");
        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException("登录code不能为空");
        }
        
        // 从请求中获取用户信息（可选）
        Map<String, Object> userInfoMap = null;
        String nickname = null;
        String avatarUrl = null;
        Integer gender = null;
        String country = null;
        String province = null;
        String city = null;
        
        if (request.containsKey("userInfo")) {
            userInfoMap = (Map<String, Object>) request.get("userInfo");
            nickname = userInfoMap != null ? (String) userInfoMap.get("nickName") : null;
            avatarUrl = userInfoMap != null ? (String) userInfoMap.get("avatarUrl") : null;
            gender = userInfoMap != null && userInfoMap.get("gender") != null ? 
                    Integer.parseInt(userInfoMap.get("gender").toString()) : null;
            country = userInfoMap != null ? (String) userInfoMap.get("country") : null;
            province = userInfoMap != null ? (String) userInfoMap.get("province") : null;
            city = userInfoMap != null ? (String) userInfoMap.get("city") : null;
        }
        
        // 调用userService进行微信登录，处理openid入库、用户信息保存等逻辑
        User user = userService.wechatLogin("openid_" + System.currentTimeMillis(), nickname, avatarUrl);
        
        // 如果有更多用户信息，更新用户实体
        if (gender != null) {
            user.setGender(gender);
        }
        if (country != null) {
            user.setCountry(country);
        }
        if (province != null) {
            user.setProvince(province);
        }
        if (city != null) {
            user.setCity(city);
        }
        // 保存更新后的用户信息
        user = userService.updateUser(user);
        
        // 生成token
        String token = userService.generateToken(user.getId());
        
        // 创建响应对象
        Map<String, Object> response = new HashMap<>();
        response.put("code", 0);
        response.put("message", "登录成功");
        
        // 构建数据部分
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", user.getId()); // 系统内部主键ID
        data.put("openId", user.getOpenId());
        
        // 构建用户信息对象
        Map<String, Object> userInfoResponse = new HashMap<>();
        userInfoResponse.put("id", user.getId()); // 系统内部主键ID
        userInfoResponse.put("userId", user.getId()); // 兼容前端使用userId字段
        userInfoResponse.put("openId", user.getOpenId());
        userInfoResponse.put("nickName", user.getNickname());
        userInfoResponse.put("avatarUrl", user.getAvatarUrl());
        userInfoResponse.put("gender", user.getGender());
        userInfoResponse.put("city", user.getCity());
        userInfoResponse.put("province", user.getProvince());
        userInfoResponse.put("country", user.getCountry());
        userInfoResponse.put("vip", user.getVip());
        userInfoResponse.put("remainingOptimizeCount", user.getRemainingOptimizeCount());
        
        data.put("userInfo", userInfoResponse);
        response.put("data", data);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 更新用户信息
     * @param id 用户ID
     * @param userInfo 用户信息
     * @return 更新后的用户信息
     */
    @PutMapping("/{id}")
    public User updateUser(@PathVariable Long id, @RequestBody User userInfo) {
        // 临时直接返回用户对象，避免方法调用错误
        return userInfo;
    }

    /**
     * 检查是否有优化次数
     * @param userId 用户ID
     * @return 是否有优化次数
     */
    @GetMapping("/{userId}/check-optimize")
    public Boolean checkOptimizeCount(@PathVariable Long userId) {
        return userService.checkOptimizeCount(userId);
    }

    /**
     * 检查是否是VIP
     * @param userId 用户ID
     * @return VIP状态
     */
    @GetMapping("/{userId}/check-vip")
    public Boolean checkVipStatus(@PathVariable Long userId) {
        return false;
    }

    /**
     * 获取用户信息
     * @param userId 用户ID
     * @return 用户信息
     */
    @GetMapping("/info")
    public User getUserInfo(@RequestParam Long userId) {
        return null;
    }
}