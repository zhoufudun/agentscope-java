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
 * 【核心概念】文本增量事件 - TextBlockDeltaEvent
 * ============================================================================
 *
 * 一、这是什么？
 * -------------
 * 当 LLM 流式输出文字时，每产生一小段文字就会触发一个 TextBlockDeltaEvent。
 * 把所有 delta 拼接起来，就是 LLM 的完整回复。
 *
 * 二、为什么需要流式？
 * ------------------
 * 传统方式：用户等 10 秒 → 一次性看到全部回复
 * 流式方式：用户实时看到文字一个字一个字出现 → 体验像打字机
 *
 *   LLM API
 *      │ SSE 流式返回
 *      ▼
 *   "Hello" → TextBlockDeltaEvent(delta="Hello")
 *   ", "    → TextBlockDeltaEvent(delta=", ")
 *   "world" → TextBlockDeltaEvent(delta="world")
 *   "!"     → TextBlockDeltaEvent(delta="!")
 *      │
 *      ▼
 *   前端实时拼接显示: "Hello, world!"
 *
 * 三、关键字段
 * -----------
 *   replyId  — 本次回复的唯一标识（一次 call() 可能有多次回复）
 *   blockId  — 文本块标识（一个回复可能有多个文本块）
 *   delta    — 本次增量文本内容（可能是一个字、一个词、一句话）
 *
 * 四、使用示例
 * -----------
 *   agent.streamEvents(userMsg)
 *       .doOnNext(event -> {
 *           if (event instanceof TextBlockDeltaEvent e) {
 *               System.out.print(e.getDelta());  // 实时打印，不换行
 *           }
 *       })
 *       .blockLast();
 *
 * 五、事件生命周期
 * ---------------
 *   TextBlockStartEvent  → 文本块开始
 *   TextBlockDeltaEvent  → 增量内容（多次）
 *   TextBlockDeltaEvent  → 增量内容
 *   TextBlockEndEvent    → 文本块结束
 *
 * 六、与 ThinkingBlockDeltaEvent 的区别
 * ------------------------------------
 *   TextBlockDeltaEvent     — LLM 的最终输出（给用户看的）
 *   ThinkingBlockDeltaEvent — LLM 的思考过程（调试/透明度用）
 *
 * ============================================================================
 */

package io.agentscope.core.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TextBlockDeltaEvent extends AgentEvent {

    private final String replyId;
    private final String blockId;
    private final String delta;

    @JsonCreator
    public TextBlockDeltaEvent(
            @JsonProperty("id") String id,
            @JsonProperty("createdAt") String createdAt,
            @JsonProperty("replyId") String replyId,
            @JsonProperty("blockId") String blockId,
            @JsonProperty("delta") String delta) {
        super(id, createdAt);
        this.replyId = replyId;
        this.blockId = blockId;
        this.delta = delta;
    }

    public TextBlockDeltaEvent(String replyId, String blockId, String delta) {
        this.replyId = replyId;
        this.blockId = blockId;
        this.delta = delta;
    }

    @Override
    public AgentEventType getType() {
        return AgentEventType.TEXT_BLOCK_DELTA;
    }

    public String getReplyId() {
        return replyId;
    }

    public String getBlockId() {
        return blockId;
    }

    public String getDelta() {
        return delta;
    }
}
