# -*- coding: utf-8 -*-
"""只读文件操作：让手机端 AI 直接浏览/读取电脑上的文件，不必为看一眼文件就起 CLI。

借鉴 claudecodeui 的文件 API（browse-filesystem / file content），但仅保留只读，
不提供写/删/改名，降低远程风险。读取有大小上限、二进制按需截断。
"""
import os

DEFAULT_MAX_BYTES = 64 * 1024
HARD_MAX_BYTES = 1024 * 1024
MAX_ENTRIES = 1000


def list_dir(path: str) -> dict:
    """列目录：返回 {path, entries:[{name,isDir,size}]}；不存在/非目录抛 ValueError。"""
    if not path:
        raise ValueError("路径不能为空")
    abspath = os.path.abspath(path)
    if not os.path.exists(abspath):
        raise ValueError(f"路径不存在: {abspath}")
    if not os.path.isdir(abspath):
        raise ValueError(f"不是目录: {abspath}")
    entries = []
    try:
        names = sorted(os.listdir(abspath))
    except PermissionError:
        raise ValueError(f"没有权限读取目录: {abspath}")
    for name in names[:MAX_ENTRIES]:
        full = os.path.join(abspath, name)
        try:
            is_dir = os.path.isdir(full)
            size = 0 if is_dir else os.path.getsize(full)
        except OSError:
            is_dir, size = False, 0
        entries.append({"name": name, "isDir": is_dir, "size": size})
    return {"path": abspath, "entries": entries, "truncated": len(names) > MAX_ENTRIES}


def read_file(path: str, max_bytes: int = DEFAULT_MAX_BYTES) -> dict:
    """读文件文本：返回 {path, content, size, truncated}；不存在/是目录/过大抛 ValueError。"""
    if not path:
        raise ValueError("路径不能为空")
    abspath = os.path.abspath(path)
    if not os.path.exists(abspath):
        raise ValueError(f"文件不存在: {abspath}")
    if os.path.isdir(abspath):
        raise ValueError(f"这是目录不是文件: {abspath}")
    cap = max(1, min(int(max_bytes or DEFAULT_MAX_BYTES), HARD_MAX_BYTES))
    size = os.path.getsize(abspath)
    try:
        with open(abspath, "rb") as f:
            raw = f.read(cap)
    except PermissionError:
        raise ValueError(f"没有权限读取文件: {abspath}")
    truncated = size > cap
    # 以 UTF-8 解码，非法字节用替换符，保证总能给 AI 看到文本
    content = raw.decode("utf-8", errors="replace")
    return {"path": abspath, "content": content, "size": size, "truncated": truncated}


def write_file(path: str, content: str, append: bool = False) -> dict:
    """写文本文件（覆盖或追加）：返回 {path, bytes}。

    危险操作——手机端在发送本请求前已经过用户审批；这里只做基本防护：
    父目录必须存在（不自动创建，避免误写到意外位置），内容大小有上限。
    """
    if not path:
        raise ValueError("路径不能为空")
    abspath = os.path.abspath(path)
    parent = os.path.dirname(abspath)
    if parent and not os.path.isdir(parent):
        raise ValueError(f"父目录不存在: {parent}（不自动创建，请先建目录）")
    if os.path.isdir(abspath):
        raise ValueError(f"这是目录不是文件: {abspath}")
    data = (content or "").encode("utf-8")
    if len(data) > HARD_MAX_BYTES:
        raise ValueError(f"内容过大（{len(data)} 字节，上限 {HARD_MAX_BYTES}）")
    mode = "ab" if append else "wb"
    try:
        with open(abspath, mode) as f:
            f.write(data)
    except PermissionError:
        raise ValueError(f"没有权限写入文件: {abspath}")
    return {"path": abspath, "bytes": len(data)}
