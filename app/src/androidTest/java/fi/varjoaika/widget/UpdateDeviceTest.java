package fi.varjoaika.widget;

import android.app.job.*;
import android.content.*;
import android.content.pm.Signature;
import android.net.Uri;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;
import java.io.File;
import java.util.Map;
import static org.junit.Assert.*;

public class UpdateDeviceTest {
    private Context c;private Map<String,?> original;
    @Before public void setup() {
        c=InstrumentationRegistry.getInstrumentation().getTargetContext();
        original=Updates.prefs(c).getAll();
        Updates.prefs(c).edit().clear().putBoolean("auto_check",false).commit();
        UpdateJob.schedule(c,false);
    }
    @After public void restore() {
        SharedPreferences.Editor edit=Updates.prefs(c).edit().clear();
        for(Map.Entry<String,?> e:original.entrySet()) {
            Object v=e.getValue();
            if(v instanceof String)edit.putString(e.getKey(),(String)v);
            else if(v instanceof Boolean)edit.putBoolean(e.getKey(),(Boolean)v);
            else if(v instanceof Long)edit.putLong(e.getKey(),(Long)v);
        }
        edit.commit();UpdateJob.schedule(c,false);
    }
    @Test public void automaticCheckIsPersistentAndCanBeDisabled() {
        JobScheduler jobs=(JobScheduler)c.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        Updates.prefs(c).edit().putBoolean("auto_check",true).putLong("attempt",System.currentTimeMillis()).commit();
        UpdateJob.schedule(c,true);JobInfo periodic=null;
        for(JobInfo info:jobs.getAllPendingJobs())if(info.getId()==UpdateJob.PERIODIC)periodic=info;
        assertNotNull(periodic);assertTrue(periodic.isPeriodic());assertTrue(periodic.isPersisted());
        assertEquals(Updates.DAY,periodic.getIntervalMillis());assertEquals(JobInfo.NETWORK_TYPE_ANY,periodic.getNetworkType());
        for(JobInfo info:jobs.getAllPendingJobs())assertNotEquals(UpdateJob.IMMEDIATE,info.getId());
        Updates.prefs(c).edit().putBoolean("auto_check",false).commit();UpdateJob.schedule(c,true);
        for(JobInfo info:jobs.getAllPendingJobs()){assertNotEquals(UpdateJob.PERIODIC,info.getId());assertNotEquals(UpdateJob.IMMEDIATE,info.getId());}
    }
    @Test public void olderVersionsAndMissingApksAreNotOfferedForInstallation() {
        Updates.prefs(c).edit().putString("version","0.0.1").putString("url","https://github.com/update.apk").commit();
        assertFalse(Updates.available(c));
        Updates.prefs(c).edit().putString("version","999.0.0").putString("url","").commit();assertFalse(Updates.available(c));
        Updates.prefs(c).edit().putString("url","https://github.com/"+ReleaseInfo.REPOSITORY+"/releases/download/v999.0.0/Varjoaika-999.0.0.apk").commit();
        assertTrue(Updates.available(c));assertFalse(Updates.ready(c));
    }
    @Test public void installerUsesGrantableContentUriAndDoesNotInstallSilently() {
        Uri uri=Uri.parse("content://downloads/my_downloads/42");Intent intent=Updates.installerIntent(uri);
        assertEquals(Intent.ACTION_VIEW,intent.getAction());
        assertEquals("application/vnd.android.package-archive",intent.getType());
        assertEquals(uri,intent.getData());assertNotNull(intent.getClipData());
        assertTrue((intent.getFlags()&Intent.FLAG_GRANT_READ_URI_PERMISSION)!=0);
    }
    @Test public void onlySameSigningCertificatesAreAccepted() {
        Signature one=new Signature(new byte[]{1,2,3}),two=new Signature(new byte[]{4,5,6});
        assertTrue(Updates.sameSigners(new Signature[]{one},new Signature[]{one}));
        assertFalse(Updates.sameSigners(new Signature[]{one},new Signature[]{two}));
        assertFalse(Updates.sameSigners(new Signature[]{one},null));
        assertFalse(Updates.sameSigners(new Signature[]{one},new Signature[0]));
    }
    @Test public void sameVersionApkIsRejectedBeforeInstaller() throws Exception {
        try{Updates.verifyApk(c,new File(c.getApplicationInfo().sourceDir),"");fail("Same-version APK accepted");}
        catch(java.io.IOException expected){assertTrue(expected.getMessage().contains("uudempi"));}
    }
    @Test public void corruptedDownloadIsRejectedByChecksum() throws Exception {
        File apk=new File(c.getCacheDir(),"bad-update.apk");
        try {
            try(java.io.FileOutputStream out=new java.io.FileOutputStream(apk)){out.write(new byte[]{1,2,3});}
            Updates.verifyApk(c,apk,"sha256:"+new String(new char[64]).replace('\0','0'));fail("Corrupted download accepted");
        }catch(java.io.IOException expected){assertTrue(expected.getMessage().contains("tarkistussumma"));}
        finally{assertTrue(!apk.exists()||apk.delete());}
    }
    @Test public void spoofedDownloadBroadcastDoesNotChangePendingState() {
        Updates.prefs(c).edit().putLong("download_id",123).putString("download_status","downloading").commit();
        new UpdateReceiver().onReceive(c,new Intent(android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE).putExtra(android.app.DownloadManager.EXTRA_DOWNLOAD_ID,999L));
        assertEquals("downloading",Updates.prefs(c).getString("download_status",""));
        assertEquals(123,Updates.prefs(c).getLong("download_id",-1));
    }
    @Test public void apkDestinationRejectsInvalidVersionPaths() {
        assertNull(Updates.apkFile(c,"../other"));
        assertNull(Updates.apkFile(c,"1.0.2/other"));
        assertNotNull(Updates.apkFile(c,"1.0.2"));
    }
}
