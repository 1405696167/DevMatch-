package com.devmatch.security;

import com.devmatch.mapper.UserMapper;
import com.devmatch.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);
        if (StringUtils.hasText(token)) {
            try {
                Claims claims = jwtUtil.parseToken(token);
                Long userId = Long.parseLong(claims.getSubject());
                User user = userMapper.selectById(userId);
                if (user != null && "ACTIVE".equals(user.getStatus())) {
                    SecurityUser securityUser = new SecurityUser(user);
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (JwtException e) {
                log.debug("JWT解析失败: {}", e.getMessage());
            } catch (Exception e) {
                log.warn("认证处理异常", e);
            }
        }
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        // 交付物下载：浏览器/下载管理器用 <a href> 打开时无法带 Authorization，且 fetch 易被 IDM 等插件拦截为 204。
        // 仅对该 GET 路径接受 query 中的 access_token（与 Bearer 等价）。
        if (isDeliverableDownloadGet(request)) {
            String qp = request.getParameter("access_token");
            if (StringUtils.hasText(qp)) {
                return qp.trim();
            }
        }
        return null;
    }

    private static boolean isDeliverableDownloadGet(HttpServletRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String uri = request.getRequestURI();
        if (uri == null) {
            return false;
        }
        return uri.contains("/projects/deliverables/") && uri.endsWith("/download");
    }
}
