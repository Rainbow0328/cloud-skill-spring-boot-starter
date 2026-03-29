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
package com.cloudskill.sdk.agent.annotation;

import java.lang.annotation.*;

/**
 * 启用动态技能注入
 * 可以标注在类或方法上，细粒度控制是否启用动态技能注入
 * 全局默认由配置文件控制：cloud.skill.dynamic-skills.enabled
 *
 * 优先级：方法注解 > 类注解 > 全局配置
 *
 * @author Cloud Skill Team
 * @version 1.0.0
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface EnableDynamicSkills {

    /**
     * 是否启用动态技能注入
     * @return true启用，false禁用
     */
    boolean value() default true;
}
