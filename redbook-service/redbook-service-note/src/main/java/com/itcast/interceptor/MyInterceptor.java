package com.itcast.interceptor;

import com.itcast.context.UserContext;
import com.itcast.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 自定义拦截器
 */
@Slf4j
public class MyInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        log.info("前置拦截器");
        // 1. 先尝试从请求头获取 userId（网关传递的）
        String userId = request.getHeader("userId");

        // 2. 如果没有 userId，尝试从 token 中解析（直接访问微服务的情况）
        if (userId == null) {
            String token = request.getHeader("token");
            if (token != null) {
                try {
                    userId = JwtUtil.parseToken(token);
                    log.info("从 token 中解析出用户id：{}", userId);
                } catch (Exception e) {
                    log.error("解析 token 失败", e);
                }
            }
        }

        // 3. 设置用户id到上下文
        if (userId != null) {
            log.info("用户id为：{}", userId);
            UserContext.setUserId(Integer.valueOf(userId));
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        log.info("后置拦截器");
        UserContext.clear();
    }
}
