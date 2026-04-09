# cloud-skill-spring-boot-starter

<p align="center">
  <img src="https://img.shields.io/badge/JDK-17+-green.svg" alt="JDK Version">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.2.0+-blue.svg" alt="Spring Boot Version">
  <img src="https://img.shields.io/badge/Spring%20AI-1.0.0+-purple.svg" alt="Spring AI Version">
  <img src="https://img.shields.io/badge/License-Apache%202.0-red.svg" alt="License">
  <img src="https://img.shields.io/maven-central/v/com.cloudskill/cloud-skill-spring-boot-starter.svg" alt="Maven Central">
</p>

<p align="center">
  <strong>企业级云技能平台 Spring Boot Starter</strong><br>
  为Spring AI应用提供生产级动态技能注入与全生命周期管理能力
</p>

<p align="center">
  <a href="#概述">概述</a> •
  <a href="#核心特性">核心特性</a> •
  <a href="#技术架构">技术架构</a> •
  <a href="#快速开始">快速开始</a> •
  <a href="#开发指南">开发指南</a> •
  <a href="#配置参考">配置参考</a> •
  <a href="#架构设计">架构设计</a> •
  <a href="#故障排查">故障排查</a>
</p>

---


## 概述

cloud-skill-spring-boot-starter 是面向 Cloud Skill 管理端的 Spring Boot 客户端套件，通过 HTTP API 与云端技能平台通信，实现 AI 应用与工具能力的解耦。通过统一的技能元数据与自动化的集成机制，让 Spring AI 应用能够**动态调用远程注册的工具技能**，降低工具函数硬编码、重复开发与运维成本。

### 设计理念
- **零侵入集成**：基于Spring Boot自动配置，无需修改现有业务代码
- **技能共享平台**：一次开发技能，全平台所有AI应用自动获得调用能力，无需重复开发
- **动态增强**：远程技能动态注册后，自动同步到所有依赖应用，实时生效无需重启
- **云原生友好**：支持K8s环境、服务发现、弹性伸缩
- **生产级可用**：提供完善的容错、监控、安全机制
- **开放生态**：兼容Spring AI和Spring AI Alibaba生态，支持自定义扩展
- **模板方法架构**：抽象父类定义骨架，具体子类实现细节，易于扩展

### 解决的行业痛点
1. **开发效率低下**：多AI应用重复开发相同工具函数，维护成本指数级增长
2. **发布周期漫长**：工具更新需要重新部署所有依赖应用，无法快速迭代
3. **管控能力缺失**：缺乏统一的权限控制、调用审计、流量管控机制
4. **数据孤岛问题**：技能调用数据分散，无法进行全局统计和优化
5. **兼容性问题**：不同AI框架的工具调用规范不统一，迁移成本高

### 业务价值
| 维度 | 传统开发模式 | Cloud Skill模式 |
|------|--------------|-----------------|
| 工具开发 | 每个AI应用重复开发相同工具 | **一次开发，全平台所有AI应用自动获得调用能力** |
| 更新发布 | 每个应用重新打包部署，按周/月发布 | 服务端注册后，所有消费者实时获得，**秒级生效无需重启** |
| 运维成本 | 多应用分散维护，重复劳动成本高 | 集中管理，一次更新全平台同步 |
| 版本一致性 | 不同应用版本参差不齐，难以统一 | 所有消费者自动同步最新版本 |
| 安全管控 | 无统一管控，风险高 | 统一权限、审计、限流 |
| 可观测性 | 无统一监控，问题定位难 | 全链路监控，可观测性强 |

---

## 核心特性

### 🎯 动态技能管理
- **技能共享平台**：一次开发部署，全平台所有AI应用自动获得调用能力，彻底消除重复开发
- **动态实时增强**：技能在服务端注册后，自动推送到所有消费者，AI应用无需重启即可获得新能力
- **多模式同步**：支持全量同步、增量同步、Redis发布订阅实时推送三种同步机制
- **本地缓存优先**：技能元数据全量缓存到本地内存，毫秒级访问性能
- **版本连续性校验**：增量更新基于时间戳，不连续自动触发全量同步
- **注入前版本校验**：每次调用前校验版本，过期自动同步保证一致性
- **降级策略**：服务端不可用时自动降级使用本地缓存，保障业务连续性
- **自动过期**：支持基于TTL的缓存自动失效，保证数据一致性

### 🔌 Spring AI & Spring AI Alibaba 生态深度融合
- **自动适配**：自动将远程技能转换为Spring AI标准`ToolCallback`接口
- **DashScope特殊适配**：完美适配Spring AI Alibaba DashScope，正确转换工具格式
- **无缝集成**：支持Spring AI ChatModel、ChatClient、ReactAgent全场景
- **ReactAgent自动注入**：通过BeanPostProcessor自动检测ReactAgent Bean，零侵入注入
- **模板方法架构**：统一抽象`AbstractToolInjector`，不同场景不同子类，易于扩展
- **协议兼容**：兼容OpenAI Function Calling、Anthropic Tool Use等主流协议

### 🛡️ 企业级生产特性
- **权限体系**：支持公开/私有技能、服务级、租户级多级权限校验
- **高可用保障**：超时控制、重试机制、熔断降级、流量控制全链路保护
- **可观测性**：内置Metrics指标、调用链路追踪、审计日志输出
- **安全加固**：参数校验、敏感数据脱敏、异常行为检测
- **合规支持**：满足等保2.0、数据安全法等合规要求
- **GET参数正确处理**：GET请求参数正确拼接到URL，不会放到body中

### 🎛️ 灵活的启用控制
- **全局配置**：通过`cloud.skill.dynamic-skills.enabled`全局开关
- **类注解**：`@EnableDynamicSkills`标注在类上，控制整个类
- **方法注解**：`@EnableDynamicSkills`标注在方法上，细粒度控制
- **优先级**：方法注解 > 类注解 > 全局配置，灵活满足各种场景
- **支持关闭**：可以在注解中设置`@EnableDynamicSkills(false)`禁用特定方法

### 🔧 高度可扩展架构
- **模板方法模式**：技能变更监听和工具注入都采用抽象模板方法架构
- **SPI扩展点**：提供`SkillConverter`、`SkillExecutionHook`等标准扩展接口
- **消息队列扩展**：支持Redis/RabbitMQ/Kafka多种消息队列，新增只需要子类
- **AI客户端扩展**：支持ChatModel/ChatClient/ReactAgent，新增只需要子类
- **自定义实现**：支持自定义技能转换、调用拦截、结果处理逻辑

---

## 技术架构

### 整体架构
```
┌───────────────────────────────────────────────────────────────────────────────┐
│                          Spring Boot Application                              │
│                                                                               │
│  ┌───────────────┐    ┌──────────────────┐    ┌──────────────────────────┐   │
│  │ Business Logic│    │ Spring AI ChatModel│    │ @EnableDynamicSkills   │   │
│  └───────────────┘    └──────────────────┘    └──────────────────────────┘   │
│         │                     │                          │                    │
│         └─────────────────────┼──────────────────────────┘                    │
│                               │                                               │
│  ┌───────────────────────────────────────────────────────────────────────┐    │
│  │               DynamicSkillsAnnotationMatcher + AOP Advisor             │    │
│  └───────────────────────────────────────────────────────────────────────┘    │
│                               │                                               │
│  ┌───────────────────────────────────────────────────────────────────────┐    │
│  │     AbstractToolInjector (模板方法：版本校验 + 工具注入)               │    │
│  │         └─ ChatModelToolInjector (ChatModel具体实现)                   │    │
│  └───────────────────────────────────────────────────────────────────────┘    │
│                               │                                               │
│  ┌───────────────────────────────────────────────────────────────────────┐    │
│  │                         CloudSkillToolManager                                │    │
│  │         - 技能转换：Skill → ToolCallback                               │    │
│  │         - 版本刷新：refreshSkillTools()                                │    │
│  └───────────────────────────────────────────────────────────────────────┘    │
│                               │                                               │
│  ┌───────────────────────────────────────────────────────────────────────┐    │
│  │                         CloudSkillClient                               │    │
│  └───────────────────────────────────────────────────────────────────────┘    │
│            │                          │                          │            │
│  ┌──────────────────┐    ┌────────────────────────────┐    ┌─────────────┐   │
│  │   SkillCache     │    │  AbstractSkillChangeListener  │    │ Redis 订阅 │   │
│  │  (本地缓存)      │    │   (模板方法：消息处理)       │    │            │   │
│  └──────────────────┘    └────────────────────────────┘    └─────────────┘   │
└───────────────────────┬───────────────────────────┬───────────────────────────┘
                        │                           │
                        └───────────────────────────┘
                                    │
                                    ▼
┌───────────────────────────────────────────────────────────────────────────────┐
│                        Cloud Skill Admin（管理端）                             │
│                                                                               │
│  ┌──────────────────┐  ┌──────────────────┐  ┌────────────────────────────┐  │
│  │ Skill Management │  │ Permission Control│  │ Service Registry & Discovery│ │
│  └──────────────────┘  └──────────────────┘  └────────────────────────────┘  │
│  ┌──────────────────┐  ┌──────────────────┐  ┌────────────────────────────┐  │
│  │ Load Balancing   │  │ Monitoring & Audit│  │ Access Control & Rate Limit│ │
│  └──────────────────┘  └──────────────────┘  └────────────────────────────┘  │
└───────────────────────────────────────────────────────────────────────────────┘
```

### 核心组件分层

#### 注入架构（工具注入到AI模型）

**核心设计**：充分利用 Spring AI 官方原生接口，通过 AOP + ThreadLocal 优化避免重复注入：

| 层级 | 组件 | 职责 |
|------|------|------|
| 注解 | `@EnableDynamicSkills` | 细粒度控制启用/禁用，支持类和方法 |
| AOP切面 | `DynamicSkillInjectAspect` | 三重切面协作：<ul><li>`aroundAnnotatedMethod`: 配合注解，ThreadLocal标记是否需要注入</li><li>`aroundChatClientBuild`: 拦截`ChatClient.Builder.build()`，标记**已注入**</li><li>`aroundChatModelCall`: 拦截`ChatModel.call()`，未注入才执行增强</li></ul> |
| 抽象增强 | `AbstractDynamicSkillToolEnhancement` | 骨架实现：提取已有工具 → 合并去重 → 反射注入到Prompt |
| 管理层 | `CloudSkillToolManager` | 技能转换 Skill → ToolCallback，刷新工具列表 |
| 去重合并 | `ToolCallbackMergeSupport` | 智能合并，同名去重优先保留用户手动定义 |

#### ChatClient 完整注入流程（默认启用）

```
启动阶段：
├─ Spring 容器注册 ChatClientCustomizer
└─ 自动持有动态技能 ToolCallbackProvider

业务调用 - 构建 ChatClient：
1. 业务调用 ChatClient.Builder.build()
2. DynamicSkillInjectAspect.aroundChatClientBuild 拦截
   → 无论如何设置 ALREADY_INJECTED_IN_BUILD = true (ThreadLocal)
   → Spring AI 内部调用 ChatClientCustomizer
   → 自动把动态技能添加到 builder.defaultToolCallbacks()
3. build 完成返回 ChatClient
   → ThreadLocal 保持标记，留给下一阶段读取

业务调用 - ChatClient 对话：
1. chatClient.call() 内部组装 Prompt，携带 defaultToolCallbacks
2. 内部调用 ChatModel.call(prompt)
3. DynamicSkillInjectAspect.aroundChatModelCall 拦截
   → 读取 ALREADY_INJECTED_IN_BUILD = true
   → 直接跳过增强，原生调用
   → finally 强制 remove ThreadLocal，干净退出
```

**设计优势**：
- ✅ 遵循 Spring AI 原生规范，利用官方接口注入，性能最优
- ✅ ThreadLocal 标记保证"一次构建，一次跳过"，完全避免重复注入
- ✅ 调用完成自动清理，无内存泄漏风险
- ✅ 分布式场景完全兼容，ThreadLocal 仅在当前调用线程有效

#### 监听架构（技能变更通知）
| 层级 | 组件 | 职责 |
|------|------|------|
| 抽象基类 | `AbstractSkillChangeListener` | 模板方法，统一处理：版本校验 + 消息分发 + 全量同步 |
| 具体实现 | `RedisSkillChangeListener` | Redis发布订阅具体实现，接收消息调用父类处理 |
| 存储层 | `SkillCache` | 本地技能缓存，存储技能元数据 |

#### 核心服务
| 层级 | 组件 | 职责 |
|------|------|------|
| 通信层 | `CloudSkillClient` | 与管理端 HTTP 通信，获取全局时间戳，全量同步 |
| 定时任务 | `SkillSyncTask` | 定时同步任务，保证最终一致性 |
| 定时任务 | `SkillCacheRefresher` | 缓存刷新，检查过期 |
| SPI扩展 | `SkillConverter` | 自定义技能转换器 |
| SPI扩展 | `SkillExecutionHook` | 技能调用钩子，前置/后置处理 |

### 核心工作流程
1. **启动初始化**：应用启动时连接管理端，同步全量技能元数据
2. **本地缓存**：技能元数据缓存到本地内存，支持毫秒级访问
3. **实时更新**：通过Redis发布订阅接收技能变更事件，实时更新本地缓存
4. **ChatClient 构建**：`ChatClient.Builder.build()` 被 AOP 拦截，ThreadLocal 标记已注入
   - Spring AI 通过 `ChatClientCustomizer` 自动注入动态技能到 `defaultToolCallbacks`
5. **对话调用**：`chatClient.call()` → 内部调用 `ChatModel.call()`
   - AOP 拦截读取标记 → 发现已注入 → 直接跳过增强，原生调用 → 清理 ThreadLocal
6. **版本校验**：如果未经过 ChatClient 构建（直接调用 ChatModel），每次调用前对比本地 vs Redis 全局时间戳
7. **自动同步**：如果本地版本过期，自动触发全量同步，更新缓存后再注入
8. **调用拦截**：AOP 拦截 ChatModel 调用，自动合并注入可用技能列表到 Prompt
9. **路由执行**：AI Agent 调用工具时，经 SDK 按技能配置的 HTTP 协议访问 Provider 执行
10. **结果返回**：执行结果经过转换后返回给 AI Agent，完成调用流程
11. **监控统计**：全链路记录调用日志、性能指标，上报监控平台

---

## 快速开始

### 前置依赖
| 依赖 | 版本要求 | 说明 |
|------|----------|------|
| JDK | 17+ | 完全支持JDK 17及以上版本 |
| Spring Boot | 3.2.0+ | 基于Spring Boot 3.x新特性开发 |
| Spring AI | 1.0.0+ | 兼容Spring AI官方版本 |
| Spring AI Alibaba | 1.1.0+ | 兼容Spring AI Alibaba官方版本 |
| Redis | 任意 | 用于技能变更发布订阅 |
| Maven | 3.8.0+ | 推荐使用最新稳定版 |
| Cloud Skill Admin | 1.0.0+ | 云端技能管理服务端 |

### 步骤1：引入依赖
```xml
<dependencies>
    <!-- Cloud Skill Starter -->
    <dependency>
        <groupId>com.cloudskill</groupId>
        <artifactId>cloud-skill-spring-boot-starter</artifactId>
        <version>1.0.0</version>
    </dependency>

    <!-- Spring AI Alibaba DashScope (可选) -->
    <dependency>
        <groupId>com.alibaba.cloud.ai</groupId>
        <artifactId>spring-ai-alibaba-starter-dashscope</artifactId>
        <version>1.1.2.0</version>
    </dependency>
</dependencies>
```

### 步骤2：基础配置
```yaml
# application.yml
cloud:
  skill:
    # 基础配置
    enabled: true
    server-url: https://cloud-skill-admin.yourcompany.com  # 管理端 Base URL
    api-key: ${CLOUD_SKILL_API_KEY}          # 从环境变量读取API Key，避免硬编码
    service-name: ${spring.application.name} # 服务名称，默认自动取Spring应用名
    service-version: 1.0.0                   # 服务版本
    
    # 技能同步配置
    auto-sync: true
    sync-interval: 30
    enable-listener: true
    
    # 运行时配置
    call-timeout: 30000
    retry-count: 3
    enable-local-cache: true
    cache-expire-time: 3600
    cache-check-interval: 300000
    
    # Spring AI集成（动态技能注入）
    enable-agent-integration: true
    dynamic-skills:
      enabled: true
      order: 2147483547
    
    # Spring AI Alibaba 特定配置
    alibaba:
      enable-agent-support: true
      auto-inject-skills: true

# Redis配置（用于技能变更订阅）
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: your-redis-password

# Spring AI DashScope配置
  ai:
    dashscope:
      api-key: ${DASHSCOPE_API_KEY}
      chat:
        options:
          model: qwen-plus
          temperature: 0.7
```

### 步骤3：使用（全局模式 - 所有ChatModel都启用）
```java
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AiAssistantController {

    private final ChatClient chatClient;

    public AiAssistantController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @GetMapping("/assistant/chat")
    public String chat(@RequestParam String message) {
        // 动态技能会自动注入，无需手动注册
        return chatClient.prompt()
                .user(message)
                .call()
                .content();
    }
}
```

### 步骤3（可选）：使用（注解模式 - 细粒度控制）
```java
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.cloudskill.sdk.agent.annotation.EnableDynamicSkills;

@RestController
@EnableDynamicSkills  // 整个类启用
public class AiAssistantController {

    private final ChatClient chatClient;

    public AiAssistantController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @GetMapping("/assistant/chat")
    public String chat(@RequestParam String message) {
        // 继承类注解，启用动态技能
        return chatClient.prompt()
                .user(message)
                .call()
                .content();
    }

    @GetMapping("/status")
    @EnableDynamicSkills(false)  // 这个方法禁用
    public String status() {
        return "ok";
    }
}
```

启动应用，访问接口测试技能调用是否正常。

---

## 开发指南

### 基础使用场景

#### 场景1：直接调用技能API
```java
import com.cloudskill.sdk.core.CloudSkillClient;
import com.cloudskill.sdk.model.SkillCallResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class SkillInvokeService {

    @Autowired
    private CloudSkillClient cloudSkillClient;

    public String queryWeather(String city, String date) {
        Map<String, Object> params = Map.of(
            "city", city,
            "date", date,
            "include_forecast", true
        );
        
        SkillCallResult result = cloudSkillClient.invokeSkill("weather-query", params);
        
        if (result.isSuccess()) {
            return result.getData().toString();
        } else {
            throw new RuntimeException("天气查询失败: " + result.getMessage());
        }
    }
}
```

#### 场景2：注解模式精细化控制
```java
import com.cloudskill.sdk.agent.annotation.EnableDynamicSkills;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class CustomerServiceAgent {

    private final ChatClient chatClient;

    public CustomerServiceAgent(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @EnableDynamicSkills
    public String afterSalesConsult(String userQuestion) {
        return chatClient.prompt()
                .system("你是专业的售后客服，回答用户问题时优先使用工具查询最新信息")
                .user(userQuestion)
                .call()
                .content();
    }

    @EnableDynamicSkills
    public String generalConsult(String userQuestion) {
        return chatClient.prompt()
                .user(userQuestion)
                .call()
                .content();
    }
}
```

#### 场景3：ReactAgent 自动注入
```java
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.ReactAgentBuilder;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AgentConfiguration {
    
    @Bean
    public ReactAgent reactAgent(ChatModel chatModel) {
        // SDK 自动检测 ReactAgent Bean 并自动注入动态技能
        // 你只需要正常定义 Bean，不需要做任何额外工作
        return ReactAgentBuilder.builder(chatModel)
                .build();
    }
}
```

#### 场景4：手动管理技能列表
```java
import com.cloudskill.sdk.agent.CloudSkillToolManager;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CustomAgentController {

    @Autowired
    private CloudSkillToolManager cloudSkillToolManager;

    @Autowired
    private ChatClient chatClient;

    @GetMapping("/agent/custom-chat")
    public String customChat(@RequestParam String message, @RequestParam String scene) {
        var skills = switch (scene) {
            default -> cloudSkillToolManager.getSkillTools();
        };

        return chatClient.prompt()
                .user(message)
                .tools(skills)
                .call()
                .content();
    }
}
```

### 高级扩展开发

#### 自定义技能转换器
```java
import com.cloudskill.sdk.spi.SkillConverter;
import com.cloudskill.sdk.model.Skill;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class OpenApiSkillConverter implements SkillConverter {

    @Override
    public boolean support(Skill skill) {
        return "OPEN_API".equals(skill.getProtocol());
    }

    @Override
    public ToolCallback convert(Skill skill, com.cloudskill.sdk.core.CloudSkillClient client) {
        return FunctionToolCallback.builder(skill.getId(), (Map<String, Object> params) -> {
                    return client.invokeSkill(skill.getId(), params).getData();
                })
                .description(skill.getDescription())
                .inputSchema(skill.getParameterSchema())
                .build();
    }

    @Override
    public int getOrder() {
        return 100;
    }
}
```

#### 技能调用全局钩子
```java
import com.cloudskill.sdk.spi.SkillExecutionHook;
import com.cloudskill.sdk.model.SkillCallRequest;
import com.cloudskill.sdk.model.SkillCallResult;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class MonitoringSkillHook implements SkillExecutionHook {

    private final MeterRegistry meterRegistry;
    private final Timer timer;

    public MonitoringSkillHook(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.timer = Timer.builder("cloud_skill_invoke_duration_seconds")
                .description("Cloud skill invocation duration")
                .register(meterRegistry);
    }

    @Override
    public void beforeInvoke(SkillCallRequest request) {
        timer.record(() -> {});
    }

    @Override
    public void afterInvoke(SkillCallRequest request, SkillCallResult result) {
        // 记录调用结果
    }
}
```

#### 扩展新的消息队列（比如RabbitMQ）
```java
import com.cloudskill.sdk.listener.AbstractSkillChangeListener;
import com.cloudskill.sdk.core.CloudSkillClient;
import com.cloudskill.sdk.core.SkillCache;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class RabbitMqSkillChangeListener extends AbstractSkillChangeListener {
    
    public RabbitMqSkillChangeListener(
            CloudSkillClient cloudSkillClient,
            SkillCache skillCache) {
        super(cloudSkillClient, skillCache);
    }

    @Override
    public void subscribe() {
        // RabbitMQ 订阅实现
        // 监听技能变更队列，收到消息后调用 handleMessage(message)
    }
}
```

父类 `AbstractSkillChangeListener` 已经处理了：
- 版本连续性校验
- 根据操作类型分发（全量同步/增量更新/删除）
- 更新本地时间戳
- 刷新 `CloudSkillToolManager`

你只需要实现 `subscribe()` 方法订阅消息即可。

---

## 配置参考

### 完整配置示例
```yaml
cloud:
  skill:
    # 全局开关
    enabled: true
    
    # 基础配置
    server-url: http://localhost:8766
    api-key: your-api-key
    service-name: your-service-name
    service-version: 1.0.0
    service-port: 8080
    service-ip: 192.168.1.100
    
    # 技能同步
    auto-sync: true
    sync-interval: 30
    enable-listener: true
    
    # WebSocket（已废弃，使用Redis发布订阅）
    enable-web-socket: false
    reconnect-interval: 5
    
    # 调用配置
    call-timeout: 30000
    retry-count: 3
    
    # 缓存配置
    enable-local-cache: true
    cache-expire-time: 3600
    cache-check-interval: 300000
    
    # 服务注册
    enable-service-registry: false
    heartbeat-interval: 30
    
    # Spring AI集成
    enable-agent-integration: true
    dynamic-skills:
      enabled: true
      order: 2147483547
    
    # Spring AI Alibaba
    alibaba:
      enable-agent-support: true
      auto-inject-skills: true
    
    # 参数校验
    validation:
      enable-request-validation: true
      enable-response-validation: true
      fail-on-error: true
    
    # 调试
    debug: false
```

### 配置项说明

| 配置项 | 默认值 | 说明 |
|--------|---------|------|
| `cloud.skill.enabled` | `true` | 是否启用Cloud Skill SDK |
| `cloud.skill.server-url` | `http://localhost:8766` | Cloud Skill 管理端地址 |
| `cloud.skill.api-key` | - | API密钥，用于身份认证 |
| `cloud.skill.service-name` | `spring.application.name` | 服务名称，自动获取 |
| `cloud.skill.service-version` | `1.0.0` | 服务版本 |
| `cloud.skill.auto-sync` | `true` | 是否自动同步技能 |
| `cloud.skill.sync-interval` | `30` | 定时同步间隔（秒） |
| `cloud.skill.enable-listener` | `true` | 是否启用Redis监听器 |
| `cloud.skill.call-timeout` | `30000` | 调用超时（毫秒） |
| `cloud.skill.retry-count` | `3` | 调用重试次数 |
| `cloud.skill.enable-local-cache` | `true` | 是否启用本地缓存 |
| `cloud.skill.cache-expire-time` | `3600` | 缓存过期时间（秒） |
| `cloud.skill.cache-check-interval` | `300000` | 缓存检查间隔（毫秒） |
| `cloud.skill.enable-service-registry` | `false` | 是否启用服务注册 |
| `cloud.skill.heartbeat-interval` | `30` | 心跳间隔（秒） |
| `cloud.skill.enable-agent-integration` | `true` | 是否启用Spring AI集成 |
| `cloud.skill.dynamic-skills.enabled` | `true` | 是否启用动态技能注入 |
| `cloud.skill.dynamic-skills.order` | `2147483547` | AOP切面执行顺序 |
| `cloud.skill.alibaba.enable-agent-support` | `true` | 是否启用Spring AI Alibaba支持 |
| `cloud.skill.alibaba.auto-inject-skills` | `true` | 是否自动注入技能到ReactAgent |

---

## 架构设计

### 模板方法模式应用

本项目采用模板方法模式统一架构，方便扩展：

#### 技能变更监听
```
AbstractSkillChangeListener (抽象基类)
├── 通用逻辑：版本校验、消息分发、全量同步、更新时间戳
└── RedisSkillChangeListener (具体实现)
    └── 实现：Redis 发布订阅
```

未来扩展：
- `RabbitMqSkillChangeListener` → RabbitMQ
- `KafkaSkillChangeListener` → Kafka

只需要继承 `AbstractSkillChangeListener`，实现 `subscribe()` 方法即可。

#### 工具注入
```
AbstractToolInjector (抽象基类)
├── 通用逻辑：版本校验、获取动态工具、创建Prompt、合并工具、DashScope特殊处理
└── ChatModelToolInjector (具体实现)
    └── 实现：拦截 ChatModel.call(Prompt)
```

未来扩展：
- `ChatClientToolInjector` → ChatClient
- `ReactAgentToolInjector` → ReactAgent

只需要继承 `AbstractToolInjector`，实现拦截逻辑即可。

### 版本一致性保证

1. **全局时间戳**：Redis 中保存全局版本时间戳
2. **本地缓存**：本地保存每个技能更新时间戳 + 全局时间戳
3. **注入前校验**：每次调用前对比本地 vs Redis 全局时间戳
4. **自动同步**：如果本地版本落后，自动触发全量同步
5. **刷新缓存**：同步完成后刷新 `CloudSkillToolManager` 工具列表

这样保证了：
- 用户拿到的永远是最新版本的技能
- 即使Redis消息丢失，也能通过校验自动恢复
- 性能好，直接从本地缓存获取，只在需要时同步

### 启用控制优先级

```
优先级：方法注解 > 类注解 > 全局配置

示例：
- 方法有 @EnableDynamicSkills → 按方法注解
- 方法没有，类有 @EnableDynamicSkills → 按类注解
- 方法和类都没有 → 按 cloud.skill.dynamic-skills.enabled
```

非常灵活：
- 全局开启，个别方法关闭
- 全局关闭，个别方法开启
- 类开启，个别方法关闭

满足各种业务场景。

---

## 最佳实践

### 1. 生产环境配置建议
```yaml
cloud:
  skill:
    enabled: true
    auto-sync: true
    enable-listener: true
    enable-local-cache: true
    dynamic-skills:
      enabled: true
```

- 启用本地缓存：性能最好
- 启用Redis监听：实时更新
- 定时同步：兜底保证最终一致性
- 注入前版本校验：保证一致性

### 2. 注解模式推荐
生产环境推荐使用注解模式：
```java
@RestController
@EnableDynamicSkills  // 整个类开启
public class MyController {
    
    @GetMapping("/chat")
    public String chat(String message) {
        // 开启
    }
    
    @GetMapping("/status")
    @EnableDynamicSkills(false)  // 关闭
    public String status() {
        // 不需要技能的接口
    }
}
```

优点：
- 只在需要的地方启用，性能更好
- 细粒度控制，灵活满足业务需求
- 避免对不需要技能的接口产生影响

### 3. ReactAgent 零侵入使用
```java
@Configuration
public class AgentConfig {
    @Bean
    public ReactAgent reactAgent(ChatModel chatModel) {
        // 只需要正常定义Bean，SDK自动检测并注入技能
        return ReactAgentBuilder.builder(chatModel).build();
    }
}
```

完全零侵入，不需要任何额外代码。

### 4. 自定义扩展
- 优先使用SPI扩展（`SkillConverter`、`SkillExecutionHook`）
- 需要新增消息队列或AI客户端时，继承抽象基类实现子类
- 不需要修改核心代码，符合开闭原则

---

## 故障排查

### Q: 技能没有注入进去？
A: 检查：
1. 配置 `cloud.skill.dynamic-skills.enabled: true`
2. 如果使用注解，检查方法/类是否有 `@EnableDynamicSkills(true)`
3. 检查 `ChatModel.call(Prompt)` 方法，参数必须有 `Prompt`
4. 检查日志是否有 "动态技能自动注入已启用" 启动日志

### Q: 调用provider技能时，GET参数找不到？
A: 本SDK已经修复这个问题，GET请求参数会正确拼接到URL，不会放到body中。如果还是有问题，请检查provider端代码是否正确从URL query获取参数。

### Q: 版本冲突 `NoSuchMethodError: treeToValue`？
A: Spring AI Alibaba 需要 Jackson ≥ 2.15.0，在pom.xml中添加：
```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <version>2.15.2</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### Q: 本地缓存和服务端不一致？
A: SDK会在每次注入前自动校验版本，如果不一致自动触发全量同步。你也可以手动调用 `cloudSkillClient.syncSkills()` 触发同步。

### Q: DashScope 报错 `object is not an instance of declaring class`？
A: 本SDK已经通过反射正确构造 `DashScopeApiSpec.FunctionTool`，这个问题已经解决。

### Q: ChatClient 调用时，动态技能为什么没有出现在工具列表？
A: 检查：
1. 确认 `cloud.skill.enable-agent-integration: true`
2. 确认 `cloud.skill.dynamic-skills.enabled: true`
3. 打开 debug 日志 `logging.level.com.cloudskill.sdk.agent=debug`，检查是否有 `marked for ChatModel skip` 日志
4. 跳过是正常设计，因为已经在 `ChatClientCustomizer` 注入了

### Q: 会出现重复注入动态技能吗？
A: 不会，通过 `ALREADY_INJECTED_IN_BUILD` ThreadLocal 标记，ChatClient 注入后 ChatModel 会直接跳过，保证只注入一次。

---

## 贡献

欢迎提交Issue和Pull Request！

## 许可证

Apache License 2.0 - see [LICENSE](LICENSE) for details.

## 链接

- [项目主页](https://github.com/your-org/cloud-skill)
- [问题反馈](https://github.com/your-org/cloud-skill/issues)
