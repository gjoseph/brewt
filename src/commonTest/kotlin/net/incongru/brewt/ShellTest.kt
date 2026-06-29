package net.incongru.brewt

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ShellTest {

    @Test
    fun capturesStdoutAndExitCode() {
        val r = runCommand("echo hi")
        assertEquals(0, r.exitCode)
        assertEquals("hi", r.output)
        assertTrue(r.ok)
    }

    /**
     * Regression: a bare `command 2>&1` binds the redirect to the *last* element only,
     * so the stderr of an earlier command in a compound line escapes capture. The group
     * wrap `{ command ; } 2>&1` fixes it.
     */
    @Test
    fun mergesStderrForCompoundCommand() {
        val r = runCommand("echo out && echo err >&2")
        assertContains(r.output, "out")
        assertContains(r.output, "err")
    }

    /** Same hazard across a pipe: the left side's stderr must still be merged. */
    @Test
    fun mergesStderrFromLeftSideOfPipe() {
        val r = runCommand("ls /no_such_path_xyz | cat")
        assertContains(r.output, "no_such_path_xyz")
    }

    @Test
    fun mergeStderrFalseCapturesStdoutOnly() {
        val r = runCommand("echo out; echo err >&2", mergeStderr = false)
        assertEquals("out", r.output)
    }

    @Test
    fun nonZeroExitIsReportedAndOrThrowThrows() {
        val r = runCommand("ls /definitely_nonexistent_xyz")
        assertFalse(r.ok)
        assertNotEquals(0, r.exitCode)
        assertFailsWith<IllegalStateException> { r.orThrow() }
    }

    @Test
    fun orThrowReturnsSelfOnSuccess() {
        assertEquals("ok", runCommand("echo ok").orThrow().output)
    }

    /** The tee: output is streamed to [onOutput] live AND captured in the result. */
    @Test
    fun teeStreamsAndCaptures() {
        val streamed = StringBuilder()
        val r = runCommand("printf 'a\nb\nc\n'", onOutput = { streamed.append(it) })
        assertEquals("a\nb\nc", r.output)          // captured copy is trimEnd()-ed
        assertEquals("a\nb\nc\n", streamed.toString())  // streamed chunks are raw
    }
}
