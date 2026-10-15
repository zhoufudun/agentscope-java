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

/*
 * ============================================================================
 * 【核心概念】代理开始事件 - AgentStartEvent
 * ============================================================================
 *
 * 一、这是什么？
 * -------------
 * 代理开始处理用户请求时触发的第一个事件。
 * 相当于"开工通知"，告诉前端代理已经收到请求开始工作了。
 *
 * 二、事件流完整生命周期
 * ---------------------
 *   agent.call("你好")
 *         │
 *         ▼
 *   ┌─────────────────────────────────────────────────┐
 *   │  AgentStartEvent         ← 代理开始工作         │
 *   │       │                                         │
 *   │       ▼                                         │
 *   │  TextBlockStartEvent     ← 文本块开始           │
 *   │  TextBlockDeltaEvent     ← 增量文本 "你"        │
 *   │  TextBlockDeltaEvent     ← 增量文本 "好"        │
 *   │  TextBlockEndEvent       ← 文本块结束           │
 *   │       │                                         │
 *   │       ▼ (如果有工具调用)                        │
 *   │  ToolCallStartEvent      ← 工具开始             │
 *   │  ToolCallEndEvent        ← 工具结束             │
 *   │       │                                         │
 *   │       ▼                                         │
 *   │  AgentResultEvent        ← 最终结果             │
 *   │  AgentEndEvent           ← 代理结束工作         │
 *   └─────────────────────────────────────────────────┘
 *
 * 三、关键字段
 * -----------
 *   sessionId — 会话标识（多轮对话共享同一 sessionId）
 *   replyId   — 本次回复标识（每次 call() 生成新的 replyId）
 *   name      — 代理名称（如 "assistant", "coding-agent"）
 *   role      — 角色（通常是 "assistant"）
 *
 * 四、使用示例
 * -----------
 *   agent.streamEvents(userMsg)
 *       .doOnNext(event -> {
 *           if (event instanceof AgentStartEvent e) {
 *               System.out.println("🤖 " + e.getName() + " 开始处理...");
 *               // 前端可以显示 loading 动画
 *           }
 *       })
 *       .blockLast();
 *
 * 五、sessionId vs replyId
 * ------------------------
 *   sessionId: 整个会话的标识，多轮对话共享
 *     call("你好")     → sessionId = "s1", replyId = "r1"
 *     call("天气如何") → sessionId = "s1", replyId = "r2"
 *     call("谢谢")     → sessionId = "s1", replyId = "r3"
 *
 *   replyId: 单次回复的标识，用于关联同一回复的所有事件
 *
 * ============================================================================
 */

package io.agentscope.core.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Emitted when an agent begins processing an invocation.
 */
public class AgentStartEvent extends AgentEvent {

    private final String sessionId;
    private final String replyId;
    private final String name;
    private final String role;

    @JsonCreator
    public AgentStartEvent(
            @JsonProperty("id") String id,
            @JsonProperty("createdAt") String createdAt,
            @JsonProperty("sessionId") String sessionId,
            @JsonProperty("replyId") String replyId,
            @JsonProperty("name") String name,
            @JsonProperty("role") String role) {
        super(id, createdAt);
        this.sessionId = sessionId;
        this.replyId = replyId;
        this.name = name;
        this.role = role != null ? role : "assistant";
    }

    public AgentStartEvent(String sessionId, String replyId, String name) {
        this.sessionId = sessionId;
        this.replyId = replyId;
        this.name = name;
        this.role = "assistant";
    }

    @Override
    public AgentEventType getType() {
        return AgentEventType.AGENT_START;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getReplyId() {
        return replyId;
    }

    public String getName() {
        return name;
    }

    public String getRole() {
        return role;
    }
}
