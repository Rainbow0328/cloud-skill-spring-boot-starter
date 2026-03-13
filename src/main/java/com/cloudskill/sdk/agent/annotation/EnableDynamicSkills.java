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
package com.cloudskill.sdk.agent.annotation;

import java.lang.annotation.*;

/**
 * 启用动态技能注解
 * 标注在类或方法上，表示该类/方法调用ChatModel时需要注入动态技能
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EnableDynamicSkills {
    
    /**
     * 要注入的技能ID列表，为空则注入所有可用技能
     */
    String[] value() default {};
    
    /**
     * 要排除的技能ID列表
     */
    String[] exclude() default {};
}
