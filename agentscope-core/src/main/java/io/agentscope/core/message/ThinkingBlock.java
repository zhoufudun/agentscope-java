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
 * 【核心概念】ThinkingBlock - LLM 思考过程
 * ============================================================================
 *
 * 一、这是什么？
 * -------------
 * ThinkingBlock 记录 LLM 的内部思考过程。
 * 它让代理的决策过程变得透明可见。
 *
 * 二、为什么需要？
 * ---------------
 * 现代 LLM（如 Claude）支持"Extended Thinking"（扩展思考）。
 * LLM 会先思考，再给出答案，这提高了复杂问题的解决能力。
 *
 *   没有思考:
 *   Q: "1+1=?" → A: "2"
 *
 *   有思考:
 *   Q: "1+1=?"
 *   Thinking: "这是一个简单的加法运算，1+1=2"
 *   A: "2"
 *
 * 三、在 ReAct 循环中的作用
 * ------------------------
 *   ┌─────────────────────────────────────────────────┐
 *   │  用户: "北京天气如何？"                          │
 *   │          ↓                                     │
 *   │  LLM 回复:                                      │
 *   │    ThinkingBlock("用户问天气，我需要调用       │
 *   │                   get_weather 工具查询北京")   │
 *   │    ToolUseBlock("get_weather", {"city":"北京"})│
 *   │          ↓                                     │
 *   │  [执行工具，获取结果]                           │
 *   │          ↓                                     │
 *   │  LLM 回复:                                      │
 *   │    ThinkingBlock("工具返回了天气数据，         │
 *   │                   我来整理给用户")             │
 *   │    TextBlock("北京今天晴天，气温25°C")         │
 *   └─────────────────────────────────────────────────┘
 *
 * 四、创建方式
 * -----------
 *   ThinkingBlock block = ThinkingBlock.builder()
 *       .thinking("我正在分析用户的问题...")
 *       .build();
 *
 * 五、流式输出
 * -----------
 *   思考过程可以流式输出：
 *   - ThinkingBlockStartEvent  — 开始思考
 *   - ThinkingBlockDeltaEvent  — 思考内容增量
 *   - ThinkingBlockEndEvent    — 思考结束
 *
 *   这让用户可以实时看到 LLM "在想什么"。
 *
 * 六、metadata 用途
 * -----------------
 *   不同 LLM 提供商有特殊的推理详情：
 *   - OpenRouter: reasoning_details
 *   - Gemini: reasoningDetails
 *
 *   这些信息存在 metadata 中，框架需要在后续调用中传回。
 *
 * ============================================================================
 */

package io.agentscope.core.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents reasoning or thinking content in a message.
 *
 * <p>This content block is used to capture the internal reasoning process of an agent before
 * taking action. It provides transparency into how the agent arrived at its decisions or tool
 * choices.
 *
 * <p>Thinking blocks are particularly useful in ReAct agents and other reasoning-intensive systems
 * where understanding the agent's thought process is valuable for debugging and analysis.
 *
 * <p>The optional metadata field can store additional reasoning information such as OpenRouter's
 * reasoning_details (reasoning.text, reasoning.encrypted, reasoning.summary) that need to be
 * preserved and restored when formatting messages back to the API.
 */
public final class ThinkingBlock extends ContentBlock {

    /** Metadata key for storing OpenRouter/Gemini reasoning details list. */
    public static final String METADATA_REASONING_DETAILS = "reasoningDetails";

    private final String thinking;
    private final Map<String, Object> metadata;

    /**
     * Creates a new thinking block for JSON deserialization.
     *
     * @param text The thinking content (null will be converted to empty string)
     * @param metadata Optional metadata for storing additional reasoning information
     */
    @JsonCreator
    private ThinkingBlock(
            @JsonProperty("thinking") String text,
            @JsonProperty("metadata") Map<String, Object> metadata) {
        this.thinking = text != null ? text : "";
        this.metadata = metadata != null ? new HashMap<>(metadata) : null;
    }

    /**
     * Gets the thinking/reasoning content of this block.
     *
     * @return The thinking content
     */
    public String getThinking() {
        return thinking;
    }

    /**
     * Gets the metadata associated with this thinking block.
     *
     * <p>Metadata can contain additional reasoning information such as:
     *
     * <ul>
     *   <li>{@link #METADATA_REASONING_DETAILS} - List of OpenAIReasoningDetail objects from
     *       OpenRouter/Gemini
     * </ul>
     *
     * @return The metadata map, or null if no metadata is set
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    /**
     * Creates a new builder for constructing ThinkingBlock instances.
     *
     * @return A new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /** Builder for constructing ThinkingBlock instances. */
    public static class Builder {

        private String thinking;
        private Map<String, Object> metadata;

        /**
         * Sets the thinking content for the block.
         *
         * @param thinking The thinking content
         * @return This builder for chaining
         */
        public Builder thinking(String thinking) {
            this.thinking = thinking;
            return this;
        }

        /**
         * Sets the metadata for the block.
         *
         * <p>Metadata can store additional reasoning information that needs to be preserved, such
         * as OpenRouter's reasoning_details.
         *
         * @param metadata The metadata map
         * @return This builder for chaining
         */
        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        /**
         * Builds a new ThinkingBlock with the configured thinking content and metadata.
         *
         * @return A new ThinkingBlock instance (null thinking will be converted to empty string)
         */
        public ThinkingBlock build() {
            return new ThinkingBlock(thinking != null ? thinking : "", metadata);
        }
    }
}
