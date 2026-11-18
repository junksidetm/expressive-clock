package app.expressiveclock.installer

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Assume.assumeNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.json.JSONObject
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.time.Instant

/** Real installed APK bytes, read only. Never commits an installation or authenticates. */
@RunWith(AndroidJUnit4::class)
class VerificationTest {
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
    private fun candidate(source:File):Pair<Work,Policy.Request> {
        val work=Work.get(context)
        work.cancel()
        val target=File(context.cacheDir,"verified.apk")
        source.copyTo(target,overwrite=true)
        val info=context.packageManager.getPackageArchiveInfo(target.path,0)!!
        val evidence=JSONObject().put("version_code",info.longVersionCode)
            .put("version_name",info.versionName).put("apk_bytes",target.length())
            .put("built_at",Instant.now().toString())
        Work::class.java.getDeclaredField("evidence").apply {isAccessible=true}.set(work,evidence)
        return work to Policy.Request(1,2,3,"a".repeat(32),Policy.digest(target))
    }
    private fun verify(work:Work,request:Policy.Request) {
        Work::class.java.getDeclaredMethod("verify",Policy.Request::class.java).apply {isAccessible=true}.invoke(work,request)
    }
    private fun installed():File {
        val app=runCatching {context.packageManager.getApplicationInfo(Policy.PACKAGE,0)}.getOrNull()
        assumeNotNull(app)
        return File(app!!.sourceDir)
    }
    @Test fun acceptsActualInstalledClockDebugSignatureWithoutInstalling() {
        val (work,request)=candidate(installed());verify(work,request)
    }
    @Test fun rejectsCompanionApkAsClockUpdate() {
        val (work,request)=candidate(File(context.applicationInfo.sourceDir))
        try {verify(work,request);fail("Helper APK accepted as Clock")}
        catch(e:InvocationTargetException){assertTrue(e.cause is SecurityException)}
    }
    @Test fun rejectsChangedApkBytes() {
        val (work,request)=candidate(installed())
        java.io.RandomAccessFile(File(context.cacheDir,"verified.apk"),"rw").use {
            it.seek(100);val b=it.readByte();it.seek(100);it.writeByte(b.toInt() xor 1)
        }
        try {verify(work,request);fail("Changed bytes accepted")}
        catch(e:InvocationTargetException){assertTrue(e.cause is SecurityException)}
    }
    @Test fun keystoreCredentialsRoundTripAndDisconnectRejectsLateSave() {
        val store=CredentialStore(context)
        val revision=store.revision()
        val value=JSONObject().put("test","synthetic-not-a-token")
        store.save(value,revision)
        assertEquals("synthetic-not-a-token",store.load().getString("test"))
        store.clear();assertFalse(store.hasCredentials())
        try {store.save(value,revision);fail("Late save restored disconnected credentials")}
        catch(expected:SecurityException){}
    }

    @Test fun rejectsTamperedApkEvenWhenAdvertisedHashMatches() {
        val (work,request)=candidate(installed())
        val apk=File(context.cacheDir,"verified.apk")
        java.io.RandomAccessFile(apk,"rw").use {
            it.seek(100);val b=it.readByte();it.seek(100);it.writeByte(b.toInt() xor 1)
        }
        val tampered=Policy.Request(request.run(),request.artifact(),request.manifest(),request.build(),Policy.digest(apk))
        try {verify(work,tampered);fail("Broken signature accepted")}
        catch(e:InvocationTargetException){assertTrue(e.cause is SecurityException)}
    }
}
