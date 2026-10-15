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
 * 【核心概念】MiddlewareChain - 中间件链（洋葱模型）
 * ============================================================================
 *
 * 一、这是什么？
 * -------------
 * MiddlewareChain 将多个中间件组装成"洋葱"结构。
 * 请求从外向内穿透，响应从内向外返回。
 *
 * 二、洋葱模型图解
 * ---------------
 *        请求进入
 *            │
 *   ┌────────▼────────┐
 *   │   中间件 1       │  ← 外层（先执行前置逻辑）
 *   │ ┌──────────────┐│
 *   │ │  中间件 2    ││
 *   │ │ ┌──────────┐ ││
 *   │ │ │ 中间件 3 │ ││
 *   │ │ │ ┌──────┐ │ ││
 *   │ │ │ │ 核心 │ │ ││  ← 最内层（实际逻辑）
 *   │ │ │ └──────┘ │ ││
 *   │ │ └──────────┘ ││
 *   │ └──────────────┘│
 *   └─────────────────┘
 *            │
 *        响应返回
 *
 * 三、五个拦截点
 * -------------
 *   ┌─────────────────────────────────────────────────────┐
 *   │  onAgent       — 整个代理调用                       │
 *   │  onReasoning   — LLM 推理阶段                       │
 *   │  onActing      — 工具执行阶段                       │
 *   │  onModelCall   — 单次 LLM API 调用                  │
 *   │  onSystemPrompt — 系统提示生成                      │
 *   └─────────────────────────────────────────────────────┘
 *
 * 四、执行顺序
 * -----------
 *   假设有 [MW1, MW2, MW3] 三个中间件：
 *
 *   请求阶段：
 *   MW1.before() → MW2.before() → MW3.before() → 核心逻辑
 *
 *   响应阶段：
 *   MW3.after() → MW2.after() → MW1.after() → 返回
 *
 * 五、中间件示例
 * -------------
 *   public class LoggingMiddleware extends MiddlewareBase {
 *       @Override
 *       public Flux<AgentEvent> onAgent(
 *               Agent agent,
 *               RuntimeContext ctx,
 *               AgentInput input,
 *               Function<AgentInput, Flux<AgentEvent>> next) {
 *
 *           log.info("开始执行: {}", input.getUserMessage());
 *           long start = System.currentTimeMillis();
 *
 *           return next.apply(input)  // 调用下一层
 *               .doOnComplete(() -> {
 *                   log.info("执行完成，耗时: {}ms",
 *                       System.currentTimeMillis() - start);
 *               });
 *       }
 *   }
 *
 * 六、构建链的方式
 * ---------------
 *   Function<AgentInput, Flux<AgentEvent>> chain =
 *       MiddlewareChain.build(
 *           middlewares,           // 中间件列表
 *           agent,                 // 代理实例
 *           ctx,                   // 运行时上下文
 *           MiddlewareBase::onAgent, // 拦截点方法
 *           coreLogic              // 核心逻辑
 *       );
 *
 *   chain.apply(input);  // 执行整个链
 *
 * ============================================================================
 */

package io.agentscope.core.middleware;

import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import java.util.List;
import java.util.function.Function;
import reactor.core.publisher.Flux;

/**
 * Builds an onion-style middleware chain for a given interception point.
 *
 * <p>The chain is constructed back-to-front: the last middleware wraps
 * the core logic, and the first middleware is the outermost wrapper.
 */
public final class MiddlewareChain {

    private MiddlewareChain() {}

    /**
     * Build a middleware chain that produces {@code Flux<AgentEvent>}.
     *
     * @param middlewares ordered list of middlewares (first = outermost)
     * @param agent      the agent instance passed to each middleware
     * @param ctx        per-call runtime context passed to each middleware
     * @param method     reference to the middleware hook method
     * @param core       the innermost logic to execute when all middlewares delegate
     * @param <I>        the input type for the interception point
     * @return a function that, when applied to an input, runs the full chain
     */
    public static <I> Function<I, Flux<AgentEvent>> build(
            List<? extends MiddlewareBase> middlewares,
            Agent agent,
            RuntimeContext ctx,
            MiddlewareMethod<I> method,
            Function<I, Flux<AgentEvent>> core) {
        if (middlewares == null || middlewares.isEmpty()) {
            return core;
        }
        Function<I, Flux<AgentEvent>> chain = core;
        for (int i = middlewares.size() - 1; i >= 0; i--) {
            MiddlewareBase mw = middlewares.get(i);
            Function<I, Flux<AgentEvent>> next = chain;
            chain = input -> method.apply(mw, agent, ctx, input, next);
        }
        return chain;
    }

    /**
     * Functional interface representing one of the onion-pattern middleware hooks.
     *
     * @param <I> the input type
     */
    @FunctionalInterface
    public interface MiddlewareMethod<I> {
        Flux<AgentEvent> apply(
                MiddlewareBase mw,
                Agent agent,
                RuntimeContext ctx,
                I input,
                Function<I, Flux<AgentEvent>> next);
    }
}
