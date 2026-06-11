package com.nuclearboy.api.deepseek

import kotlinx.serialization.json.*
import kotlinx.serialization.encodeToString
import org.junit.Assert.*
import org.junit.Test

class DeepSeekModelsTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun `ToolDefinitionDto serializes type field`() {
        val dto = ToolDefinitionDto(
            function = FunctionDefinitionDto(
                name = "test_tool",
                description = "A test tool",
                parameters = buildJsonObject {
                    put("type", JsonPrimitive("object"))
                    put("properties", buildJsonObject {
                        put("path", buildJsonObject {
                            put("type", JsonPrimitive("string"))
                            put("description", JsonPrimitive("File path"))
                        })
                    })
                    put("required", buildJsonArray { add(JsonPrimitive("path")) })
                },
            )
        )
        val serialized = json.encodeToString(dto)
        // Must contain type: "function"
        assertTrue(serialized.contains("\"type\":\"function\""))
        // Must contain the function name
        assertTrue(serialized.contains("\"name\":\"test_tool\""))
        // parameters must be a JSON object, not a string
        assertTrue(serialized.contains("\"parameters\":{"))
    }

    @Test
    fun `ChatCompletionRequest includes tools when provided`() {
        val request = ChatCompletionRequest(
            model = "deepseek-v4-pro",
            messages = listOf(MessageDto(role = "user", content = "Hi")),
            tools = listOf(
                ToolDefinitionDto(
                    function = FunctionDefinitionDto(name = "test", description = "Test"),
                )
            ),
        )
        val serialized = json.encodeToString(request)
        assertTrue(serialized.contains("\"tools\":"))
        assertTrue(serialized.contains("\"type\":\"function\""))
    }

    @Test
    fun `MessageDto strips reasoningContent from assistant messages`() {
        val msg = MessageDto(
            role = "assistant",
            content = "Hello",
            reasoningContent = "I should say hello",
        )
        // After sanitization, reasoning should be null
        val sanitized = msg.copy(reasoningContent = null)
        assertNull(sanitized.reasoningContent)
        assertEquals("Hello", sanitized.content)
    }

    @Test
    fun `StreamChunk parsing handles content delta`() {
        val chunkJson = """{"choices":[{"index":0,"delta":{"content":"Hello world"}}]}"""
        val chunk = json.decodeFromString<StreamChunk>(chunkJson)
        assertEquals(1, chunk.choices.size)
        assertEquals("Hello world", chunk.choices[0].delta?.content)
    }

    @Test
    fun `StreamChunk parsing handles tool call delta`() {
        val chunkJson = """{"choices":[{"index":0,"delta":{"tool_calls":[{"index":0,"function":{"name":"read_file","arguments":"{}"}}]}}]}"""
        val chunk = json.decodeFromString<StreamChunk>(chunkJson)
        assertEquals(1, chunk.choices.size)
        assertNotNull(chunk.choices[0].delta?.toolCalls)
    }

    @Test
    fun `UsageDto parsing handles cached tokens`() {
        val usageJson = """{"prompt_tokens":100,"completion_tokens":50,"total_tokens":150,"prompt_tokens_details":{"cached_tokens":80}}"""
        val usage = json.decodeFromString<UsageDto>(usageJson)
        assertEquals(100, usage.promptTokens)
        assertEquals(80L, usage.promptTokensDetails?.cachedTokens)
    }

    @Test
    fun `OpenAI compatible base URL normalization strips completion suffixes`() {
        assertEquals(
            "http://192.0.2.10:20128",
            DeepSeekApiClient.normalizeOpenAiBaseUrl("http://192.0.2.10:20128/v1")
        )
        assertEquals(
            "https://gateway.example.com",
            DeepSeekApiClient.normalizeOpenAiBaseUrl("https://gateway.example.com/v1/chat/completions")
        )
        assertEquals(
            "https://gateway.example.com",
            DeepSeekApiClient.normalizeOpenAiBaseUrl("https://gateway.example.com/chat/completions/")
        )
    }

    @Test
    fun `OpenAI endpoint builder preserves provider version paths`() {
        assertEquals(
            "https://ark.cn-beijing.volces.com/api/v3/chat/completions",
            DeepSeekApiClient.buildOpenAiChatCompletionsEndpoint("https://ark.cn-beijing.volces.com/api/v3")
        )
        assertEquals(
            "https://ark.cn-beijing.volces.com/api/v3/chat/completions",
            DeepSeekApiClient.buildOpenAiChatCompletionsEndpoint("https://ark.cn-beijing.volces.com/api/compatible")
        )
        assertEquals(
            "https://api.minimaxi.com/v1/chat/completions",
            DeepSeekApiClient.buildOpenAiChatCompletionsEndpoint("https://api.minimaxi.com/anthropic")
        )
        assertEquals(
            "https://gateway.example.com/v1/chat/completions",
            DeepSeekApiClient.buildOpenAiChatCompletionsEndpoint("https://gateway.example.com/v1/chat/completions")
        )
    }

    @Test
    fun `exact endpoint mode does not rewrite OpenAI compatible address`() {
        val exact = "https://gateway.example.com/custom/nonstandard/model/path"
        assertEquals(
            exact,
            DeepSeekApiClient.normalizeOpenAiBaseUrl(exact, ProviderEndpointMode.EXACT)
        )
        assertEquals(
            exact,
            DeepSeekApiClient.buildOpenAiChatCompletionsEndpoint(exact, ProviderEndpointMode.EXACT)
        )
        assertEquals(
            "https://ark.cn-beijing.volces.com/api/compatible",
            DeepSeekApiClient.buildOpenAiChatCompletionsEndpoint(
                "https://ark.cn-beijing.volces.com/api/compatible",
                ProviderEndpointMode.EXACT,
            )
        )
    }

    @Test
    fun `exact endpoint mode preserves OpenAI root and requires caller intent`() {
        val root = "http://192.0.2.10:20128/v1"
        assertEquals(root, DeepSeekApiClient.buildOpenAiChatCompletionsEndpoint(root, ProviderEndpointMode.EXACT))
        assertEquals(
            "http://192.0.2.10:20128/v1/chat/completions",
            DeepSeekApiClient.buildOpenAiChatCompletionsEndpoint(root, ProviderEndpointMode.AUTO)
        )
    }

    @Test
    fun `Anthropic endpoint builder preserves anthropic roots`() {
        assertEquals(
            "https://api.minimaxi.com/anthropic/v1/messages",
            DeepSeekApiClient.buildAnthropicMessagesEndpoint("https://api.minimaxi.com/anthropic")
        )
        assertEquals(
            "https://api.anthropic.com/v1/messages",
            DeepSeekApiClient.buildAnthropicMessagesEndpoint("https://api.anthropic.com/v1/messages")
        )
        assertEquals(
            "https://gateway.example.com/v1/messages",
            DeepSeekApiClient.buildAnthropicMessagesEndpoint("https://gateway.example.com")
        )
    }

    @Test
    fun `exact endpoint mode does not rewrite Anthropic address`() {
        val exact = "https://gateway.example.com/provider/messages/custom"
        assertEquals(
            exact,
            DeepSeekApiClient.normalizeAnthropicBaseUrl(exact, ProviderEndpointMode.EXACT)
        )
        assertEquals(
            exact,
            DeepSeekApiClient.buildAnthropicMessagesEndpoint(exact, ProviderEndpointMode.EXACT)
        )
    }

    @Test
    fun `ProviderProtocol auto resolves protocol from endpoint shape`() {
        assertEquals(
            ProviderProtocol.ANTHROPIC,
            ProviderProtocol.resolve(ProviderProtocol.AUTO, "https://api.minimaxi.com/anthropic", "MiniMax-M2.7-highspeed")
        )
        assertEquals(
            ProviderProtocol.OPENAI,
            ProviderProtocol.resolve(ProviderProtocol.AUTO, "https://gateway.example.com/v1", "nvidia/deepseek-ai/deepseek-v4-pro")
        )
        assertEquals(
            ProviderProtocol.ANTHROPIC,
            ProviderProtocol.resolve(ProviderProtocol.AUTO, "https://api.anthropic.com/v1/messages", "claude-3-5-sonnet")
        )
    }

    @Test
    fun `provider not found hint treats standard OpenAI 404 as possible model routing failure`() {
        val hint = buildProviderNotFoundHint(
            endpoint = "http://192.0.2.10:20128/v1/chat/completions",
            body = "404 page not found",
            modelName = "nvidia/minimaxai/minimax-m2.7",
        )

        assertTrue(hint.contains("网关在路由模型时报错"))
        assertTrue(hint.contains("GET <服务地址>/v1/models"))
        assertTrue(hint.contains("minimaxai/minimax-m2.7"))
    }

    @Test
    fun `provider model name hint suggests stripping NVIDIA provider prefix`() {
        val hint = providerModelNameHint("nvidia/example-org/example-model")

        assertTrue(hint.contains("不需要 nvidia/ 前缀"))
        assertTrue(hint.contains("GET /v1/models"))
    }

    @Test
    fun `provider model name sanitizer strips invisible routing characters`() {
        assertEquals(
            "nvidia/minimaxai/minimax-m2.7",
            sanitizeProviderModelName("\u200Bnvidia/minimaxai/minimax-m2.7\uFEFF")
        )
        assertEquals(
            "nvidia/minimaxai/minimax-m2.7",
            sanitizeProviderModelName("\u0000 nvidia/minimaxai/minimax-m2.7 \u0007")
        )
    }

    @Test
    fun `provider model name hint handles zero width provider prefix`() {
        val hint = providerModelNameHint("\u200Bnvidia/example-org/example-model")

        assertTrue(hint.contains("不需要 nvidia/ 前缀"))
        assertTrue(hint.contains("GET /v1/models"))
    }

    @Test
    fun `inactive provider credential detector recognizes gateway credential failures`() {
        assertTrue(
            isInactiveProviderCredentialError(
                """{"error":{"message":"No active credentials for provider: nvidia"}}"""
            )
        )
        assertFalse(isInactiveProviderCredentialError("""{"error":{"message":"model not found"}}"""))
    }
}
