package com.aicv.airesume.interceptor;

import com.aicv.airesume.model.vo.BaseResponseVO;
import com.aicv.airesume.utils.GlobalContextUtil;
import com.aicv.airesume.utils.TokenUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Token拦截器
 * 用于验证请求中的token是否有效，并在需要时刷新token有效期
 */
@Slf4j
@Component
public class TokenRefreshInterceptor implements HandlerInterceptor {

    @Autowired
    private TokenUtils tokenUtils;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取请求路径
        String requestURI = request.getRequestURI();
        
        // 处理OPTIONS请求，直接放行
        if ("OPTIONS".equals(request.getMethod())) {
            log.info("OPTIONS请求，直接放行");
            return true;
        }
        
        // 不需要token验证的路径
        if (requestURI.startsWith("/api/user/login") || 
            requestURI.startsWith("/api/user/wechat-login") ||
            requestURI.startsWith("/api/user/register") || 
            requestURI.startsWith("/api/job-types") ||
            requestURI.startsWith("/api/interview/update-remaining-time") ||
            requestURI.startsWith("/swagger") ||
            requestURI.startsWith("/v3/api-docs")) {
            return true;
        }
        
        // 获取Authorization头
        String authorizationHeader = request.getHeader("Authorization");
        
        // 验证token
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            log.info("token为空或格式不正确，返回401错误");
            sendErrorResponse(response, "Token无效，请重新登录");
            return false;
        }
        
        // 移除"Bearer "前缀
        String token = authorizationHeader.substring(7);
        // 验证token是否有效
        if (!tokenUtils.validateToken(token)) {
            log.info("token验证失败，返回401错误");
            sendErrorResponse(response, "Token无效，请重新登录");
            return false;
        }
        // 从token中获取用户ID
        Long userId = tokenUtils.getUserIdFromToken(token);
        
        if (userId == null) {
            log.info("从token中获取userId失败，返回401错误");
            sendErrorResponse(response, "Token无效，请重新登录");
            return false;
        }
        
        // 将用户ID存入ThreadLocal中，方便全局获取
        GlobalContextUtil.setUserId(userId);
        
        // 获取token剩余时间
        long remainingTime = tokenUtils.getRemainingTime(token);
        
        // 如果token剩余时间少于30分钟（1800000毫秒），则刷新token
        if (remainingTime > 0 && remainingTime < 1800000) {
            String newToken = tokenUtils.refreshToken(token);
            if (newToken != null) {
                // 将新token添加到响应头中
                response.setHeader("X-New-Token", newToken);
                response.setHeader("X-Token-Refreshed", "true");
                log.info("token已刷新，新token: {}", newToken);
            }
        }
        
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 清除ThreadLocal中的数据，防止内存泄漏
        GlobalContextUtil.clearUserId();
    }
    
    /**
     * 发送错误响应
     */
    private void sendErrorResponse(HttpServletResponse response, String message) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        PrintWriter writer = response.getWriter();
        // 使用401错误码而不是默认的500，保持与HTTP状态码一致
        writer.write(objectMapper.writeValueAsString(BaseResponseVO.error(HttpServletResponse.SC_UNAUTHORIZED, message)));
        writer.flush();
        writer.close();
    }
}