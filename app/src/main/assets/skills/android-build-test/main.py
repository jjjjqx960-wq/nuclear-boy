"""
Android build/test skill for Nuclear Boy.

Default mode prints a deterministic command plan. Pass execute=true to run
only allowlisted Gradle/ADB commands. Output is redacted before printing.
"""

import os
import re
import shutil
import subprocess
import sys
from pathlib import Path


SECRET_PATTERNS = [
    re.compile(r"Bearer\s+[A-Za-z0-9._~+/=-]+", re.I),
    re.compile(r"sk-[A-Za-z0-9_-]+"),
    re.compile(r"(?i)(api[-_ ]?key\s*[:=]\s*)[A-Za-z0-9._~+/=-]+"),
]

ALLOWED_ACTIONS = {
    "doctor",
    "assemble",
    "unit-test",
    "app-test",
    "api-test",
    "adb-diag",
    "all",
    "clean",
}


def _bool(value):
    return str(value or "").strip().lower() in {"1", "true", "yes", "y", "on"}


def _redact(text):
    safe = text or ""
    for pattern in SECRET_PATTERNS:
        safe = pattern.sub(lambda m: (m.group(1) if m.groups() else "") + "<REDACTED_TOKEN>", safe)
    return safe


def _find_project_root(start):
    current = Path(start or ".").resolve()
    if current.is_file():
        current = current.parent
    for item in [current, *current.parents]:
        if (item / "settings.gradle.kts").is_file() and (item / "gradlew.bat").is_file():
            return item
    return current


def _gradle_cmd(root, *args):
    if os.name == "nt":
        return ["cmd.exe", "/c", str(root / "gradlew.bat"), *args]
    return [str(root / "gradlew"), *args]


def _adb_cmd(*args):
    return ["adb", *args]


def _command_label(cmd):
    if cmd[:2] == ["cmd.exe", "/c"] and len(cmd) >= 3:
        return " ".join([Path(cmd[2]).name, *cmd[3:]])
    return " ".join(str(x) for x in cmd)


def _run(cmd, cwd, timeout=300):
    try:
        result = subprocess.run(
            cmd,
            cwd=str(cwd),
            text=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            timeout=timeout,
            shell=False,
        )
        output = (result.stdout or "") + ("\n" + result.stderr if result.stderr else "")
        return result.returncode, _redact(output)
    except FileNotFoundError as exc:
        return 127, f"命令不存在：{exc.filename}"
    except subprocess.TimeoutExpired:
        return 124, f"命令超时：{_command_label(cmd)}"


def _has_adb_device(root):
    code, output = _run(_adb_cmd("devices"), root, timeout=20)
    if code != 0:
        return False
    lines = [line.strip() for line in output.splitlines()]
    return any(line.endswith("\tdevice") for line in lines)


def _adb_diag_plan(root):
    return [
        _adb_cmd("devices"),
        _gradle_cmd(root, ":app:assembleDebug"),
        _adb_cmd("install", "-r", "app/build/outputs/apk/debug/app-debug.apk"),
        _adb_cmd("logcat", "-c"),
        _adb_cmd("shell", "am", "force-stop", "com.nuclearboy.app.debug"),
        _adb_cmd(
            "shell",
            "am",
            "broadcast",
            "-a",
            "com.nuclearboy.app.RUN_DIAGNOSTICS",
            "-n",
            "com.nuclearboy.app.debug/com.nuclearboy.app.diagnostics.DiagnosticsReceiver",
        ),
        _adb_cmd("shell", "sleep", "5"),
        _adb_cmd("logcat", "-d", "-t", "200", "-s", "NuclearBoy"),
    ]


def _build_plan(action, root, variant, module):
    variant_name = "Debug" if variant.lower() != "release" else "Release"
    module_name = module.strip().strip(":") or "app"
    module_task = f":{module_name}:assemble{variant_name}"
    if action == "doctor":
        return []
    if action == "clean":
        return [_gradle_cmd(root, "clean")]
    if action == "assemble":
        return [_gradle_cmd(root, module_task)]
    if action == "unit-test":
        return [_gradle_cmd(root, "test")]
    if action == "app-test":
        return [_gradle_cmd(root, ":app:testDebugUnitTest")]
    if action == "api-test":
        return [_gradle_cmd(root, ":api-deepseek:testDebugUnitTest")]
    if action == "adb-diag":
        return _adb_diag_plan(root)
    if action == "all":
        return [
            _gradle_cmd(root, ":app:assembleDebug"),
            _gradle_cmd(root, "test"),
            *_adb_diag_plan(root),
        ]
    return []


def _doctor(root):
    checks = [
        ("settings.gradle.kts", (root / "settings.gradle.kts").is_file()),
        ("gradlew.bat", (root / "gradlew.bat").is_file()),
        ("app module", (root / "app" / "build.gradle.kts").is_file()),
        ("api-deepseek module", (root / "api-deepseek" / "build.gradle.kts").is_file()),
        ("adb in PATH", shutil.which("adb") is not None),
    ]
    lines = ["核弹男孩 Android 编译测试 Doctor", f"项目根目录：{root}"]
    for name, ok in checks:
        lines.append(f"{'PASS' if ok else 'WARN'} {name}")
    return lines


def run(args=None):
    args = args or {}
    action = str(args.get("action", "doctor")).strip().lower() or "doctor"
    if action not in ALLOWED_ACTIONS:
        action = "doctor"
    execute = _bool(args.get("execute", "false"))
    variant = str(args.get("variant", "debug")).strip().lower() or "debug"
    module = str(args.get("module", "app")).strip() or "app"
    root = _find_project_root(args.get("project_dir", "."))

    lines = []
    lines.extend(_doctor(root))
    lines.append("")
    lines.append(f"操作：{action}")
    lines.append(f"模式：{'执行' if execute else '只生成计划'}")

    plan = _build_plan(action, root, variant, module)
    if plan:
        lines.append("命令计划：")
        for idx, cmd in enumerate(plan, 1):
            lines.append(f"{idx}. {_command_label(cmd)}")

    if not execute:
        lines.append("")
        lines.append("未执行命令。需要实际运行时传 execute=true。")
        output = "\n".join(lines)
        print(output)
        return {"output": output}

    lines.append("")
    lines.append("执行结果：")
    for cmd in plan:
        if cmd and cmd[0] == "adb" and action == "all" and not _has_adb_device(root):
            lines.append("SKIP adb-diag：未检测到在线 ADB 设备")
            break
        label = _command_label(cmd)
        lines.append(f"RUN {label}")
        code, output = _run(cmd, root, timeout=900 if "test" in label or "assemble" in label else 120)
        lines.append(f"EXIT {code}")
        tail = "\n".join(output.splitlines()[-40:])
        if tail.strip():
            lines.append(tail)
        if code != 0:
            break

    output = "\n".join(lines)
    print(output)
    return {"output": output}


if __name__ == "__main__":
    cli_args = {}
    for item in sys.argv[1:]:
        if item == "--execute":
            cli_args["execute"] = "true"
        elif "=" in item:
            key, value = item.split("=", 1)
            cli_args[key.strip("-")] = value
        elif "action" not in cli_args:
            cli_args["action"] = item
    run(cli_args)
