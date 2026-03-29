# cloud-skill-spring-boot-starter

<p align="center">
  <img src="https://img.shields.io/badge/JDK-17+-green.svg" alt="JDK Version">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.2.0+-blue.svg" alt="Spring Boot Version">
  <img src="https://img.shields.io/badge/Spring%20AI-1.0.0+-purple.svg" alt="Spring AI Version">
  <img src="https://img.shields.io/badge/License-Apache%202.0-red.svg" alt="License">
  <img src="https://img.shields.io/maven-central/v/com.cloudskill/cloud-skill-spring-boot-starter.svg" alt="Maven Central">
</p>

<p align="center">
  <strong>Enterprise-grade Cloud Skill Platform Spring Boot Starter</strong><br>
  Production-grade dynamic skill injection and full lifecycle management for Spring AI applications
</p>

<p align="center">
  <a href="#overview">Overview</a> •
  <a href="#core-features">Core Features</a> •
  <a href="#technical-architecture">Technical Architecture</a> •
  <a href="#quick-start">Quick Start</a> •
  <a href="#development-guide">Development Guide</a> •
  <a href="#configuration-reference">Configuration Reference</a> •
  <a href="#best-practices">Best Practices</a> •
  <a href="#troubleshooting">Troubleshooting</a>
</p>

---

## Overview

cloud-skill-spring-boot-starter is a cloud-native skill management platform client toolkit based on the MCP (Model Control Protocol) protocol, which enables complete decoupling between AI applications and tool capabilities. Through standardized protocols and automated integration mechanisms, Spring AI applications can **dynamically call remotely registered tool skills** without hardcoding tool implementations,彻底解决传统AI应用中工具函数硬编码、重复开发、难以运维的痛点。

### Design Philosophy
- **Zero-intrusion Integration**: Based on Spring Boot auto-configuration, no modification to existing business code required
- **Skill Sharing Platform**: Develop once, all AI applications across the platform automatically gain the ability to call, eliminating repetitive development
- **Dynamic Enhancement**: After a skill is dynamically registered on the server, it is automatically synchronized to all consumer applications, taking effect in real-time without restarting
- **Cloud Native Friendly**: Supports K8s environment, service discovery, elastic scaling
- **Production-grade Availability**: Provides comprehensive fault tolerance, monitoring, and security mechanisms
- **Open Ecosystem**: Compatible with both Spring AI and Spring AI Alibaba ecosystems, supports custom extensions
- **Template Method Architecture**: Abstract parent class defines the skeleton, concrete subclasses implement details, easy to extend

### Industry Pain Points Solved
1. **Low Development Efficiency**: Repeated development of the same tool functions across multiple AI applications leads to exponential maintenance cost growth
2. **Long Release Cycles**: Tool updates require redeployment of all dependent applications, preventing rapid iteration
3. **Lack of Governance Capabilities**: No unified permission control, call auditing, or traffic management mechanisms
4. **Data Silo Problem**: Skill call data is scattered, making global statistics and optimization impossible
5. **Compatibility Issues**: Inconsistent tool calling specifications across different AI frameworks result in high migration costs

### Business Value
| Dimension | Traditional Development Model | Cloud Skill Model |
|-----------|--------------------------------|-------------------|
| Tool Development | Every AI application develops the same tool repeatedly | **Develop once, all AI applications across the platform automatically gain calling capability** |
| Update Release | Each application needs to be repackaged and deployed, released weekly/monthly | After registration on the server, all consumers get it in real-time, **takes effect in seconds without restart** |
| Operation & Maintenance | Decentralized maintenance across multiple applications, high repetitive work cost | Centralized management, one update synchronized across the entire platform |
| Version Consistency | Different applications have inconsistent versions, difficult to unify | All consumers automatically synchronize to the latest version |
| Security Governance | No unified control, high risk | Unified permissions, auditing, rate limiting |
| Observability | No unified monitoring, difficult problem location | Full-link monitoring, strong observability |

---

## Core Features

### 🎯 Dynamic Skill Management
- **Skill Sharing Platform**: Develop once and deploy, all AI applications across the platform automatically get the calling capability, completely eliminating repetitive development
- **Dynamic Real-time Enhancement**: After a skill is registered on the server, it is automatically pushed to all consumers, AI applications get new capabilities without restarting
- **Multi-mode Synchronization**: Supports full synchronization, incremental synchronization, and Redis publish/subscribe real-time push mechanisms
- **Local Cache First**: Full caching of skill metadata in local memory for millisecond access performance
- **Version Continuity Check**: Incremental updates based on timestamp, trigger full synchronization automatically when discontinuous
- **Version Check Before Injection**: Check version before each call, automatically synchronize when expired to ensure consistency
- **Degradation Strategy**: Automatically degrades to use local cache when server is unavailable, ensuring business continuity
- **Automatic Expiration**: Supports TTL-based cache automatic invalidation to ensure data consistency

### 🔌 Deep Integration with Spring AI & Spring AI Alibaba Ecosystem
- **Automatic Adaptation**: Automatically converts remote skills to Spring AI standard `ToolCallback` interfaces
- **DashScope Special Adaptation**: Perfectly adapts Spring AI Alibaba DashScope, correctly converts tool format
- **Seamless Integration**: Supports Spring AI ChatModel, ChatClient, ReactAgent full scenarios
- **ReactAgent Auto-injection**: Automatically detects ReactAgent Bean through BeanPostProcessor, zero-invasion injection
- **Template Method Architecture**: Unified abstract `AbstractToolInjector`, different scenarios use different subclasses, easy to extend
- **Protocol Compatibility**: Compatible with mainstream protocols such as OpenAI Function Calling, Anthropic Tool Use

### 🛡️ Enterprise-grade Production Features
- **Permission System**: Supports multi-level permission verification including public/private skills, service-level, and tenant-level
- **High Availability Guarantee**: Full-link protection including timeout control, retry mechanism, circuit breaker degradation, and traffic control
- **Observability**: Built-in Metrics indicators, call link tracing, and audit log output
- **Security Hardening**: Parameter verification, sensitive data desensitization, abnormal behavior detection
- **Compliance Support**: Meets compliance requirements such as Equal Protection 2.0 and Data Security Law
- **Correct GET Parameter Handling**: GET request parameters are correctly spliced into URL, will not be placed in body

### 🎛️ Flexible Enable Control
- **Global Configuration**: Global switch via `cloud.skill.dynamic-skills.enabled`
- **Class Annotation**: `@EnableDynamicSkills` annotated on class, controls the entire class
- **Method Annotation**: `@EnableDynamicSkills` annotated on method, fine-grained control
- **Priority**: Method Annotation > Class Annotation > Global Configuration, flexible for various scenarios
- **Supports Disabling**: You can set `@EnableDynamicSkills(false)` to disable specific methods

### 🔧 Highly Extensible Architecture
- **Template Method Pattern**: Both skill change listening and tool injection adopt abstract template method architecture
- **SPI Extension Points**: Provides standard extension interfaces such as `SkillConverter` and `SkillExecutionHook`
- **Message Queue Extension**: Supports Redis/RabbitMQ/Kafka multiple message queues, only need subclasses for new message queue
- **AI Client Extension**: Supports ChatModel/ChatClient/ReactAgent, only need subclasses for new AI client
- **Custom Implementation**: Supports custom skill conversion, call interception, and result processing logic

---

## Technical Architecture

### Overall Architecture
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

### Core Component Layering

#### Injection Architecture (Inject tools into AI model)
| Layer | Component | Responsibility |
|-------|-----------|----------------|
| Annotation | `@EnableDynamicSkills` | Fine-grained enable/disable control, supports class and method |
| AOP Aspect | `DynamicSkillsAdvisor` | Pointcut matching, only intercept when all conditions are met |
| Abstract Base | `AbstractToolInjector` | Template method, unified processing: version check + tool injection |
| Concrete Implementation | `ChatModelToolInjector` | ChatModel scenario specific implementation, intercepts `call(Prompt)` |
| Management Layer | `McpSkillManager` | Skill conversion Skill → ToolCallback, refresh tool list |

#### Listening Architecture (Skill change notification)
| Layer | Component | Responsibility |
|-------|-----------|----------------|
| Abstract Base | `AbstractSkillChangeListener` | Template method, unified processing: version check + message dispatch + full sync |
| Concrete Implementation | `RedisSkillChangeListener` | Redis publish/subscribe specific implementation, receive message and call parent processing |
| Storage Layer | `SkillCache` | Local skill cache, stores skill metadata |

#### Core Services
| Layer | Component | Responsibility |
|-------|-----------|----------------|
| Communication Layer | `CloudSkillClient` | Communicate with MCP server, HTTP call, get global timestamp, full sync |
| Scheduled Task | `SkillSyncTask` | Scheduled synchronization task, ensures eventual consistency |
| Scheduled Task | `SkillCacheRefresher` | Cache refresh, check expiration |
| SPI Extension | `SkillConverter` | Custom skill converter |
| SPI Extension | `SkillExecutionHook` | Skill call hook, pre/post processing |

### Core Workflow
1. **Startup Initialization**: Automatically registers with MCP server on application startup, synchronizes full skill metadata
2. **Local Caching**: Skill metadata is cached in local memory, supporting millisecond-level access
3. **Real-time Updates**: Receives skill change events through Redis publish/subscribe, updates local cache in real-time
4. **Version Check**: Before each ChatModel call, compare local timestamp with Redis global timestamp
5. **Auto Sync**: If local version is expired, automatically trigger full synchronization, inject after updating cache
6. **Call Interception**: AOP intercepts ChatModel calls, automatically injects available skill lists into Prompt
7. **Route Execution**: When AI Agent calls tools, automatically routes to MCP server for execution
8. **Result Return**: Execution results are converted and returned to AI Agent, completing the call process
9. **Monitoring Statistics**: Full-link recording of call logs, performance indicators, and reporting to monitoring platform

---

## Quick Start

### Prerequisites
| Dependency | Version Requirement | Description |
|------------|----------------------|-------------|
| JDK | 17+ | Fully supports JDK 17 and above |
| Spring Boot | 3.2.0+ | Developed based on Spring Boot 3.x new features |
| Spring AI | 1.0.0-M4+ | Compatible with official Spring AI versions |
| Maven | 3.8.0+ | Latest stable version recommended |
| MCP Server | 1.0.0+ | Cloud skill management server |

### Step 1: Add Dependencies
```xml
<dependencies>
    <!-- Cloud Skill Starter -->
    <dependency>
        <groupId>com.cloudskill</groupId>
        <artifactId>cloud-skill-spring-boot-starter</artifactId>
        <version>1.0.0</version>
    </dependency>

    <!-- Spring AI Alibaba (Optional, for AI capabilities) -->
    <dependency>
        <groupId>com.alibaba.cloud.ai</groupId>
        <artifactId>spring-ai-alibaba-starter-dashscope</artifactId>
        <version>1.1.2.0</version>
    </dependency>
</dependencies>
```

### Step 2: Basic Configuration
```yaml
# application.yml
cloud:
  skill:
    # Basic Configuration
    enabled: true
    server-url: https://mcp.yourcompany.com  # MCP server address
    api-key: ${CLOUD_SKILL_API_KEY}          # Read API Key from environment variable to avoid hardcoding
    service-name: ${spring.application.name} # Service name, defaults to Spring application name
    service-version: 1.0.0                   # Service version
    
    # Skill Synchronization Configuration
    auto-sync: true
    sync-interval: 30
    enable-listener: true
    
    # Runtime Configuration
    call-timeout: 30000
    retry-count: 3
    enable-local-cache: true
    cache-expire-time: 3600
    cache-check-interval: 300000
    
    # Spring AI Integration (dynamic skill injection)
    enable-agent-integration: true
    dynamic-skills:
      enabled: true
      order: 2147483547
    
    # Spring AI Alibaba Specific Configuration
    alibaba:
      enable-agent-support: true
      auto-inject-skills: true

# Spring AI Configuration
spring:
  ai:
    dashscope:
      api-key: ${DASHSCOPE_API_KEY}
      chat:
        options:
          model: qwen-plus
          temperature: 0.7
```

### Step 3: Verify Integration
```java
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.cloudskill.sdk.agent.annotation.EnableDynamicSkills;

@RestController
public class AiAssistantController {

    private final ChatClient chatClient;

    // Constructor injection of ChatClient
    public AiAssistantController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @GetMapping("/assistant/chat")
    @EnableDynamicSkills(value = {"weather-query", "calendar-schedule"})
    public String chat(@RequestParam String message) {
        // Dynamic skills are automatically injected, no manual registration required
        return chatClient.prompt()
                .user(message)
                .call()
                .content();
    }
}
```

Start the application and access the interface to test if skill calls work properly.

---

## Development Guide

### Basic Usage Scenarios

#### Scenario 1: Direct Skill API Call
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
     * Call weather query skill
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
            throw new BusinessException("Weather query failed: " + result.getMessage());
        }
    }
}
```

#### Scenario 2: Fine-grained Control with Annotation Mode
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
     * After-sales consultation scenario: Only inject order query, return and exchange related skills
     */
    @EnableDynamicSkills(
        value = {"order-query", "refund-apply", "return-address-query"},
        exclude = {"user-sensitive-info-query"}
    )
    public String afterSalesConsult(String userQuestion) {
        return chatClient.prompt()
                .system("You are a professional after-sales customer service representative, prioritize using tools to query the latest information when answering user questions")
                .user(userQuestion)
                .call()
                .content();
    }

    /**
     * General consultation scenario: Inject all available public skills
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

#### Scenario 3: Manual Skill List Management
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
        // Dynamically select skills based on business scenario
        var skills = switch (scene) {
            case "customer-service" -> mcpSkillManager.getSkillToolsByCategory("customer-service");
            case "data-analysis" -> mcpSkillManager.getSkillToolsByTag("data", "analysis");
            default -> mcpSkillManager.getSkillTools();
        };

        return chatClient.prompt()
                .user(message)
                .tools(skills) // Manually inject dynamic skills
                .call()
                .content();
    }
}
```

### Advanced Extension Development

#### Custom Skill Converter
```java
import com.cloudskill.sdk.agent.SkillConverter;
import com.cloudskill.sdk.model.Skill;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.stereotype.Component;
import java.util.Map;

/**
 * Custom OpenAPI protocol skill converter
 */
@Component
public class OpenApiSkillConverter implements SkillConverter {

    @Override
    public boolean support(Skill skill) {
        // Only process skills of OpenAPI protocol type
        return "OPEN_API".equals(skill.getProtocol());
    }

    @Override
    public ToolCallback convert(Skill skill) {
        return FunctionToolCallback.builder(skill.getName(), (Map<String, Object> params) -> {
                    // Custom OpenAPI call logic
                    return invokeOpenApiSkill(skill, params);
                })
                .description(skill.getDescription())
                .inputType(skill.getParameterSchema())
                .build();
    }

    @Override
    public int getOrder() {
        // Higher priority than default converter
        return 1;
    }

    private Object invokeOpenApiSkill(Skill skill, Map<String, Object> params) {
        // Custom implementation...
        return null;
    }
}
```

#### Global Skill Execution Hook
```java
import com.cloudskill.sdk.agent.SkillExecutionHook;
import com.cloudskill.sdk.model.SkillCallRequest;
import com.cloudskill.sdk.model.SkillCallResult;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

/**
 * Skill call monitoring hook
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
        // Start timing
        Timer.Sample sample = Timer.start(meterRegistry);
        sampleThreadLocal.set(sample);
        
        // Log call information
        log.info("Skill invoke start: skillId={}, parameters={}", skillId, request.getParameters());
    }

    @Override
    public void afterCall(String skillId, SkillCallRequest request, SkillCallResult result) {
        // Record duration metrics
        Timer.Sample sample = sampleThreadLocal.get();
        if (sample != null) {
            sample.stop(Timer.builder("skill.invoke.duration")
                    .tag("skillId", skillId)
                    .tag("result", result.isSuccess() ? "success" : "fail")
                    .register(meterRegistry));
            sampleThreadLocal.remove();
        }
        
        // Record audit log
        auditService.logSkillInvoke(skillId, request, result);
    }

    @Override
    public void onError(String skillId, SkillCallRequest request, Throwable throwable) {
        // Exception alert
        alertService.sendAlert("Skill invoke error", 
            String.format("Skill %s invoke failed: %s", skillId, throwable.getMessage()));
        
        // Record error metrics
        meterRegistry.counter("skill.invoke.errors", "skillId", skillId).increment();
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
```

#### Custom Context Propagation
```java
import com.cloudskill.sdk.agent.context.SkillContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Context propagation interceptor
 */
@Component
public class SkillContextInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Get context information from request headers
        String userId = request.getHeader("X-User-Id");
        String tenantId = request.getHeader("X-Tenant-Id");
        
        // Set to skill context
        SkillContextHolder.getContext().setAttribute("userId", userId);
        SkillContextHolder.getContext().setAttribute("tenantId", tenantId);
        SkillContextHolder.getContext().setAttribute("requestId", request.getHeader("X-Request-Id"));
        
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // Clear context to avoid memory leaks
        SkillContextHolder.clearContext();
    }
}
```

---

## Configuration Reference

### Complete Configuration Item List
| Configuration Item | Type | Default Value | Description |
|---------------------|------|---------------|-------------|
| `cloudskill.sdk.enabled` | Boolean | `true` | Whether to enable Cloud Skill SDK |
| `cloudskill.sdk.server-url` | String | `http://localhost:8080` | MCP server access address |
| `cloudskill.sdk.api-key` | String | - | API authentication key, required |
| `cloudskill.sdk.service-name` | String | `${spring.application.name}` | Current service name, required for service registration |
| `cloudskill.sdk.service-version` | String | `1.0.0` | Current service version number |
| `cloudskill.sdk.service-ip` | String | Auto-detected | Service IP address, automatically recognized by default |
| `cloudskill.sdk.service-port` | Integer | `${server.port}` | Service port, defaults to server.port |
| `cloudskill.sdk.auto-sync` | Boolean | `true` | Whether to automatically synchronize skill lists |
| `cloudskill.sdk.sync-interval` | Integer | `30` | Full synchronization interval in seconds |
| `cloudskill.sdk.enable-web-socket` | Boolean | `true` | Whether to enable WebSocket real-time push |
| `cloudskill.sdk.reconnect-interval` | Integer | `5` | WebSocket reconnection interval in seconds |
| `cloudskill.sdk.call-timeout` | Integer | `30000` | Skill call timeout in milliseconds |
| `cloudskill.sdk.retry-count` | Integer | `3` | Number of retries on call failure |
| `cloudskill.sdk.enable-local-cache` | Boolean | `true` | Whether to enable local memory cache |
| `cloudskill.sdk.cache-expire-time` | Long | `3600` | Local cache expiration time in seconds |
| `cloudskill.sdk.enable-service-registry` | Boolean | `false` | Whether to enable service registration and discovery |
| `cloudskill.sdk.heartbeat-interval` | Integer | `30` | Heartbeat reporting interval in seconds |
| `cloudskill.sdk.enable-agent-integration` | Boolean | `true` | Whether to enable Spring AI Agent integration |
| `cloudskill.sdk.dynamic-skills.mode` | Enum | `GLOBAL` | Dynamic skill mode: OFF/GLOBAL/ANNOTATION |
| `cloudskill.sdk.dynamic-skills.order` | Integer | `2147483547` | AOP Advisor execution order |
| `cloudskill.sdk.debug` | Boolean | `false` | Whether to enable debug mode for detailed log output |

### Mode Configuration Description
| Mode | Applicable Scenarios | Advantages | Disadvantages |
|------|----------------------|------------|---------------|
| OFF | Environments without skill functionality | No performance overhead | Cannot use dynamic skills |
| GLOBAL | Small applications, test environments | Easy to use, automatically injects all skills | Coarse permission control, potential performance overhead |
| ANNOTATION | Medium/large applications, production environments | Fine-grained control, optimal performance | Requires adding annotations at usage points |
| PROXY | Scenarios requiring fully transparent integration | Completely non-intrusive, no business code modification | Relatively complex configuration |

---

## Best Practices

### Deployment Recommendations
1. **Environment Isolation**: Use independent MCP clusters and API Keys for development, testing, and production environments
2. **Network Optimization**: Use intranet communication between SDK and server to reduce latency and network fluctuation impacts
3. **High Availability Deployment**: Deploy MCP server in cluster with load balancer in front
4. **Configuration Management**: Use configuration center or environment variables for sensitive configurations (e.g., API Key) to avoid hardcoding
5. **Gray Release**: Gradually roll out new SDK versions to a small subset of applications first, then full rollout after verification

### Performance Optimization
1. **Cache Configuration**: Extend cache time to 1 hour or more for infrequently changing skills
2. **Batch Calls**: Merge multiple skill calls where possible to reduce network overhead
3. **Asynchronous Calls**: Use asynchronous calls for non-real-time scenarios to improve interface response speed
4. **Resource Isolation**: Execute important skills and ordinary skills in separate thread pools to avoid mutual interference
5. **Degradation Strategy**: Configure degradation logic for core business scenarios to return fallback results when skill calls fail

### Security Specifications
1. **Minimum Privilege Principle**: Assign only the minimum necessary skill permissions to each service to avoid over-authorization
2. **Parameter Verification**: Perform secondary parameter verification on the server side for sensitive skills to prevent injection attacks
3. **Data Desensitization**: Automatically desensitize sensitive information (e.g., phone numbers, ID numbers) in skill return results
4. **Audit Logging**: Enable detailed audit logs for important skill calls, retain for at least 6 months
5. **Access Control**: Configure IP whitelists on MCP server to allow only trusted service addresses to access

### Operations & Monitoring
1. **Core Metrics Monitoring**:
   - Skill call success rate (Target: >99.9%)
   - Average skill call response time (Target: <1s)
   - Skill synchronization success rate
   - WebSocket connection status
2. **Alert Configuration**:
   - Alert when call success rate drops below 99%
   - Alert when average response time exceeds 3s
   - Alert when WebSocket reconnection exceeds 3 times
   - Alert when skill synchronization fails
3. **Logging Specifications**:
   - Disable debug logs in production environment to avoid performance loss
   - Error logs should include complete request ID, skill ID, and error information
   - Log output should follow company unified specifications for easy collection and analysis

---

## Troubleshooting

### Common Issues

#### Q: Skill list is empty after application startup?
**Troubleshooting Steps:**
1. Verify that the API Key is correct and has permission to access skills
2. Check if skills have been assigned to this API Key on the server side
3. Ensure network connectivity to MCP server's `/cloud-skill/v1/skills` endpoint
4. Enable debug mode to view detailed error information in synchronization logs
5. Verify that skills returned by the server meet permission conditions (public or assigned to current service)

#### Q: WebSocket connections disconnect frequently?
**Troubleshooting Steps:**
1. Check if server WebSocket port is open and allowed by security groups
2. Confirm no firewall or proxy in the network environment is actively dropping long connections
3. Adjust `reconnect-interval` parameter to shorten reconnection interval appropriately
4. Investigate if server has performance issues causing active connection drops
5. Consider configuring long connection timeout to 300s or higher at load balancer level

#### Q: Dynamic skills are not automatically injected?
**Troubleshooting Steps:**
1. Check if `dynamic-skills.mode` is set to GLOBAL or ANNOTATION
2. In annotation mode, confirm target method or class is annotated with `@EnableDynamicSkills`
3. Ensure Spring AI dependencies are included in the project and ChatModel is a Spring-managed bean
4. Check if other AOP aspects have higher priority, adjust `dynamic-skills.order` to change execution order
5. View startup logs to confirm `DynamicSkillsAdvisor` was successfully initialized

#### Q: Skill calls frequently time out?
**Optimization Solutions:**
1. Increase `call-timeout` configuration appropriately based on actual skill execution time
2. Investigate server performance bottlenecks and optimize skill execution efficiency
3. Use asynchronous call patterns for long-running skills
4. Check network bandwidth and latency, consider deploying MCP server closer to applications
5. Enable retry mechanism by adjusting `retry-count` configuration

#### Q: Performance degradation under high concurrency?
**Optimization Solutions:**
1. Enable local cache and extend cache expiration time
2. Adjust synchronization strategy to reduce full synchronization frequency
3. Disable unnecessary debug logs and audit logs
4. Consider using external process cache (e.g., Redis) to share skill data
5. Upgrade to latest version for 30%+ performance improvement

### Log Troubleshooting
Enable debug mode to view detailed logs:
```yaml
cloudskill:
  sdk:
    debug: true

logging:
  level:
    com.cloudskill.sdk: debug
```

---

## Community & Support

### Versioning Scheme
- **Stable Release**: Version number x.y.z, fully tested, recommended for production use
- **Milestone Release**: Version number x.y.z-Mn, includes new features, for preview and testing
- **Snapshot Release**: Version number x.y.z-SNAPSHOT, development version, not recommended for production use

### Contributing Guidelines
1. Fork this repository to your own account
2. Create feature branch: `git checkout -b feature/your-feature`
3. Commit changes: `git commit -am 'Add some feature'`
4. Push to branch: `git push origin feature/your-feature`
5. Submit Pull Request

### Code Specifications
- Follow Alibaba Java Development Guidelines
- All public methods must have complete Javadoc comments
- All unit tests and code style checks must pass before submission
- New features must include corresponding unit test cases

### Contact Information
- Project Homepage: [https://github.com/cloud-skill/cloud-skill-spring-boot-starter](https://github.com/cloud-skill/cloud-skill-spring-boot-starter)
- Issue Reporting: [Submit Issue](https://github.com/cloud-skill/cloud-skill-spring-boot-starter/issues)
- Mailing List: dev@cloudskill.com
- Enterprise Support: enterprise@cloudskill.com

---

## License
This project is licensed under the [Apache License 2.0](LICENSE), which permits free use, modification, and distribution.

---

<p align="center">
  <sub>© 2024 Cloud Skill Team. All rights reserved.</sub>
</p>