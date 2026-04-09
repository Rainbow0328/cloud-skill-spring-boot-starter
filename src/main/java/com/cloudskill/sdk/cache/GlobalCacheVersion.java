package com.cloudskill.sdk.cache;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 全局缓存版本管理器
 * 【严格权限控制】：只有Redis消息监听模块有权限更新版本，其他模块只有查询权
 * 所有缓存组件都统一使用该全局版本，避免多缓存版本不一致
 */
public final class GlobalCacheVersion {

    /**
     * 全局版本号，与admin全局版本保持一致
     */
    private static final AtomicLong GLOBAL_VERSION = new AtomicLong(0);

    private GlobalCacheVersion() {
        // 私有构造，禁止实例化
    }

    /**
     * 获取当前全局版本号（所有模块都可以调用）
     *
     * @return 当前版本号
     */
    public static long getCurrentVersion() {
        return GLOBAL_VERSION.get();
    }

    /**
     * 更新全局版本号（仅消息监听模块可以调用，由admin驱动更新）
     *
     * @param newVersion 从admin获取的最新全局版本号
     */
    public static void update(long newVersion) {
        // 只有新版本大于当前版本才更新，避免版本回退
        if (newVersion > GLOBAL_VERSION.get()) {
            GLOBAL_VERSION.set(newVersion);
        }
    }

    /**
     * 重置全局版本号（仅测试用，生产环境禁止调用）
     */
    @Deprecated
    public static void reset() {
        GLOBAL_VERSION.set(0);
    }
}
