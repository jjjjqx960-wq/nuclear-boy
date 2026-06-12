package com.nuclearboy.remotepc

import java.net.URLDecoder

/**
 * 配对二维码载荷：电脑端 `bridge.py pair` 生成的 `nbpair://pair?u=<url>&t=<token>`。
 *
 * 手机扫码后解析出地址和 token 直接填入设置——局域网与公网中继统一格式
 * （中继的 room/key 已经包含在 url 里，无需单独字段）。
 *
 * 不依赖 android.net.Uri，纯 JVM 解析便于单元测试；url 里自带的 ?key= 等
 * query 会被整体 percent-encode 在 u 参数内，解码后原样还原。
 */
data class PcPairingPayload(val url: String, val token: String) {
    companion object {
        const val SCHEME = "nbpair"
        private const val PREFIX = "$SCHEME://"

        /** 解析 nbpair URI，非法（scheme 不符 / 缺字段）返回 null。 */
        fun parse(raw: String?): PcPairingPayload? {
            val text = raw?.trim().orEmpty()
            if (!text.startsWith(PREFIX)) return null
            val queryStart = text.indexOf('?')
            if (queryStart < 0 || queryStart == text.length - 1) return null
            val params = parseQuery(text.substring(queryStart + 1))
            val url = params["u"]?.trim().orEmpty()
            val token = params["t"]?.trim().orEmpty()
            if (url.isBlank() || token.isBlank()) return null
            return PcPairingPayload(url, token)
        }

        private fun parseQuery(query: String): Map<String, String> =
            query.split('&').mapNotNull { pair ->
                val eq = pair.indexOf('=')
                if (eq <= 0) return@mapNotNull null
                val key = pair.substring(0, eq)
                val value = runCatching {
                    URLDecoder.decode(pair.substring(eq + 1), "UTF-8")
                }.getOrNull() ?: return@mapNotNull null
                key to value
            }.toMap()
    }
}
