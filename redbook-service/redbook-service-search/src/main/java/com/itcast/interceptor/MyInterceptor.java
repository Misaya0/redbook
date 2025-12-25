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
        Long userId = null;

        String headerUserId = request.getHeader("userId");
        if (headerUserId != null) {
            try {
                userId = Long.valueOf(headerUserId);
            } catch (NumberFormatException e) {
                log.warn("请求头 userId 非法: {}", headerUserId);
            }
        }

        if (userId == null) {
            String authorization = request.getHeader("Authorization");
            String token;
            if (authorization != null && authorization.startsWith("Bearer ")) {
                token = authorization.substring(7);
            } else {
                token = request.getHeader("token");
            }

            if (token != null) {
                try {
                    userId = Long.valueOf(JwtUtil.parseToken(token));
                } catch (Exception e) {
                    log.error("解析 token 失败", e);
                }
            }
        }

        if (userId != null) {
            log.info("用户id为：{}", userId);
            UserContext.setUserId(userId);
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
