package com.commons.observability.http;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.slf4j.MDC;

import java.util.UUID;

/**
 * 📡 CidFeignInterceptor
 *
 * This interceptor ensures that every outbound Feign HTTP request carries a
 * `X-Request-Id` header — which represents the **correlation ID (cid)**.
 *
 * ✅ Purpose:
 * - Enable **end-to-end request traceability** across distributed microservices.
 * - Ensure downstream services log the same `cid` so logs can be correlated.
 *
 * 🧠 How it works:
 * - Attempts to read the existing correlation ID from the SLF4J MDC (Mapped Diagnostic Context).
 * - If no `cid` is present (e.g., first hop or missing in context), generates a new UUID.
 * - Adds this `cid` as an `X-Request-Id` header to the outgoing Feign request.
 *
 * ⚙️ This class is automatically picked up and registered as a `RequestInterceptor`
 *     when enabled by `commons.logging.feign.enabled=true` in the auto-configuration.
 *
 */
public class CidFeignInterceptor implements RequestInterceptor {

  /**
   * Called for every Feign request before it is sent.
   * Adds or propagates the `X-Request-Id` header based on the MDC context.
   *
   * @param template the Feign request template that will be sent downstream
   */
  @Override
  public void apply(RequestTemplate template) {
    // Try to read existing correlation ID from MDC (set earlier by the web filter)
    String cid = MDC.get("cid");

    // If none exists (first hop or missing context), generate a new one
    if (cid == null || cid.trim().isEmpty()) {
      cid = UUID.randomUUID().toString();
    }

    // Attach the CID as a header so downstream services receive and propagate it
    template.header("X-Request-Id", cid);
  }
}
