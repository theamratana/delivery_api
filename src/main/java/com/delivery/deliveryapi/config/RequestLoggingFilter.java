package com.delivery.deliveryapi.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Value("${logging.http.include-headers:true}")
    private boolean includeHeaders;

    @Value("${logging.http.include-payload:false}")
    private boolean includePayload;

    @Value("${logging.http.include-response-payload:false}")
    private boolean includeResponsePayload;

    @Value("${logging.http.max-payload-length:2048}")
    private int maxPayloadLength;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String reqId = Optional.ofNullable(request.getHeader("X-Request-Id"))
                .filter(s -> !s.isBlank())
                .orElseGet(() -> UUID.randomUUID().toString().substring(0, 8));
        MDC.put("reqId", reqId);

        ContentCachingRequestWrapper reqWrapper = new ContentCachingRequestWrapper(request, Math.max(0, maxPayloadLength));
        ContentCachingResponseWrapper resWrapper = new ContentCachingResponseWrapper(response);

        long start = System.nanoTime();
        int status = 200;
        try {
            filterChain.doFilter(reqWrapper, resWrapper);
            status = resWrapper.getStatus();
        } catch (Exception ex) {
            status = 500;
            throw ex;
        } finally {
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            String method = reqWrapper.getMethod();
            String path = reqWrapper.getRequestURI();
            String qs = reqWrapper.getQueryString();
            if (qs != null && !qs.isEmpty()) {
                path = path + "?" + qs;
            }
            String ip = Optional.ofNullable(reqWrapper.getHeader("X-Forwarded-For")).orElseGet(reqWrapper::getRemoteAddr);
            String ua = Optional.ofNullable(reqWrapper.getHeader("User-Agent")).orElse("-");
            String referer = Optional.ofNullable(reqWrapper.getHeader("Referer")).orElse("-");

            int inBytes = reqWrapper.getContentAsByteArray() != null ? reqWrapper.getContentAsByteArray().length : 0;
            int outBytes = resWrapper.getContentAsByteArray() != null ? resWrapper.getContentAsByteArray().length : 0;

            log.info("request method={} path={} status={} durationMs={} ip={} ua={} referer={} bytesIn={} bytesOut={} reqId={}",
                    method, path, status, durationMs, ip, ua, referer, inBytes, outBytes, reqId);

            if (includeHeaders) {
                String contentType = Optional.ofNullable(reqWrapper.getContentType()).orElse("-");
                log.debug("headers contentType={} accept={} reqId={}", contentType,
                        Optional.ofNullable(reqWrapper.getHeader("Accept")).orElse("-"), reqId);
            }

            if (includePayload && inBytes > 0) {
                String body = new String(reqWrapper.getContentAsByteArray(), Optional.ofNullable(reqWrapper.getCharacterEncoding()).orElse(StandardCharsets.UTF_8.name()));
                body = truncate(body, maxPayloadLength);
                log.debug("requestBody={} reqId={}", body, reqId);
            }

            if (includeResponsePayload && outBytes > 0) {
                String respBody = new String(resWrapper.getContentAsByteArray(), Optional.ofNullable(resWrapper.getCharacterEncoding()).orElse(StandardCharsets.UTF_8.name()));
                respBody = truncate(respBody, maxPayloadLength);
                log.debug("responseBody={} reqId={}", respBody, reqId);
            }

            // important: copy cached response back to the client
            resWrapper.copyBodyToResponse();
            MDC.remove("reqId");
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        if (s.length() <= max) return s;
        return s.substring(0, Math.max(0, max)) + "…(truncated)";
    }
}
