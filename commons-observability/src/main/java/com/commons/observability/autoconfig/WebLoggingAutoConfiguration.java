package com.commons.observability.autoconfig;

import com.commons.observability.web.CorrelationAndAccessLogFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * 📊 Auto-configuration class for web logging and correlation ID propagation.
 * 
 * This configuration is automatically loaded by Spring Boot if present on the classpath.
 * It registers filters and interceptors to:
 *   - Generate and propagate a Correlation ID (cid) across microservices.
 *   - Log request/response metadata consistently.
 *   - Optionally propagate the CID in Feign client calls.
 */
@AutoConfiguration
public class WebLoggingAutoConfiguration {

  /**
   * ✅ Registers the CorrelationAndAccessLogFilter bean.
   * 
   * - This filter generates a correlation ID (cid) for every incoming HTTP request
   *   if one is not already present, and logs request/response metadata.
   * - The filter is registered only if:
   *    1. No other CorrelationAndAccessLogFilter bean is already defined.
   *    2. Property `commons.logging.filter.enabled=true` (default: true).
   */
  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnProperty(
      name = "commons.logging.filter.enabled", 
      havingValue = "true", 
      matchIfMissing = true
  )
  public CorrelationAndAccessLogFilter correlationAndAccessLogFilter() {
    return new CorrelationAndAccessLogFilter();
  }

  /**
   * 📡 Nested configuration class for Feign client integration.
   * 
   * This class is only activated if Feign (`feign.RequestInterceptor`) is present on the classpath.
   * It defines an interceptor that attaches the current correlation ID to outbound Feign requests,
   * enabling end-to-end traceability across microservice calls.
   */
  @ConditionalOnClass(name = "feign.RequestInterceptor")
  static class FeignCidConfig {

    /**
     * ✅ Registers the Feign RequestInterceptor for correlation ID propagation.
     * 
     * - Bean name: `cidFeignInterceptor`
     * - Registered only if:
     *    1. A bean with that name does not already exist.
     *    2. Property `commons.logging.feign.enabled=true` (default: true).
     */
    @Bean(name = "cidFeignInterceptor")
    @ConditionalOnMissingBean(name = "cidFeignInterceptor")
    @ConditionalOnProperty(
        name = "commons.logging.feign.enabled", 
        havingValue = "true", 
        matchIfMissing = true
    )
    public feign.RequestInterceptor cidFeignInterceptor() {
      return new com.commons.observability.http.CidFeignInterceptor();
    }
  }
}
