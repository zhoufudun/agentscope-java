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
package io.agentscope.harness.coding.middleware;

import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.middleware.MiddlewareBase;
import io.agentscope.core.middleware.ReasoningInput;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

/**
 * Middleware that enforces per-thread token and model-call budgets.
 *
 * <p>Tracks cumulative model call counts per thread (using
 * {@link MessageQueueMiddleware#CURRENT_THREAD_ID}). When the limit is reached, raises an
 * exception to terminate the run.
 *
 * <p>The new per-thread budget concept.
 */
/**
 * 【学习注释：为何预算按 threadId 而不是按 Agent 实例计算】
 *
 * <p>一个 CodingAgent 实例可以服务多个会话。若把计数器放在 Agent 全局，会出现某位用户耗尽
 * 预算后影响其他用户的错误；此中间件以 {@code CURRENT_THREAD_ID} 为键保存 AtomicLong，
 * 让限制与一次外部会话绑定。
 *
 * <pre>
 * onReasoning()
 *   │
 *   ├─ 无 threadId：直接 next，保持普通 Agent 行为
 *   └─ 有 threadId：计数 +1 ──► 未超限：next.apply(input)
 *                               └► 已超限：Flux.error，终止本轮执行
 * </pre>
 *
 * <p>它拦在 {@code onReasoning}，因此限制的是模型调用次数，而不是工具次数。任务完成、会话
 * 重置或管理员清理时，应调用 {@code resetThread} 回收对应计数。
 */
public class ThreadBudgetMiddleware implements MiddlewareBase {

    private static final Logger log = LoggerFactory.getLogger(ThreadBudgetMiddleware.class);

    private final int maxModelCallsPerThread;
    private final ConcurrentHashMap<String, AtomicLong> callCounts = new ConcurrentHashMap<>();

    public ThreadBudgetMiddleware(int maxModelCallsPerThread) {
        this.maxModelCallsPerThread = maxModelCallsPerThread;
    }

    @Override
    public Flux<AgentEvent> onReasoning(
            Agent agent,
            RuntimeContext ctx,
            ReasoningInput input,
            Function<ReasoningInput, Flux<AgentEvent>> next) {
        String threadId = MessageQueueMiddleware.CURRENT_THREAD_ID.get();
        if (threadId == null || threadId.isBlank()) {
            return next.apply(input);
        }
        long count = callCounts.computeIfAbsent(threadId, k -> new AtomicLong(0)).incrementAndGet();
        if (count > maxModelCallsPerThread) {
            log.warn(
                    "[thread-budget-middleware] Thread {} exceeded model call budget ({}/{})."
                            + " Terminating run.",
                    threadId,
                    count,
                    maxModelCallsPerThread);
            return Flux.error(
                    new RuntimeException(
                            "Thread model call budget exceeded: "
                                    + count
                                    + "/"
                                    + maxModelCallsPerThread));
        }
        log.debug(
                "[thread-budget-middleware] Thread {} model call {}/{}",
                threadId,
                count,
                maxModelCallsPerThread);
        return next.apply(input);
    }

    public void resetThread(String threadId) {
        callCounts.remove(threadId);
    }
}
