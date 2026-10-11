/*
 * Copyright 2024-2026 the original author or authors.
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
package io.agentscope.examples.agui;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AG-UI Example Application.
 *
 * <p>This application demonstrates how to expose AgentScope agents via the AG-UI protocol
 * using Spring WebFlux.
 *
 * <p><b>Usage:</b>
 * <ol>
 *   <li>Set the DASHSCOPE_API_KEY environment variable</li>
 *   <li>Run this application</li>
 *   <li>Open http://localhost:8080 in a browser</li>
 *   <li>Or use curl: curl -X POST http://localhost:8080/agui/run -H "Content-Type: application/json" -d '{"threadId":"test","runId":"1","messages":[{"id":"m1","role":"user","content":"Hello!"}]}'</li>
 * </ol>
 */
/**
 * 【学习注释：AG-UI 服务的最小启动入口】
 *
 * <p>此类本身不创建 Agent；Spring Boot 启动后会扫描配置类，并由 AG-UI Starter 暴露
 * {@code /agui/run} 流式接口。请求所带的 {@code threadId} 是客户端会话标识，
 * {@code runId} 是本次执行标识；前端据此把连续事件归并为同一轮对话。
 *
 * <pre>
 * 浏览器 / AG-UI 前端
 *        │ POST /agui/run
 *        ▼
 * Spring WebFlux + AG-UI Starter ──► AgentConfiguration 中注册的 Agent 工厂
 *        │                                  │
 *        └──────── SSE / 事件流 ◄────────── ReActAgent.streamEvents()
 * </pre>
 *
 * <p>学习顺序：先运行此入口观察协议事件，再阅读 {@code AgentConfiguration}，理解
 * 如何按 agentId 构建不同 Agent，以及如何把 AgentScope 的事件转成 AG-UI 事件。
 */
@SpringBootApplication
public class AguiExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(AguiExampleApplication.class, args);
        printStartupInfo();
    }

    private static void printStartupInfo() {
        System.out.println("\n=== AG-UI Example Application Started ===");
        System.out.println("Open: http://localhost:8080");
        System.out.println("API:  POST http://localhost:8080/agui/run");
        System.out.println("\nExample curl command:");
        System.out.println("  curl -N -X POST http://localhost:8080/agui/run \\");
        System.out.println("    -H \"Content-Type: application/json\" \\");
        System.out.println(
                "    -d"
                    + " '{\"threadId\":\"test\",\"runId\":\"1\",\"messages\":[{\"id\":\"m1\",\"role\":\"user\",\"content\":\"Hello!\"}]}'");
        System.out.println("\nPress Ctrl+C to stop.");
    }
}
