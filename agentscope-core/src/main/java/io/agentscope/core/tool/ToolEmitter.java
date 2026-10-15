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
 * 【核心概念】ToolEmitter - 工具流式进度输出
 * ============================================================================
 *
 * 一、这是什么？
 * -------------
 * ToolEmitter 让工具在执行过程中发送进度更新给前端。
 * 类似于"正在处理..."、"已完成 50%..."这样的实时反馈。
 *
 * 二、核心区别
 * -----------
 *   emit()  → 发送给前端显示，LLM 看不到
 *   return  → 发送给 LLM 作为工具结果
 *
 *   ┌─────────────────────────────────────────────────────────────┐
 *   │  工具执行                                                   │
 *   │      │                                                      │
 *   │      ▼                                                      │
 *   │  emitter.emit("正在下载...")   ───────► 前端显示进度         │
 *   │      │                          （LLM 看不到）              │
 *   │      ▼                                                      │
 *   │  emitter.emit("下载完成，正在解析...")  ───► 前端显示进度    │
 *   │      │                                                      │
 *   │      ▼                                                      │
 *   │  return "解析结果: ..."        ───────► LLM 收到最终结果     │
 *   └─────────────────────────────────────────────────────────────┘
 *
 * 三、使用方法
 * -----------
 *   @Tool(name = "download_file", description = "下载文件")
 *   public String downloadFile(
 *       @ToolParam(name = "url") String url,
 *       ToolEmitter emitter  // 框架自动注入，无需 @ToolParam
 *   ) {
 *       emitter.emit(ToolResultBlock.text("开始下载..."));
 *
 *       for (int i = 0; i <= 100; i += 10) {
 *           // 模拟下载进度
 *           Thread.sleep(100);
 *           emitter.emit(ToolResultBlock.text("进度: " + i + "%"));
 *       }
 *
 *       emitter.emit(ToolResultBlock.text("下载完成！"));
 *       return "文件已保存到 /tmp/file.zip";  // 这个给 LLM
 *   }
 *
 * 四、前端接收
 * -----------
 *   emitter.emit() 触发 ToolResultTextDeltaEvent
 *
 *   agent.streamEvents(userMsg)
 *       .doOnNext(event -> {
 *           if (event instanceof ToolResultTextDeltaEvent e) {
 *               System.out.println("[进度] " + e.getDelta());
 *           }
 *       });
 *
 * 五、典型应用场景
 * ---------------
 *   - 文件下载/上传进度
 *   - 数据库大量查询进度
 *   - 长时间 API 调用状态
 *   - 多步骤任务的当前步骤
 *
 * 六、与 metadata 配合
 * -------------------
 *   emitter.emit(ToolResultBlock.builder()
 *       .name("download")
 *       .metadata(Map.of("progress", "50%", "speed", "1.2MB/s"))
 *       .build());
 *
 *   前端可以根据 metadata 渲染进度条。
 *
 * ============================================================================
 */

package io.agentscope.core.tool;

import io.agentscope.core.message.ToolResultBlock;

/**
 * Interface for emitting streaming responses during tool execution.
 *
 * <p>Tool methods can declare a ToolEmitter parameter to send intermediate progress updates and
 * messages during execution. These streaming chunks are delivered to registered hooks via
 * {@code onActingChunk()} events but are NOT sent to the LLM. Only the final return value of the
 * tool method is sent to the LLM as the tool result.
 *
 * <p><b>Key Characteristics:</b>
 * <ul>
 *   <li>ToolEmitter is auto-injected by the framework - no {@link ToolParam} annotation needed</li>
 *   <li>Emitted chunks go to hooks (for monitoring/logging), not to the LLM</li>
 *   <li>Useful for long-running tools to provide progress feedback</li>
 *   <li>Does not affect the tool schema visible to the LLM</li>
 * </ul>
 *
 * <p><b>Usage Example:</b>
 *
 * <pre>{@code
 * @Tool(name = "long_task", description = "Execute a long running task")
 * public ToolResultBlock execute(
 *     @ToolParam(name = "input") String input,
 *     ToolEmitter emitter  // Automatically injected by framework
 * ) {
 *     emitter.emit(ToolResultBlock.text("Starting task..."));
 *
 *     // Step 1
 *     processStep1(input);
 *     emitter.emit(ToolResultBlock.text("Step 1 completed"));
 *
 *     // Step 2
 *     processStep2(input);
 *     emitter.emit(ToolResultBlock.text("Step 2 completed"));
 *
 *     // Final result (this is what the LLM sees)
 *     return ToolResultBlock.text("Task completed successfully");
 * }
 * }</pre>
 */
public interface ToolEmitter {

    /**
     * Emit a streaming response chunk during tool execution.
     *
     * <p>This method sends intermediate messages to registered hooks via {@code onToolChunk()}.
     * The emitted chunks do NOT affect what the LLM receives - only the tool method's return value
     * is sent to the LLM.
     *
     * @param chunk The chunk to emit
     */
    void emit(ToolResultBlock chunk);
}
