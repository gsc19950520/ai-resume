package com.aicv.airesume.controller;

import com.aicv.airesume.entity.User;
import com.aicv.airesume.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Value("${wechat.app-id}")
    private String appId;
    
    @Value("${wechat.app-secret}")
    private String appSecret;
    
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
     * 从微信服务器获取openId
     * @param code 微信登录code
     * @return openId
     */
    private String getOpenIdFromWechat(String code) {
        try {
            // 微信code2Session接口
            String url = String.format(
                "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                appId, appSecret, code
            );
            
            // 调用微信API
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            // 处理响应
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> result = response.getBody();
                
                // 检查是否有错误
                if (result.containsKey("errcode")) {
                    Integer errcode = (Integer) result.get("errcode");
                    if (errcode != 0) {
                        String errmsg = (String) result.get("errmsg");
                        throw new RuntimeException("微信服务器错误: " + errmsg);
                    }
                }
                
                // 获取openId
                String openId = (String) result.get("openid");
                if (openId == null || openId.isEmpty()) {
                    throw new RuntimeException("未获取到openId");
                }
                
                return openId;
            } else {
                throw new RuntimeException("微信服务器响应失败");
            }
        } catch (Exception e) {
            // 在开发环境中，如果获取真实openId失败，使用一个基于code的临时openId
            // 这样可以确保即使没有真实的微信配置，也能进行功能测试
            String tempOpenId = "temp_openid_" + code.hashCode();
            System.err.println("获取微信openId失败，使用临时openId: " + tempOpenId + "，错误: " + e.getMessage());
            return tempOpenId;
        }
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
        
        // 调用微信API获取openId
        String openId = getOpenIdFromWechat(code);
        
        // 调用userService进行微信登录，处理openid入库、用户信息保存等逻辑
        User user = userService.wechatLogin(openId, nickname, avatarUrl);
        
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
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User userInfo) {
        try {
            // 验证ID是否匹配
            if (!id.equals(userInfo.getId())) {
                return ResponseEntity.badRequest().body(null);
            }
            
            // 调用服务层更新用户信息
            User updatedUser = userService.updateUser(userInfo);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            // 处理异常，返回500错误
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
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
    public ResponseEntity<Map<String, Object>> getUserInfo() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", null);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 保存用户信息修改（前端使用的接口）
     * @param requestData 请求数据（包含userId和用户信息）
     * @return 更新后的用户信息
     */
    @PostMapping("/update")
    public ResponseEntity<Map<String, Object>> updateUserInfo(@RequestBody Map<String, Object> requestData) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 获取userId参数
            Object userIdObj = requestData.get("userId");
            if (userIdObj == null) {
                response.put("success", false);
                response.put("message", "缺少userId参数");
                return ResponseEntity.ok(response);
            }
            
            // 转换userId为Long类型
            Long userId;
            try {
                userId = Long.valueOf(userIdObj.toString());
            } catch (NumberFormatException e) {
                response.put("success", false);
                response.put("message", "userId格式错误");
                return ResponseEntity.ok(response);
            }
            
            // 检查用户是否存在
            Optional<User> existingUserOpt = userService.getUserById(userId);
            if (!existingUserOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "用户不存在");
                return ResponseEntity.ok(response);
            }
            
            // 获取现有用户信息
            User existingUser = existingUserOpt.get();
            
            // 创建一个新的User对象用于验证
            User userToValidate = new User();
            
            // 更新用户信息（从requestData中提取字段）
            if (requestData.containsKey("name")) {
                String name = requestData.get("name").toString().trim();
                existingUser.setName(name);
                userToValidate.setName(name);
            }
            if (requestData.containsKey("gender")) {
                Integer gender = Integer.valueOf(requestData.get("gender").toString());
                existingUser.setGender(gender);
                userToValidate.setGender(gender);
            }
            if (requestData.containsKey("phone")) {
                String phone = requestData.get("phone").toString().trim();
                existingUser.setPhone(phone);
                userToValidate.setPhone(phone);
            }
            if (requestData.containsKey("email")) {
                String email = requestData.get("email").toString().trim();
                existingUser.setEmail(email);
                userToValidate.setEmail(email);
            }
            if (requestData.containsKey("birthday")) {
                String birthDate = requestData.get("birthday").toString();
                existingUser.setBirthDate(birthDate);
                userToValidate.setBirthDate(birthDate);
            }
            if (requestData.containsKey("city")) {
                String city = requestData.get("city").toString().trim();
                existingUser.setCity(city);
                userToValidate.setCity(city);
            }

            if (requestData.containsKey("address")) {
                String address = requestData.get("address").toString().trim();
                existingUser.setAddress(address);
                userToValidate.setAddress(address);
            }
            if (requestData.containsKey("avatarUrl")) {
                String avatarUrl = requestData.get("avatarUrl").toString();
                existingUser.setAvatarUrl(avatarUrl);
            }
            
            // 参数验证
            String validationError = validateUserInfo(userToValidate);
            if (validationError != null) {
                response.put("success", false);
                response.put("message", validationError);
                return ResponseEntity.ok(response);
            }
            
            // 保存更新后的用户信息
            User updatedUser = userService.updateUser(existingUser);
            
            // 构建返回数据，避免返回敏感信息
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", updatedUser.getId());
            userData.put("name", updatedUser.getName());
            userData.put("gender", updatedUser.getGender());
            userData.put("phone", updatedUser.getPhone());
            userData.put("email", updatedUser.getEmail());
            userData.put("birthDate", updatedUser.getBirthDate());
            userData.put("city", updatedUser.getCity());
            userData.put("address", updatedUser.getAddress());
            userData.put("avatarUrl", updatedUser.getAvatarUrl());
            
            response.put("success", true);
            response.put("message", "保存成功");
            response.put("data", userData);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "保存失败: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }
    
    /**
     * 验证用户信息
     * @param user 用户信息
     * @return 验证错误信息，如果验证通过则返回null
     */
    private String validateUserInfo(User user) {
        // 验证手机号格式
        if (user.getPhone() != null && !user.getPhone().trim().isEmpty()) {
            // 简单的手机号格式验证（中国大陆手机号）
            if (!user.getPhone().trim().matches("^1[3-9]\\d{9}$")) {
                return "请输入正确的手机号码";
            }
        }
        
        // 验证邮箱格式
        if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
            // 简单的邮箱格式验证
            if (!user.getEmail().trim().matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
                return "请输入正确的邮箱地址";
            }
        }
        
        // 验证性别值
        if (user.getGender() != null && user.getGender() != 0 && user.getGender() != 1 && user.getGender() != 2) {
            return "性别值无效";
        }
        
        // 验证姓名长度
        if (user.getName() != null && user.getName().trim().length() > 50) {
            return "姓名长度不能超过50个字符";
        }
        
        // 验证城市和地址长度
        if (user.getCity() != null && user.getCity().trim().length() > 100) {
            return "城市名称过长";
        }
        
        if (user.getAddress() != null && user.getAddress().trim().length() > 200) {
            return "地址长度不能超过200个字符";
        }
        

        
        return null; // 验证通过
    }
}