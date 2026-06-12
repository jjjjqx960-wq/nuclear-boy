package com.nuclearboy.remotepc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PcPairingPayloadTest {

    @Test
    fun `parse lan pairing uri`() {
        val p = PcPairingPayload.parse("nbpair://pair?u=ws%3A%2F%2F192.168.1.10%3A7860&t=tok123")
        assertEquals("ws://192.168.1.10:7860", p?.url)
        assertEquals("tok123", p?.token)
    }

    @Test
    fun `parse relay uri preserves embedded query`() {
        val p = PcPairingPayload.parse(
            "nbpair://pair?u=ws%3A%2F%2F1.2.3.4%3A8970%2Fclient%2Fmyroom%3Fkey%3Dsecret&t=abc"
        )
        assertEquals("ws://1.2.3.4:8970/client/myroom?key=secret", p?.url)
        assertEquals("abc", p?.token)
    }

    @Test
    fun `reject wrong scheme`() {
        assertNull(PcPairingPayload.parse("https://evil/pair?u=a&t=b"))
        assertNull(PcPairingPayload.parse("ws://1.2.3.4:7860"))
    }

    @Test
    fun `reject missing fields`() {
        assertNull(PcPairingPayload.parse("nbpair://pair?u=ws%3A%2F%2Fx%3A1"))
        assertNull(PcPairingPayload.parse("nbpair://pair?t=tok"))
        assertNull(PcPairingPayload.parse("nbpair://pair"))
    }

    @Test
    fun `reject blank and null`() {
        assertNull(PcPairingPayload.parse(null))
        assertNull(PcPairingPayload.parse(""))
        assertNull(PcPairingPayload.parse("   "))
    }

    @Test
    fun `trims surrounding whitespace from scanned value`() {
        val p = PcPairingPayload.parse("  nbpair://pair?u=ws%3A%2F%2Fx%3A1&t=k  ")
        assertEquals("ws://x:1", p?.url)
        assertEquals("k", p?.token)
    }
}
