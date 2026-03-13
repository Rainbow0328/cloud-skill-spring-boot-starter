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
package com.cloudskill.sdk.agent;

import com.cloudskill.sdk.config.CloudSkillProperties;
import com.cloudskill.sdk.core.CloudSkillClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.Advisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 动态技能自动配置
 * 基于Spring AI Alibaba Advisor机制实现
 * 支持三种模式：OFF/GLOBAL/ANNOTATION
 */
@Configuration
@ConditionalOnClass(ChatModel.class) // 只有项目中引入了Spring AI才生效
public class DynamicSkillAutoConfiguration {
    
    /**
     * 技能调用统计（全局模式）
     */
    @Bean
    @ConditionalOnProperty(prefix = "cloudskill.sdk.dynamic-skills", name = "mode", havingValue = "GLOBAL", matchIfMissing = false)
    public SkillCallMetrics skillCallMetricsForGlobal() {
        return new SkillCallMetrics();
    }
    
    /**
     * 技能调用统计（注解模式）
     */
    @Bean
    @ConditionalOnProperty(prefix = "cloudskill.sdk.dynamic-skills", name = "mode", havingValue = "ANNOTATION", matchIfMissing = false)
    public SkillCallMetrics skillCallMetricsForAnnotation() {
        return new SkillCallMetrics();
    }
    
    private static final Logger log = LoggerFactory.getLogger(DynamicSkillAutoConfiguration.class);
    private final CloudSkillClient cloudSkillClient;
    private final CloudSkillProperties properties;
    
    /**
     * 构造方法
     */
    public DynamicSkillAutoConfiguration(CloudSkillClient cloudSkillClient, CloudSkillProperties properties) {
        this.cloudSkillClient = cloudSkillClient;
        this.properties = properties;
    }
    
    /**
     * 动态技能增强逻辑
     */
    @Bean
    @ConditionalOnProperty(prefix = "cloudskill.sdk.dynamic-skills", name = "mode", havingValue = "GLOBAL", matchIfMissing = false)
    public DynamicSkillsAdvice dynamicSkillsAdviceForGlobal(
            ObjectProvider<List<SkillConverter>> convertersProvider,
            ObjectProvider<List<SkillExecutionHook>> hooksProvider,
            ObjectProvider<SkillCallMetrics> metricsProvider) {
        return new DynamicSkillsAdvice(cloudSkillClient, properties, convertersProvider, hooksProvider, metricsProvider);
    }
    
    /**
     * 动态技能增强逻辑（注解模式）
     */
    @Bean
    @ConditionalOnProperty(prefix = "cloudskill.sdk.dynamic-skills", name = "mode", havingValue = "ANNOTATION", matchIfMissing = false)
    public DynamicSkillsAdvice dynamicSkillsAdviceForAnnotation(
            ObjectProvider<List<SkillConverter>> convertersProvider,
            ObjectProvider<List<SkillExecutionHook>> hooksProvider,
            ObjectProvider<SkillCallMetrics> metricsProvider) {
        return new DynamicSkillsAdvice(cloudSkillClient, properties, convertersProvider, hooksProvider, metricsProvider);
    }
    
    /**
     * 全局模式Advisor：拦截所有ChatModel的call方法
     */
    @Bean
    @ConditionalOnProperty(prefix = "cloudskill.sdk.dynamic-skills", name = "mode", havingValue = "GLOBAL", matchIfMissing = false)
    public Advisor globalDynamicSkillsAdvisor(DynamicSkillsAdvice advice) {
        DynamicSkillsAdvisor advisor = new DynamicSkillsAdvisor(advice, false);
        advisor.setOrder(properties.getDynamicSkills().getOrder());
        log.info("动态技能全局模式已启用，所有ChatModel调用将自动注入技能");
        return advisor;
    }
    
    /**
     * 注解模式Advisor：仅拦截标注了@EnableDynamicSkills的类/方法
     */
    @Bean
    @ConditionalOnProperty(prefix = "cloudskill.sdk.dynamic-skills", name = "mode", havingValue = "ANNOTATION", matchIfMissing = false)
    public Advisor annotationDynamicSkillsAdvisor(DynamicSkillsAdvice advice) {
        DynamicSkillsAdvisor advisor = new DynamicSkillsAdvisor(advice, true);
        advisor.setOrder(properties.getDynamicSkills().getOrder());
        log.info("动态技能注解模式已启用，仅对标注了@EnableDynamicSkills的类/方法生效");
        return advisor;
    }
}
