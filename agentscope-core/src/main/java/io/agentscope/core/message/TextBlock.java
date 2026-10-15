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
 * 【核心概念】TextBlock - 纯文本内容块
 * ============================================================================
 *
 * 一、这是什么？
 * -------------
 * TextBlock 是最基础的内容块，包含纯文本内容。
 * 它是消息的"原子"单位之一。
 *
 * 二、消息结构
 * -----------
 *   一条消息（Msg）包含多个内容块（ContentBlock）：
 *
 *   Msg {
 *     role: USER / ASSISTANT / SYSTEM / TOOL
 *     content: [
 *       TextBlock("你好"),           ← 文本块
 *       ImageBlock(...),             ← 图片块
 *       ToolUseBlock(...),           ← 工具调用块
 *       ...
 *     ]
 *   }
 *
 * 三、内容块类型
 * -------------
 *   ┌────────────────────────────────────────────────────┐
 *   │  TextBlock      — 纯文本                          │
 *   │  ImageBlock     — 图片（Base64 或 URL）           │
 *   │  AudioBlock     — 音频                            │
 *   │  VideoBlock     — 视频                            │
 *   │  ToolUseBlock   — LLM 请求调用工具                 │
 *   │  ToolResultBlock — 工具执行结果                   │
 *   │  ThinkingBlock  — LLM 思考过程                    │
 *   └────────────────────────────────────────────────────┘
 *
 * 四、创建方式
 * -----------
 *   // 方式 1: Builder
 *   TextBlock block = TextBlock.builder()
 *       .text("Hello, world!")
 *       .build();
 *
 *   // 方式 2: 直接用 UserMessage
 *   Msg msg = new UserMessage("Hello, world!");
 *   // 内部自动创建 TextBlock
 *
 * 五、从消息获取文本
 * -----------------
 *   Msg msg = agent.call("你好").block();
 *
 *   // 方式 1: 直接获取合并文本
 *   String text = msg.getTextContent();
 *
 *   // 方式 2: 遍历内容块
 *   for (ContentBlock block : msg.getContent()) {
 *       if (block instanceof TextBlock tb) {
 *           System.out.println(tb.getText());
 *       }
 *   }
 *
 * 六、流式场景
 * -----------
 *   TextBlock 对应的事件：
 *   - TextBlockStartEvent  — 文本块开始
 *   - TextBlockDeltaEvent  — 增量文本（多次）
 *   - TextBlockEndEvent    — 文本块结束
 *
 *   最终的 TextBlock 是所有 delta 拼接的结果。
 *
 * ============================================================================
 */

package io.agentscope.core.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents plain text content in a message.
 *
 * <p>This is the most basic content block type, containing simple text content.
 * Text blocks are commonly used for user messages, assistant responses,
 * and any other textual communication.
 *
 * <p>The text content can be empty but never null. The toString() method
 * returns the text content for convenience.
 */
public final class TextBlock extends ContentBlock {

    private final String text;

    /**
     * Creates a new text block for JSON deserialization.
     *
     * @param text The text content (null will be converted to empty string)
     */
    @JsonCreator
    private TextBlock(@JsonProperty("text") String text) {
        this.text = text != null ? text : "";
    }

    /**
     * Gets the text content of this block.
     *
     * @return The text content
     */
    public String getText() {
        return text;
    }

    @Override
    public String toString() {
        return text;
    }

    /**
     * Creates a new builder for constructing TextBlock instances.
     *
     * @return A new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing TextBlock instances.
     */
    public static class Builder {

        private String text;

        /**
         * Sets the text content for the block.
         *
         * @param text The text content
         * @return This builder for chaining
         */
        public Builder text(String text) {
            this.text = text;
            return this;
        }

        /**
         * Builds a new TextBlock with the configured text.
         *
         * @return A new TextBlock instance (null text will be converted to empty string)
         */
        public TextBlock build() {
            return new TextBlock(text != null ? text : "");
        }
    }
}
