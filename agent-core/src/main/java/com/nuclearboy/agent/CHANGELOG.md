2026-06-14 1.1.22 AgentEngine 接入 RetryableErrorGate，临时可重试错误只发 Retrying，最终失败才发 Error；新增 RetryableErrorGate 零件。
2026-06-14 1.1.21 AgentEngine 移除固定 20 次工具调用迭代上限，循环停止条件改为最终回复或取消；连续可重试 API 错误最多重试 3 次，并通过 ToolCallLoopGuard 阻断连续重复同一批工具调用。
2026-06-12 1.1.6 SystemPromptBuilder/ProjectContext 加 customInstructions 注入（用户自定义指令段），SystemPromptBuilderTest 3 例。
