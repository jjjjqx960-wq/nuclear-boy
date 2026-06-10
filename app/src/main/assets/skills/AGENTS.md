## 目录职责

`app/src/main/assets/skills` 承载核弹男孩内置技能包。每个技能目录应包含 `skill.yaml` 和入口脚本。

## 边界

这里定义可安装/可执行技能，不承载 SkillManager Kotlin 解析逻辑，也不存放用户项目数据。

## 允许依赖

技能可以通过 `skill.yaml` 声明文件系统、网络、包和 shell 权限。入口脚本必须与声明权限保持一致。

## 禁止事项

不要在技能文件中硬编码凭据、私人接口地址或个人数据。涉及命令执行的技能必须使用白名单和脱敏输出。

## 常用命令

- `./gradlew :app:assembleDebug`
- `./gradlew :skills:testDebugUnitTest`

## 验证方式

新增技能后检查 `skill.yaml` 可解析，入口脚本可被本地 Python 导入或执行基础模式。
