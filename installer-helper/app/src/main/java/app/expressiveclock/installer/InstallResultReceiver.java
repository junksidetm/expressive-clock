package app.expressiveclock.installer;
import android.content.*;
public final class InstallResultReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent){Work.get(context).result(intent);}
}
