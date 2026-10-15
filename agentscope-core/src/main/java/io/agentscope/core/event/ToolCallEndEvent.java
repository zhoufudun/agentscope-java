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
 * 【核心概念】工具调用结束事件 - ToolCallEndEvent
 * ============================================================================
 *
 * 一、这是什么？
 * -------------
 * 工具执行完成后触发的事件。与 ToolCallStartEvent 配对使用。
 *
 * 二、工具执行完整流程
 * -------------------
 *
 *   LLM 决定调用 get_weather(city="北京")
 *          │
 *          ▼
 *   ┌─────────────────────────────────────────┐
 *   │  ToolCallStartEvent                     │
 *   │    toolCallId: "tc_001"                 │
 *   │    toolCallName: "get_weather"          │
 *   └─────────────────────────────────────────┘
 *          │
 *          ▼ (执行工具，可能耗时)
 *   ┌─────────────────────────────────────────┐
 *   │  ToolResultTextDeltaEvent (可选，流式)  │
 *   │    delta: "正在查询..."                 │
 *   └─────────────────────────────────────────┘
 *          │
 *          ▼
 *   ┌─────────────────────────────────────────┐
 *   │  ToolCallEndEvent                       │
 *   │    toolCallId: "tc_001"                 │
 *   │    toolCallName: "get_weather"          │
 *   └─────────────────────────────────────────┘
 *          │
 *          ▼
 *   工具结果进入上下文 → LLM 继续推理
 *
 * 三、关键字段
 * -----------
 *   replyId      — 回复标识
 *   toolCallId   — 工具调用标识（与 StartEvent 匹配）
 *   toolCallName — 工具名称
 *
 * 四、使用示例：计算工具耗时
 * -------------------------
 *   Map<String, Long> startTimes = new HashMap<>();
 *
 *   agent.streamEvents(userMsg)
 *       .doOnNext(event -> {
 *           if (event instanceof ToolCallStartEvent e) {
 *               startTimes.put(e.getToolCallId(), System.currentTimeMillis());
 *               System.out.println("🔧 开始: " + e.getToolCallName());
 *           }
 *           if (event instanceof ToolCallEndEvent e) {
 *               long start = startTimes.getOrDefault(e.getToolCallId(), 0L);
 *               long elapsed = System.currentTimeMillis() - start;
 *               System.out.println("✅ 完成: " + e.getToolCallName()
 *                   + " (" + elapsed + "ms)");
 *           }
 *       })
 *       .blockLast();
 *
 * 五、注意事项
 * -----------
 *   - 如果工具执行失败，仍会触发 ToolCallEndEvent
 *   - 错误信息在工具结果中，不在此事件中
 *   - 可用于统计工具调用频率和耗时
 *
 * ============================================================================
 */

package io.agentscope.core.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ToolCallEndEvent extends AgentEvent {

    private final String replyId;
    private final String toolCallId;
    private final String toolCallName;

    @JsonCreator
    public ToolCallEndEvent(
            @JsonProperty("id") String id,
            @JsonProperty("createdAt") String createdAt,
            @JsonProperty("replyId") String replyId,
            @JsonProperty("toolCallId") String toolCallId,
            @JsonProperty("toolCallName") String toolCallName) {
        super(id, createdAt);
        this.replyId = replyId;
        this.toolCallId = toolCallId;
        this.toolCallName = toolCallName;
    }

    public ToolCallEndEvent(String replyId, String toolCallId, String toolCallName) {
        this.replyId = replyId;
        this.toolCallId = toolCallId;
        this.toolCallName = toolCallName;
    }

    @Override
    public AgentEventType getType() {
        return AgentEventType.TOOL_CALL_END;
    }

    public String getReplyId() {
        return replyId;
    }

    public String getToolCallId() {
        return toolCallId;
    }

    public String getToolCallName() {
        return toolCallName;
    }
}
