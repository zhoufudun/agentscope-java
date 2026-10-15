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
 * 【核心概念】用户确认结果 - ConfirmResult
 * ============================================================================
 *
 * 一、这是什么？
 * -------------
 * 用户对 RequireUserConfirmEvent 中某个工具调用的确认结果。
 * 每个 ToolUseBlock 对应一个 ConfirmResult。
 *
 * 二、三个关键字段
 * ---------------
 *   confirmed — 用户是否批准（true=执行, false=跳过）
 *   toolCall  — 可选的修改后工具调用（用户可以调整参数）
 *   rules     — 可选的新权限规则（"记住此选择"功能）
 *
 * 三、典型使用场景
 * ---------------
 *
 *   场景 1: 简单批准
 *   ─────────────────
 *   new ConfirmResult(true, originalToolCall);
 *   // 用户点击 [允许]，按原样执行
 *
 *   场景 2: 简单拒绝
 *   ─────────────────
 *   new ConfirmResult(false, originalToolCall);
 *   // 用户点击 [拒绝]，跳过此工具调用
 *
 *   场景 3: 修改参数后执行
 *   ─────────────────────
 *   ToolUseBlock modified = originalToolCall.withInput(newArgs);
 *   new ConfirmResult(true, modified);
 *   // 用户修改了工具参数（如把删除路径改为移动到回收站）
 *
 *   场景 4: 批准并记住
 *   ───────────────────
 *   PermissionRule allowRule = PermissionRule.builder()
 *       .behavior(PermissionBehavior.ALLOW)
 *       .toolName("delete_file")
 *       .scope(PermissionScope.SESSION)
 *       .build();
 *   new ConfirmResult(true, toolCall, List.of(allowRule));
 *   // 用户勾选了"本次会话不再询问"
 *
 * 四、权限规则的作用域
 * -------------------
 *   SESSION  — 仅本次会话有效
 *   USER     — 该用户永久有效
 *   GLOBAL   — 所有用户永久有效
 *
 * 五、流程图
 * ----------
 *   RequireUserConfirmEvent
 *          │
 *          ▼ 前端显示对话框
 *   ┌──────────────────────────────┐
 *   │  删除文件 /important.txt？   │
 *   │  ○ 允许                      │
 *   │  ○ 拒绝                      │
 *   │  ☐ 本次会话不再询问          │
 *   │  [确定]                      │
 *   └──────────────────────────────┘
 *          │
 *          ▼ 构造 ConfirmResult
 *   confirmSink.tryEmitNext(List.of(confirmResult))
 *          │
 *          ▼ 代理继续执行
 *
 * ============================================================================
 */

package io.agentscope.core.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.permission.PermissionRule;
import java.util.List;

/**
 * Represents the result of a single user confirmation decision for a tool call.
 *
 * <p>When confirmed, the caller may supply a modified {@link #toolCall} (allowing the user to
 * tweak input) and/or new {@link #rules} that the {@code PermissionEngine} should remember for
 * future calls — e.g. "always allow this command going forward".
 */
public class ConfirmResult {

    private final boolean confirmed;
    private final ToolUseBlock toolCall;
    private final List<PermissionRule> rules;

    @JsonCreator
    public ConfirmResult(
            @JsonProperty("confirmed") boolean confirmed,
            @JsonProperty("toolCall") ToolUseBlock toolCall,
            @JsonProperty("rules") List<PermissionRule> rules) {
        this.confirmed = confirmed;
        this.toolCall = toolCall;
        this.rules = rules;
    }

    /** Convenience constructor without rules. */
    public ConfirmResult(boolean confirmed, ToolUseBlock toolCall) {
        this(confirmed, toolCall, null);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public ToolUseBlock getToolCall() {
        return toolCall;
    }

    /**
     * New permission rules to register with the engine when {@link #confirmed} is true. Each rule
     * extends the engine's allow / deny / ask tables based on its {@code behavior}.
     *
     * @return list of rules (may be null or empty)
     */
    public List<PermissionRule> getRules() {
        return rules;
    }
}
