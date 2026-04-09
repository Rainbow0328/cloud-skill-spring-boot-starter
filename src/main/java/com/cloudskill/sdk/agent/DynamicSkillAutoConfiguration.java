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
package com.cloudskill.sdk.agent;

import com.cloudskill.sdk.agent.aop.DynamicSkillInjectAspect;
import com.cloudskill.sdk.agent.enhancement.AbstractDynamicSkillToolEnhancement;
import com.cloudskill.sdk.agent.enhancement.DefaultDynamicSkillToolEnhancement;
import com.cloudskill.sdk.core.CloudSkillClient;
import com.cloudskill.sdk.core.SkillCache;
import com.cloudskill.sdk.config.CloudSkillProperties;
import org.springframework.ai.chat.client.ChatClientCustomizer;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * 动态技能自动配置
 * 基于Spring AI标准规范，零侵入自动注入动态技能
 * 在 {@link ChatModel}、{@link org.springframework.ai.chat.client.ChatClient} 路径上合并动态技能工具
 */
@Configuration
@ConditionalOnClass(ChatModel.class)
@EnableAspectJAutoProxy(proxyTargetClass = true)
@ConditionalOnProperty(prefix = "cloud.skill", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DynamicSkillAutoConfiguration {

    /**
     * AI工具缓存，独立存储转换好的ToolCallback
     */
    @Bean
    @ConditionalOnMissingBean
    public ToolCache toolCache(SkillCache skillCache, CloudSkillClient cloudSkillClient) {
        return new ToolCache(skillCache, cloudSkillClient);
    }

    /**
     * Spring AI 标准工具提供者
     * 供业务注入 {@link CloudSkillToolCallbackProvider#getToolCallbacks()} 或配合全局 {@link ToolCallbackProvider} 使用
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "cloud.skill", name = "enabled", havingValue = "true", matchIfMissing = true)
    public CloudSkillToolCallbackProvider cloudSkillToolCallbackProvider() {
        return new CloudSkillToolCallbackProvider();
    }

    /**
     * Spring AI 标准全局工具提供者
     * 默认注册，供ChatClient/Agent原生自动注入
     */
    @Bean
    public ToolCallbackProvider dynamicSkillToolCallbackProvider(CloudSkillToolCallbackProvider provider) {
        return provider::getToolCallbacks;
    }

    /**
     * ChatClient 全局自定义器，自动注入动态技能
     * 默认注册，所有ChatClient.Builder都会自动挂载动态技能
     */
    @Configuration
    @ConditionalOnClass(ChatClientCustomizer.class)
    static class ChatClientToolCallbackCustomizerConfiguration {
        @Bean
        public ChatClientCustomizer cloudSkillChatClientToolCustomizer(ToolCallbackProvider dynamicSkillToolCallbackProvider) {
            return builder -> builder.defaultToolCallbacks(dynamicSkillToolCallbackProvider);
        }
    }

    /**
     * 默认可替换：合并逻辑与钩子见 {@link AbstractDynamicSkillToolEnhancement}。
     */
    @Bean
    @ConditionalOnMissingBean(AbstractDynamicSkillToolEnhancement.class)
    @ConditionalOnProperty(prefix = "cloud.skill", name = "enabled", havingValue = "true", matchIfMissing = true)
    public AbstractDynamicSkillToolEnhancement dynamicSkillToolEnhancement(CloudSkillToolCallbackProvider provider,
                                                                           CloudSkillProperties properties) {
        return new DefaultDynamicSkillToolEnhancement(provider, properties);
    }

    /**
     * 动态技能 AOP：委托 {@link AbstractDynamicSkillToolEnhancement}.
     */
    @Bean
    @ConditionalOnProperty(prefix = "cloud.skill", name = "enabled", havingValue = "true", matchIfMissing = true)
    public DynamicSkillInjectAspect dynamicSkillInjectAspect(AbstractDynamicSkillToolEnhancement enhancement) {
        return new DynamicSkillInjectAspect(enhancement);
    }
}
