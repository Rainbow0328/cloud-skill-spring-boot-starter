/*
 * Copyright 2024 Cloud Skill Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cloudskill.sdk.core.resilience;

import com.cloudskill.sdk.config.CloudSkillProperties;
import com.cloudskill.sdk.model.Skill;
import com.cloudskill.sdk.model.SkillCallResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

/**
 * 熔断降级管理器
 * 可选依赖Resilience4j，不存在时自动降级
 */
@Slf4j
public class ResilienceManager {
    
    private final CloudSkillProperties properties;
    private Object circuitBreakerRegistry;
    private Object rateLimiterRegistry;
    private final ConcurrentMap<String, Object> circuitBreakerCache;
    private final ConcurrentMap<String, Object> rateLimiterCache;
    private final boolean resilienceEnabled;
    
    // 反射缓存
    private Method circuitBreakerRegistryCircuitBreakerMethod;
    private Method rateLimiterRegistryRateLimiterMethod;
    private Method rateLimiterDecorateSupplierMethod;
    private Method circuitBreakerDecorateSupplierMethod;
    
    public ResilienceManager(CloudSkillProperties properties) {
        this.properties = properties;
        this.circuitBreakerCache = new ConcurrentHashMap<>();
        this.rateLimiterCache = new ConcurrentHashMap<>();
        
        // 检查Resilience4j是否在类路径中
        boolean resilienceAvailable = isResilienceAvailable();
        this.resilienceEnabled = resilienceAvailable;
        
        if (resilienceAvailable) {
            try {
                // 初始化反射方法
                initReflectionMethods();
                
                // 创建Registry实例
                Class<?> circuitBreakerRegistryClass = Class.forName("io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry");
                Method ofDefaultsMethod = circuitBreakerRegistryClass.getMethod("ofDefaults");
                this.circuitBreakerRegistry = ofDefaultsMethod.invoke(null);
                
                Class<?> rateLimiterRegistryClass = Class.forName("io.github.resilience4j.ratelimiter.RateLimiterRegistry");
                Method rateLimiterOfDefaultsMethod = rateLimiterRegistryClass.getMethod("ofDefaults");
                this.rateLimiterRegistry = rateLimiterOfDefaultsMethod.invoke(null);
                
                log.info("Resilience4j is available, circuit breaker and rate limiter enabled");
            } catch (Exception e) {
                log.warn("Failed to initialize Resilience4j: {}", e.getMessage());
                this.circuitBreakerRegistry = null;
                this.rateLimiterRegistry = null;
            }
        } else {
            this.circuitBreakerRegistry = null;
            this.rateLimiterRegistry = null;
            log.info("Resilience4j not found in classpath, resilience features disabled");
        }
    }
    
    /**
     * 执行受保护的技能调用
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public SkillCallResult execute(Skill skill, Supplier<SkillCallResult> supplier) {
        if (!resilienceEnabled) {
            return supplier.get();
        }
        
        try {
            // 先限流
            Object rateLimiter = getOrCreateRateLimiter(skill);
            Supplier<SkillCallResult> rateLimitedSupplier = (Supplier<SkillCallResult>) 
                    rateLimiterDecorateSupplierMethod.invoke(null, rateLimiter, supplier);
            
            // 再熔断
            Object circuitBreaker = getOrCreateCircuitBreaker(skill);
            Supplier<SkillCallResult> decoratedSupplier = (Supplier<SkillCallResult>)
                    circuitBreakerDecorateSupplierMethod.invoke(null, circuitBreaker, rateLimitedSupplier);
            
            return decoratedSupplier.get();
        } catch (Exception e) {
            log.warn("Skill {} call rejected by resilience: {}", skill.getId(), e.getMessage());
            return getFallbackResult(skill, e);
        }
    }
    
    /**
     * 获取或创建熔断器
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object getOrCreateCircuitBreaker(Skill skill) {
        String key = "circuit-breaker:" + skill.getId();
        return circuitBreakerCache.computeIfAbsent(key, k -> {
            try {
                // 创建CircuitBreakerConfig
                Class<?> configClass = Class.forName("io.github.resilience4j.circuitbreaker.CircuitBreakerConfig");
                Class<?> builderClass = Class.forName("io.github.resilience4j.circuitbreaker.CircuitBreakerConfig$Builder");
                
                Method customMethod = configClass.getMethod("custom");
                Object builder = customMethod.invoke(null);
                
                Method failureRateThresholdMethod = builderClass.getMethod("failureRateThreshold", float.class);
                failureRateThresholdMethod.invoke(builder, 50f);
                
                Method waitDurationMethod = builderClass.getMethod("waitDurationInOpenState", Duration.class);
                waitDurationMethod.invoke(builder, Duration.ofSeconds(10));
                
                Method slidingWindowSizeMethod = builderClass.getMethod("slidingWindowSize", int.class);
                slidingWindowSizeMethod.invoke(builder, 100);
                
                Method minimumCallsMethod = builderClass.getMethod("minimumNumberOfCalls", int.class);
                minimumCallsMethod.invoke(builder, 10);
                
                Method buildMethod = builderClass.getMethod("build");
                Object config = buildMethod.invoke(builder);
                
                // 创建CircuitBreaker
                return circuitBreakerRegistryCircuitBreakerMethod.invoke(circuitBreakerRegistry, k, config);
            } catch (Exception e) {
                log.warn("Failed to create circuit breaker for skill {}: {}", skill.getId(), e.getMessage());
                return null;
            }
        });
    }
    
    /**
     * 获取或创建限流器
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object getOrCreateRateLimiter(Skill skill) {
        String key = "rate-limiter:" + skill.getId();
        return rateLimiterCache.computeIfAbsent(key, k -> {
            try {
                // 创建RateLimiterConfig
                Class<?> configClass = Class.forName("io.github.resilience4j.ratelimiter.RateLimiterConfig");
                Class<?> builderClass = Class.forName("io.github.resilience4j.ratelimiter.RateLimiterConfig$Builder");
                
                Method customMethod = configClass.getMethod("custom");
                Object builder = customMethod.invoke(null);
                
                // 优先使用技能配置的限流值，默认1000 QPS
                int limitForPeriod = skill.getRateLimit() != null ? skill.getRateLimit() : 1000;
                
                Method limitForPeriodMethod = builderClass.getMethod("limitForPeriod", int.class);
                limitForPeriodMethod.invoke(builder, limitForPeriod);
                
                Method limitRefreshPeriodMethod = builderClass.getMethod("limitRefreshPeriod", Duration.class);
                limitRefreshPeriodMethod.invoke(builder, Duration.ofSeconds(1));
                
                Method timeoutDurationMethod = builderClass.getMethod("timeoutDuration", Duration.class);
                timeoutDurationMethod.invoke(builder, Duration.ofMillis(500));
                
                Method buildMethod = builderClass.getMethod("build");
                Object config = buildMethod.invoke(builder);
                
                // 创建RateLimiter
                return rateLimiterRegistryRateLimiterMethod.invoke(rateLimiterRegistry, k, config);
            } catch (Exception e) {
                log.warn("Failed to create rate limiter for skill {}: {}", skill.getId(), e.getMessage());
                return null;
            }
        });
    }
    
    /**
     * 获取降级结果
     */
    private SkillCallResult getFallbackResult(Skill skill, Exception e) {
        SkillCallResult result = new SkillCallResult();
        result.setSuccess(false);
        result.setCode(429);
        result.setMessage("Service unavailable: " + e.getMessage());
        
        // 如果配置了降级值，使用降级值
        if (StringUtils.hasText(skill.getErrorHandlingStrategy()) 
                && "FALLBACK".equals(skill.getErrorHandlingStrategy())
                && skill.getMetadata() != null
                && skill.getMetadata().containsKey("fallbackValue")) {
            result.setData(skill.getMetadata().get("fallbackValue"));
            result.setSuccess(true);
            result.setMessage("Fallback response");
        }
        
        return result;
    }
    
    /**
     * 检查Resilience4j是否可用
     */
    private boolean isResilienceAvailable() {
        try {
            Class.forName("io.github.resilience4j.circuitbreaker.CircuitBreaker");
            Class.forName("io.github.resilience4j.ratelimiter.RateLimiter");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    /**
     * 初始化反射方法
     */
    private void initReflectionMethods() throws Exception {
        // CircuitBreakerRegistry.circuitBreaker
        Class<?> circuitBreakerRegistryClass = Class.forName("io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry");
        Class<?> circuitBreakerConfigClass = Class.forName("io.github.resilience4j.circuitbreaker.CircuitBreakerConfig");
        this.circuitBreakerRegistryCircuitBreakerMethod = circuitBreakerRegistryClass.getMethod("circuitBreaker", String.class, circuitBreakerConfigClass);
        
        // RateLimiterRegistry.rateLimiter
        Class<?> rateLimiterRegistryClass = Class.forName("io.github.resilience4j.ratelimiter.RateLimiterRegistry");
        Class<?> rateLimiterConfigClass = Class.forName("io.github.resilience4j.ratelimiter.RateLimiterConfig");
        this.rateLimiterRegistryRateLimiterMethod = rateLimiterRegistryClass.getMethod("rateLimiter", String.class, rateLimiterConfigClass);
        
        // RateLimiter.decorateSupplier
        Class<?> rateLimiterClass = Class.forName("io.github.resilience4j.ratelimiter.RateLimiter");
        this.rateLimiterDecorateSupplierMethod = rateLimiterClass.getMethod("decorateSupplier", rateLimiterClass, Supplier.class);
        
        // CircuitBreaker.decorateSupplier
        Class<?> circuitBreakerClass = Class.forName("io.github.resilience4j.circuitbreaker.CircuitBreaker");
        this.circuitBreakerDecorateSupplierMethod = circuitBreakerClass.getMethod("decorateSupplier", circuitBreakerClass, Supplier.class);
    }
    
    /**
     * 熔断降级是否启用
     */
    public boolean isResilienceEnabled() {
        return resilienceEnabled;
    }
}
