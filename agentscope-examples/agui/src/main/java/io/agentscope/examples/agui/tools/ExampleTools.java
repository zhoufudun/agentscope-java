/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.agentscope.examples.agui.tools;

import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolEmitter;
import io.agentscope.core.tool.ToolParam;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Random;

/**
 * Example tools for the AG-UI demo.
 *
 * <p>These tools demonstrate how to create tool functions that can be called
 * by the agent during conversation.
 */
/**
 * 【学习注释：面向 AG-UI 的普通工具与进度事件】
 *
 * <p>AG-UI 不要求工具使用特殊接口：带 {@code @Tool} 的普通 Java 方法仍由 Toolkit 注册，
 * 模型仅能看到 {@code @ToolParam} 所描述的业务参数。{@code ToolEmitter} 不是模型参数，
 * 而是框架在调用工具时注入的“进度通道”。
 *
 * <pre>
 * 模型选择 get_weather(city)
 *       │
 *       ▼
 * Toolkit 调用 Java 方法 ──► emitter.emit(...) ──► ToolResult*DeltaEvent ──► 前端进度
 *       │
 *       └──────────────────► return ToolResultBlock ───────────────────────► 给模型的最终结果
 * </pre>
 *
 * <p>因此，{@code emit} 适合状态、日志和进度，{@code return} 才是模型应依据的最终事实。
 * 示例中的天气数据和表达式解析器仅用于演示工具协议，生产环境应替换为经过校验的服务调用。
 */
public class ExampleTools {

    private final Random random = new Random();

    /**
     * Get weather information for a city.
     *
     * <p>Note: This is a mock implementation for demo purposes.
     *
     * @param city The city name to get weather for
     * @return Weather information
     */
    @Tool(name = "get_weather", description = "Get current weather information for a city")
    public ToolResultBlock getWeather(
            @ToolParam(name = "city", description = "The city name (e.g., 'Beijing', 'New York')")
                    String city,
            ToolEmitter emitter) {
        // Mock weather data
        String[] conditions = {"Sunny", "Cloudy", "Partly Cloudy", "Rainy", "Overcast"};
        String condition = conditions[random.nextInt(conditions.length)];
        int temperature = random.nextInt(35) + 5; // 5-40 degrees
        int humidity = random.nextInt(60) + 30; // 30-90%

        // Pass progress information in metadata
        // To take effect, it is necessary to customize the ToolResultBlockConverter to override the
        // framework default
        emitter.emit(
                ToolResultBlock.builder()
                        .name("get_weather")
                        .metadata(Map.of("progress", "50%"))
                        .build());

        String result =
                String.format(
                        "Weather in %s:\n- Condition: %s\n- Temperature: %d°C\n- Humidity: %d%%",
                        city, condition, temperature, humidity);

        emitter.emit(
                ToolResultBlock.builder()
                        .name("get_weather")
                        .metadata(Map.of("progress", "100%"))
                        .build());
        return ToolResultBlock.text(result);
    }

    /**
     * Perform a simple calculation.
     *
     * @param expression The math expression to evaluate
     * @return The calculation result
     */
    @Tool(
            name = "calculate",
            description = "Perform a simple arithmetic calculation (supports +, -, *, /)")
    public ToolResultBlock calculate(
            @ToolParam(
                            name = "expression",
                            description =
                                    "The arithmetic expression to calculate (e.g., '2 + 3 * 4')")
                    String expression) {
        try {
            // Simple expression evaluator (for demo purposes)
            double result = evaluateExpression(expression);
            return ToolResultBlock.text("Result: " + formatNumber(result));
        } catch (Exception e) {
            return ToolResultBlock.error("Failed to calculate: " + e.getMessage());
        }
    }

    /**
     * Get the current date and time.
     *
     * @return Current date and time
     */
    @Tool(name = "get_current_time", description = "Get the current date and time")
    public ToolResultBlock getCurrentTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return ToolResultBlock.text("Current time: " + now.format(formatter));
    }

    /**
     * Simple expression evaluator.
     * Supports basic arithmetic: +, -, *, /
     */
    private double evaluateExpression(String expression) {
        // Remove whitespace
        expression = expression.replaceAll("\\s+", "");

        // Handle addition and subtraction (lowest precedence)
        int lastPlusOrMinus = -1;
        int parenDepth = 0;
        for (int i = expression.length() - 1; i >= 0; i--) {
            char c = expression.charAt(i);
            if (c == ')') parenDepth++;
            else if (c == '(') parenDepth--;
            else if (parenDepth == 0 && (c == '+' || c == '-') && i > 0) {
                lastPlusOrMinus = i;
                break;
            }
        }

        if (lastPlusOrMinus > 0) {
            String left = expression.substring(0, lastPlusOrMinus);
            String right = expression.substring(lastPlusOrMinus + 1);
            char op = expression.charAt(lastPlusOrMinus);
            if (op == '+') {
                return evaluateExpression(left) + evaluateExpression(right);
            } else {
                return evaluateExpression(left) - evaluateExpression(right);
            }
        }

        // Handle multiplication and division
        int lastMultOrDiv = -1;
        parenDepth = 0;
        for (int i = expression.length() - 1; i >= 0; i--) {
            char c = expression.charAt(i);
            if (c == ')') parenDepth++;
            else if (c == '(') parenDepth--;
            else if (parenDepth == 0 && (c == '*' || c == '/')) {
                lastMultOrDiv = i;
                break;
            }
        }

        if (lastMultOrDiv > 0) {
            String left = expression.substring(0, lastMultOrDiv);
            String right = expression.substring(lastMultOrDiv + 1);
            char op = expression.charAt(lastMultOrDiv);
            if (op == '*') {
                return evaluateExpression(left) * evaluateExpression(right);
            } else {
                return evaluateExpression(left) / evaluateExpression(right);
            }
        }

        // Handle parentheses
        if (expression.startsWith("(") && expression.endsWith(")")) {
            return evaluateExpression(expression.substring(1, expression.length() - 1));
        }

        // Parse number
        return parseNumber(expression);
    }

    private double parseNumber(String expression) {
        try {
            return Double.parseDouble(expression);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Invalid numeric value in expression: '" + expression + "'", e);
        }
    }

    private String formatNumber(double value) {
        if (value == (long) value) {
            return String.valueOf((long) value);
        }
        return String.format("%.4f", value);
    }
}
