package com.example.studentmanagement.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtService jwtService;
	private final CustomerUserDetailsService userDetailsService;

	public JwtAuthenticationFilter(JwtService jwtService, CustomerUserDetailsService userDetailsService) {
		this.jwtService = jwtService;
		this.userDetailsService = userDetailsService;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
									FilterChain filterChain) throws ServletException, IOException {
		String authorization = request.getHeader("Authorization");
		if (authorization != null && authorization.startsWith("Bearer ")
				&& SecurityContextHolder.getContext().getAuthentication() == null) {
			authenticate(authorization.substring(7), request);
		}
		filterChain.doFilter(request, response);
	}

	private void authenticate(String token, HttpServletRequest request) {
		try {
			Claims claims = jwtService.parseAccessToken(token);
			UserDetails user = userDetailsService.loadUserByUsername(claims.getSubject());
			if (!user.isEnabled()) {
				SecurityContextHolder.clearContext();
				return;
			}
			UsernamePasswordAuthenticationToken authentication =
					new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
			authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
			SecurityContext context = SecurityContextHolder.createEmptyContext();
			context.setAuthentication(authentication);
			SecurityContextHolder.setContext(context);
		} catch (JwtException | IllegalArgumentException | UsernameNotFoundException exception) {
			SecurityContextHolder.clearContext();
		}
	}
}
