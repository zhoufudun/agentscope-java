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
 * 【核心概念】PermissionBehavior - 权限行为枚举
 * ============================================================================
 *
 * 一、这是什么？
 * -------------
 * PermissionBehavior 定义了权限检查的四种结果。
 * 它是 HITL（人在回路）系统的核心。
 *
 * 二、四种行为
 * -----------
 *   ┌──────────────┬─────────────────────────────────────┐
 *   │ ALLOW        │ 直接允许执行，无需用户确认          │
 *   │ DENY         │ 直接拒绝执行，不询问用户            │
 *   │ ASK          │ 暂停执行，询问用户是否允许          │
 *   │ PASSTHROUGH  │ 工具放弃决策，交给权限引擎          │
 *   └──────────────┴─────────────────────────────────────┘
 *
 * 三、决策流程
 * -----------
 *   LLM 决定调用工具
 *        │
 *        ▼
 *   PermissionEngine 检查规则表
 *        │
 *        ├─ 匹配 ALLOW 规则 → 直接执行
 *        ├─ 匹配 DENY 规则  → 直接拒绝
 *        ├─ 匹配 ASK 规则   → 请求用户确认
 *        └─ 无匹配规则      ─┬→ 调用 tool.checkPermissions()
 *                           │
 *                           ├─ ALLOW → 执行
 *                           ├─ DENY  → 拒绝
 *                           ├─ ASK   → 请求用户确认
 *                           └─ PASSTHROUGH → 使用默认模式
 *
 * 四、典型场景
 * -----------
 *   工具         │ 典型权限
 *   ─────────────┼─────────────
 *   read_file    │ ALLOW（只读，安全）
 *   write_file   │ ASK（可能覆盖文件）
 *   execute_bash │ ASK（可能执行危险命令）
 *   delete_all   │ DENY（太危险）
 *
 * 五、配置示例
 * -----------
 *   // 在 ReActAgent 中配置权限
 *   ReActAgent.builder()
 *       .permissionConfig(PermissionConfig.builder()
 *           .defaultBehavior(PermissionBehavior.ASK)
 *           .allowRule("read_file", "*")          // 允许读取任何文件
 *           .denyRule("write_file", "/etc/*")    // 禁止写入 /etc
 *           .build())
 *       .build();
 *
 * ============================================================================
 */

package io.agentscope.core.permission;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;

/**
 * Outcome of a permission rule or tool self-check.
 *
 * <ul>
 *   <li>{@link #ALLOW}: proceed without further checks.
 *   <li>{@link #DENY}: reject the tool invocation.
 *   <li>{@link #ASK}: defer to the user for a confirmation.
 *   <li>{@link #PASSTHROUGH}: tool defers decision back to the engine for rule matching.
 * </ul>
 */
public enum PermissionBehavior {
    ALLOW("allow"),
    DENY("deny"),
    ASK("ask"),
    PASSTHROUGH("passthrough");

    private final String value;

    PermissionBehavior(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    /**
     * Resolves a string (case-insensitive) to a {@code PermissionBehavior}.
     *
     * @param value JSON / config string representation
     * @return matching behavior
     * @throws IllegalArgumentException if no enum value matches
     */
    @JsonCreator
    public static PermissionBehavior fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("PermissionBehavior value must not be null");
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        for (PermissionBehavior behavior : values()) {
            if (behavior.value.equals(normalized)) {
                return behavior;
            }
        }
        throw new IllegalArgumentException("Unknown PermissionBehavior: " + value);
    }
}
