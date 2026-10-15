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
 * 【核心概念】用户确认请求事件 - RequireUserConfirmEvent (HITL)
 * ============================================================================
 *
 * 一、这是什么？
 * -------------
 * HITL = Human-In-The-Loop（人在回路中）
 * 当工具调用的权限设置为 ASK 时，代理会暂停并发出此事件，
 * 等待用户确认后才继续执行。
 *
 * 二、为什么需要 HITL？
 * -------------------
 * 有些操作风险较高，需要人工确认：
 *   - 删除文件
 *   - 发送邮件
 *   - 执行数据库操作
 *   - 调用付费 API
 *
 * 三、权限三种行为
 * ---------------
 *   ┌─────────────────────────────────────────────────┐
 *   │  ALLOW  — 直接执行，不询问                       │
 *   │  DENY   — 直接拒绝，不执行                       │
 *   │  ASK    — 暂停，发出 RequireUserConfirmEvent     │
 *   └─────────────────────────────────────────────────┘
 *
 * 四、HITL 流程
 * ------------
 *   LLM 决定调用 delete_file(path="/important.txt")
 *      │
 *      ▼
 *   PermissionEngine 检查规则 → 匹配到 ASK
 *      │
 *      ▼
 *   发出 RequireUserConfirmEvent (包含 toolCalls 列表)
 *      │
 *      ▼
 *   代理流暂停（Flux 阻塞）
 *      │
 *      ▼
 *   前端显示确认对话框: "是否允许删除 /important.txt？"
 *      │
 *      ▼ 用户点击 [允许] 或 [拒绝]
 *      │
 *      ▼
 *   调用 confirmSink.tryEmitNext(confirmResults)
 *      │
 *      ▼
 *   代理流恢复，根据结果执行或跳过
 *
 * 五、关键字段
 * -----------
 *   replyId   — 回复标识
 *   toolCalls — 需要确认的工具调用列表（ToolUseBlock）
 *
 * 六、前端处理示例
 * ---------------
 *   agent.streamEvents(userMsg)
 *       .doOnNext(event -> {
 *           if (event instanceof RequireUserConfirmEvent e) {
 *               // 显示确认对话框
 *               boolean userApproved = showConfirmDialog(e.getToolCalls());
 *
 *               // 构造确认结果
 *               List<ConfirmResult> results = e.getToolCalls().stream()
 *                   .map(tc -> new ConfirmResult(userApproved, tc))
 *                   .toList();
 *
 *               // 发送确认结果，恢复代理执行
 *               confirmSink.tryEmitNext(results);
 *           }
 *       });
 *
 * 七、安全最佳实践
 * ---------------
 *   - 敏感操作默认设置为 ASK
 *   - 用户可在确认时选择"记住此选择"→ 添加 ALLOW 规则
 *   - 生产环境应记录所有 ASK 确认日志
 *
 * ============================================================================
 */

package io.agentscope.core.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.agentscope.core.message.ToolUseBlock;
import java.util.List;

/**
 * Emitted when the agent requires user confirmation before executing tool calls (HITL).
 */
public class RequireUserConfirmEvent extends AgentEvent {

    private final String replyId;
    private final List<ToolUseBlock> toolCalls;

    @JsonCreator
    public RequireUserConfirmEvent(
            @JsonProperty("id") String id,
            @JsonProperty("createdAt") String createdAt,
            @JsonProperty("replyId") String replyId,
            @JsonProperty("toolCalls") List<ToolUseBlock> toolCalls) {
        super(id, createdAt);
        this.replyId = replyId;
        this.toolCalls = toolCalls != null ? List.copyOf(toolCalls) : List.of();
    }

    public RequireUserConfirmEvent(String replyId, List<ToolUseBlock> toolCalls) {
        this.replyId = replyId;
        this.toolCalls = toolCalls != null ? List.copyOf(toolCalls) : List.of();
    }

    @Override
    public AgentEventType getType() {
        return AgentEventType.REQUIRE_USER_CONFIRM;
    }

    public String getReplyId() {
        return replyId;
    }

    public List<ToolUseBlock> getToolCalls() {
        return toolCalls;
    }
}
