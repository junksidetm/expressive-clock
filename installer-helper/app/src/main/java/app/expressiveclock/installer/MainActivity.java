package app.expressiveclock.installer;

import androidx.activity.ComponentActivity;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;

/** Same Island presentation/controller separation; Android owns installation and confirmation. */
public final class MainActivity extends ComponentActivity {
    private Work work;
    private InstallerUi ui;
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);work=Work.get(this);
        ui=new InstallerUi(this,new InstallerUi.Actions(){
            public void connect(String client){getPreferences(0).edit().putString("client",client).apply();work.connect(client);}
            public void authorize(){startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://github.com/login/device")));}
            public void retry(){work.download();}
            public void install(){maybeInstall();}
            public void cancel(){work.cancel();}
            public void disconnect(){work.disconnect();}
        },getPreferences(0).getString("client",""));
        // No inherited Island client or credentials. Reopening the link resumes evidence checks.
        if(saved==null)handle(getIntent());render();
    }
    private void handle(Intent i){if(Intent.ACTION_VIEW.equals(i.getAction()) && i.getData()!=null)work.beginLink(i.getDataString());}
    @Override protected void onNewIntent(Intent i){super.onNewIntent(i);setIntent(i);handle(i);}
    @Override protected void onResume(){super.onResume();work.screen=this;render();if(work.autoInstallPending)maybeInstall();}
    @Override protected void onPause(){work.screen=null;super.onPause();}
    void render(){
        if(ui!=null)ui.update(new UiSnapshot(work.phase,work.status,work.credentials.hasCredentials(),work.busy,
            work.ready,work.request!=null,work.progress,work.code,work.metadata));
        if(work.confirmation!=null && work.screen==this) {
            Intent confirmation=work.confirmation;work.confirmation=null;
            try{startActivity(confirmation);}catch(Exception e){work.cancel();work.phase=Presentation.Phase.FAILED;
                work.status="Could not open Android confirmation. Retry the verified download to continue.";render();}
        }
    }
    void maybeInstall(){
        if(work.screen!=this || work.busy || !work.ready)return;
        work.autoInstallPending=false;
        if(!getPackageManager().canRequestPackageInstalls()) {
            work.status="Allow this helper to request installs, then return and tap Install. Android will still ask for confirmation.";render();
            try{startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+getPackageName())));}
            catch(Exception e){work.status="Open Android Settings and allow installs from Expressive Clock Installer Helper.";render();}
            return;
        }
        work.install();
    }
}
