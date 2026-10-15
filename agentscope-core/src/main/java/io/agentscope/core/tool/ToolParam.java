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
 * 【核心概念】@ToolParam 注解 - 描述工具参数
 * ============================================================================
 *
 * 一、这是什么？
 * -------------
 * @ToolParam 描述 @Tool 方法的每个参数。
 * LLM 通过这些描述知道应该传什么值。
 *
 * 二、为什么必须加这个注解？
 * -------------------------
 * Java 默认不保留方法参数名（除非编译时加 -parameters）。
 * 所以必须通过 @ToolParam(name="xxx") 明确告诉框架参数名。
 *
 * 三、基本用法
 * -----------
 *   @Tool(name = "calculate", description = "计算矩形面积")
 *   public double calculate(
 *       @ToolParam(name = "width", description = "宽度(米)") double width,
 *       @ToolParam(name = "height", description = "高度(米)") double height
 *   ) {
 *       return width * height;
 *   }
 *
 *   生成的 JSON Schema:
 *   {
 *     "type": "object",
 *     "properties": {
 *       "width": { "type": "number", "description": "宽度(米)" },
 *       "height": { "type": "number", "description": "高度(米)" }
 *     },
 *     "required": ["width", "height"]
 *   }
 *
 * 四、核心属性
 * -----------
 *   name        — 参数名（必填，推荐 snake_case）
 *   description — 参数描述（帮助 LLM 理解应该传什么）
 *   required    — 是否必填（默认 true）
 *
 * 五、可选参数示例
 * ---------------
 *   @Tool(name = "search", description = "搜索")
 *   public List<Result> search(
 *       @ToolParam(name = "keyword", description = "关键词") String keyword,
 *       @ToolParam(name = "limit", description = "最大数量", required = false) Integer limit
 *   ) {
 *       int maxResults = limit != null ? limit : 10;
 *       return doSearch(keyword, maxResults);
 *   }
 *
 * 六、不需要 @ToolParam 的参数
 * --------------------------
 *   - ToolEmitter — 框架自动注入，用于发送进度
 *   - ToolExecutionContext — 框架自动注入，提供运行时上下文
 *   - AgentState — 如果 @Tool(stateInjected=true)
 *
 *   这些参数不会出现在 JSON Schema 中。
 *
 * 七、好的描述示例
 * ---------------
 *   ✅ "城市名，如：北京、上海、广州"
 *   ✅ "日期格式：YYYY-MM-DD"
 *   ✅ "温度单位：celsius 或 fahrenheit"
 *
 *   ❌ "city"（太简单，LLM 可能传错格式）
 *   ❌ "输入"（没有提供任何有用信息）
 *
 * ============================================================================
 */

package io.agentscope.core.tool;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to describe parameters of a tool method.
 *
 * <p>This annotation is required for all parameters of methods annotated with {@link Tool} (except
 * {@link ToolEmitter} which is auto-injected). It provides metadata for generating JSON schemas
 * that describe the tool's parameters to LLMs.
 *
 * <p><b>Usage Example:</b>
 *
 * <pre>{@code
 * @Tool(name = "calculate_area", description = "Calculate rectangle area")
 * public double calculateArea(
 *     @ToolParam(name = "width", description = "Width in meters", required = true)
 *     double width,
 *     @ToolParam(name = "height", description = "Height in meters", required = true)
 *     double height,
 *     @ToolParam(name = "unit", description = "Unit of measurement", required = false)
 *     String unit
 * ) {
 *     // Implementation
 * }
 * }</pre>
 *
 * <p><b>Important Notes:</b>
 * <ul>
 *   <li>The {@code name} attribute is <b>required</b> because Java does not preserve parameter
 *       names at runtime by default</li>
 *   <li>Parameter names should follow snake_case convention for LLM compatibility</li>
 *   <li>Descriptions help the LLM understand what values to provide</li>
 *   <li>{@link ToolEmitter} parameters do not need this annotation (they are framework-injected)</li>
 * </ul>
 *
 * @see Tool
 * @see ToolEmitter
 */
@Target({ElementType.PARAMETER, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ToolParam {

    /**
     * The name of the tool parameter.
     *
     * <p><b>This attribute is required</b> because Java does not preserve parameter names at
     * runtime by default (unless compiled with {@code -parameters} flag, which is not reliable).
     * The toolkit uses this name to map LLM-provided arguments to method parameters.
     *
     * <p>Names should follow snake_case convention (e.g., "file_path", "max_results") for
     * compatibility with various LLM providers.
     *
     * @return The parameter name as it should appear in the tool schema
     */
    String name();

    /**
     * Whether this parameter is required.
     *
     * <p>Required parameters must be provided by the LLM when invoking the tool. Optional
     * parameters can be omitted, and the method will receive null (for objects) or default values
     * (for primitives).
     *
     * @return true if required (default), false if optional
     */
    boolean required() default true;

    /**
     * The description of this parameter.
     *
     * <p>This description is sent to the LLM as part of the tool schema to help it understand:
     * <ul>
     *   <li>What this parameter represents</li>
     *   <li>What format or values are expected</li>
     *   <li>Any constraints or validation rules</li>
     * </ul>
     *
     * <p>Good descriptions improve the LLM's ability to provide correct parameter values.
     *
     * @return The parameter description, or empty string if not provided
     */
    String description() default "";
}
