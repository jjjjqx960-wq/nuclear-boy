# -*- coding: utf-8 -*-
"""配对二维码载荷：把手机连接所需的 url + token 编码成 nbpair URI。

手机扫码后只需解析出 url 和 token 两项即可填入设置并保存——
局域网直连 url 形如 ws://192.168.1.10:7860；
公网中继 url 形如 ws://服务器:8970/client/<room>?key=口令
（room/key 已经包含在 url 里，无需单独传递）。
"""
from urllib.parse import urlencode, urlparse, parse_qs

PAIR_SCHEME = "nbpair"


def make_pair_uri(url: str, token: str) -> str:
    """把连接地址和 token 编码成 nbpair://pair?u=...&t=... URI。"""
    if not url or not token:
        raise ValueError("url 和 token 都不能为空")
    query = urlencode({"u": url, "t": token})
    return f"{PAIR_SCHEME}://pair?{query}"


def parse_pair_uri(uri: str):
    """解析 nbpair URI，返回 {"url":..., "token":...}；非法返回 None。"""
    if not uri or not uri.startswith(f"{PAIR_SCHEME}://"):
        return None
    parsed = urlparse(uri)
    qs = parse_qs(parsed.query)
    url = qs.get("u", [""])[0]
    token = qs.get("t", [""])[0]
    if not url or not token:
        return None
    return {"url": url, "token": token}
