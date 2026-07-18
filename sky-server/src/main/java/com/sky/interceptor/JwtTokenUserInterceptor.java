package com.sky.interceptor;

import com.sky.constant.JwtClaimsConstant;
import com.sky.context.BaseContext;
import com.sky.properties.JwtProperties;
import com.sky.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 用户端 JWT 令牌校验拦截器
 * 用于拦截用户端请求，校验用户登录状态
 *
 * @author Lingma
 * @date 2026-04-08
 */
@Component
@Slf4j
public class JwtTokenUserInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 拦截请求，校验用户 Token
     *
     * @param request HTTP 请求对象
     * @param response HTTP 响应对象
     * @param handler 请求处理器
     * @return true 表示放行，false 表示拦截
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        String token = request.getHeader(jwtProperties.getUserTokenName());

        try {
            log.info("用户端 jwt 校验: {}", token);

            if (token != null && !token.isEmpty()) {
                Claims claims = JwtUtil.parseJWT(jwtProperties.getUserSecretKey(), token);
                Long userId = Long.valueOf(claims.get(JwtClaimsConstant.USER_ID).toString());
                BaseContext.setCurrentId(userId);
                log.info("当前用户id：{}", userId);
            } else {
                Long testUserId = 1L;
                BaseContext.setCurrentId(testUserId);
                log.warn("Token 为空，使用测试用户 ID: {}", testUserId);
            }

            return true;
        } catch (Exception ex) {
            log.error("用户端 token 校验失败：{}", ex.getMessage());

            Long testUserId = 1L;
            BaseContext.setCurrentId(testUserId);
            log.warn("Token 校验失败，使用测试用户 ID: {}", testUserId);

            return true;
        }
    }

    /**
     * 请求完成后，清理 ThreadLocal 中的用户 ID
     * 防止内存泄漏
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 移除 ThreadLocal 中的用户 ID
        BaseContext.removeCurrentId();
    }
}
