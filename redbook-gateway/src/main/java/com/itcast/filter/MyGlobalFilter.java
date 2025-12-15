package com.itcast.filter;

import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.itcast.enums.ResponseEnum;
import com.itcast.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 全局过滤器
 */
@Slf4j
@Configuration
public class MyGlobalFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        log.info("网关全局过滤器开启");
        // 1.获取请求和响应对象
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        // 2.判断是否是登录接口
        String path = request.getURI().getPath();
        log.info("请求接口为：{}", path);
        if (path.contains("/user/send") || path.contains("/user/verify") || path.contains("/note/v2/api-docs") ||
                path.contains("/note/getNoteList") || path.contains("/user/uploads") || path.contains("/note/uploads")
                || path.contains("/search") || path.contains("/api/message/v3/api-docs")) {
            log.info("请求接口放行");
            return chain.filter(exchange);
        }
        // 3. 获取 token：优先从 Authorization 取，其次兼容旧的 token 头
        String token = request.getHeaders().getFirst("Authorization");
        if (StringUtils.isNotBlank(token) && token.startsWith("Bearer ")) {
            token = token.substring(7); // 去掉 "Bearer "
        } else {
            token = request.getHeaders().getFirst("token"); // 兼容老客户端
        }
        // 4.判断token是否为空
        if (StringUtils.isBlank(token)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }
        // 5.判断token是否过期
        try {
            String userId = JwtUtil.parseToken(token);
            // 6.向请求头添加userId
            ServerHttpRequest modifiedRequest = request.mutate().header("userId", userId).build();
            ServerWebExchange modifiedExchange = exchange.mutate().request(modifiedRequest).build();
            log.info("网关全局过滤器结束，userId: {}", userId);
            return chain.filter(modifiedExchange);
        } catch (TokenExpiredException | JWTDecodeException e2) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
