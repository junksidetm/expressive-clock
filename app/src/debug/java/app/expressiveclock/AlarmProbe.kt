package app.expressiveclock

import android.app.Instrumentation
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import app.expressiveclock.alarm.*
import app.expressiveclock.core.Alarm
import app.expressiveclock.data.ClockStore
import kotlinx.coroutines.runBlocking
import java.time.*

/** Opt-in adb instrument probe, compiled only into debug. Adds/removes only its own alarm. */
class AlarmProbe : Instrumentation() {
    private var document: String? = null
    private var deferred = false
    override fun onCreate(arguments: Bundle?) { document = arguments?.getString("document"); deferred = arguments?.getString("deferred") == "true"; super.onCreate(arguments); start() }
    private fun verifyDocument(name: String) {
        val context=targetContext
        val result=Bundle()
        var selected: android.net.Uri? = null
        var player: AlarmAudio? = null
        var code=Activity.RESULT_OK
        try {
            startActivitySync(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            selected=context.contentResolver.persistedUriPermissions.map { it.uri }.firstOrNull { uri ->
                if (uri.authority !in setOf("com.android.providers.downloads.documents","com.android.externalstorage.documents")) false
                else context.contentResolver.query(uri,arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),null,null,null)?.use { cursor ->
                    cursor.moveToFirst() && cursor.getString(0)==name
                } == true
            }
            checkNotNull(selected) { "Selected document has no persisted read grant" }
            val started=java.util.concurrent.CountDownLatch(1)
            runOnMainSync {
                player=AlarmAudio(context,false).also { sound ->
                    sound.onStarted={ source -> if (source==selected.toString()) started.countDown() }
                    sound.play(selected.toString())
                }
            }
            check(started.await(8,java.util.concurrent.TimeUnit.SECONDS)) { "Persisted document did not play after process restart" }
            result.putString("stream","PASS: OpenDocument grant and selected audio playback survived process restart\n")
        } catch (e: Throwable) { code=Activity.RESULT_CANCELED; result.putString("stream","failure: ${e.stackTraceToString()}\n") }
        finally {
            runOnMainSync { player?.stop() }
            selected?.let { uri ->
                context.contentResolver.releasePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        finish(code,result)
    }
    override fun onStart() {
        document?.let { verifyDocument(it); return }

        val report = Bundle()
        val context = targetContext
        val store = ClockStore(context)
        var customUri: android.net.Uri? = null
        var id = -1L
        var media: android.media.MediaPlayer? = null
        var mediaFocus: android.media.AudioFocusRequest? = null
        val audioManager=context.getSystemService(android.media.AudioManager::class.java)
        var testAudio: AlarmAudio? = null
        var ownsRinger = false
        var ownsTimer = false
        var result = Activity.RESULT_OK
        try {
            startActivitySync(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            // DataStore mutation tests are scoped to a newly allocated ID.
            runBlocking {
                id = store.nextId()
                val time = ZonedDateTime.now().plusMinutes(if (deferred) 32 else 10).withSecond(0).withNano(0)
                val alarm = Alarm(id,time.toLocalTime(),"ADB regression probe",DayOfWeek.entries.toSet())
                store.updateAlarms { it + alarm }
                AlarmScheduler.schedule(context,alarm)
                val at=alarm.nextFire(ZonedDateTime.now())!!.toInstant().toEpochMilli()
                val manager=context.getSystemService(android.app.NotificationManager::class.java)
                if (deferred && manager.areNotificationsEnabled()) {
                    check(manager.activeNotifications.none { it.tag=="upcoming:$id" }) { "Upcoming notification posted too early" }
                    val notifyAt=at-30*60_000L
                    while (System.currentTimeMillis()<notifyAt+15_000 && manager.activeNotifications.none { it.tag=="upcoming:$id" })
                        kotlinx.coroutines.delay(200)
                    check(System.currentTimeMillis()>=notifyAt) { "Notification preceded 30 minute threshold" }
                    report.putString("deferredNotification","PASS: native AlarmManager delivered 30-minute callback")
                }
                val notification=manager.activeNotifications.firstOrNull { it.tag=="upcoming:$id" }
                if (manager.areNotificationsEnabled()) {
                    checkNotNull(notification) { "Upcoming notification missing" }
                    notification.notification.actions.first().actionIntent.send()
                    repeat(100) {
                        if (store.alarmsNow().first { it.id==id }.skippedOccurrenceMillis != at)
                            kotlinx.coroutines.delay(50)
                    }
                    report.putString("notificationAction","PASS: real notification PendingIntent dismissed occurrence")
                } else {
                    UpcomingAlarm.receive(context,Intent().setAction(UpcomingAlarm.SKIP)
                        .putExtra(AlarmReceiver.EXTRA_ID,id).putExtra(UpcomingAlarm.AT,at))
                    report.putString("notificationAction","SKIP: notification permission denied; receiver tested directly")
                }
                val skipped=store.alarmsNow().first { it.id==id }
                check(skipped.enabled && skipped.days==alarm.days)
                check(skipped.skippedOccurrenceMillis==at)
                check(skipped.nextFire(ZonedDateTime.now())!!.toInstant().toEpochMilli()>at)
                report.putString("skipOccurrence","PASS: stored skip and preserved repeat")
            }
            // A real MediaStore custom file is read through the same content URI API as picked audio.
            val values=android.content.ContentValues().apply {
                put(android.provider.MediaStore.Audio.Media.DISPLAY_NAME,"ExpressiveClock-ADB-probe.wav")
                put(android.provider.MediaStore.Audio.Media.MIME_TYPE,"audio/wav")
                put(android.provider.MediaStore.Audio.Media.RELATIVE_PATH,"Music/ExpressiveClockTests")
            }
            customUri=context.contentResolver.insert(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,values)!!
            context.contentResolver.openOutputStream(customUri!!).use { output ->
                checkNotNull(output)
                context.resources.openRawResource(R.raw.alarm_fallback).use { it.copyTo(output) }
            }
            val customStarted=java.util.concurrent.CountDownLatch(1)
            runOnMainSync {
                testAudio=AlarmAudio(context,false).also { sound ->
                    sound.onStarted={ source -> if (source==customUri.toString()) customStarted.countDown() }
                    sound.play(customUri.toString())
                }
            }
            check(customStarted.await(8,java.util.concurrent.TimeUnit.SECONDS)) { "Custom content URI failed" }
            runOnMainSync { testAudio?.stop() }
            report.putString("customAudio","PASS: custom MediaStore WAV content URI started")
            val bundledStarted=java.util.concurrent.CountDownLatch(1)
            val bundled="android.resource://${context.packageName}/${R.raw.alarm_fallback}"
            runOnMainSync {
                testAudio=AlarmAudio(context,false).also { sound ->
                    sound.onStarted={ source -> if (source==bundled) bundledStarted.countDown() }
                    sound.play(bundled)
                }
            }
            check(bundledStarted.await(8,java.util.concurrent.TimeUnit.SECONDS)) { "Bundled fallback URI failed" }
            runOnMainSync { testAudio?.stop() }
            report.putString("bundledFallback","PASS: packaged last-resort tone started")
            val focusLost=java.util.concurrent.CountDownLatch(1)
            val focusRestored=java.util.concurrent.CountDownLatch(1)
            runOnMainSync {
                mediaFocus=android.media.AudioFocusRequest.Builder(android.media.AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_MEDIA).build())
                    .setOnAudioFocusChangeListener { if (it<0) { media?.pause(); focusLost.countDown() } else if (focusLost.count==0L) focusRestored.countDown() }
                    .build()
                check(audioManager.requestAudioFocus(mediaFocus!!)==android.media.AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
                media=android.media.MediaPlayer.create(context,R.raw.alarm_fallback).apply { isLooping=true; setVolume(.1f,.1f); start() }
            }
            val done=java.util.concurrent.CountDownLatch(1)
            var fallback=false
            var audio: AlarmAudio? = null
            runOnMainSync {
                audio=AlarmAudio(context,looping=false).also {
                    testAudio=it
                    it.onFallback={fallback=true}
                    it.onStarted={done.countDown()}
                    it.play("content://app.expressiveclock.missing/nonexistent")
                }
            }
            // Exercise actual platform error/fallback asynchronously, bounded and without volume changes.
            check(done.await(8, java.util.concurrent.TimeUnit.SECONDS)) { "No playback start after invalid URI recovery" }
            runOnMainSync { audio?.stop(); }
            check(fallback)
            check(focusLost.await(3,java.util.concurrent.TimeUnit.SECONDS)) { "Media did not receive focus loss" }
            check(focusRestored.await(3,java.util.concurrent.TimeUnit.SECONDS)) { "Media focus not restored" }
            report.putString("audioFocus","PASS: temporary media received focus loss")
            report.putString("invalidAudio","PASS: invalid URI triggered recovery; inspect audio logs for playback")
            // Real AlarmManager -> receiver -> foreground ringer, using a short snooze deadline.
            runBlocking {
                val now=ZonedDateTime.now()
                val imminent=store.alarmsNow().filter { it.id!=id }.any {
                    it.nextFire(now)?.toInstant()?.toEpochMilli()?.let { due -> due-System.currentTimeMillis()<90_000 } == true
                }
                if (!imminent) {
                    val one=Alarm(id,now.toLocalTime(),"ADB scheduled alarm",soundUri="content://app.expressiveclock.missing/nonexistent",snoozedUntilMillis=System.currentTimeMillis()+6000)
                    store.updateAlarms { list -> list.map { if (it.id==id) one else it } }
                    ownsRinger=true
                    AlarmScheduler.schedule(context,one)
                    repeat(240) {
                        if (store.alarmsNow().first { it.id==id }.enabled) kotlinx.coroutines.delay(100)
                    }
                    check(!store.alarmsNow().first { it.id==id }.enabled) { "Exact alarm callback did not fire" }
                    ownsRinger=true
                    kotlinx.coroutines.delay(1500)
                    @Suppress("DEPRECATION")
                    val running=context.getSystemService(android.app.ActivityManager::class.java).getRunningServices(100)
                    check(running.any { it.service.className==AlarmService::class.java.name }) { "Ringer service missing" }
                    report.putString("scheduledAlarm","PASS: exact snooze callback disarmed one-shot and started ringer")
                    androidx.core.content.ContextCompat.startForegroundService(context,
                        Intent(context,AlarmService::class.java).setAction(AlarmService.ACTION_START)
                            .putExtra(AlarmReceiver.EXTRA_ID,-98765L))
                    kotlinx.coroutines.delay(400)
                    @Suppress("DEPRECATION")
                    val afterStale=context.getSystemService(android.app.ActivityManager::class.java).getRunningServices(100)
                    check(afterStale.any { it.service.className==AlarmService::class.java.name }) { "Stale start silenced active ringer" }
                    report.putString("staleStart","PASS: missing alarm start did not stop active ringer")
                    val manager = context.getSystemService(android.app.NotificationManager::class.java)
                    if (manager.areNotificationsEnabled()) {
                        val ringing = manager.activeNotifications.firstOrNull { it.id == Notifications.ID_RINGING }
                        checkNotNull(ringing) { "Ringing notification missing" }
                        checkNotNull(ringing.notification.contentIntent) { "Ringing notification cannot reopen controls" }
                        check(ringing.notification.actions.size >= 2) { "Ringing actions missing" }
                        checkNotNull(ringing.notification.deleteIntent) { "Ringing swipe dismissal has no handler" }.send()
                        kotlinx.coroutines.delay(500)
                        @Suppress("DEPRECATION")
                        val afterDismiss = context.getSystemService(android.app.ActivityManager::class.java).getRunningServices(100)
                        check(afterDismiss.none { it.service.className == AlarmService::class.java.name }) { "Notification dismissal left alarm ringing" }
                        report.putString("ringingNotificationDismiss", "PASS: actual ringing delete PendingIntent stopped ringer; reopen/snooze/dismiss controls present")
                    } else {
                        AlarmService.stop(context)
                        report.putString("ringingNotificationDismiss", "SKIP: notification permission denied")
                    }
                    kotlinx.coroutines.delay(300)
                    if (store.timerNow() == null) {
                        ownsTimer = true
                        val timer=app.expressiveclock.core.ClockTimer(id,"ADB timer regression",Duration.ofSeconds(3),
                            app.expressiveclock.core.TimerState.RUNNING,android.os.SystemClock.elapsedRealtime()+3000)
                        store.putTimer(timer)
                        TimerScheduler.sync(context,timer)
                        repeat(80) {
                            if (store.timerNow()?.state != app.expressiveclock.core.TimerState.FINISHED) kotlinx.coroutines.delay(100)
                        }
                        check(store.timerNow()?.state==app.expressiveclock.core.TimerState.FINISHED) { "Timer callback did not finish" }
                        kotlinx.coroutines.delay(500)
                        @Suppress("DEPRECATION")
                        val services=context.getSystemService(android.app.ActivityManager::class.java).getRunningServices(100)
                        check(services.any { it.service.className==AlarmService::class.java.name }) { "Timer ringer missing" }
                        report.putString("timerExpiry","PASS: native timer expiry and foreground ringer")
                    } else report.putString("timerExpiry","SKIP: existing timer preserved")
                } else report.putString("scheduledAlarm","SKIP: existing alarm imminent")
            }
            report.putString("stream", "PASS: occurrence persisted, repeat preserved; custom WAV URI started; invalid URI fallback started; media focus loss and restoration observed; exact alarm callback exercised when no existing alarm is imminent. Speaker audibility not asserted.\n")

        } catch (e: Throwable) {
            report.putString("failure",e.stackTraceToString())
            result = Activity.RESULT_CANCELED
        } finally {
            if (ownsRinger) {
                AlarmService.stop(context)
                context.startActivity(Intent(context,MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
            }
            runOnMainSync {
                testAudio?.stop()
                media?.release()
                mediaFocus?.let { audioManager.abandonAudioFocusRequest(it) }
            }
            customUri?.let { context.contentResolver.delete(it,null,null) }
            if (ownsTimer) runBlocking {
                if (store.timerNow()?.id == id) { store.putTimer(null); TimerScheduler.sync(context,null) }
            }
            if (id>=0) runBlocking {
                AlarmScheduler.cancel(context,id)
                store.updateAlarms { list -> list.filterNot { it.id==id } }
            }
        }
        report.putString("stream", report.keySet().filter { it != "stream" }.joinToString("\n") { "$it: ${report.getString(it)}" } + "\n")
        finish(result,report)
    }
}
