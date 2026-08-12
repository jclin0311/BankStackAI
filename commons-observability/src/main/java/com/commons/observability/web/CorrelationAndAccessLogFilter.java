package com.commons.observability.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.Principal;
import java.util.UUID;

/**
 * 📊 CorrelationAndAccessLogFilter
 *
 * This servlet filter is executed once per HTTP request. It is responsible for:
 * 
 * ✅ Generating or propagating a **Correlation ID (cid)** for end-to-end traceability  
 * ✅ Logging a concise "enter" and "exit" line for every controller request  
 * ✅ Capturing request method, path, response status, and total duration  
 * ✅ Setting the `X-Request-Id` header on all responses  
 * 
 * 🛡️ Design principles:
 * - Does NOT log request or response bodies (privacy + performance).
 * - Does NOT use lambdas or `var` (for compatibility and readability).
 * - Uses MDC (Mapped Diagnostic Context) so that every log line for a given request
 *   automatically carries the same correlation metadata.
 */
@Order(1) // Ensures this filter runs early in the chain
public class CorrelationAndAccessLogFilter extends OncePerRequestFilter {

  private static final Logger LOG = LoggerFactory.getLogger(CorrelationAndAccessLogFilter.class);

  // MDC key name for correlation ID
  public static final String CID = "cid";

  @Override
  protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {

    // 📌 Try to read the correlation ID from the incoming request header
    String cid = req.getHeader("X-Request-Id");

    // If none present (first hop), generate a new UUID
    if (cid == null || cid.trim().isEmpty()) {
      cid = UUID.randomUUID().toString();
    }

    // Record start time for duration calculation
    long startNs = System.nanoTime();

    // 🧠 Put core request metadata into MDC — this will auto-attach to all logs during this request
    MDC.put(CID, cid);
    MDC.put("method", req.getMethod());
    MDC.put("path", req.getRequestURI());

    // Set X-Request-Id on response early so clients and downstream services always see it
    res.setHeader("X-Request-Id", cid);

    try {
      // 📥 Log request entry — controller just received the call
      LOG.info("ENTER controller cid={} method={} path={}", cid, MDC.get("method"), MDC.get("path"));

      // Continue the filter chain (this hands over control to Spring MVC and controllers)
      chain.doFilter(req, res);

    } finally {
      // 📏 Compute request duration in milliseconds
      long durMs = (System.nanoTime() - startNs) / 1_000_000L;

      // Put response metadata into MDC before exit log
      MDC.put("status", Integer.toString(res.getStatus()));
      MDC.put("durMs", Long.toString(durMs));

      // Echo header again (defensive) — ensures response always has X-Request-Id
      res.setHeader("X-Request-Id", cid);

      // 📤 Log the exit line — a single concise access log line
      LOG.info(
          "Exit controller cid={} method={} path={} status={} durMs={}",
          cid,
          MDC.get("method"),
          MDC.get("path"),
          MDC.get("status"),
          MDC.get("durMs")
      );

      // 🧹 Always clear MDC to avoid leaking context into unrelated threads/requests
      MDC.clear();
    }
  }
}
