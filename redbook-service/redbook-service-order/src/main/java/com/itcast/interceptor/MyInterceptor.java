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
        String userId = request.getHeader("userId");

        if (userId == null) {
            String authorization = request.getHeader("Authorization");
            String token = null;
            if (authorization != null && authorization.startsWith("Bearer ")) {
                token = authorization.substring(7);
            } else {
                token = request.getHeader("token");
            }

            if (token != null) {
                try {
                    userId = JwtUtil.parseToken(token);
                } catch (Exception e) {
                    log.error("解析 token 失败", e);
                }
            }
        }

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
