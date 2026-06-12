package com.nuclearboy.remotepc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PcCryptoTest {

    // 固定向量：与电脑端 tests/test_crypto.py 完全一致，证明 Python↔Kotlin 字节级互通
    private val vecToken = "testtoken123"
    private val vecPlaintext = """{"type":"auth"}"""
    private val vecB64 = "AAAAAAAAAAAAAAAAndB43PZpKc4nVxp+hCGGiHP3QuznWO3KmNNIYC9aKg=="

    @Test
    fun `fixed vector matches python output byte-for-byte`() {
        val key = PcCrypto.deriveKey(vecToken)
        // 用全零 nonce 复现固定密文（与 Python 同）
        val produced = PcCrypto.encrypt(key, vecPlaintext, nonce = ByteArray(12))
        assertEquals(vecB64, produced)
    }

    @Test
    fun `decrypts python-produced ciphertext`() {
        val key = PcCrypto.deriveKey(vecToken)
        assertEquals(vecPlaintext, PcCrypto.decrypt(key, vecB64))
    }

    @Test
    fun `roundtrip with random nonce`() {
        val key = PcCrypto.deriveKey("hello")
        val b64 = PcCrypto.encrypt(key, "你好 nuclear")
        assertEquals("你好 nuclear", PcCrypto.decrypt(key, b64))
    }

    @Test
    fun `random nonce makes ciphertext differ each time`() {
        val key = PcCrypto.deriveKey("k")
        assertNotEquals(PcCrypto.encrypt(key, "same"), PcCrypto.encrypt(key, "same"))
    }

    @Test
    fun `wrong key fails to decrypt`() {
        val b64 = PcCrypto.encrypt(PcCrypto.deriveKey("token-a"), "secret")
        try {
            PcCrypto.decrypt(PcCrypto.deriveKey("token-b"), b64)
            assertTrue("应解密失败", false)
        } catch (e: Exception) {
            // expected (GCM tag mismatch)
        }
    }

    @Test
    fun `envelope wraps as enc json`() {
        val key = PcCrypto.deriveKey("t")
        val env = PcCrypto.envelope(key, """{"type":"ping"}""")
        assertTrue(env.startsWith("""{"enc":""""))
        // 解析 enc 字段并解密还原
        val b64 = env.substringAfter(""""enc":"""").substringBeforeLast("\"")
        assertEquals("""{"type":"ping"}""", PcCrypto.decrypt(key, b64))
    }
}
