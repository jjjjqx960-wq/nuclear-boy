# -*- coding: utf-8 -*-
"""通道加密单元测试 + 跨语言互通向量（须与安卓 PcCryptoTest 完全一致）。

用法: python tests/test_crypto.py
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from core import crypto

# 固定向量：与 remote-pc 的 PcCryptoTest 保持字节级一致，证明 Python↔Kotlin 互通
VEC_TOKEN = "testtoken123"
VEC_PLAINTEXT = '{"type":"auth"}'
VEC_B64 = "AAAAAAAAAAAAAAAAndB43PZpKc4nVxp+hCGGiHP3QuznWO3KmNNIYC9aKg=="


def test_roundtrip():
    key = crypto.derive_key("hello")
    b64 = crypto.encrypt(key, "你好 nuclear")
    assert crypto.decrypt(key, b64) == "你好 nuclear"


def test_fixed_vector():
    key = crypto.derive_key(VEC_TOKEN)
    # 用全零 nonce 复现固定密文
    assert crypto.encrypt(key, VEC_PLAINTEXT, nonce=bytes(12)) == VEC_B64, "向量不一致"
    # 解回明文
    assert crypto.decrypt(key, VEC_B64) == VEC_PLAINTEXT


def test_wrong_key_fails():
    key1 = crypto.derive_key("token-a")
    key2 = crypto.derive_key("token-b")
    b64 = crypto.encrypt(key1, "secret")
    try:
        crypto.decrypt(key2, b64)
        assert False, "错误密钥应解密失败"
    except Exception:
        pass


def test_envelope_detection():
    key = crypto.derive_key("t")
    env = crypto.envelope(key, '{"type":"ping"}')
    import json
    obj = json.loads(env)
    assert crypto.is_envelope(obj)
    assert not crypto.is_envelope({"type": "auth"})
    assert crypto.decrypt(key, obj["enc"]) == '{"type":"ping"}'


if __name__ == "__main__":
    test_roundtrip()
    test_fixed_vector()
    test_wrong_key_fails()
    test_envelope_detection()
    print("[PASS] 通道加密 + 固定向量全部通过")
