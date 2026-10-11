/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.agentscope.claw2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the local agentscope-claw assistant.
 *
 * <p>Starts a Spring Boot WebFlux server that hosts:
 *
 * <ul>
 *   <li>{@code /api/agents/**} — agent catalog, per-agent chat (SSE), workspace CRUD,
 *       sessions inbox, channel bindings
 *   <li>{@code /api/channels} — registered channels directory
 *   <li>{@code /api/templates/**} — bundled starter templates
 *   <li>{@code /**} — React SPA static assets with SPA fallback to {@code index.html}
 * </ul>
 *
 * <p>Designed for single-user local deployment. No authentication, no multi-user
 * isolation. All persistent state lives under {@code ${claw.home}} (default
 * {@code ~/.agentscope/claw}).
 */
/**
 * 【学习注释：本地单用户 Agent 工作台入口】
 *
 * <p>PAW/Claw2 演示的是“个人 Agent 工作台”形态：一个本地 WebFlux 服务同时承载 Agent 管理、
 * 工作区编辑、SSE 聊天和前端静态资源。与 DataAgent 的多租户方案不同，它省略认证与用户隔离，
 * 将数据统一放在 {@code claw.home}。
 *
 * <pre>
 * React SPA ──► /api/agents/** ──► ChatController / Catalog / Workspace API
 *                                      │
 *                                      ▼
 *                               ClawBootstrap ──► HarnessAgent
 * </pre>
 *
 * <p>它适合学习“本地优先”部署：先读此入口，再读 ClawBootstrap 和 ChatController，最后比较
 * DataAgent 对认证、多用户会话所增加的层次。
 */
@SpringBootApplication
public class Claw2App {
    public static void main(String[] args) {
        SpringApplication.run(Claw2App.class, args);
    }
}
