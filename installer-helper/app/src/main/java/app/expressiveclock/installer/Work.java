package app.expressiveclock.installer;

import android.app.PendingIntent;
import android.content.*;
import android.content.pm.*;
import android.os.*;
import org.json.JSONObject;
import java.io.*;
import java.nio.file.Files;
import java.util.concurrent.*;

/** One process-owned operation; rotations only replace the UI observer, never the download. */
final class Work {
    // Only application context is retained; the resumed Activity observer is detached in onPause.
    @android.annotation.SuppressLint("StaticFieldLeak")
    private static Work instance;
    static synchronized Work get(Context c) { if(instance==null)instance=new Work(c.getApplicationContext());return instance; }
    final Context context;
    final CredentialStore credentials;
    final GitHub github;
    final Handler main=new Handler(Looper.getMainLooper());
    final ExecutorService executor=Executors.newSingleThreadExecutor();
    MainActivity screen;
    Presentation.Phase phase=Presentation.Phase.READY;
    String status="Development builds, verified before installation.", code="", metadata="";
    int progress=-1;
    boolean busy=false,ready=false,autoInstallPending=false;
    Intent confirmation;
    Policy.Request request;
    private Future<?> task;
    private int generation=0;
    private int installSession=-1;
    private final File apk;
    private volatile JSONObject evidence;
    private Work(Context c) {context=c;credentials=new CredentialStore(c);github=new GitHub(credentials);apk=new File(c.getCacheDir(),"verified.apk");installSession=c.getSharedPreferences("install",0).getInt("session",-1);
        if(installSession!=-1){phase=Presentation.Phase.INSTALLING;busy=true;status="An Android installation is pending. Finish it or cancel to retry.";}
        else if(c.getSharedPreferences("install",0).contains("result")) {
            boolean ok=c.getSharedPreferences("install",0).getBoolean("result",false);
            phase=ok?Presentation.Phase.COMPLETED:Presentation.Phase.FAILED;
            status=ok?"Expressive Clock debug update installed.":"The last installation was cancelled or rejected by Android.";
        }
    }
    void changed() {if(screen!=null)screen.render();}
    void post(int owner,Runnable action) {main.post(()->{if(owner==generation){action.run();changed();}});}
    void beginLink(String link) {
        try {
            Policy.Request next=Policy.parse(link);
            if(next.equals(request) && (busy || ready)){changed();return;}
            if(installSession!=-1){status="Finish the current Android installation before opening another build.";changed();return;}
            if(busy) {status="Another operation is running. Finish it or cancel before opening a different build.";changed();return;}
            request=next;metadata="";evidence=null;ready=false;phase=Presentation.Phase.READY;status="Build selected. Connect GitHub to continue.";
            if(credentials.hasCredentials())download();else changed();
        } catch(Exception e){if(!busy){ready=false;autoInstallPending=false;request=null;}phase=Presentation.Phase.FAILED;status="Invalid or unsupported installation link.";changed();}
    }
    void connect(String client) {
        if(busy)return;
        if(!client.matches("[A-Za-z0-9_]{5,100}")){phase=Presentation.Phase.FAILED;status="Enter your approved GitHub App client ID first.";changed();return;}
        phase=Presentation.Phase.CONNECTING;busy=true;ready=false;code="";progress=-1;int owner=++generation;status="Requesting GitHub authorization…";changed();
        long revision=credentials.revision();
        task=executor.submit(()->{
            try {
                JSONObject device=GitHub.oauth("device/code",new JSONObject().put("client_id",client));
                if(!device.has("user_code"))throw new GitHub.UserError("GitHub rejected the request. Check the approved client ID and device-flow configuration.");
                String userCode=device.getString("user_code");
                post(owner,()->{code=userCode;status="Open GitHub, enter this code and approve repository read access.";});
                long deadline=System.nanoTime()+device.getLong("expires_in")*1_000_000_000L;
                long interval=Math.max(5,device.optLong("interval",5));
                while(System.nanoTime()<deadline) {
                    Thread.sleep(interval*1000);
                    JSONObject result=GitHub.oauth("oauth/access_token",new JSONObject().put("client_id",client)
                        .put("device_code",device.getString("device_code")).put("grant_type","urn:ietf:params:oauth:grant-type:device_code"));
                    String error=result.optString("error","");
                    if(error.equals("authorization_pending"))continue;
                    if(error.equals("slow_down")){interval+=5;continue;}
                    post(owner,()->{
                        try {github.saveTokens(result,client,revision);phase=Presentation.Phase.READY;busy=false;code="";status="GitHub connected.";if(request!=null)download();}
                        catch(Exception e){phase=Presentation.Phase.FAILED;busy=false;code="";status="Authorization could not be saved. Reconnect GitHub.";}
                    });return;
                }
                throw new GitHub.UserError("Authorization expired. Try connecting again.");
            } catch(Exception e){failed(owner,e);}
        });
    }
    void cancel() {
        generation++;if(installSession!=-1){try{context.getPackageManager().getPackageInstaller().abandonSession(installSession);}catch(Exception ignored){}setSession(-1);}
        if(task!=null)task.cancel(true);busy=false;ready=false;autoInstallPending=false;confirmation=null;code="";progress=-1;
        phase=Presentation.Phase.READY;status="Cancelled. Retry when ready.";changed();
    }
    void disconnect() {cancel();credentials.clear();status="GitHub disconnected. Stored tokens removed.";changed();}
    void download() {
        if(busy)return;
        if(request==null){phase=Presentation.Phase.READY;status="Open an Install APK notification to choose a build.";changed();return;}
        phase=Presentation.Phase.VERIFYING;busy=true;ready=false;progress=-1;status="Checking development build evidence…";int owner=++generation;Policy.Request selected=request;changed();
        task=executor.submit(()->{
            try {
                evidence=github.evidence(selected);
                String available="Version "+evidence.getString("version_name")+" · code "+evidence.getLong("version_code")+"\nBuilt "+evidence.getString("built_at")+" · Run "+selected.run();
                post(owner,()->{metadata=available;}); // Always revalidate access, expiry, run and authenticated manifest, even for cached APKs.
                if(!apk.isFile() || apk.length()>Policy.MAX_BYTES || !Policy.digest(apk).equals(selected.sha())) {
                    File source=new File(context.getCacheDir(),"artifact.tmp");
                    File pending=new File(context.getCacheDir(),"download.tmp");
                    try {
                        post(owner,()->{phase=Presentation.Phase.DOWNLOADING;status="Downloading your development build…";});
                        github.download("repos/"+Policy.REPO+"/actions/artifacts/"+selected.artifact()+"/zip",source,
                            (bytes,total)->post(owner,()->{phase=Presentation.Phase.DOWNLOADING;progress=total>0?(int)Math.min(100,bytes*100/total):-1;status="Downloading development build · "+bytes/1048576+" MB";}));
                        post(owner,()->{phase=Presentation.Phase.VERIFYING;status="Checking APK integrity and signing certificate…";});
                        Transport.unpack(source,pending,"ExpressiveClock-debug.apk",Policy.MAX_BYTES);
                        if(!Policy.digest(pending).equals(selected.sha()))throw new SecurityException("APK checksum does not match trusted evidence");
                        Files.move(pending.toPath(),apk.toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    } finally {source.delete();pending.delete();}
                }
                PackageInfo verified = verify(selected);
                String details=verified.versionName+" · version "+versionCode(verified)+"\nBuilt "+evidence.getString("built_at")+
                    "\nRun "+selected.run()+" · "+apk.length()/1048576+" MB";
                post(owner,()->{metadata=details;phase=Presentation.Phase.READY_TO_INSTALL;busy=false;ready=true;autoInstallPending=true;progress=100;status="Verified. Ready for Android installation confirmation.";if(screen!=null)screen.maybeInstall();});
            }catch(Exception e){failed(owner,e);}
        });
    }
    private PackageInfo verify(Policy.Request selected) throws Exception {
        if(apk.length()>Policy.MAX_BYTES || !Policy.digest(apk).equals(selected.sha()))throw new SecurityException("APK checksum verification failed");
        PackageManager pm=context.getPackageManager();
        PackageInfo info=pm.getPackageArchiveInfo(apk.getPath(),Build.VERSION.SDK_INT>=28?PackageManager.GET_SIGNING_CERTIFICATES:PackageManager.GET_SIGNATURES);
        if(info==null)throw new SecurityException("APK could not be parsed");
        Signature[] signers=Build.VERSION.SDK_INT>=28 && info.signingInfo!=null?info.signingInfo.getApkContentsSigners():info.signatures;
        if(signers==null)throw new SecurityException("APK signing certificate missing");
        String[] pins=new String[signers.length];for(int i=0;i<signers.length;i++)pins[i]=Policy.digest(signers[i].toByteArray());
        Policy.checkPackage(info.packageName,pins);
        com.android.apksig.ApkVerifier.Result signature = new com.android.apksig.ApkVerifier.Builder(apk)
            .setMinCheckedPlatformVersion(26).build().verify();
        if(!signature.isVerified() || signature.getSignerCertificates().size()!=1 ||
            !Policy.CERT.equals(Policy.digest(signature.getSignerCertificates().get(0).getEncoded())))
            throw new SecurityException("APK signature verification failed");
        if(evidence==null || versionCode(info)!=evidence.getLong("version_code") ||
            !info.versionName.equals(evidence.getString("version_name")) || apk.length()!=evidence.getLong("apk_bytes"))
            throw new SecurityException("APK build metadata mismatch");
        try {
            PackageInfo installed=pm.getPackageInfo(Policy.PACKAGE,Build.VERSION.SDK_INT>=28?PackageManager.GET_SIGNING_CERTIFICATES:PackageManager.GET_SIGNATURES);
            Signature[] existing=Build.VERSION.SDK_INT>=28 && installed.signingInfo!=null?installed.signingInfo.getApkContentsSigners():installed.signatures;
            if(existing==null)throw new SecurityException("Installed signing identity unavailable");
            String[] installedPins=new String[existing.length];
            for(int i=0;i<existing.length;i++)installedPins[i]=Policy.digest(existing[i].toByteArray());
            try { Policy.checkPackage(installed.packageName,installedPins); }
            catch(SecurityException mismatch) { throw new GitHub.UserError("Installed Clock has a different signing certificate. Update refused; existing app and data untouched."); }
            boolean same=Policy.digest(new File(installed.applicationInfo.sourceDir)).equals(selected.sha());
            try { Policy.checkUpdate(versionCode(info),versionCode(installed),same,
                java.time.Instant.parse(evidence.getString("built_at")).toEpochMilli(),installed.lastUpdateTime); }
            catch(SecurityException older) { throw new GitHub.UserError("This build is older than installed Clock. Downgrade refused; existing app and data untouched."); }
        } catch(PackageManager.NameNotFoundException absent) { /* Fresh installation is permitted. */ }
        return info;
    }
    private static long versionCode(PackageInfo info){return Build.VERSION.SDK_INT>=28?info.getLongVersionCode():info.versionCode;}
    void install() {
        if(busy || !ready || request==null)return;
        phase=Presentation.Phase.INSTALLING;busy=true;autoInstallPending=false;
        status="Preparing verified APK for Android confirmation…";changed();
        int owner=++generation;Policy.Request selected=request;
        task=executor.submit(()->{
            int sessionId=-1;
            try {
                PackageInfo verified=verify(selected);
                PackageInstaller installer=context.getPackageManager().getPackageInstaller();
                PackageInstaller.SessionParams params=new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
                params.setAppPackageName(Policy.PACKAGE);params.setSize(apk.length());
                if(Build.VERSION.SDK_INT>=31)params.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_REQUIRED);
                sessionId=installer.createSession(params);
                try(PackageInstaller.Session session=installer.openSession(sessionId)) {
                    try(InputStream in=new FileInputStream(apk);OutputStream out=session.openWrite("base.apk",0,apk.length())) {
                        byte[] buffer=new byte[32768];int n;
                        while((n=in.read(buffer))!=-1) {
                            if(Thread.currentThread().isInterrupted())throw new InterruptedException();
                            out.write(buffer,0,n);
                        }
                        session.fsync(out);
                    }
                    int id=sessionId;
                    main.post(()->{
                        if(owner!=generation || screen==null) {
                            try{installer.abandonSession(id);}catch(Exception ignored){}
                            if(owner==generation){busy=false;phase=Presentation.Phase.READY_TO_INSTALL;status="Return and tap Install to continue.";changed();}
                            return;
                        }
                        try(PackageInstaller.Session committing=installer.openSession(id)) {
                            // Persist correlation before commit. Only this explicit, non-exported receiver gets results.
                            setSession(id);
                            Intent callback=new Intent(context,InstallResultReceiver.class).putExtra("owner_session",id);
                            PendingIntent result=PendingIntent.getBroadcast(context,id,callback,
                                PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_MUTABLE);
                            committing.commit(result.getIntentSender());
                            status="Waiting for Android's installation confirmation.";changed();
                        }catch(Exception e){setSession(-1);failed(owner,e);}
                    });
                }
            }catch(Exception e){if(sessionId!=-1)try{context.getPackageManager().getPackageInstaller().abandonSession(sessionId);}catch(Exception ignored){}failed(owner,e);}
        });
    }
    // Persist before committing so the result can be correlated after process recreation.
    @android.annotation.SuppressLint("ApplySharedPref")
    private void setSession(int id){installSession=id;context.getSharedPreferences("install",0).edit().putInt("session",id).commit();}
    void result(Intent result) {
        if(installSession<0 || result.getIntExtra("owner_session",-2)!=installSession)return;
        int value=result.getIntExtra(PackageInstaller.EXTRA_STATUS,PackageInstaller.STATUS_FAILURE);
        busy=false;
        if(value==PackageInstaller.STATUS_PENDING_USER_ACTION) {
            phase=Presentation.Phase.INSTALLING;busy=true;confirmation=result.getParcelableExtra(Intent.EXTRA_INTENT);
            if(confirmation==null){cancel();phase=Presentation.Phase.FAILED;status="Android did not provide installation confirmation. Retry.";changed();return;}
            status="Confirm the update in Android's installer.";
        } else if(value==PackageInstaller.STATUS_SUCCESS) {setSession(-1);phase=Presentation.Phase.COMPLETED;ready=false;status="Expressive Clock debug update installed. Existing app data retained.";}
        else {setSession(-1);phase=Presentation.Phase.FAILED;status="Installation cancelled or rejected by Android. You can retry.";}
        if(value!=PackageInstaller.STATUS_PENDING_USER_ACTION)
            context.getSharedPreferences("install",0).edit().putBoolean("result",value==PackageInstaller.STATUS_SUCCESS).apply();
        changed();
    }
    private void failed(int owner,Exception e) {
        if(e instanceof InterruptedException || Thread.currentThread().isInterrupted())return;
        post(owner,()->{phase=Presentation.Phase.FAILED;busy=false;ready=false;autoInstallPending=false;confirmation=null;code="";progress=-1;
            // No network bodies, URLs, tokens or server-provided errors are ever rendered/logged.
            status=e instanceof GitHub.UserError?e.getMessage():e instanceof SecurityException?"Verification failed: wrong identity, signature, checksum, metadata or older build. Nothing installed.":
                "Could not complete the request. Check connectivity and GitHub access, then retry.";});
    }
}
