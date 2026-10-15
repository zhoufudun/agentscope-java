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
 * 【核心概念】AgentTool - 工具接口
 * ============================================================================
 *
 * 一、这是什么？
 * -------------
 * AgentTool 是所有工具的标准接口。
 * LLM 可以调用实现此接口的工具来与外部世界交互。
 *
 * 二、与 @Tool 注解的关系
 * ----------------------
 *   @Tool 注解:  声明式，适合简单工具
 *   AgentTool 接口: 编程式，适合复杂工具
 *
 *   @Tool 注解的方法最终会被包装成 AgentTool 实现。
 *
 * 三、接口方法
 * -----------
 *   ┌─────────────────────────────────────────────────────┐
 *   │  getName()        — 工具名称（snake_case）         │
 *   │  getDescription() — 工具描述（帮助 LLM 选择）      │
 *   │  getParameters()  — 参数 JSON Schema              │
 *   │  isReadOnly()     — 是否只读（影响权限默认行为）   │
 *   │  callAsync()      — 异步执行工具                   │
 *   └─────────────────────────────────────────────────────┘
 *
 * 四、实现方式
 * -----------
 *   方式 1: 继承 ToolBase（推荐，有权限检查支持）
 *   ─────────────────────────────────────────────
 *   public class MyTool extends ToolBase {
 *       public MyTool() {
 *           super(ToolBase.builder()
 *               .name("my_tool")
 *               .description("我的工具")
 *               .inputSchema(...)
 *               .build());
 *       }
 *
 *       @Override
 *       public Mono<ToolResultBlock> callAsync(ToolCallParam param) {
 *           // 实现逻辑
 *       }
 *   }
 *
 *   方式 2: 直接实现接口
 *   ────────────────────
 *   public class SimpleTool implements AgentTool {
 *       @Override public String getName() { return "simple"; }
 *       @Override public String getDescription() { return "..."; }
 *       @Override public Map<String,Object> getParameters() { return ...; }
 *       @Override public Mono<ToolResultBlock> callAsync(ToolCallParam p) { ... }
 *   }
 *
 * 五、注册到代理
 * -------------
 *   ReActAgent.builder()
 *       .tools(new MyTool())           // 直接注册 AgentTool
 *       .tools(myServiceBean)          // 注册带 @Tool 的对象
 *       .build();
 *
 * 六、参数 Schema 格式
 * -------------------
 *   Map.of(
 *       "type", "object",
 *       "properties", Map.of(
 *           "path", Map.of(
 *               "type", "string",
 *               "description", "文件路径"
 *           ),
 *           "content", Map.of(
 *               "type", "string",
 *               "description", "文件内容"
 *           )
 *       ),
 *       "required", List.of("path")
 *   )
 *
 * ============================================================================
 */

package io.agentscope.core.tool;

import io.agentscope.core.message.ToolResultBlock;
import java.util.Map;
import reactor.core.publisher.Mono;

/**
 * Interface for agent tools that can be called by models.
 *
 * <p>Agent tools are functions that AI agents can invoke to perform actions or retrieve
 * information. They bridge the gap between the agent's reasoning and the external world.
 *
 * <p><b>Implementation Guidelines:</b>
 * <ul>
 *   <li>Tools should have clear, descriptive names</li>
 *   <li>Descriptions should explain what the tool does and when to use it</li>
 *   <li>Parameter schemas must follow JSON Schema format</li>
 *   <li>All operations should be implemented asynchronously using Reactor Mono</li>
 * </ul>
 *
 * @see Tool
 * @see ToolParam
 * @see Toolkit
 */
public interface AgentTool {

    /**
     * Gets the name of the tool.
     *
     * <p>The name should be unique within a toolkit and follow snake_case convention for
     * compatibility with various LLM providers.
     *
     * @return The tool name (never null)
     */
    String getName();

    /**
     * Gets the description of the tool.
     *
     * <p>The description should clearly explain what the tool does, when it should be used, and
     * what kind of results it returns. This helps the LLM decide when to invoke the tool.
     *
     * @return The tool description (never null)
     */
    String getDescription();

    /**
     * Gets the parameters schema for this tool in JSON Schema format.
     *
     * <p>The schema defines the structure of the input parameters that this tool accepts. It
     * should include:
     * <ul>
     *   <li>type: "object"</li>
     *   <li>properties: Map of parameter names to their schemas</li>
     *   <li>required: List of required parameter names</li>
     * </ul>
     *
     * @return Map representing the JSON Schema for tool parameters (never null)
     */
    Map<String, Object> getParameters();

    /**
     * Gets strict mode configuration for this tool schema.
     *
     * <p>When strict mode is enabled, compatible model providers are expected to enforce stricter
     * adherence to the tool parameter schema. Returning {@code null} means no explicit strict mode
     * preference is provided.
     *
     * @return strict mode value ({@code true}/{@code false}) or {@code null} when unspecified
     */
    default Boolean getStrict() {
        return null;
    }

    /**
     * Gets the optional output schema for this tool in JSON Schema format.
     *
     * <p>Most tools do not expose a structured output schema to models, so the default
     * implementation returns {@code null}. MCP tools can override this to surface the
     * server-provided {@code outputSchema} definition.
     *
     * @return Map representing the JSON Schema for tool outputs, or null if unsupported
     */
    default Map<String, Object> getOutputSchema() {
        return null;
    }

    /**
     * Returns whether this tool performs only read operations and never mutates state.
     *
     * <p>Read-only tools are automatically permitted in restricted execution modes such as
     * Plan Mode, where write operations require explicit approval.
     *
     * @return {@code true} if the tool is read-only; {@code false} by default
     */
    default boolean isReadOnly() {
        return false;
    }

    /**
     * Execute the tool with the given parameters (asynchronous).
     *
     * <p>This method accepts a {@link ToolCallParam} object containing all necessary context for
     * tool execution, including:
     * <ul>
     *   <li>toolUseBlock: Contains tool call ID and name for tracking</li>
     *   <li>input: Input parameters for the tool</li>
     *   <li>agent: The calling agent (may be null), provides access to agent context</li>
     * </ul>
     *
     * @param param The tool call parameters
     * @return Mono containing ToolResultBlock
     */
    Mono<ToolResultBlock> callAsync(ToolCallParam param);
}
