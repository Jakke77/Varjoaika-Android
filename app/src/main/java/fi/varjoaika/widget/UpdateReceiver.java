package fi.varjoaika.widget;

import android.app.DownloadManager;
import android.content.*;

public class UpdateReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent intent) {
        String action=intent.getAction();
        if(Intent.ACTION_BOOT_COMPLETED.equals(action)||Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            UpdateJob.schedule(c,true);return;
        }
        if(!DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action))return;
        long id=intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID,-1);
        if(id<0||id!=Updates.prefs(c).getLong("download_id",-1))return;
        PendingResult pending=goAsync();
        Updates.fileTask(()->{try{Updates.reconcile(c,id);}finally{pending.finish();}});
    }
}
