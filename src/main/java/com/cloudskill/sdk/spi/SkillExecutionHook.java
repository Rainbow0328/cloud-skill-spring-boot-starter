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
package com.cloudskill.sdk.spi;

import com.cloudskill.sdk.model.SkillCallRequest;
import com.cloudskill.sdk.model.SkillCallResult;
import org.springframework.core.Ordered;

/**
 * 技能调用钩子扩展点
 * 可以在技能调用前后进行自定义处理
 */
public interface SkillExecutionHook extends Ordered {
    
    /**
     * 技能调用前执行
     * @param skillId 技能ID
     * @param request 调用请求
     */
    default void beforeCall(String skillId, SkillCallRequest request) {}
    
    /**
     * 技能调用后执行
     * @param skillId 技能ID
     * @param request 调用请求
     * @param result 调用结果
     */
    default void afterCall(String skillId, SkillCallRequest request, SkillCallResult result) {}
    
    /**
     * 技能调用异常时执行
     * @param skillId 技能ID
     * @param request 调用请求
     * @param throwable 异常信息
     */
    default void onError(String skillId, SkillCallRequest request, Throwable throwable) {}
    
    /**
     * 优先级，数值越小优先级越高
     * @return 优先级值
     */
    @Override
    default int getOrder() {
        return 0;
    }
}
