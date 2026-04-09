package com.cloudskill.sdk.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Spring AI 标准工具提供者
 * 自动将Cloud Skill Platform同步的所有技能注册为Spring AI可调用的Tool
 * 兼容所有支持Spring AI规范的大模型：通义千问、OpenAI、豆包、DeepSeek等
 * 改造：完全委托给ToolCache，不再维护独立缓存，职责简化为适配Spring AI接口
 */
@Slf4j
public class CloudSkillToolCallbackProvider implements org.springframework.ai.tool.ToolCallbackProvider {

    @Autowired
    private ToolCache toolCache;

    /**
     * Spring AI 原生注入的接口方法，返回工具数组。
     * 委托 {@link ToolCache#getToolCallbackArray()}，内部会先向 Admin 校验全局版本再返回缓存。
     */
    @Override
    public ToolCallback[] getToolCallbacks() {
        return toolCache.getToolCallbackArray();
    }

    /**
     * 供 AOP / 手动注入使用，返回 List；同样会先校验 Admin 全局版本。
     */
    public List<ToolCallback> getToolCallbackList() {
        return toolCache.getToolCallbackList();
    }
}