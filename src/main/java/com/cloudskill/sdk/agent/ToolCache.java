package com.cloudskill.sdk.agent;

import com.cloudskill.sdk.cache.GlobalCacheVersion;
import com.cloudskill.sdk.core.CloudSkillClient;
import com.cloudskill.sdk.core.SkillCache;
import com.cloudskill.sdk.model.Skill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI工具专用缓存
 * 独立于SkillCache，专门负责存储转换好的Spring AI ToolCallback对象
 * 仅服务于AI调用链路，和业务调用链路完全解耦
 * 版本统一使用GlobalCacheVersion，不单独管理版本
 */
@Slf4j
public class ToolCache {

    private final SkillCache skillCache;
    private final CloudSkillClient cloudSkillClient;

    // 单个ToolCallback对象缓存：skillId -> ToolCallback
    private final Map<Long, ToolCallback> toolCallbackMap = new ConcurrentHashMap<>();

    // 最终可用工具列表缓存
    private volatile List<ToolCallback> cachedToolList = Collections.emptyList();

    // 工具数组缓存，适配Spring AI接口
    private volatile ToolCallback[] cachedToolArray = new ToolCallback[0];

    public ToolCache(SkillCache skillCache, CloudSkillClient cloudSkillClient) {
        this.skillCache = skillCache;
        this.cloudSkillClient = cloudSkillClient;
    }

    /**
     * 刷新Tool缓存，从SkillCache获取最新的启用技能，转换为ToolCallback
     * 版本完全跟随GlobalCacheVersion，不需要单独管理
     */
    public void refreshCache() {
        synchronized (this) {
            log.debug("Refreshing tool cache, globalVersion={}", GlobalCacheVersion.getCurrentVersion());

            List<ToolCallback> newToolList = new ArrayList<>();
            Map<Long, Skill> allSkills = skillCache.getAll();

            for (Skill skill : allSkills.values()) {
                if (Boolean.TRUE.equals(skill.getEnabled()) && Boolean.TRUE.equals(skill.getIsPublic())) {
                    ToolCallback toolCallback = getOrCreateToolCallback(skill);
                    newToolList.add(toolCallback);
                }
            }

            // 缓存不可变列表，防止外部修改
            this.cachedToolList = Collections.unmodifiableList(newToolList);
            this.cachedToolArray = newToolList.toArray(new ToolCallback[0]);

            log.info("Tool cache refreshed: loaded={}, globalVersion={}", newToolList.size(), GlobalCacheVersion.getCurrentVersion());
        }
    }

    /**
     * 获取工具数组，适配Spring AI ToolCallbackProvider接口
     * <p>
     * 返回前会通过 {@link CloudSkillClient#syncIfGlobalVersionChanged()} 请求 Admin 全局版本号，
     * 仅当高于本地水位时触发全量同步，并{@linkplain #refreshCache() 重建}本缓存，避免直接返回过期 Tool 列表。
     */
    public ToolCallback[] getToolCallbackArray() {
        ensureToolCallbacksAlignedWithAdmin();
        return cachedToolArray.clone();
    }

    /**
     * 获取工具列表，供AOP/手动注入使用
     * <p>
     * 与 {@link #getToolCallbackArray()} 相同，会先校验 Admin 全局版本再决定是否刷新。
     */
    public List<ToolCallback> getToolCallbackList() {
        ensureToolCallbacksAlignedWithAdmin();
        return cachedToolList;
    }

    /**
     * 读取工具前对齐 Admin：轻量拉取全局时间戳，若高于 {@link CloudSkillClient#getLastSyncedGlobalTimestamp()}
     * 则全量同步技能缓存，并刷新 Tool 派生缓存。
     */
    private void ensureToolCallbacksAlignedWithAdmin() {
        if (cloudSkillClient.syncIfGlobalVersionChanged()) {
            refreshCache();
        }
    }

    /**
     * 构造或从缓存获取ToolCallback
     */
    private ToolCallback getOrCreateToolCallback(Skill skill) {
        return toolCallbackMap.computeIfAbsent(skill.getId(), skillId -> {
            return FunctionToolCallback.builder(String.valueOf(skillId), (Map<String, Object> params) -> {
                        log.debug("ToolCallback invoked: skillId={}, skillName={}", skillId, skill.getName());
                        return cloudSkillClient.invokeSkill(skillId, params).getData();
                    })
                    .inputType(Map.class)
                    .description(skill.getDescription())
                    .inputSchema(skill.getRequestSchema())
                    .build();
        });
    }

    /**
     * 增量更新：单个技能更新或新增
     * 适用于 INFO_UPDATE、ASSIGN 等单个技能变更场景
     */
    public void updateSkill(Long skillId) {
        synchronized (this) {
            Skill skill = skillCache.get(skillId);
            if (skill == null) {
                // 技能不存在，可能是删除操作，调用 removeSkill
                removeSkill(skillId);
                return;
            }

            if (Boolean.TRUE.equals(skill.getEnabled()) && Boolean.TRUE.equals(skill.getIsPublic())) {
                // 技能启用且公开，更新或添加到缓存
                ToolCallback toolCallback = getOrCreateToolCallback(skill);

                // 直接重建列表：从 toolCallbackMap 中获取所有有效的 ToolCallback
                rebuildCachedList();
                log.debug("Tool cache incrementally updated: skillId={}, total={}", skillId, cachedToolList.size());
            } else {
                // 技能禁用或非公开，移除
                removeSkill(skillId);
            }
        }
    }

    /**
     * 增量更新：移除单个技能
     * 适用于 UNASSIGN、技能禁用等场景
     */
    public void removeSkill(Long skillId) {
        synchronized (this) {
            // 从 ToolCallback 缓存中移除
            toolCallbackMap.remove(skillId);

            // 重建列表
            rebuildCachedList();
            log.debug("Tool cache incrementally removed: skillId={}, total={}", skillId, cachedToolList.size());
        }
    }

    /**
     * 批量移除：按 Provider 移除所有技能
     * 适用于 PROVIDER_OFFLINE、SKILL_DELETE_BY_PROVIDER 等场景
     */
    public void removeSkillsByProvider(String providerId) {
        synchronized (this) {
            // 找出该 Provider 的所有技能 ID
            List<Long> skillIdsToRemove = new ArrayList<>();
            Map<Long, Skill> allSkills = skillCache.getAll();
            for (Map.Entry<Long, Skill> entry : allSkills.entrySet()) {
                if (providerId.equals(entry.getValue().getProviderId())) {
                    skillIdsToRemove.add(entry.getKey());
                }
            }

            // 批量移除
            for (Long skillId : skillIdsToRemove) {
                toolCallbackMap.remove(skillId);
            }

            // 重建列表
            rebuildCachedList();
            log.info("Tool cache batch removed by provider: providerId={}, removed={}, total={}",
                    providerId, skillIdsToRemove.size(), cachedToolList.size());
        }
    }

    /**
     * 从 toolCallbackMap 重建缓存列表
     * 只包含 SkillCache 中仍然存在且启用的技能
     */
    private void rebuildCachedList() {
        List<ToolCallback> newToolList = new ArrayList<>();
        Map<Long, Skill> allSkills = skillCache.getAll();

        // 遍历 toolCallbackMap，只保留仍然有效的技能
        for (Map.Entry<Long, ToolCallback> entry : toolCallbackMap.entrySet()) {
            Long skillId = entry.getKey();
            Skill skill = allSkills.get(skillId);

            if (skill != null && Boolean.TRUE.equals(skill.getEnabled()) && Boolean.TRUE.equals(skill.getIsPublic())) {
                newToolList.add(entry.getValue());
            } else {
                // 技能已不存在或已禁用，从 map 中移除
                toolCallbackMap.remove(skillId);
            }
        }

        this.cachedToolList = Collections.unmodifiableList(newToolList);
        this.cachedToolArray = newToolList.toArray(new ToolCallback[0]);
    }

    /**
     * 清空缓存，同时清空单个工具缓存和列表缓存
     */
    public void clearCache() {
        synchronized (this) {
            toolCallbackMap.clear();
            this.cachedToolList = Collections.emptyList();
            this.cachedToolArray = new ToolCallback[0];
            log.info("Tool cache cleared");
        }
    }
}