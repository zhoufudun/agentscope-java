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
 * 【核心概念】ContentBlock - 内容块基类
 * ============================================================================
 *
 * 一、这是什么？
 * -------------
 * ContentBlock 是所有消息内容块的抽象基类。
 * 一条消息（Msg）包含多个内容块，就像乐高积木拼成的建筑。
 *
 * 二、为什么需要内容块？
 * ---------------------
 * 现代 LLM 支持多模态（文字、图片、音频、视频）。
 * 用内容块可以灵活组合不同类型的内容。
 *
 *   传统方式:  message = "你好"
 *   内容块方式: message.content = [
 *                 TextBlock("请描述这张图片"),
 *                 ImageBlock(图片数据)
 *              ]
 *
 * 三、内容块类型
 * -------------
 *   ┌─────────────────────────────────────────────────────────┐
 *   │  数据类型                                               │
 *   ├─────────────────────────────────────────────────────────┤
 *   │  TextBlock      — 纯文本（最常用）                      │
 *   │  ImageBlock     — 图片（Base64 或 URL）                 │
 *   │  AudioBlock     — 音频                                  │
 *   │  VideoBlock     — 视频                                  │
 *   │  DataBlock      — 通用二进制（统一 image/audio/video） │
 *   ├─────────────────────────────────────────────────────────┤
 *   │  交互类型                                               │
 *   ├─────────────────────────────────────────────────────────┤
 *   │  ToolUseBlock   — LLM 请求调用工具                      │
 *   │  ToolResultBlock — 工具执行结果                         │
 *   ├─────────────────────────────────────────────────────────┤
 *   │  特殊类型                                               │
 *   ├─────────────────────────────────────────────────────────┤
 *   │  ThinkingBlock  — LLM 思考过程（Extended Thinking）    │
 *   │  HintBlock      — 提示块（如 RAG 检索结果）             │
 *   └─────────────────────────────────────────────────────────┘
 *
 * 四、密封类（sealed）
 * -------------------
 *   Java 17+ 的密封类特性：
 *   - 限制哪些类可以继承 ContentBlock
 *   - 编译器可以检查 switch 是否穷尽
 *   - 提高类型安全性
 *
 *   switch (block) {
 *       case TextBlock tb -> handleText(tb);
 *       case ImageBlock ib -> handleImage(ib);
 *       case ToolUseBlock tub -> handleToolUse(tub);
 *       // 编译器警告你是否遗漏了某个类型
 *   }
 *
 * 五、JSON 序列化
 * --------------
 *   使用 Jackson 多态序列化：
 *   - @JsonTypeInfo: 用 "type" 字段区分类型
 *   - @JsonSubTypes: 定义类型映射
 *
 *   JSON 示例：
 *   {"type": "text", "text": "你好"}
 *   {"type": "tool_use", "id": "tc_001", "name": "get_weather", ...}
 *
 * ============================================================================
 */

package io.agentscope.core.message;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.agentscope.core.state.State;

/**
 * Base sealed class for all content blocks in messages.
 *
 * <p>Content blocks represent different types of content that can be included in a message,
 * such as text, images, audio, video, or thinking content. This sealed hierarchy ensures
 * type safety and enables exhaustive pattern matching.
 *
 * <p><b>Supported Content Types:</b>
 * <ul>
 *   <li>{@link TextBlock} - Plain text content
 *   <li>{@link ThinkingBlock} - Agent reasoning/thinking content
 *   <li>{@link ImageBlock} - Image content (URL or Base64)
 *   <li>{@link AudioBlock} - Audio content (URL or Base64)
 *   <li>{@link VideoBlock} - Video content (URL or Base64)
 *   <li>{@link ToolUseBlock} - Tool execution requests
 *   <li>{@link ToolResultBlock} - Tool execution results
 *   <li>{@link HintBlock} - Hints for LLM reasoning (e.g., from RAG)
 *   <li>{@link DataBlock} - Generic binary data block unifying image/audio/video
 * </ul>
 *
 * <p>Uses Jackson annotations for polymorphic JSON serialization with the "type" discriminator
 * field. The sealed modifier restricts subclasses to the specified permits list, enabling
 * compile-time exhaustiveness checking in pattern matching.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = TextBlock.class, name = "text"),
    @JsonSubTypes.Type(value = ThinkingBlock.class, name = "thinking"),
    @JsonSubTypes.Type(value = ImageBlock.class, name = "image"),
    @JsonSubTypes.Type(value = AudioBlock.class, name = "audio"),
    @JsonSubTypes.Type(value = VideoBlock.class, name = "video"),
    @JsonSubTypes.Type(value = ToolUseBlock.class, name = "tool_use"),
    @JsonSubTypes.Type(value = ToolResultBlock.class, name = "tool_result"),
    @JsonSubTypes.Type(value = HintBlock.class, name = "hint"),
    @JsonSubTypes.Type(value = DataBlock.class, name = "data")
})
public sealed class ContentBlock implements State
        permits TextBlock,
                ImageBlock,
                AudioBlock,
                VideoBlock,
                ThinkingBlock,
                ToolUseBlock,
                ToolResultBlock,
                HintBlock,
                DataBlock {}
