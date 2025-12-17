package com.itcast.interceptor;
import org.slf4j.MDC;
import java.util.UUID;

import com.itcast.context.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 自定义拦截器
 */
@Slf4j
@Component
public class MyInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        log.info("前置拦截器");
        // ===== 1. 生成 traceId =====
        String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        // ===== 2. 记录接口信息 =====
        String qs = request.getQueryString();
        String req = request.getMethod() + " " + request.getRequestURI() + (qs == null ? "" : "?" + qs);

        MDC.put("traceId", traceId);
        MDC.put("req", req);

        // ===== 3. 用户信息 =====
        String userId = request.getHeader("userId");
        if (userId != null) {
            MDC.put("uid", userId);              // ★ 关键：放入 MDC
            UserContext.setUserId(Integer.valueOf(userId));
            log.info("用户id为：{}", userId);
        } else {
            MDC.put("uid", "anonymous");
        }

        log.info("========== 请求开始 ==========");
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        log.info("后置拦截器");
        log.info("========== 请求结束 ==========");
        UserContext.clear();
        MDC.clear();   // ★ 非常重要，防止线程复用污染
    }
}
