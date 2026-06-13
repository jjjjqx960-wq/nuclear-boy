# -*- coding: utf-8 -*-
"""配对二维码载荷编解码测试。

用法: python tests/test_pairing.py
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from core.pairing import make_pair_uri, parse_pair_uri


def test_lan_roundtrip():
    uri = make_pair_uri("ws://192.168.1.10:7860", "tok123")
    assert parse_pair_uri(uri) == {"url": "ws://192.168.1.10:7860", "token": "tok123"}


def test_relay_url_with_query_roundtrip():
    url = "ws://1.2.3.4:8970/client/myroom?key=secret"
    uri = make_pair_uri(url, "abc")
    # url 里的 ? & 必须被正确编码且解析还原，不破坏外层 query
    assert parse_pair_uri(uri) == {"url": url, "token": "abc"}


def test_reject_invalid():
    assert parse_pair_uri("bad") is None
    assert parse_pair_uri("nbpair://pair?u=x") is None  # 缺 token
    assert parse_pair_uri("nbpair://pair?t=x") is None  # 缺 url
    assert parse_pair_uri("https://evil/pair?u=a&t=b") is None  # scheme 不符


def test_make_requires_both():
    for bad in [("", "t"), ("u", "")]:
        try:
            make_pair_uri(*bad)
            assert False, "应抛 ValueError"
        except ValueError:
            pass


if __name__ == "__main__":
    test_lan_roundtrip()
    test_relay_url_with_query_roundtrip()
    test_reject_invalid()
    test_make_requires_both()
    print("[PASS] 配对载荷编解码全部通过")
