package com.aicv.airesume.controller;

import com.aicv.airesume.entity.User;
import com.aicv.airesume.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
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
     * @param request 请求参数，包含code
     * @return 登录结果
     */
    @PostMapping("/wechat-login")
    public Map<String, Object> wechatLogin(@RequestBody Map<String, String> request) {
        String code = request.get("code");
        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException("登录code不能为空");
        }
        
        // 创建响应对象
        Map<String, Object> response = new HashMap<>();
        response.put("code", 0);
        response.put("message", "登录成功");
        response.put("data", new HashMap<String, String>() {{ 
            put("token", "mock_token_" + System.currentTimeMillis());
            put("openId", "mock_openid_" + code.substring(0, 8));
        }});
        
        return response;
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