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

    public static final int BEGIN_INDEX = 7;
    private final JWTService jwtService;
    private final ApplicationContext context;

    public JwtFilter(final JWTService jwtServiceParam, final ApplicationContext contextParam) {
        this.jwtService = jwtServiceParam;
        this.context = contextParam;
    }

    @Override
    protected final void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response,
                                          final FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        String token = null;
        String email = null;


        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                if ("token".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }


        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                email = jwtService.extractEmail(token);
                UserDetails userDetails = context.getBean(CustomUserDetailsService.class).loadUserByUsername(email);
                if (jwtService.validateToken(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            } catch (Exception e) {
                ResponseCookie clearCookie = ResponseCookie.from("token", "").httpOnly(true).path("/").maxAge(0).build();
                response.setHeader(HttpHeaders.SET_COOKIE, clearCookie.toString());
            }
        }
        filterChain.doFilter(request, response);

    }
}
