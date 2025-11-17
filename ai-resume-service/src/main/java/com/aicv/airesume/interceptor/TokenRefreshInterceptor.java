package com.aicv.airesume.interceptor;

import com.aicv.airesume.utils.TokenUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Token刷新拦截器
 * 当用户调用需要认证的接口时，自动刷新token有效期
 */
@Component
public class TokenRefreshInterceptor implements HandlerInterceptor {

    @Autowired
    private TokenUtils tokenUtils;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取Authorization头
        String authorizationHeader = request.getHeader("Authorization");
        
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);
            
            // 验证token是否有效
            if (tokenUtils.validateToken(token)) {
                // 获取token剩余时间
                long remainingTime = tokenUtils.getRemainingTime(token);
                
                // 如果token剩余时间少于30分钟（1800000毫秒），则刷新token
                if (remainingTime > 0 && remainingTime < 1800000) {
                    String newToken = tokenUtils.refreshToken(token);
                    if (newToken != null) {
                        // 将新token添加到响应头中
                        response.setHeader("X-New-Token", newToken);
                        response.setHeader("X-Token-Refreshed", "true");
                    }
                }
            }
        }
        
        return true;
    }
}