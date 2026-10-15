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
 * 【核心概念】请求停止事件 - RequestStopEvent
 * ============================================================================
 *
 * 一、这是什么？
 * -------------
 * 中间件请求代理暂停执行的信号事件。
 * 代理会完成当前步骤，然后优雅停止，状态会被保存以便后续恢复。
 *
 * 二、典型使用场景
 * ---------------
 *
 *   1. 成本/预算控制
 *   ────────────────
 *   public class BudgetMiddleware implements MiddlewareBase {
 *       @Override
 *       public Flux<AgentEvent> onModelCall(...) {
 *           if (tokenUsage > budget) {
 *               return Flux.just(new RequestStopEvent("预算超限"));
 *           }
 *           return next.apply(input);
 *       }
 *   }
 *
 *   2. 审计/合规检查点
 *   ──────────────────
 *   需要人工审核后才能执行敏感操作
 *
 *   3. 调试单步执行
 *   ────────────────
 *   每次迭代后暂停，方便开发者检查中间状态
 *
 *   4. 内容审核门控
 *   ────────────────
 *   检测到敏感内容时暂停
 *
 * 三、停止后如何恢复？
 * -------------------
 *
 *   第一次调用:
 *   agent.call("执行复杂任务")
 *        ↓
 *   中间件发出 RequestStopEvent
 *        ↓
 *   代理停止，状态保存到 AgentState
 *        ↓
 *   返回 Msg (generateReason = MIDDLEWARE_STOP_REQUESTED)
 *
 *   恢复调用:
 *   agent.call("继续")  // 或任何消息
 *        ↓
 *   从 AgentState 恢复待执行的工具调用
 *        ↓
 *   继续执行
 *
 * 四、关键字段
 * -----------
 *   reason         — 人类可读的停止原因
 *   generateReason — 设置到返回 Msg 上的原因枚举
 *
 * 五、GenerateReason 枚举
 * ----------------------
 *   MIDDLEWARE_STOP_REQUESTED — 中间件请求停止（默认）
 *   PERMISSION_ASKING         — 等待权限确认（HITL 用）
 *   BUDGET_EXCEEDED           — 预算超限
 *   USER_INTERRUPTED          — 用户中断
 *
 * 六、与 agent.interrupt() 的区别
 * ------------------------------
 *   RequestStopEvent: 中间件发起，优雅停止，状态保存
 *   agent.interrupt(): 外部调用，强制中断，可能丢失状态
 *
 * ============================================================================
 */

package io.agentscope.core.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.agentscope.core.message.GenerateReason;

/**
 * Requests the agent to pause execution after the current phase completes.
 *
 * <p>Emit this event from a {@link io.agentscope.core.middleware.MiddlewareBase} (e.g. inside
 * {@code onReasoning} or {@code onActing}) to stop the agent loop. The agent finishes the
 * in-flight step, then returns a {@code Msg} whose {@link GenerateReason} is set to
 * {@code generateReason} (defaults to {@link GenerateReason#MIDDLEWARE_STOP_REQUESTED}).
 *
 * <p>The pending state is persisted via {@link io.agentscope.core.message.ToolCallState} in
 * {@code AgentState.context}, so callers can resume by issuing a second {@code agent.call(...)}
 * — pending tool calls will be picked up automatically and execution continues.
 *
 * <p>Typical use cases:
 * <ul>
 *   <li>Cost / budget guards (stop when token usage exceeds a threshold)
 *   <li>Audit / compliance checkpoints (manual review before destructive actions)
 *   <li>Debug stepping (pause every iteration)
 *   <li>Content moderation gates
 * </ul>
 */
public class RequestStopEvent extends AgentEvent {

    private final String reason;
    private final GenerateReason generateReason;

    @JsonCreator
    public RequestStopEvent(
            @JsonProperty("id") String id,
            @JsonProperty("createdAt") String createdAt,
            @JsonProperty("reason") String reason,
            @JsonProperty("generateReason") GenerateReason generateReason) {
        super(id, createdAt);
        this.reason = reason;
        this.generateReason =
                generateReason != null ? generateReason : GenerateReason.MIDDLEWARE_STOP_REQUESTED;
    }

    /**
     * Create a stop request with the default {@link GenerateReason#MIDDLEWARE_STOP_REQUESTED}.
     *
     * @param reason human-readable description of why the stop was requested
     */
    public RequestStopEvent(String reason) {
        this(reason, GenerateReason.MIDDLEWARE_STOP_REQUESTED);
    }

    /**
     * Create a stop request with an explicit generate reason. Used internally by Permission HITL
     * to surface {@link GenerateReason#PERMISSION_ASKING}.
     *
     * @param reason human-readable description
     * @param generateReason the reason to set on the returned Msg
     */
    public RequestStopEvent(String reason, GenerateReason generateReason) {
        this.reason = reason;
        this.generateReason =
                generateReason != null ? generateReason : GenerateReason.MIDDLEWARE_STOP_REQUESTED;
    }

    @Override
    public AgentEventType getType() {
        return AgentEventType.REQUEST_STOP;
    }

    /** Human-readable description of why the stop was requested. */
    public String getReason() {
        return reason;
    }

    /** The {@link GenerateReason} to assign to the returned Msg. */
    public GenerateReason getGenerateReason() {
        return generateReason;
    }
}
