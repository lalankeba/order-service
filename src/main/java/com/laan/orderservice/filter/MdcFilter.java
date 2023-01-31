package com.laan.orderservice.filter;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

@Component
public class MdcFilter extends OncePerRequestFilter {

    private static final String MDC_KEY = "mdcId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        MDC.put(MDC_KEY, get32LengthId());
        chain.doFilter(request, response);
        MDC.remove(MDC_KEY);
    }

    private String get32LengthId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
