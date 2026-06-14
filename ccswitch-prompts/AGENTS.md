## 目录职责

本目录承载用于 CC Switch 统一管理 Claude Code 与 Codex 的仓库级提示词预设、同步说明和提示词版本记录。

## 边界

只存放提示词、提示词说明、导入说明和目录级变更记录。不得放业务实现代码、构建产物、模型密钥、账号密码、Token 或个人数据。

## 允许依赖

本目录可以引用仓库根级 `AGENTS.md`、`CLAUDE.md`、`README.md` 和各模块 `AGENTS.md` 中的规范内容。提示词内容应保持工具无关，必要时只在专门小节区分 Claude Code 与 Codex 的运行差异。

## 禁止事项

不要把明文密钥、个人信息、完整日志、攻击载荷或未脱敏凭据写入提示词。不要在本目录新增 Android/Kotlin/Python 业务代码。

## 常用命令

- `Get-Content -Raw ccswitch-prompts/nuclear-boy-unified.md`
- `rg -n "Token|password|secret|api[_-]?key" ccswitch-prompts`

## 验证方式

确认 Markdown 可正常读取，且敏感信息搜索只出现规则说明，不出现明文秘密值。
