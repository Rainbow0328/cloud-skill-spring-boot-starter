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
 * distributed under an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cloudskill.sdk.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cloud Skill SDK 核心配置
 * 极简设计：只将用户需要关心的配置暴露在顶层，其他都给合理默认值
 */
@ConfigurationProperties(prefix = "cloud.skill")
public class CloudSkillProperties {

    // ============================================================================
    // 用户需要关心和配置的核心属性 - 顶层直接暴露
    // ============================================================================

    /**
     * 是否启用 Cloud Skill SDK 总开关
     */
    private boolean enabled = true;

    /**
     * Cloud Skill Admin 平台服务地址
     */
    private String serverUrl = "http://localhost:8766";

    /**
     * API 认证密钥
     */
    private String apiKey;

    /**
     * 动态技能注入模式
     * - auto: 全局自动增强 ChatModel / ChatClient 调用
     * - annotation: 仅对标注 @EnableDynamicSkills 的类/方法进行增强
     */
    private String mode = "annotation";

    // ============================================================================
    // 以下系统配置都有合理默认值，用户一般不需要修改
    // ============================================================================

    /**
     * 是否自动同步技能列表（从 Admin 拉取技能）
     */
    private boolean autoSync = true;

    /**
     * 技能同步间隔（秒）
     */
    private int syncInterval = 30;

    /**
     * 是否启用 WebSocket 实时技能更新推送
     */
    private boolean enableWebSocket = true;

    /**
     * WebSocket 重连间隔（秒）
     */
    private int reconnectInterval = 5;

    /**
     * 调用超时时间（毫秒）
     */
    private int callTimeout = 30000;

    /**
     * 调用失败重试次数
     */
    private int retryCount = 3;

    /**
     * 是否启用本地缓存（推荐开启）
     */
    private boolean enableLocalCache = true;

    /**
     * 本地缓存过期时间（秒）
     */
    private long cacheExpireTime = 3600;

    /**
     * 后台检查缓存更新间隔（毫秒），默认 5 分钟
     */
    private long cacheCheckInterval = 300000;

    /**
     * 是否启用服务自动注册（向 Admin 注册本实例）
     */
    private boolean enableServiceRegistry = true;

    /**
     * 服务名称
     */
    private String serviceName;

    /**
     * 服务版本
     */
    private String serviceVersion = "1.0.0";

    /**
     * 服务 IP 地址（自动获取）
     */
    private String serviceIp;

    /**
     * 服务端口
     */
    private Integer servicePort;

    /**
     * 心跳间隔（秒）
     */
    private int heartbeatInterval = 30;

    /**
     * 实例 ID（自动生成）
     */
    private String instanceId;

    /**
     * 参数校验配置
     */
    private Validation validation = new Validation();

    /**
     * 参数校验配置类
     */
    public static class Validation {
        /**
         * 是否启用请求参数校验
         */
        private boolean enableRequestValidation = true;

        /**
         * 是否启用响应结果校验
         */
        private boolean enableResponseValidation = true;

        /**
         * 校验失败时是否阻止调用（false时仅记录警告）
         */
        private boolean failOnError = true;

        public boolean isEnableRequestValidation() { return enableRequestValidation; }
        public void setEnableRequestValidation(boolean enableRequestValidation) { this.enableRequestValidation = enableRequestValidation; }
        public boolean isEnableResponseValidation() { return enableResponseValidation; }
        public void setEnableResponseValidation(boolean enableResponseValidation) { this.enableResponseValidation = enableResponseValidation; }
        public boolean isFailOnError() { return failOnError; }
        public void setFailOnError(boolean failOnError) { this.failOnError = failOnError; }
    }

    /**
     * 是否启用调试模式，输出详细日志
     */
    private boolean debug = false;

    // ============================================================================
    // Getters and Setters - 全部保留给 Spring Boot 注入使用
    // ============================================================================

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getServerUrl() { return serverUrl; }
    public void setServerUrl(String serverUrl) { this.serverUrl = serverUrl; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public boolean isAutoSync() { return autoSync; }
    public void setAutoSync(boolean autoSync) { this.autoSync = autoSync; }
    public int getSyncInterval() { return syncInterval; }
    public void setSyncInterval(int syncInterval) { this.syncInterval = syncInterval; }
    public boolean isEnableWebSocket() { return enableWebSocket; }
    public void setEnableWebSocket(boolean enableWebSocket) { this.enableWebSocket = enableWebSocket; }
    public int getReconnectInterval() { return reconnectInterval; }
    public void setReconnectInterval(int reconnectInterval) { this.reconnectInterval = reconnectInterval; }
    public int getCallTimeout() { return callTimeout; }
    public void setCallTimeout(int callTimeout) { this.callTimeout = callTimeout; }
    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
    public boolean isEnableLocalCache() { return enableLocalCache; }
    public void setEnableLocalCache(boolean enableLocalCache) { this.enableLocalCache = enableLocalCache; }
    public long getCacheExpireTime() { return cacheExpireTime; }
    public void setCacheExpireTime(long cacheExpireTime) { this.cacheExpireTime = cacheExpireTime; }
    public long getCacheCheckInterval() { return cacheCheckInterval; }
    public void setCacheCheckInterval(long cacheCheckInterval) { this.cacheCheckInterval = cacheCheckInterval; }
    public boolean isEnableServiceRegistry() { return enableServiceRegistry; }
    public void setEnableServiceRegistry(boolean enableServiceRegistry) { this.enableServiceRegistry = enableServiceRegistry; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public String getServiceVersion() { return serviceVersion; }
    public void setServiceVersion(String serviceVersion) { this.serviceVersion = serviceVersion; }
    public String getServiceIp() { return serviceIp; }
    public void setServiceIp(String serviceIp) { this.serviceIp = serviceIp; }
    public Integer getServicePort() { return servicePort; }
    public void setServicePort(Integer servicePort) { this.servicePort = servicePort; }
    public int getHeartbeatInterval() { return heartbeatInterval; }
    public void setHeartbeatInterval(int heartbeatInterval) { this.heartbeatInterval = heartbeatInterval; }
    public String getInstanceId() { return instanceId; }
    public void setInstanceId(String instanceId) { this.instanceId = instanceId; }
    public Validation getValidation() { return validation; }
    public void setValidation(Validation validation) { this.validation = validation; }
    public boolean isDebug() { return debug; }
    public void setDebug(boolean debug) { this.debug = debug; }
}
