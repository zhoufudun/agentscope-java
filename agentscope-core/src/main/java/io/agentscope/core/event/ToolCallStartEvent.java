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
 * 【核心概念】工具调用开始事件 - ToolCallStartEvent
 * ============================================================================
 *
 * 一、这是什么？
 * -------------
 * 当 LLM 决定调用一个工具时，会触发 ToolCallStartEvent。
 * 这是 ReAct 模式中 "Acting" 阶段的开始标志。
 *
 * 二、ReAct 模式回顾
 * -----------------
 *   用户: "今天北京天气怎么样？"
 *      │
 *      ▼
 *   Reasoning: LLM 思考 → "我需要调用天气工具"
 *      │
 *      ▼
 *   Acting: 执行工具
 *      ├── ToolCallStartEvent ← 你在这里
 *      ├── (实际执行工具)
 *      └── ToolCallEndEvent
 *      │
 *      ▼
 *   Reasoning: LLM 根据工具结果生成回复
 *      │
 *      ▼
 *   "北京今天晴，25°C..."
 *
 * 三、关键字段
 * -----------
 *   replyId      — 本次回复的唯一标识
 *   toolCallId   — 工具调用的唯一标识（用于匹配 ToolCallEndEvent）
 *   toolCallName — 工具名称（如 "get_weather", "execute_code"）
 *
 * 四、使用示例
 * -----------
 *   agent.streamEvents(userMsg)
 *       .doOnNext(event -> {
 *           if (event instanceof ToolCallStartEvent e) {
 *               System.out.println("🔧 调用工具: " + e.getToolCallName());
 *           }
 *       })
 *       .blockLast();
 *
 * 五、完整工具事件链
 * -----------------
 *   ToolCallStartEvent      → 工具开始执行
 *   ToolResultTextDeltaEvent → 工具输出（流式，可选）
 *   ToolCallEndEvent        → 工具执行完成（含最终结果）
 *
 * 六、实际应用
 * -----------
 *   - 前端显示 "正在执行: get_weather..."
 *   - 日志记录工具调用链
 *   - 监控工具调用频率
 *   - 调试工具执行顺序
 *
 * ============================================================================
 */

package io.agentscope.core.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ToolCallStartEvent extends AgentEvent {

    private final String replyId;
    private final String toolCallId;
    private final String toolCallName;

    @JsonCreator
    public ToolCallStartEvent(
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

    public ToolCallStartEvent(String replyId, String toolCallId, String toolCallName) {
        this.replyId = replyId;
        this.toolCallId = toolCallId;
        this.toolCallName = toolCallName;
    }

    @Override
    public AgentEventType getType() {
        return AgentEventType.TOOL_CALL_START;
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
