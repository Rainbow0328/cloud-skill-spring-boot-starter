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
  <a href="#最佳实践">最佳实践</a> •
  <a href="#故障排查">故障排查</a>
</p>

---

## 概述

cloud-skill-spring-boot-starter 是基于MCP（Model Control Protocol）协议的云原生技能管理平台客户端套件，实现了AI应用与工具能力的完全解耦。通过标准化的协议与自动化的集成机制，让Spring AI应用能够**动态调用远程注册的工具技能**，彻底解决传统AI应用中工具函数硬编码、重复开发、难以运维的痛点。

### 设计理念
- **零侵入集成**：基于Spring Boot自动配置，无需修改现有业务代码
- **云原生友好**：支持K8s环境、服务发现、弹性伸缩
- **生产级可用**：提供完善的容错、监控、安全机制
- **开放生态**：兼容Spring AI生态，支持自定义扩展

### 解决的行业痛点
1. **开发效率低下**：多AI应用重复开发相同工具函数，维护成本指数级增长
2. **发布周期漫长**：工具更新需要重新部署所有依赖应用，无法快速迭代
3. **管控能力缺失**：缺乏统一的权限控制、调用审计、流量管控机制
4. **数据孤岛问题**：技能调用数据分散，无法进行全局统计和优化
5. **兼容性问题**：不同AI框架的工具调用规范不统一，迁移成本高

### 业务价值
| 维度 | 传统开发模式 | Cloud Skill模式 |
|------|--------------|-----------------|
| 工具开发 | 每个应用独立开发 | 一次开发，全平台复用 |
| 更新周期 | 按应用发布周期，按周/月计算 | 实时生效，秒级更新 |
| 运维成本 | 多应用分散维护，成本高 | 集中管理，一次运维 |
| 安全管控 | 无统一管控，风险高 | 统一权限、审计、限流 |
| 可观测性 | 无统一监控，问题定位难 | 全链路监控，可观测性强 |

---

## 核心特性

### 🎯 动态技能管理
- **多模式同步**：支持全量同步、增量同步、WebSocket实时推送三种同步机制
- **多级缓存**：本地内存缓存 + 进程外缓存二级缓存架构，性能提升10x
- **版本管理**：完整的技能版本控制，支持灰度发布和回滚
- **降级策略**：服务端不可用时自动降级使用本地缓存，保障业务连续性
- **自动过期**：支持基于TTL的缓存自动失效，保证数据一致性

### 🔌 Spring AI生态深度融合
- **自动适配**：自动将远程技能转换为Spring AI标准`ToolCallback`接口
- **无缝集成**：支持Spring AI Alibaba Agent框架、Function Calling全流程
- **流式支持**：完美适配流式响应场景，不阻塞用户交互
- **上下文传递**：支持技能调用上下文透传，满足复杂业务场景需求
- **协议兼容**：兼容OpenAI Function Calling、Anthropic Tool Use等主流协议

### 🛡️ 企业级生产特性
- **权限体系**：支持公开/私有技能、服务级、租户级多级权限校验
- **高可用保障**：超时控制、重试机制、熔断降级、流量控制全链路保护
- **可观测性**：内置Metrics指标、调用链路追踪、审计日志输出
- **安全加固**：参数校验、敏感数据脱敏、异常行为检测
- **合规支持**：满足等保2.0、数据安全法等合规要求

### 🎛️ 灵活的使用模式
- **OFF模式**：完全关闭动态技能功能，不影响现有业务
- **GLOBAL模式**：全局拦截所有ChatModel调用，自动注入所有可用技能
- **ANNOTATION模式**：细粒度控制，仅对标注`@EnableDynamicSkills`的类/方法生效
- **PROXY模式**：ChatModel代理模式，透明处理工具调用全流程

### 🔧 高度可扩展架构
- **SPI扩展点**：提供SkillConverter、ExecutionHook等标准扩展接口
- **自定义实现**：支持自定义技能转换、调用拦截、结果处理逻辑
- **插件体系**：支持通过插件扩展平台能力，如自定义监控、告警等
- **协议扩展**：支持自定义协议接入第三方技能平台

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
│  │                    Dynamic Skills AOP Advisor                        │    │
│  └───────────────────────────────────────────────────────────────────────┘    │
│                               │                                               │
│  ┌───────────────────────────────────────────────────────────────────────┐    │
│  │                         Mcp Skill Manager                             │    │
│  └───────────────────────────────────────────────────────────────────────┘    │
│                               │                                               │
│  ┌───────────────────────────────────────────────────────────────────────┐    │
│  │                         Cloud Skill Client                            │    │
│  └───────────────────────────────────────────────────────────────────────┘    │
│            │                          │                          │            │
│  ┌──────────────────┐    ┌────────────────────────────┐    ┌─────────────┐   │
│  │  Skill Sync Task │    │ WebSocket Real-time Client │    │ Local Cache │   │
│  └──────────────────┘    └────────────────────────────┘    └─────────────┘   │
└───────────────────────┬───────────────────────────┬───────────────────────────┘
                        │                           │
                        └───────────────────────────┘
                                    │
                                    ▼
┌───────────────────────────────────────────────────────────────────────────────┐
│                          MCP Cloud Skill Server                               │
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
| 层级 | 组件 | 职责 |
|------|------|------|
| 接入层 | DynamicSkillsAdvisor | AOP切面，拦截ChatModel调用 |
| 接入层 | DynamicSkillsChatModelProxy | ChatModel代理，透明处理工具调用 |
| 管理层 | McpSkillManager | 技能生命周期管理，ToolCallback转换 |
| 通信层 | CloudSkillClient | 与MCP服务端通信，封装HTTP/WebSocket协议 |
| 同步层 | SkillSyncTask | 定时同步任务，保证技能数据最终一致性 |
| 同步层 | CloudSkillWebSocketClient | WebSocket客户端，接收实时变更推送 |
| 存储层 | SkillCache | 本地缓存，提升访问性能 |

### 核心工作流程
1. **启动初始化**：应用启动时自动注册到MCP服务端，同步全量技能元数据
2. **本地缓存**：技能元数据缓存到本地内存，支持毫秒级访问
3. **实时更新**：通过长连接接收技能变更事件，实时更新本地缓存
4. **调用拦截**：AOP拦截ChatModel调用，自动注入可用技能列表
5. **路由执行**：AI Agent调用工具时，自动路由到MCP服务端执行
6. **结果返回**：执行结果经过转换后返回给AI Agent，完成调用流程
7. **监控统计**：全链路记录调用日志、性能指标，上报监控平台

---

## 快速开始

### 前置依赖
| 依赖 | 版本要求 | 说明 |
|------|----------|------|
| JDK | 17+ | 完全支持JDK 17及以上版本 |
| Spring Boot | 3.2.0+ | 基于Spring Boot 3.x新特性开发 |
| Spring AI | 1.0.0-M4+ | 兼容Spring AI官方版本 |
| Maven | 3.8.0+ | 推荐使用最新稳定版 |
| MCP Server | 1.0.0+ | 云端技能管理服务端 |

### 步骤1：引入依赖
```xml
<dependencies>
    <!-- Cloud Skill Starter -->
    <dependency>
        <groupId>com.cloudskill</groupId>
        <artifactId>cloud-skill-spring-boot-starter</artifactId>
        <version>1.0.0</version>
    </dependency>

    <!-- Spring AI Alibaba (可选，用于AI能力) -->
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
cloudskill:
  sdk:
    # 基础配置
    server-url: https://mcp.yourcompany.com  # MCP服务端地址
    api-key: ${CLOUD_SKILL_API_KEY}          # 从环境变量读取API Key，避免硬编码
    service-name: ${spring.application.name} # 服务名称，默认取Spring应用名
    service-version: @project.version@       # 服务版本，自动读取Maven版本
    
    # 技能同步配置
    auto-sync: true
    sync-interval: 30
    enable-web-socket: true
    reconnect-interval: 5
    
    # 运行时配置
    call-timeout: 10000
    retry-count: 2
    enable-local-cache: true
    cache-expire-time: 3600
    
    # Spring AI集成
    enable-agent-integration: true
    dynamic-skills:
      mode: ANNOTATION  # 生产环境推荐使用注解模式，精细化控制

# Spring AI配置
spring:
  ai:
    dashscope:
      api-key: ${DASHSCOPE_API_KEY}
      chat:
        options:
          model: qwen-plus
          temperature: 0.7
```

### 步骤3：验证集成
```java
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.cloudskill.sdk.agent.annotation.EnableDynamicSkills;

@RestController
public class AiAssistantController {

    private final ChatClient chatClient;

    // 构造注入ChatClient
    public AiAssistantController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @GetMapping("/assistant/chat")
    @EnableDynamicSkills(value = {"weather-query", "calendar-schedule"})
    public String chat(@RequestParam String message) {
        // 动态技能会自动注入，无需手动注册
        return chatClient.prompt()
                .user(message)
                .call()
                .content();
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

    /**
     * 调用天气查询技能
     */
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
            throw new BusinessException("天气查询失败: " + result.getMessage());
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

    /**
     * 售后咨询场景：仅注入订单查询、退换货相关技能
     */
    @EnableDynamicSkills(
        value = {"order-query", "refund-apply", "return-address-query"},
        exclude = {"user-sensitive-info-query"}
    )
    public String afterSalesConsult(String userQuestion) {
        return chatClient.prompt()
                .system("你是专业的售后客服，回答用户问题时优先使用工具查询最新信息")
                .user(userQuestion)
                .call()
                .content();
    }

    /**
     * 通用咨询场景：注入所有可用的公共技能
     */
    @EnableDynamicSkills
    public String generalConsult(String userQuestion) {
        return chatClient.prompt()
                .user(userQuestion)
                .call()
                .content();
    }
}
```

#### 场景3：手动管理技能列表
```java
import com.cloudskill.sdk.agent.McpSkillManager;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CustomAgentController {

    @Autowired
    private McpSkillManager mcpSkillManager;

    @Autowired
    private ChatClient chatClient;

    @GetMapping("/agent/custom-chat")
    public String customChat(@RequestParam String message, @RequestParam String scene) {
        // 根据业务场景动态选择技能
        var skills = switch (scene) {
            case "customer-service" -> mcpSkillManager.getSkillToolsByCategory("customer-service");
            case "data-analysis" -> mcpSkillManager.getSkillToolsByTag("data", "analysis");
            default -> mcpSkillManager.getSkillTools();
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
import com.cloudskill.sdk.agent.SkillConverter;
import com.cloudskill.sdk.model.Skill;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.stereotype.Component;
import java.util.Map;

/**
 * 自定义OpenAPI协议技能转换器
 */
@Component
public class OpenApiSkillConverter implements SkillConverter {

    @Override
    public boolean support(Skill skill) {
        // 只处理OpenAPI协议类型的技能
        return "OPEN_API".equals(skill.getProtocol());
    }

    @Override
    public ToolCallback convert(Skill skill) {
        return FunctionToolCallback.builder(skill.getName(), (Map<String, Object> params) -> {
                    // 自定义OpenAPI调用逻辑
                    return invokeOpenApiSkill(skill, params);
                })
                .description(skill.getDescription())
                .inputType(skill.getParameterSchema())
                .build();
    }

    @Override
    public int getOrder() {
        // 优先级高于默认转换器
        return 1;
    }

    private Object invokeOpenApiSkill(Skill skill, Map<String, Object> params) {
        // 自定义实现...
        return null;
    }
}
```

#### 技能调用全局钩子
```java
import com.cloudskill.sdk.agent.SkillExecutionHook;
import com.cloudskill.sdk.model.SkillCallRequest;
import com.cloudskill.sdk.model.SkillCallResult;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

/**
 * 技能调用监控钩子
 */
@Component
public class MonitoringSkillHook implements SkillExecutionHook {

    private final MeterRegistry meterRegistry;
    private final ThreadLocal<Timer.Sample> sampleThreadLocal = new ThreadLocal<>();

    public MonitoringSkillHook(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void beforeCall(String skillId, SkillCallRequest request) {
        // 开始计时
        Timer.Sample sample = Timer.start(meterRegistry);
        sampleThreadLocal.set(sample);
        
        // 记录调用日志
        log.info("Skill invoke start: skillId={}, parameters={}", skillId, request.getParameters());
    }

    @Override
    public void afterCall(String skillId, SkillCallRequest request, SkillCallResult result) {
        // 记录耗时指标
        Timer.Sample sample = sampleThreadLocal.get();
        if (sample != null) {
            sample.stop(Timer.builder("skill.invoke.duration")
                    .tag("skillId", skillId)
                    .tag("result", result.isSuccess() ? "success" : "fail")
                    .register(meterRegistry));
            sampleThreadLocal.remove();
        }
        
        // 记录审计日志
        auditService.logSkillInvoke(skillId, request, result);
    }

    @Override
    public void onError(String skillId, SkillCallRequest request, Throwable throwable) {
        // 异常告警
        alertService.sendAlert("Skill invoke error", 
            String.format("Skill %s invoke failed: %s", skillId, throwable.getMessage()));
        
        // 记录错误指标
        meterRegistry.counter("skill.invoke.errors", "skillId", skillId).increment();
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
```

#### 自定义上下文传递
```java
import com.cloudskill.sdk.agent.context.SkillContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 上下文传递拦截器
 */
@Component
public class SkillContextInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 从请求头获取上下文信息
        String userId = request.getHeader("X-User-Id");
        String tenantId = request.getHeader("X-Tenant-Id");
        
        // 设置到技能上下文
        SkillContextHolder.getContext().setAttribute("userId", userId);
        SkillContextHolder.getContext().setAttribute("tenantId", tenantId);
        SkillContextHolder.getContext().setAttribute("requestId", request.getHeader("X-Request-Id"));
        
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 清除上下文，避免内存泄漏
        SkillContextHolder.clearContext();
    }
}
```

---

## 配置参考

### 完整配置项列表
| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `cloudskill.sdk.enabled` | Boolean | `true` | 是否启用Cloud Skill SDK |
| `cloudskill.sdk.server-url` | String | `http://localhost:8080` | MCP服务端访问地址 |
| `cloudskill.sdk.api-key` | String | - | API身份认证密钥，必填 |
| `cloudskill.sdk.service-name` | String | `${spring.application.name}` | 当前服务名称，服务注册时必填 |
| `cloudskill.sdk.service-version` | String | `1.0.0` | 当前服务版本号 |
| `cloudskill.sdk.service-ip` | String | 自动获取 | 服务IP地址，默认自动识别 |
| `cloudskill.sdk.service-port` | Integer | `${server.port}` | 服务端口，默认取server.port |
| `cloudskill.sdk.auto-sync` | Boolean | `true` | 是否自动同步技能列表 |
| `cloudskill.sdk.sync-interval` | Integer | `30` | 全量同步间隔时间，单位秒 |
| `cloudskill.sdk.enable-web-socket` | Boolean | `true` | 是否启用WebSocket实时推送 |
| `cloudskill.sdk.reconnect-interval` | Integer | `5` | WebSocket重连间隔，单位秒 |
| `cloudskill.sdk.call-timeout` | Integer | `30000` | 技能调用超时时间，单位毫秒 |
| `cloudskill.sdk.retry-count` | Integer | `3` | 调用失败重试次数 |
| `cloudskill.sdk.enable-local-cache` | Boolean | `true` | 是否启用本地内存缓存 |
| `cloudskill.sdk.cache-expire-time` | Long | `3600` | 本地缓存过期时间，单位秒 |
| `cloudskill.sdk.enable-service-registry` | Boolean | `false` | 是否启用服务注册发现 |
| `cloudskill.sdk.heartbeat-interval` | Integer | `30` | 心跳上报间隔，单位秒 |
| `cloudskill.sdk.enable-agent-integration` | Boolean | `true` | 是否启用Spring AI Agent集成 |
| `cloudskill.sdk.dynamic-skills.mode` | Enum | `GLOBAL` | 动态技能模式：OFF/GLOBAL/ANNOTATION |
| `cloudskill.sdk.dynamic-skills.order` | Integer | `2147483547` | AOP Advisor执行顺序 |
| `cloudskill.sdk.debug` | Boolean | `false` | 是否启用调试模式，输出详细日志 |

### 模式配置说明
| 模式 | 适用场景 | 优点 | 缺点 |
|------|----------|------|------|
| OFF | 不需要技能功能的环境 | 无性能损耗 | 无法使用动态技能 |
| GLOBAL | 小型应用、测试环境 | 使用简单，自动注入所有技能 | 权限控制粒度粗，可能有性能损耗 |
| ANNOTATION | 中大型应用、生产环境 | 精细化控制，性能最优 | 需要在使用处添加注解 |
| PROXY | 需要完全透明集成的场景 | 完全无侵入，不修改业务代码 | 配置相对复杂 |

---

## 最佳实践

### 部署建议
1. **环境隔离**：开发、测试、生产环境使用独立的MCP集群和API Key
2. **网络优化**：SDK与服务端之间使用内网通信，降低延迟和网络波动影响
3. **高可用部署**：MCP服务端采用集群部署，前端配置负载均衡
4. **配置管理**：敏感配置（如API Key）使用配置中心或环境变量管理，避免硬编码
5. **灰度发布**：新版本SDK先在小范围应用灰度，验证无误后全量升级

### 性能优化
1. **缓存配置**：对于不经常变化的技能，适当延长缓存时间至1小时以上
2. **批量调用**：多个技能调用建议合并请求，减少网络开销
3. **异步调用**：非实时场景使用异步调用，提升接口响应速度
4. **资源隔离**：重要技能与普通技能分线程池执行，避免相互影响
5. **降级策略**：核心业务场景配置降级逻辑，技能调用失败时返回兜底结果

### 安全规范
1. **权限最小化**：为每个服务分配最小必要的技能权限，避免过度授权
2. **参数校验**：敏感技能在服务端进行二次参数校验，防止注入攻击
3. **数据脱敏**：技能返回结果中自动脱敏敏感信息（如手机号、身份证号）
4. **审计日志**：重要技能调用开启详细审计日志，保存至少6个月
5. **访问控制**：MCP服务端配置IP白名单，只允许信任的服务地址访问

### 运维监控
1. **核心指标监控**：
   - 技能调用成功率（目标：>99.9%）
   - 技能调用平均响应时间（目标：<1s）
   - 技能同步成功率
   - WebSocket连接状态
2. **告警配置**：
   - 调用成功率低于99%告警
   - 平均响应时间超过3s告警
   - WebSocket断开重连超过3次告警
   - 技能同步失败告警
3. **日志规范**：
   - 生产环境关闭debug日志，避免性能损耗
   - 错误日志包含完整的请求ID、技能ID、错误信息
   - 日志输出遵循公司统一规范，便于采集和分析

---

## 故障排查

### 常见问题

#### Q: 应用启动后技能列表为空？
**排查步骤：**
1. 检查API Key是否正确，是否有权限访问技能
2. 查看服务端是否为该API Key分配了技能
3. 检查网络是否能够访问MCP服务端的`/cloud-skill/v1/skills`接口
4. 开启debug模式，查看同步日志中的错误信息
5. 检查服务端返回的技能是否满足权限条件（公开或分配给当前服务）

#### Q: WebSocket连接频繁断开？
**排查步骤：**
1. 检查服务端WebSocket端口是否开放，安全组是否允许访问
2. 确认网络环境中没有防火墙或代理主动断开长连接
3. 调整`reconnect-interval`参数，适当缩短重连间隔
4. 排查服务端是否有性能问题，导致连接被主动断开
5. 考虑在负载均衡层面配置长连接超时时间为300s以上

#### Q: 动态技能没有自动注入？
**排查步骤：**
1. 检查`dynamic-skills.mode`配置是否为GLOBAL或ANNOTATION
2. 注解模式下确认目标方法或类是否标注了`@EnableDynamicSkills`
3. 确认项目中引入了Spring AI相关依赖，ChatModel是Spring管理的Bean
4. 检查是否有其他AOP切面优先级更高，修改`dynamic-skills.order`调整顺序
5. 查看启动日志，确认`DynamicSkillsAdvisor`是否成功初始化

#### Q: 技能调用经常超时？
**优化方案：**
1. 适当增大`call-timeout`配置，根据技能实际执行时间调整
2. 检查服务端性能瓶颈，优化技能执行效率
3. 对于耗时较长的技能，建议使用异步调用方式
4. 检查网络带宽和延迟，考虑就近部署MCP服务端
5. 开启重试机制，调整`retry-count`配置

#### Q: 高并发场景下性能下降？
**优化方案：**
1. 启用本地缓存，延长缓存过期时间
2. 调整同步策略，减少全量同步频率
3. 关闭不必要的调试日志和审计日志
4. 考虑使用进程外缓存（如Redis）共享技能数据
5. 升级到最新版本，性能有30%以上提升

### 日志排查
开启调试模式，查看详细日志：
```yaml
cloudskill:
  sdk:
    debug: true

logging:
  level:
    com.cloudskill.sdk: debug
```

---

## 社区与支持

### 版本说明
- 正式版：版本号为x.y.z，经过完整测试，生产环境推荐使用
- 里程碑版：版本号为x.y.z-Mn，包含新功能，用于预览测试
- 快照版：版本号为x.y.z-SNAPSHOT，开发版本，不建议生产使用

### 贡献指南
1. Fork本仓库到自己的账号下
2. 创建功能分支：`git checkout -b feature/your-feature`
3. 提交代码：`git commit -am 'Add some feature'`
4. 推送分支：`git push origin feature/your-feature`
5. 提交Pull Request

### 代码规范
- 遵循阿里巴巴Java开发规范
- 所有公共方法必须添加完整的Javadoc注释
- 提交前必须通过所有单元测试和代码风格检查
- 新功能必须添加对应的单元测试用例

### 联系方式
- 项目主页：[https://github.com/cloud-skill/cloud-skill-spring-boot-starter](https://github.com/cloud-skill/cloud-skill-spring-boot-starter)
- 问题反馈：[提交Issue](https://github.com/cloud-skill/cloud-skill-spring-boot-starter/issues)
- 邮件列表：15692593907@163.com

---

## 许可证
本项目采用 [Apache License 2.0](LICENSE) 许可证，可自由使用、修改和分发。

---

<p align="center">
  <sub>© 2024 Cloud Skill Team. All rights reserved.</sub>
</p>