package app.expressiveclock.alarm

import org.junit.Assert.*
import org.junit.Test

class SoundPlaybackTest {
    private class Fake : SoundPlayback.Backend {
        var granted = true
        var focus: (Boolean) -> Unit = {}
        var abandoned = 0
        var exhausted = 0
        val players = mutableListOf<P>()
        override fun requestFocus(changed: (Boolean) -> Unit): Boolean { focus = changed; return granted }
        override fun abandonFocus() { abandoned++ }
        override fun create(source: String): SoundPlayback.Player {
            if (source == "throw") throw IllegalArgumentException("Unreadable source")
            return P(source).also { players += it }
        }
        override fun exhausted() { exhausted++ }
    }
    private class P(val source: String) : SoundPlayback.Player {
        var ready: () -> Unit = {}; var fail: () -> Unit = {}
        var starts = 0; var pauses = 0; var releases = 0
        override fun prepare(ready: () -> Unit, failed: () -> Unit) { this.ready = ready; fail = failed }
        override fun start() { starts++ }
        override fun pause() { pauses++ }
        override fun release() { releases++ }
    }
    @Test fun waitsForPreparationAndFocus() {
        val f = Fake().apply { granted = false }; val s = SoundPlayback(f)
        s.play(listOf("custom")); val p = f.players.single(); p.ready()
        assertEquals(0, p.starts); f.focus(true); assertEquals(1,p.starts)
        f.focus(false); assertEquals(1,p.pauses); s.stop(); assertEquals(1,p.releases)
        assertEquals(2,f.abandoned)
    }
    @Test fun invalidCustomFallsBackAndReleasesFailedPlayer() {
        val f=Fake(); val s=SoundPlayback(f); s.play(listOf("custom","default","bundled"))
        f.players[0].fail(); assertEquals(1,f.players[0].releases)
        f.players[1].fail(); f.players[2].ready(); assertEquals(1,f.players[2].starts)
        assertEquals(0,f.exhausted)
        assertEquals(listOf("custom","default","bundled"),f.players.map { it.source })
    }
    @Test fun lateCallbacksCannotReviveStoppedOrReplacedPlayback() {
        val f=Fake(); val s=SoundPlayback(f); s.play(listOf("first")); val old=f.players[0]
        val oldFocus=f.focus; s.play(listOf("second")); old.ready(); old.fail(); oldFocus(true)
        assertEquals(2,f.players.size); assertEquals(0,old.starts)
        val current=f.players[1]; s.stop(); current.ready(); assertEquals(0,current.starts)
    }
    @Test fun exhaustionDoesNotLoopForever() {
        val f=Fake(); val s=SoundPlayback(f); s.play(listOf("broken")); f.players[0].fail()
        assertEquals(1,f.exhausted); assertEquals(1,f.players[0].releases)
    }
    @Test fun thrownSourceFailureAdvancesToDefault() {
        val f=Fake(); val s=SoundPlayback(f); s.play(listOf("throw","default"))
        f.players.single().ready()
        assertEquals("default",f.players.single().source)
        assertEquals(1,f.players.single().starts)
    }
    @Test fun losingFocusDuringPreparationDoesNotStartSound() {
        val f=Fake(); val s=SoundPlayback(f); s.play(listOf("custom"))
        f.focus(false); f.players.single().ready()
        assertEquals(0,f.players.single().starts)
        f.focus(true); assertEquals(1,f.players.single().starts)
    }
    @Test fun previewPauseRetainsPlayerAndReleasesFocus() {
        val f = Fake(); val s = SoundPlayback(f)
        s.play(listOf("custom")); val p = f.players.single(); p.ready()
        val staleFocus = f.focus
        s.pause()
        assertEquals(1, p.pauses); assertEquals(0, p.releases)
        staleFocus(true); assertEquals(1, p.starts)
        s.resume(); assertEquals(2, p.starts); assertEquals(1, f.players.size)
        s.stop(); assertEquals(1, p.releases)
    }
    @Test fun pauseWhilePreparingCannotStartUntilResumedAndFocused() {
        val f = Fake(); val s = SoundPlayback(f)
        s.play(listOf("custom")); val p = f.players.single()
        s.pause(); p.ready(); assertEquals(0, p.starts)
        f.granted = false; s.resume(); assertEquals(0, p.starts)
        f.focus(true); assertEquals(1, p.starts)
    }
    @Test fun stoppedPausedPreviewCannotResumeOrRevive() {
        val f = Fake(); val s = SoundPlayback(f)
        s.play(listOf("custom")); val p = f.players.single(); p.ready()
        s.pause(); s.resume(); val focus = f.focus
        s.stop(); s.resume(); focus(true)
        assertEquals(2, p.starts); assertEquals(1, p.releases)
    }
}
