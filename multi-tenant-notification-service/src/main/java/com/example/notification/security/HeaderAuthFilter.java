package com.example.notification.security;
import com.example.notification.tenant.Role;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.*;

public class HeaderAuthFilter extends OncePerRequestFilter{
    protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)
         throws ServletException,IOException {
        String user=req.getHeader("X-User-Id");
        String role=req.getHeader("X-Role");
        String tenant=req.getHeader("X-Tenant-Id");
        if(user!=null && role!=null) {
            try {
                Role r=Role.valueOf(role);
                UUID tid = tenant == null ? null : UUID.fromString(tenant);
                CurrentUser principal=new CurrentUser(user,tid,r);
                var auth=new UsernamePasswordAuthenticationToken(principal,null,List.of(new SimpleGrantedAuthority("ROLE_"+r.name())));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (IllegalArgumentException ignored) {

            }
        }
        chain.doFilter(req,res);
    }
}
