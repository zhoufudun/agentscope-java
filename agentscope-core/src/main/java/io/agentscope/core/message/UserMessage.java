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
 * 【核心概念】UserMessage - 用户消息
 * ============================================================================
 *
 * 一、这是什么？
 * -------------
 * UserMessage 是用户发送给代理的消息。
 * 它是 Msg 的便捷子类，角色固定为 USER。
 *
 * 二、创建方式
 * -----------
 *   // 最简单：纯文本
 *   Msg msg = new UserMessage("你好");
 *
 *   // 带名字
 *   Msg msg = new UserMessage("Alice", "你好");
 *
 *   // 多模态（文字 + 图片）
 *   Msg msg = new UserMessage(
 *       TextBlock.builder().text("这是什么？").build(),
 *       ImageBlock.builder().url("https://...").build()
 *   );
 *
 *   // Builder 方式
 *   Msg msg = UserMessage.builder()
 *       .name("Alice")
 *       .textContent("你好")
 *       .metadata(Map.of("source", "mobile"))
 *       .build();
 *
 * 三、消息角色
 * -----------
 *   ┌─────────────────────────────────────────────────────┐
 *   │  USER       — 用户消息（UserMessage）              │
 *   │  ASSISTANT  — 代理回复（AssistantMessage）         │
 *   │  SYSTEM     — 系统提示（SystemMessage）            │
 *   │  TOOL       — 工具结果（ToolMessage）              │
 *   └─────────────────────────────────────────────────────┘
 *
 * 四、调用代理
 * -----------
 *   // 方式 1: 直接传 UserMessage
 *   agent.call(new UserMessage("你好")).block();
 *
 *   // 方式 2: 传字符串（内部转成 UserMessage）
 *   agent.call("你好").block();
 *
 *   // 方式 3: 流式
 *   agent.streamEvents(new UserMessage("你好"))
 *       .doOnNext(event -> { ... })
 *       .blockLast();
 *
 * 五、会话上下文
 * -------------
 *   多轮对话的历史存储在 AgentState.context 中：
 *
 *   [UserMessage("你好")]
 *   [AssistantMessage("你好！有什么可以帮你？")]
 *   [UserMessage("天气如何？")]
 *   [AssistantMessage(..., ToolUseBlock)]
 *   [ToolMessage(ToolResultBlock)]
 *   [AssistantMessage("今天晴天，25°C")]
 *
 * 六、允许的内容块
 * ---------------
 *   UserMessage 允许：
 *   ✅ TextBlock     — 文字
 *   ✅ ImageBlock    — 图片
 *   ✅ AudioBlock    — 音频
 *   ✅ VideoBlock    — 视频
 *   ✅ DataBlock     — 数据
 *
 *   不允许：
 *   ❌ ToolUseBlock  — 只有 LLM 能请求工具
 *   ❌ ToolResultBlock — 只有工具能返回结果
 *
 * ============================================================================
 */

package io.agentscope.core.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.agentscope.core.model.ChatUsage;
import java.util.List;
import java.util.Map;

/**
 * A {@link Msg} with {@link MsgRole#USER} pinned at construction time.
 *
 * <p>Provides convenience constructors that take a {@code String} directly
 * (wrapped into a {@link TextBlock}) or any combination of {@link ContentBlock}s,
 * removing the boilerplate of {@code Msg.builder().role(USER).content(...).build()}.
 *
 * <p>Also offers an independent {@link Builder} for cases that need to set
 * {@code metadata}, {@code timestamp}, {@code usage} or {@code generateReason}.
 *
 * <p>Allowed content blocks follow the rules enforced by {@link Msg}'s constructor:
 * text, image, audio, video, and data blocks.
 */
public final class UserMessage extends Msg {

    public UserMessage(String text) {
        this(null, text);
    }

    public UserMessage(String name, String text) {
        super(
                generateId(),
                name,
                MsgRole.USER,
                List.of(TextBlock.builder().text(text).build()),
                null,
                currentTimestamp(),
                null);
    }

    public UserMessage(ContentBlock... blocks) {
        this(null, blocks);
    }

    public UserMessage(String name, ContentBlock... blocks) {
        super(
                generateId(),
                name,
                MsgRole.USER,
                blocks == null ? List.of() : List.of(blocks),
                null,
                currentTimestamp(),
                null);
    }

    public UserMessage(List<ContentBlock> blocks) {
        super(generateId(), null, MsgRole.USER, blocks, null, currentTimestamp(), null);
    }

    @JsonCreator
    private UserMessage(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("role") MsgRole role,
            @JsonProperty("content") List<ContentBlock> content,
            @JsonProperty("metadata") Map<String, Object> metadata,
            @JsonProperty("timestamp") String timestamp,
            @JsonProperty("usage") ChatUsage usage) {
        super(id, name, MsgRole.USER, content, metadata, timestamp, usage);
    }

    /**
     * Creates a builder for {@link UserMessage} with a randomly generated ID
     * and the current timestamp pre-populated.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for {@link UserMessage}. The role is fixed to
     * {@link MsgRole#USER} and {@link #role(MsgRole)} is unsupported.
     */
    public static final class Builder extends Msg.Builder {

        public Builder() {
            super.role = MsgRole.USER;
        }

        /** Unsupported on {@link UserMessage.Builder}: role is fixed to USER. */
        @Override
        public Builder role(MsgRole role) {
            throw new UnsupportedOperationException(
                    "UserMessage role is fixed to USER; use Msg.builder() to set role.");
        }

        @Override
        public Builder id(String id) {
            super.id(id);
            return this;
        }

        @Override
        public Builder name(String name) {
            super.name(name);
            return this;
        }

        @Override
        public Builder content(List<ContentBlock> content) {
            super.content(content);
            return this;
        }

        @Override
        public Builder content(ContentBlock block) {
            super.content(block);
            return this;
        }

        @Override
        public Builder content(ContentBlock... blocks) {
            super.content(blocks);
            return this;
        }

        @Override
        public Builder textContent(String text) {
            super.textContent(text);
            return this;
        }

        @Override
        public Builder metadata(Map<String, Object> metadata) {
            super.metadata(metadata);
            return this;
        }

        @Override
        public Builder timestamp(String timestamp) {
            super.timestamp(timestamp);
            return this;
        }

        @Override
        public Builder usage(ChatUsage usage) {
            super.usage(usage);
            return this;
        }

        @Override
        public Builder generateReason(GenerateReason reason) {
            super.generateReason(reason);
            return this;
        }

        @Override
        public UserMessage build() {
            return new UserMessage(id, name, MsgRole.USER, content, metadata, timestamp, usage);
        }
    }
}
