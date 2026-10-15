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
 * 【核心概念】代理结果事件 - AgentResultEvent
 * ============================================================================
 *
 * 一、这是什么？
 * -------------
 * 代理完成处理后，携带最终结果的事件。
 * 这是整个事件流中最重要的事件之一，包含完整的回复消息。
 *
 * 二、两种获取结果的方式
 * ---------------------
 *
 *   方式 1: 使用 call() (阻塞)
 *   ──────────────────────────
 *   Msg result = agent.call("你好").block();
 *   System.out.println(result.getTextContent());
 *   // 内部通过 AgentResultEvent 提取结果
 *
 *   方式 2: 使用 streamEvents() (流式)
 *   ──────────────────────────────────
 *   agent.streamEvents("你好")
 *       .doOnNext(event -> {
 *           if (event instanceof AgentResultEvent e) {
 *               Msg result = e.getResult();
 *               System.out.println("最终结果: " + result.getTextContent());
 *           }
 *       })
 *       .blockLast();
 *
 * 三、Msg 消息结构
 * ---------------
 *   result.getRole()        → ASSISTANT
 *   result.getTextContent() → "你好！有什么可以帮助你的？"
 *   result.getContent()     → [TextBlock, ToolUseBlock, ...]
 *   result.getGenerateReason() → END_TURN / TOOL_USE / MAX_ITERS / ...
 *
 * 四、GenerateReason（生成原因）
 * ----------------------------
 *   END_TURN          — 正常完成
 *   TOOL_USE          — 需要执行工具
 *   MAX_ITERS         — 达到最大迭代次数
 *   USER_INTERRUPTED  — 用户中断
 *   PERMISSION_ASKING — 等待权限确认
 *   MIDDLEWARE_STOP   — 中间件请求停止
 *
 * 五、事件位置
 * -----------
 *   ...
 *   TextBlockEndEvent
 *   AgentResultEvent   ← 在这里，AgentEndEvent 之前
 *   AgentEndEvent
 *
 * 六、与增量事件的关系
 * ------------------
 *   TextBlockDeltaEvent 是流式的"片段"
 *   AgentResultEvent 包含"完整消息"（所有片段已合并）
 *
 *   如果只关心最终结果，过滤 AgentResultEvent 即可
 *   如果需要实时显示，监听 TextBlockDeltaEvent
 *
 * ============================================================================
 */

package io.agentscope.core.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.agentscope.core.message.Msg;

/**
 * Emitted when an agent successfully finishes processing an invocation, carrying the final result
 * message.
 *
 * <p>This event is emitted as part of the agent event stream (both {@code call()} and
 * {@code streamEvents()} paths) immediately before {@link AgentEndEvent}. Callers of
 * {@code streamEvents()} can filter for this event to obtain the final {@link Msg} directly
 * from the event stream without subscribing to the {@code Mono<Msg>} return value separately.
 *
 * <p>{@code call()} internally uses this event to extract the result from the shared
 * {@code buildAgentStream()} core, ensuring both paths run through the same {@code onAgent}
 * middleware chain.
 */
public class AgentResultEvent extends AgentEvent {

    private final Msg result;

    public AgentResultEvent(Msg result) {
        this.result = result;
    }

    @JsonCreator
    public AgentResultEvent(
            @JsonProperty("id") String id,
            @JsonProperty("createdAt") String createdAt,
            @JsonProperty("result") Msg result) {
        super(id, createdAt);
        this.result = result;
    }

    @Override
    public AgentEventType getType() {
        return AgentEventType.AGENT_RESULT;
    }

    public Msg getResult() {
        return result;
    }
}
