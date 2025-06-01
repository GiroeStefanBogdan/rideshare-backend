package com.example.blablacar.config;


import com.example.blablacar.service.CustomUserDetailsService;
import com.example.blablacar.service.JWTService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JWTService jwtService;
    private final ApplicationContext context;

    public JwtFilter(JWTService jwtService, ApplicationContext context) {
        this.jwtService = jwtService;
        this.context = context;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = null;
        String email = null;


        // 🔍 2️⃣ If no header token, look for token in cookies
        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                System.out.println("🔍 JwtFilter: Checking cookie: " + cookie.getName());
                if ("token".equals(cookie.getName())) {
                    token = cookie.getValue();
                    System.out.println("✅ JwtFilter: Token found in cookie: " + token);
                    break;
                }
            }
        }

        // 🔍 3️⃣ Extract email from token if present
        if (token != null) {
            try {
                email = jwtService.extractEmail(token);
                System.out.println("🔍 JwtFilter: Extracted email from token: " + email);
            } catch (Exception e) { // SignatureException -> Token is invalid jwtService.extractEmail(token);
                System.out.println("⚠️ JwtFilter: Invalid token (signature mismatch?). Ignoring it. " + e.getMessage());
                ResponseCookie clearCookie = ResponseCookie.from("token", "")
                        .httpOnly(true)
                        .path("/")
                        .maxAge(0)
                        .build();
                response.setHeader(HttpHeaders.SET_COOKIE, clearCookie.toString());
            }
        }

        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = context.getBean(CustomUserDetailsService.class).loadUserByUsername(email);

            if (jwtService.validateToken(token, userDetails)) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        filterChain.doFilter(request, response);

    }
}
