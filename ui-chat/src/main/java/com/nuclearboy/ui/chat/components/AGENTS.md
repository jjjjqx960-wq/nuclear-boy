## 目录职责

`components` 是 `ui-chat` 的组件层，组合零件和 Compose 基础控件，提供可复用的聊天 UI 功能单元，例如命令按钮、输入辅助控件和文件面板过滤输入。

## 边界

组件层可以渲染局部 UI 并向上抛出用户意图，但不直接访问 ViewModel、Agent、文件系统、网络或导航。

## 允许依赖

允许依赖 `ui-chat/parts`、Compose UI、Material 3 和聊天主题。

## 禁止事项

不要在组件层保存业务状态、执行斜杠命令、修改聊天记录或输出明文秘密值。

## 常用命令

- `./gradlew :ui-chat:compileDebugKotlin`
- `./gradlew :app:assembleDebug`

## 验证方式

验证组件在空对话、有历史消息和处理中三种状态下的显示与点击回调；文件面板输入类组件还要验证空查询、输入过滤和清除回调。
