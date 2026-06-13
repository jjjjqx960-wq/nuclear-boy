# -*- coding: utf-8 -*-
"""端到端通道加密：手机与电脑共享 token，据此派生 AES-256-GCM 密钥加密每条消息。

目的：走公网中继时，中继运营方/网络中间人只能看到密文，看不到任务内容，也拿不到
token（加密握手本身即"知道 token"的证明，token 不再明文上链）。借鉴 termly/happy
的 AES-256-GCM 思路，但因双方已共享 token，省去 DH，直接 key=SHA256(token)。

线路格式：明文消息 JSON 串 → AES-GCM(key, nonce) → base64(nonce[12] + ct + tag)，
外面再包一层 {"enc": "<base64>"}。服务端解出后即普通消息；GCM 校验通过即完成鉴权。
向后兼容：不带 enc 字段的明文消息走旧逻辑。
"""
import base64
import hashlib
import json

from cryptography.hazmat.primitives.ciphers.aead import AESGCM

NONCE_BYTES = 12


def derive_key(token: str) -> bytes:
    """token → 32 字节 AES-256 密钥。"""
    return hashlib.sha256(token.encode("utf-8")).digest()


def encrypt(key: bytes, plaintext: str, nonce: bytes = None) -> str:
    """加密明文串，返回 base64(nonce + 密文 + tag)。nonce 仅测试时显式传入。"""
    if nonce is None:
        import os
        nonce = os.urandom(NONCE_BYTES)
    ct = AESGCM(key).encrypt(nonce, plaintext.encode("utf-8"), None)
    return base64.b64encode(nonce + ct).decode("ascii")


def decrypt(key: bytes, b64: str) -> str:
    """解出明文串；密钥不对/被篡改会抛异常（GCM 校验失败）。"""
    blob = base64.b64decode(b64)
    nonce, ct = blob[:NONCE_BYTES], blob[NONCE_BYTES:]
    return AESGCM(key).decrypt(nonce, ct, None).decode("utf-8")


def envelope(key: bytes, message: str) -> str:
    """把明文消息串包成加密信封 {"enc": "..."} 串。"""
    return json.dumps({"enc": encrypt(key, message)})


def is_envelope(msg: dict) -> bool:
    return isinstance(msg, dict) and "enc" in msg and isinstance(msg["enc"], str)
