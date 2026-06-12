package com.nuclearboy.remotepc

import org.junit.Assert.assertEquals
import org.junit.Test

class CharWidthTest {

    @Test
    fun `ascii is single width`() {
        assertEquals(1, CharWidth.of('A'))
        assertEquals(1, CharWidth.of('9'))
        assertEquals(1, CharWidth.of(' '))
    }

    @Test
    fun `cjk han is double width`() {
        assertEquals(2, CharWidth.of(0x4E2D.toChar())) // 中
        assertEquals(2, CharWidth.of(0x6587.toChar())) // 文
        assertEquals(2, CharWidth.of(0x6838.toChar())) // 核
    }

    @Test
    fun `kana and hangul are double width`() {
        assertEquals(2, CharWidth.of(0x3042.toChar())) // あ 平假名
        assertEquals(2, CharWidth.of(0x30AB.toChar())) // カ 片假名
        assertEquals(2, CharWidth.of(0xD55C.toChar())) // 한 谚文
    }

    @Test
    fun `fullwidth forms are double width`() {
        assertEquals(2, CharWidth.of(0xFF21.toChar())) // 全角 A
        assertEquals(2, CharWidth.of(0xFF01.toChar())) // 全角感叹号
    }

    @Test
    fun `combining mark is zero width`() {
        assertEquals(0, CharWidth.of(0x0301.toChar())) // 组合锐音符
    }

    @Test
    fun `null char is zero width`() {
        assertEquals(0, CharWidth.of(0.toChar()))
    }
}
