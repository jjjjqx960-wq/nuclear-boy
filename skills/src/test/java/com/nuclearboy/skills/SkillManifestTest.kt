package com.nuclearboy.skills

import org.junit.Assert.*
import org.junit.Test

class SkillManifestTest {

    @Test
    fun `parameter validate string type`() {
        val param = SkillParameter("name", "string", "Name", required = true)
        assertTrue(param.validate("hello"))
        assertTrue(param.validate("123"))
        assertFalse(param.validate(""))
    }

    @Test
    fun `parameter validate int type`() {
        val param = SkillParameter("count", "int", "Count", required = true)
        assertTrue(param.validate("42"))
        assertTrue(param.validate("-1"))
        assertTrue(param.validate("0"))
        assertFalse(param.validate("not a number"))
        assertFalse(param.validate("3.14"))
    }

    @Test
    fun `parameter validate float type`() {
        val param = SkillParameter("rate", "float", "Rate", required = true)
        assertTrue(param.validate("3.14"))
        assertTrue(param.validate("42"))
        assertTrue(param.validate("-0.5"))
        assertFalse(param.validate("abc"))
    }

    @Test
    fun `parameter validate bool type`() {
        val param = SkillParameter("flag", "bool", "Flag", required = true)
        assertTrue(param.validate("true"))
        assertTrue(param.validate("false"))
        assertTrue(param.validate("True"))
        assertTrue(param.validate("False"))
        assertFalse(param.validate("yes"))
    }

    @Test
    fun `parameter validate choice type`() {
        val param = SkillParameter(
            name = "format",
            type = "choice",
            description = "Format",
            required = true,
            allowedValues = listOf("pdf", "docx", "txt"),
        )
        assertTrue(param.validate("pdf"))
        assertTrue(param.validate("docx"))
        assertFalse(param.validate("html"))
        assertFalse(param.validate(""))
    }

    @Test
    fun `not required parameter accepts empty`() {
        val param = SkillParameter(
            name = "optional",
            type = "string",
            description = "Optional",
            required = false,
        )
        assertTrue(param.validate(""))
    }

    @Test
    fun `required parameter rejects empty`() {
        val param = SkillParameter(
            name = "required_field",
            type = "string",
            description = "Required",
            required = true,
        )
        assertFalse(param.validate(""))
    }

    @Test
    fun `filesystem permissions glob matching`() {
        // workspace/** matches workspace/ and subdirectories
        assertTrue(FilesystemPermissions.matchesGlob("workspace/test.py", "workspace/**"))
        assertTrue(FilesystemPermissions.matchesGlob("workspace/sub/dir/file.txt", "workspace/**"))
        assertTrue(FilesystemPermissions.matchesGlob("workspace/", "workspace/**"))
        assertFalse(FilesystemPermissions.matchesGlob("/etc/passwd", "workspace/**"))
        assertFalse(FilesystemPermissions.matchesGlob("../outside.txt", "workspace/**"))
        assertFalse(FilesystemPermissions.matchesGlob("workspace/../../outside.txt", "workspace/**"))
    }

    @Test
    fun `filesystem permissions single wildcard`() {
        assertTrue(FilesystemPermissions.matchesGlob("test.py", "*.py"))
        assertFalse(FilesystemPermissions.matchesGlob("test.txt", "*.py"))
        assertFalse(FilesystemPermissions.matchesGlob("dir/test.py", "*.py"))
    }

    @Test
    fun `skill manifest isSandboxed computed correctly`() {
        val sandboxed = SkillManifest(
            name = "test",
            version = "1.0",
            description = "Test",
            permissions = SkillPermissions(
                filesystem = FilesystemPermissions(read = listOf("workspace/**"), write = listOf("workspace/**")),
                network = NetworkPermission(allowed = false),
                shell = ShellPermission(allowed = false),
            ),
        )
        assertTrue(sandboxed.permissions.isSandboxed)

        val unsandboxed = SkillManifest(
            name = "test2",
            version = "1.0",
            description = "Test2",
            permissions = SkillPermissions(
                shell = ShellPermission(allowed = true),
            ),
        )
        assertFalse(unsandboxed.permissions.isSandboxed)
    }
}
