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
package com.cloudskill.sdk.core.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

/**
 * 技能调用指标收集器
 */
@Slf4j
public class SkillMetrics {
    
    private final MeterRegistry meterRegistry;
    private final boolean metricsEnabled;
    
    // 指标名称
    private static final String SKILL_INVOKE_TOTAL = "cloud.skill.skill.invoke.total";
    private static final String SKILL_INVOKE_DURATION = "cloud.skill.skill.invoke.duration";
    private static final String SKILL_SYNC_SUCCESS_TOTAL = "cloud.skill.skill.sync.success.total";
    private static final String SKILL_SYNC_FAILURE_TOTAL = "cloud.skill.skill.sync.failure.total";
    private static final String WEBSOCKET_CONNECTION_COUNT = "cloud.skill.websocket.connection.count";
    private static final String AGENT_ROUTER_SUCCESS_TOTAL = "cloud.skill.agent.router.success.total";
    private static final String AGENT_ROUTER_DURATION = "cloud.skill.agent.router.duration";
    
    public SkillMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.metricsEnabled = meterRegistry != null;
        if (metricsEnabled) {
            log.info("Skill metrics enabled");
        }
    }
    
    /**
     * 记录技能调用
     */
    public void recordSkillInvoke(String skillId, boolean success, long durationMs, String errorCode) {
        if (!metricsEnabled) {
            return;
        }
        
        // 计数
        Counter.builder(SKILL_INVOKE_TOTAL)
                .tag("skillId", skillId)
                .tag("result", success ? "success" : "failure")
                .tag("errorCode", StringUtils.hasText(errorCode) ? errorCode : "none")
                .register(meterRegistry)
                .increment();
        
        // 耗时
        Timer.builder(SKILL_INVOKE_DURATION)
                .tag("skillId", skillId)
                .tag("result", success ? "success" : "failure")
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 记录技能同步成功
     */
    public void recordSkillSyncSuccess() {
        if (!metricsEnabled) {
            return;
        }
        Counter.builder(SKILL_SYNC_SUCCESS_TOTAL)
                .register(meterRegistry)
                .increment();
    }
    
    /**
     * 记录技能同步失败
     */
    public void recordSkillSyncFailure() {
        if (!metricsEnabled) {
            return;
        }
        Counter.builder(SKILL_SYNC_FAILURE_TOTAL)
                .register(meterRegistry)
                .increment();
    }
    
    /**
     * 记录WebSocket连接状态
     */
    public void recordWebSocketConnection(boolean connected) {
        if (!metricsEnabled) {
            return;
        }
        meterRegistry.gauge(WEBSOCKET_CONNECTION_COUNT, connected ? 1 : 0);
    }
    
    /**
     * 记录Agent路由
     */
    public void recordAgentRouter(boolean success, long durationMs) {
        if (!metricsEnabled) {
            return;
        }
        
        Counter.builder(AGENT_ROUTER_SUCCESS_TOTAL)
                .tag("result", success ? "success" : "failure")
                .register(meterRegistry)
                .increment();
        
        Timer.builder(AGENT_ROUTER_DURATION)
                .tag("result", success ? "success" : "failure")
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 指标是否启用
     */
    public boolean isMetricsEnabled() {
        return metricsEnabled;
    }
}
