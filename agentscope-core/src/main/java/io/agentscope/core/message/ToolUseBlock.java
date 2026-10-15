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
 * 【核心概念】ToolUseBlock - LLM 的工具调用请求
 * ============================================================================
 *
 * 一、这是什么？
 * -------------
 * 当 LLM 决定调用一个工具时，它返回一个 ToolUseBlock。
 * 这是"请求"，不是"结果"。
 *
 * 二、工具调用流程
 * ---------------
 *   用户: "北京今天天气如何？"
 *        │
 *        ▼
 *   LLM 返回 AssistantMessage {
 *     content: [
 *       ToolUseBlock {          ← LLM 说"我要调用这个工具"
 *         id: "tc_001",
 *         name: "get_weather",
 *         input: {"city": "北京"}
 *       }
 *     ]
 *   }
 *        │
 *        ▼
 *   框架执行 get_weather("北京")
 *        │
 *        ▼
 *   返回 ToolResultBlock      ← 工具执行结果
 *        │
 *        ▼
 *   LLM 根据结果生成最终回复
 *
 * 三、关键字段
 * -----------
 *   id      — 工具调用唯一标识（用于匹配 ToolResultBlock）
 *   name    — 工具名称（如 "get_weather"）
 *   input   — 工具参数（Map<String, Object>）
 *   state   — 调用状态（PENDING, EXECUTING, COMPLETED, DENIED, SUSPENDED）
 *
 * 四、状态流转
 * -----------
 *   PENDING    → LLM 刚返回请求
 *   EXECUTING  → 框架开始执行工具
 *   COMPLETED  → 工具执行完成
 *   DENIED     → 权限检查拒绝
 *   SUSPENDED  → 等待外部执行（外部工具）
 *
 * 五、与 HITL 的关系
 * -----------------
 *   当权限为 ASK 时：
 *   1. 框架发出 RequireUserConfirmEvent，包含 ToolUseBlock 列表
 *   2. 用户在 ConfirmResult 中可以修改 ToolUseBlock（如改参数）
 *   3. 框架用修改后的 ToolUseBlock 执行
 *
 *   ToolUseBlock modified = original.builder()
 *       .input(Map.of("city", "上海"))  // 用户改成上海
 *       .build();
 *   new ConfirmResult(true, modified);
 *
 * 六、metadata 用途
 * -----------------
 *   不同 LLM 提供商可能有特殊元数据：
 *   - Gemini: thoughtSignature（思考签名）
 *   - 其他: 可扩展
 *
 * ============================================================================
 */

package io.agentscope.core.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a tool use request within a message.
 *
 * <p>This content block is used when an agent requests to execute a tool.
 * It contains the tool's unique identifier, name, input parameters, and optionally
 * the raw content for streaming tool calls.
 *
 * <p>The tool input is stored as a generic map of string keys to object values,
 * allowing for flexible parameter passing to different tool implementations.
 */
public final class ToolUseBlock extends ContentBlock {

    /** Metadata key for Gemini thought signature (byte[] value). */
    public static final String METADATA_THOUGHT_SIGNATURE = "thoughtSignature";

    private final String id;
    private final String name;
    private final Map<String, Object> input;
    private final String content; // Raw content for streaming tool calls
    private final Map<String, Object> metadata; // Provider-specific metadata
    private final ToolCallState state;

    /**
     * Creates a new tool use block for JSON deserialization.
     *
     * @param id Unique identifier for this tool call
     * @param name Name of the tool to execute
     * @param input Input parameters for the tool (will be defensively copied)
     * @param metadata Provider-specific metadata (will be defensively copied)
     */
    public ToolUseBlock(
            String id, String name, Map<String, Object> input, Map<String, Object> metadata) {
        this(id, name, input, null, metadata, null);
    }

    /**
     * Creates a new tool use block without metadata (convenience constructor).
     *
     * @param id Unique identifier for this tool call
     * @param name Name of the tool to execute
     * @param input Input parameters for the tool (will be defensively copied)
     */
    public ToolUseBlock(String id, String name, Map<String, Object> input) {
        this(id, name, input, null, null, null);
    }

    /**
     * Creates a new tool use block with raw content for streaming.
     *
     * @param id Unique identifier for this tool call
     * @param name Name of the tool to execute
     * @param input Input parameters for the tool (will be defensively copied)
     * @param content Raw content for streaming tool calls
     * @param metadata Provider-specific metadata (will be defensively copied)
     */
    public ToolUseBlock(
            String id,
            String name,
            Map<String, Object> input,
            String content,
            Map<String, Object> metadata) {
        this(id, name, input, content, metadata, null);
    }

    /**
     * Creates a new tool use block with all fields.
     *
     * @param id Unique identifier for this tool call
     * @param name Name of the tool to execute
     * @param input Input parameters for the tool (will be defensively copied)
     * @param content Raw content for streaming tool calls
     * @param metadata Provider-specific metadata (will be defensively copied)
     * @param state The tool call state, defaults to PENDING if null
     */
    @JsonCreator
    public ToolUseBlock(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("input") Map<String, Object> input,
            @JsonProperty("content") String content,
            @JsonProperty("metadata") Map<String, Object> metadata,
            @JsonProperty("state") ToolCallState state) {
        this.id = id;
        this.name = name;
        // Defensive copy to prevent external modifications
        this.input =
                input == null
                        ? Collections.emptyMap()
                        : Collections.unmodifiableMap(new HashMap<>(input));
        this.content = content;
        this.metadata =
                metadata == null
                        ? Collections.emptyMap()
                        : Collections.unmodifiableMap(new HashMap<>(metadata));
        this.state = state != null ? state : ToolCallState.PENDING;
    }

    /**
     * Gets the unique identifier of this tool call.
     *
     * @return The tool call ID
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the name of the tool to execute.
     *
     * @return The tool name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the input parameters for the tool.
     *
     * @return The tool input parameters map
     */
    public Map<String, Object> getInput() {
        return input;
    }

    /**
     * Gets the raw content for streaming tool calls.
     *
     * @return The raw content, or null if not set
     */
    public String getContent() {
        return content;
    }

    /**
     * Gets the provider-specific metadata.
     *
     * <p>For Gemini, this may contain the thought signature under the key
     * {@link #METADATA_THOUGHT_SIGNATURE}.
     *
     * @return The metadata map, or an empty map if not set
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    /**
     * Gets the tool call state.
     *
     * @return The tool call state
     */
    public ToolCallState getState() {
        return state;
    }

    /**
     * Returns a copy of this block with the given state.
     *
     * @param state The new state
     * @return A new ToolUseBlock with the updated state
     */
    public ToolUseBlock withState(ToolCallState state) {
        return new ToolUseBlock(this.id, this.name, this.input, this.content, this.metadata, state);
    }

    /**
     * Creates a new builder for constructing a ToolUseBlock.
     *
     * @return A new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing ToolUseBlock instances.
     */
    public static class Builder {
        private String id;
        private String name;
        private Map<String, Object> input;
        private String content;
        private Map<String, Object> metadata;
        private ToolCallState state;

        /**
         * Sets the unique identifier for the tool call.
         *
         * @param id The tool call ID
         * @return This builder for chaining
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the name of the tool to execute.
         *
         * @param name The tool name
         * @return This builder for chaining
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the input parameters for the tool.
         *
         * @param input The tool input parameters map
         * @return This builder for chaining
         */
        public Builder input(Map<String, Object> input) {
            this.input = input;
            return this;
        }

        /**
         * Sets the raw content for streaming tool calls.
         *
         * @param content The raw content for streaming
         * @return This builder for chaining
         */
        public Builder content(String content) {
            this.content = content;
            return this;
        }

        /**
         * Sets the provider-specific metadata.
         *
         * <p>For Gemini, use {@link ToolUseBlock#METADATA_THOUGHT_SIGNATURE} as the key
         * to store thought signatures.
         *
         * @param metadata The metadata map
         * @return This builder for chaining
         */
        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        /**
         * Sets the tool call state.
         *
         * @param state The tool call state
         * @return This builder for chaining
         */
        public Builder state(ToolCallState state) {
            this.state = state;
            return this;
        }

        /**
         * Builds a new ToolUseBlock with the configured properties.
         *
         * @return A new ToolUseBlock instance
         */
        public ToolUseBlock build() {
            return new ToolUseBlock(id, name, input, content, metadata, state);
        }
    }
}
