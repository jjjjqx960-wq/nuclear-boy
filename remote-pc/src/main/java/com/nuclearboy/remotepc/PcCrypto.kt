package com.nuclearboy.remotepc

import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * 端到端通道加密（与电脑端 core/crypto.py 字节级一致）。
 *
 * 手机与电脑共享 token，key = SHA-256(token)，AES-256-GCM 加密每条消息。走公网中继
 * 时中继只看到密文，拿不到 token（加密握手即"知道 token"的证明）。线路格式：
 * base64(nonce[12] + 密文 + tag)，外包 {"enc":"<base64>"}。
 */
object PcCrypto {

    private const val NONCE_BYTES = 12
    private const val TAG_BITS = 128

    /** token → 32 字节 AES-256 密钥。 */
    fun deriveKey(token: String): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(token.toByteArray(Charsets.UTF_8))

    /** 加密明文串 → base64(nonce + 密文 + tag)。nonce 仅测试时显式传入。 */
    fun encrypt(key: ByteArray, plaintext: String, nonce: ByteArray = randomNonce()): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, nonce))
        val ct = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(nonce + ct)
    }

    /** 解出明文串；密钥不对/被篡改会抛异常（GCM 校验失败）。 */
    fun decrypt(key: ByteArray, b64: String): String {
        val blob = Base64.getDecoder().decode(b64)
        val nonce = blob.copyOfRange(0, NONCE_BYTES)
        val ct = blob.copyOfRange(NONCE_BYTES, blob.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, nonce))
        return String(cipher.doFinal(ct), Charsets.UTF_8)
    }

    /** 把明文消息串包成加密信封 {"enc":"..."} 串。 */
    fun envelope(key: ByteArray, message: String): String =
        """{"enc":"${encrypt(key, message)}"}"""

    private fun randomNonce(): ByteArray {
        val n = ByteArray(NONCE_BYTES)
        java.security.SecureRandom().nextBytes(n)
        return n
    }
}
