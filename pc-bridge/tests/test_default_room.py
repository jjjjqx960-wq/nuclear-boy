# -*- coding: utf-8 -*-
"""中继默认 room 用 token 哈希、不泄露 token 明文。

用法: python tests/test_default_room.py
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from core.config import default_room


def test_room_is_hash_not_prefix():
    token = "a" * 64
    room = default_room(token)
    assert len(room) == 24
    assert room != token[:24], "room 不能是 token 前缀（会泄露明文）"
    assert all(c in "0123456789abcdef" for c in room)


def test_room_is_deterministic():
    token = "deadbeef" * 8
    assert default_room(token) == default_room(token)


def test_different_tokens_different_rooms():
    assert default_room("x" * 64) != default_room("y" * 64)


if __name__ == "__main__":
    test_room_is_hash_not_prefix()
    test_room_is_deterministic()
    test_different_tokens_different_rooms()
    print("[PASS] 默认 room 哈希化全部通过")
