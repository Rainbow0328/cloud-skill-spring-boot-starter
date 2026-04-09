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
package com.cloudskill.sdk.agent.enhancement;

import com.cloudskill.sdk.agent.CloudSkillToolCallbackProvider;
import com.cloudskill.sdk.agent.annotation.EnableDynamicSkills;
import com.cloudskill.sdk.agent.support.PromptDynamicToolsSupport;
import com.cloudskill.sdk.agent.support.ToolCallbackCollectionSupport;
import com.cloudskill.sdk.agent.support.ToolCallbackMergeSupport;
import com.cloudskill.sdk.config.CloudSkillProperties;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * ChatModel / ChatClient 动态工具合并的共用骨架：合并策略一致，并通过钩子支持业务扩展。
 * <p>
 * 子类可覆盖 {@link #onBeforeChatModelEnhancement} / {@link #onAfterChatModelToolsMerged} 等钩子；
 * 也可覆盖 {@link #shouldInjectForChatModel} 等判断以定制是否参与增强。
 */
public abstract class AbstractDynamicSkillToolEnhancement implements Ordered {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final CloudSkillToolCallbackProvider toolCallbackProvider;
    private final CloudSkillProperties properties;
    private final int order;

    protected AbstractDynamicSkillToolEnhancement(CloudSkillToolCallbackProvider toolCallbackProvider,
                                                      CloudSkillProperties properties) {
        this.toolCallbackProvider = toolCallbackProvider;
        this.properties = properties;
        // Fixed order: HIGH_PRECEDENCE + 100, ensures we run before other advices
        this.order = Ordered.HIGHEST_PRECEDENCE + 100;
        // Validate mode
        String mode = properties.getMode();
        if (!"annotation".equals(mode) && !"auto".equals(mode)) {
            properties.setMode("annotation");
        }
    }

    protected CloudSkillToolCallbackProvider getToolCallbackProvider() {
        return toolCallbackProvider;
    }

    protected CloudSkillProperties getProperties() {
        return properties;
    }

    // ——— ChatModel 钩子：增强前（尚未合并）→ 合并后（已写入 context）→ 调用返回后 ———

    /** 在解析出手动工具、填入动态工具列表之后、执行合并之前调用。 */
    protected void onBeforeChatModelEnhancement(ChatModelEnhancementContext ctx) {
    }

    /** 合并完成后、写入 Prompt / 调用 {@link org.springframework.ai.chat.model.ChatModel#call} 之前调用，可 {@link ChatModelEnhancementContext#replaceMergedToolCallbacks}. */
    protected void onAfterChatModelToolsMerged(ChatModelEnhancementContext ctx) {
    }

    /** {@link ProceedingJoinPoint#proceed} 成功返回后调用（含模型响应). */
    protected void onAfterChatModelProceed(ChatModelEnhancementContext ctx, Object result) {
    }

    // ——— ChatClient 钩子 ———

    protected void onBeforeChatClientEnhancement(ChatClientEnhancementContext ctx) {
    }

    protected void onAfterChatClientToolsMerged(ChatClientEnhancementContext ctx) {
    }

    /** {@link org.springframework.ai.chat.client.ChatClient.Builder#build()} 返回后调用. */
    protected void onAfterChatClientBuild(ChatClientEnhancementContext ctx, Object builtChatClient) {
    }

    // Agent 自动注入已下线，仅保留 ChatModel / ChatClient 增强能力。

    /**
     * 由切面调用：对 ChatModel 调用做工具合并并 proceed.
     */
    public Object enhanceChatModelCall(ProceedingJoinPoint joinPoint) throws Throwable {
        log.debug("DynamicSkill: Starting ChatModel enhancement on {}", joinPoint.getTarget().getClass().getName());
        if (!shouldInjectForChatModel(joinPoint)) {
            log.debug("DynamicSkill: shouldInjectForChatModel returned false, skip enhancement");
            return joinPoint.proceed();
        }

        Object arg0 = joinPoint.getArgs()[0];
        final boolean stringOverload = arg0 instanceof String;
        final Prompt originalPrompt;
        if (stringOverload) {
            originalPrompt = new Prompt(new UserMessage((String) arg0));
            log.debug("DynamicSkill: ChatModel call(String) — wrapping as Prompt for tool merge");
        } else if (arg0 instanceof Prompt p) {
            originalPrompt = p;
        } else {
            log.debug("DynamicSkill: ChatModel first argument type {} not supported for merge, proceeding as-is",
                    arg0 == null ? "null" : arg0.getClass().getName());
            return joinPoint.proceed();
        }

        ChatModelEnhancementContext ctx = new ChatModelEnhancementContext(joinPoint, originalPrompt);
        ctx.setManualToolCallbacks(extractManualToolCallbacksForPrompt(originalPrompt));
        log.debug("DynamicSkill: ChatModel extracted {} manual tools", ctx.getManualToolCallbacks().size());
        
        List<ToolCallback> dynamicTools = new ArrayList<>(toolCallbackProvider.getToolCallbackList());
        ctx.setDynamicToolCallbacks(dynamicTools);
        log.debug("DynamicSkill: ChatModel got {} dynamic tools from provider", dynamicTools.size());

        onBeforeChatModelEnhancement(ctx);

        List<ToolCallback> merged = ToolCallbackMergeSupport.mergePreferManual(
                ctx.getManualToolCallbacks(), ctx.getDynamicToolCallbacks());
        ctx.replaceMergedToolCallbacks(merged);

        onAfterChatModelToolsMerged(ctx);
        merged = new ArrayList<>(ctx.getMergedToolCallbacks());
        log.debug("DynamicSkill: ChatModel merged to {} tools total", merged.size());

        if (merged.isEmpty()) {
            log.debug("DynamicSkill: No merged tools, proceed without changes");
            Object out = joinPoint.proceed();
            onAfterChatModelProceed(ctx, out);
            return out;
        }

        try {
            Prompt enhanced = PromptDynamicToolsSupport.copyPromptWithMergedToolCallbacks(ctx.getOriginalPrompt(), merged);
            log.debug("DynamicSkill: Created enhanced prompt with merged tools, proceeding to ChatModel.call(Prompt)");
            Object out = invokeChatModelWithEnhancedPrompt(joinPoint, enhanced, stringOverload);
            onAfterChatModelProceed(ctx, out);
            log.debug("DynamicSkill: ChatModel enhancement completed");
            return out;
        } catch (Exception primary) {
            log.debug("DynamicSkill: Primary enhancement path failed, trying legacy paths: {}", primary.getMessage());
            // legacy paths
        }

        try {
            try {
                Constructor<Prompt> constructor = Prompt.class.getConstructor(
                        List.class, ChatOptions.class, List.class);
                Prompt enhancedPrompt = constructor.newInstance(
                        ctx.getOriginalPrompt().getInstructions(),
                        ctx.getOriginalPrompt().getOptions(),
                        merged);
                Object out = invokeChatModelWithEnhancedPrompt(joinPoint, enhancedPrompt, stringOverload);
                onAfterChatModelProceed(ctx, out);
                log.debug("DynamicSkill: ChatModel enhancement completed via legacy constructor");
                return out;
            } catch (NoSuchMethodException e) {
                Field toolsField = findField(Prompt.class, "tools", "toolCallbacks", "toolDefinitions");
                if (toolsField != null) {
                    toolsField.setAccessible(true);
                    toolsField.set(ctx.getOriginalPrompt(), merged);
                    Object out = invokeChatModelWithEnhancedPrompt(joinPoint, ctx.getOriginalPrompt(), stringOverload);
                    onAfterChatModelProceed(ctx, out);
                    log.debug("DynamicSkill: ChatModel enhancement completed via legacy field reflection");
                    return out;
                }

                Method callMethod = findMethod(joinPoint.getTarget().getClass(), "call", Prompt.class, List.class);
                if (callMethod != null) {
                    Object out = callMethod.invoke(joinPoint.getTarget(), ctx.getOriginalPrompt(), merged);
                    out = adaptChatModelReturnIfStringOverload(out, stringOverload);
                    onAfterChatModelProceed(ctx, out);
                    log.debug("DynamicSkill: ChatModel enhancement completed via legacy method reflection");
                    return out;
                }
            }
            log.debug("ChatModel: no compatible API to inject merged tools");
            Object out = joinPoint.proceed();
            onAfterChatModelProceed(ctx, out);
            return out;
        } catch (Exception ex) {
            log.debug("ChatModel: inject merged tools failed: {}", ex.getMessage());
            Object out = joinPoint.proceed();
            onAfterChatModelProceed(ctx, out);
            return out;
        }
    }

    /**
     * 对原始切点为 {@code call(String)} 时不能用 {@link ProceedingJoinPoint#proceed(Object[])} 改参类型，
     * 必须直接调用 {@link ChatModel#call(Prompt)}，再把 {@link ChatResponse} 转成 {@link String}。
     */
    private Object invokeChatModelWithEnhancedPrompt(ProceedingJoinPoint joinPoint, Prompt enhanced,
                                                     boolean stringOverload) throws Throwable {
        if (!stringOverload) {
            return joinPoint.proceed(new Object[]{enhanced});
        }
        ChatModel model = (ChatModel) joinPoint.getTarget();
        Object raw = model.call(enhanced);
        return adaptChatModelReturnIfStringOverload(raw, true);
    }

    private static Object adaptChatModelReturnIfStringOverload(Object raw, boolean stringOverload) {
        if (!stringOverload) {
            return raw;
        }
        if (raw instanceof ChatResponse cr) {
            if (cr.getResult() != null && cr.getResult().getOutput() != null) {
                return cr.getResult().getOutput().getText();
            }
            return "";
        }
        return raw;
    }

    /**
     * 由切面调用：在 ChatClient.Builder.build() 前合并工具并 proceed.
     */
    public Object enhanceChatClientBuild(ProceedingJoinPoint joinPoint) throws Throwable {
        log.debug("DynamicSkill: Starting ChatClient.Builder enhancement on {}", joinPoint.getTarget().getClass().getName());
        if (!shouldInjectForChatClientBuild()) {
            log.debug("DynamicSkill: shouldInjectForChatClientBuild returned false, skip enhancement");
            return joinPoint.proceed();
        }

        Object builder = joinPoint.getTarget();
        ChatClientEnhancementContext ctx = new ChatClientEnhancementContext(joinPoint, builder);
        ctx.setManualToolCallbacks(ToolCallbackCollectionSupport.extractFromBuilderLike(builder));
        log.debug("DynamicSkill: ChatClient extracted {} manual tools", ctx.getManualToolCallbacks().size());
        
        List<ToolCallback> dynamicTools = new ArrayList<>(toolCallbackProvider.getToolCallbackList());
        ctx.setDynamicToolCallbacks(dynamicTools);
        log.debug("DynamicSkill: ChatClient got {} dynamic tools from provider", dynamicTools.size());

        onBeforeChatClientEnhancement(ctx);

        List<ToolCallback> merged = ToolCallbackMergeSupport.mergePreferManual(
                ctx.getManualToolCallbacks(), ctx.getDynamicToolCallbacks());
        ctx.replaceMergedToolCallbacks(merged);

        onAfterChatClientToolsMerged(ctx);
        merged = new ArrayList<>(ctx.getMergedToolCallbacks());
        log.debug("DynamicSkill: ChatClient merged to {} tools total", merged.size());

        if (merged.isEmpty()) {
            log.debug("DynamicSkill: No merged tools, proceed without changes");
            Object built = joinPoint.proceed();
            onAfterChatClientBuild(ctx, built);
            return built;
        }

        try {
            // 尝试反射找到 defaultToolCallbacks 字段，直接替换为合并后的列表
            // 因为 defaultToolCallbacks() 是累加（addAll），不是替换，多次调用会导致重复
            Field toolsField = null;
            Class<?> clazz = builder.getClass();
            while (clazz != null && clazz != Object.class && toolsField == null) {
                try {
                    toolsField = clazz.getDeclaredField("defaultToolCallbacks");
                } catch (NoSuchFieldException e) {
                    try {
                        toolsField = clazz.getDeclaredField("defaultTools");
                    } catch (NoSuchFieldException e2) {
                        clazz = clazz.getSuperclass();
                    }
                }
            }
            if (toolsField != null) {
                // 直接替换字段，避免累加重复
                toolsField.setAccessible(true);
                toolsField.set(builder, merged);
                log.debug("DynamicSkill: ChatClient - replaced tools field with {} merged tools", merged.size());
            } else {
                //  fallback：找不到字段就调用方法，虽然会重复但至少能工作
                Method m = findMethod(builder.getClass(), "defaultToolCallbacks", List.class);
                if (m != null) {
                    m.invoke(builder, merged);
                    log.debug("DynamicSkill: ChatClient - called defaultToolCallbacks() with {} merged tools", merged.size());
                } else {
                    Method fallback = findMethod(builder.getClass(), "defaultTools", List.class);
                    if (fallback != null) {
                        fallback.invoke(builder, merged);
                        log.debug("DynamicSkill: ChatClient - called defaultTools() with {} merged tools", merged.size());
                    } else {
                        Method varargs = findMethod(builder.getClass(), "defaultToolCallbacks", ToolCallback[].class);
                        if (varargs != null) {
                            varargs.invoke(builder, (Object) merged.toArray(new ToolCallback[0]));
                            log.debug("DynamicSkill: ChatClient - called defaultToolCallbacks(varargs) with {} merged tools", merged.size());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("ChatClient.Builder: merge dynamic tools failed: {}", e.getMessage(), e);
        }

        Object built = joinPoint.proceed();
        onAfterChatClientBuild(ctx, built);
        log.debug("DynamicSkill: ChatClient.Builder enhancement completed");
        return built;
    }

    /**
     * 是否对当前 ChatModel 调用做合并。
     *
     * 完整逻辑：
     * 1. SDK 总开关关闭 → 不注入
     * 2. {@code mode=auto} → 永远注入（全局自动注入）
     * 3. {@code mode=annotation} →:
     *    - 检查 ThreadLocal 标记：通过第一层切面（拦截 @EnableDynamicSkills）设置，精准匹配
     *    - 否则检查 ChatModel 本身是否有注解
     */
    protected boolean shouldInjectForChatModel(ProceedingJoinPoint joinPoint) {
        if (!properties.isEnabled()) {
            log.debug("DynamicSkill: Cloud Skill SDK overall disabled, skip injection");
            return false;
        }
        String mode = properties.getMode();
        if ("auto".equals(mode)) {
            log.debug("DynamicSkill: Global auto mode enabled, inject enabled");
            return true;
        }

        // annotation mode:

        // 1. First check ThreadLocal: set by the @EnableDynamicSkills interception
        // This is the most accurate way: if any caller method/class has annotation,
        // ThreadLocal will be marked TRUE before entering ChatModel.call()
        Boolean threadLocalEnabled = com.cloudskill.sdk.agent.aop.DynamicSkillInjectAspect.ENABLED_IN_CURRENT_CALL.get();
        if (threadLocalEnabled != null && threadLocalEnabled) {
            log.debug("DynamicSkill: ChatModel - ThreadLocal marked enabled, inject enabled");
            return true;
        }

        // 2. Second check: ChatModel method itself has annotation
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Class<?> targetClass = joinPoint.getTarget().getClass();

        EnableDynamicSkills methodAnnotation = AnnotationUtils.findAnnotation(method, EnableDynamicSkills.class);
        if (methodAnnotation != null) {
            log.debug("DynamicSkill: ChatModel - found @EnableDynamicSkills on method, inject enabled");
            return methodAnnotation.value();
        }

        // 3. Third check: ChatModel class has annotation
        EnableDynamicSkills classAnnotation = AnnotationUtils.findAnnotation(targetClass, EnableDynamicSkills.class);
        if (classAnnotation != null) {
            log.debug("DynamicSkill: ChatModel - found @EnableDynamicSkills on class, inject enabled");
            return classAnnotation.value();
        }

        // 如果已经在 ChatClient.Builder 阶段通过AOP注入过了，ChatModel不用再注入
        Boolean alreadyInjected = com.cloudskill.sdk.agent.aop.DynamicSkillInjectAspect.ALREADY_INJECTED_IN_BUILD.get();
        if (alreadyInjected != null && alreadyInjected) {
            log.debug("DynamicSkill: ChatModel - already injected in ChatClient.Builder.build(), skip enhancement");
            return false;
        }

        log.debug("DynamicSkill: ChatModel - no activation found, inject disabled");
        return false;
    }

    /** ChatClient.Builder：已通过Spring AI原生ToolCallbackProvider自动注入，无需AOP再注入 */
    public boolean shouldInjectForChatClientBuild() {
        return false;
    }

    private List<ToolCallback> extractManualToolCallbacksForPrompt(Prompt prompt) {
        ChatOptions options = prompt.getOptions();
        if (options instanceof ToolCallingChatOptions) {
            List<ToolCallback> existing = ((ToolCallingChatOptions) options).getToolCallbacks();
            if (existing != null && !existing.isEmpty()) {
                return new ArrayList<>(existing);
            }
        }
        return ToolCallbackCollectionSupport.extractFromPrompt(prompt);
    }

    private Field findField(Class<?> clazz, String... fieldNames) {
        for (String fieldName : fieldNames) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                // continue
            }
        }
        if (clazz.getSuperclass() != null) {
            return findField(clazz.getSuperclass(), fieldNames);
        }
        return null;
    }

    private Method findMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) {
        try {
            return clazz.getMethod(methodName, paramTypes);
        } catch (NoSuchMethodException e) {
            if (clazz.getSuperclass() != null) {
                return findMethod(clazz.getSuperclass(), methodName, paramTypes);
            }
            return null;
        }
    }

    @Override
    public int getOrder() {
        return order;
    }
}
