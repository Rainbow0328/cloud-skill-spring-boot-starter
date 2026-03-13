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

import com.cloudskill.sdk.model.Skill;

import java.util.Map;

/**
 * 技能执行钩子，可以在技能调用前后执行自定义逻辑
 */
public interface SkillExecutionHook {
    
    /**
     * 技能调用前执行
     * @param skill 要调用的技能
     * @param params 调用参数
     * @return 处理后的参数，返回null会终止调用
     */
    default Map<String, Object> beforeExecution(Skill skill, Map<String, Object> params) {
        return params;
    }
    
    /**
     * 技能调用后执行
     * @param skill 调用的技能
     * @param params 调用参数
     * @param result 调用结果
     * @param throwable 异常信息（如果有）
     * @return 处理后的结果
     */
    default Object afterExecution(Skill skill, Map<String, Object> params,
                                 Object result, Throwable throwable) {
        return result;
    }
}
