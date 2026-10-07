package fi.varjoaika.widget;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.DateFormat;
import java.util.*;
import java.util.concurrent.*;

/** GitHub checks and downloads; installation always belongs to Android's installer. */
final class Updates {
    static final long DAY=86400000L;
    static final String CHANNEL="varjoaika_updates";
    private static final ExecutorService WORKER=Executors.newSingleThreadExecutor();
    private static final ExecutorService FILES=Executors.newSingleThreadExecutor();
    static SharedPreferences prefs(Context c){return c.getSharedPreferences("updates",Context.MODE_PRIVATE);}
    static Future<?> run(Runnable task){return WORKER.submit(task);}
    static Future<?> fileTask(Runnable task){return FILES.submit(task);}
    static String installedVersion(Context c) {
        try{return c.getPackageManager().getPackageInfo(c.getPackageName(),0).versionName;}
        catch(PackageManager.NameNotFoundException impossible){throw new IllegalStateException(impossible);}
    }
    static boolean available(Context c) {
        try{return !prefs(c).getString("url","").isEmpty()&&ReleaseInfo.compare(prefs(c).getString("version",""),installedVersion(c))>0;}
        catch(IllegalArgumentException unknown){return false;}
    }
    static boolean ready(Context c){return "ready".equals(prefs(c).getString("download_status",""))&&available(c)&&prefs(c).getString("download_version","").equals(prefs(c).getString("version",""));}
    static String status(Context c) {
        SharedPreferences p=prefs(c);String state=p.getString("download_status","");
        String text="Asennettu versio "+installedVersion(c)+"\n"+p.getString("message","Päivityksiä ei ole vielä tarkistettu.");
        if("downloading".equals(state))text+="\nVersiota "+p.getString("download_version","")+" ladataan. Automaattinen lataus odottaa tarvittaessa Wi-Fiä.";
        if(ready(c))text+="\nPäivitys ladattu. Asenna päivitys painikkeesta.";
        if("error".equals(state))text+="\n"+p.getString("download_error","Lataus epäonnistui. Yritä uudelleen.");
        long checked=p.getLong("checked",0);
        if(checked>0)text+="\nViimeisin onnistunut tarkistus: "+DateFormat.getDateTimeInstance().format(new Date(checked));
        return text;
    }
    static void check(Context c,boolean automatic) {
        Context app=c.getApplicationContext();SharedPreferences p=prefs(app);long now=System.currentTimeMillis();
        if(automatic&&(!p.getBoolean("auto_check",true)||now-p.getLong("attempt",0)<DAY))return;
        p.edit().putLong("attempt",now).apply();HttpURLConnection connection=null;
        try {
            connection=(HttpURLConnection)new URL(ReleaseInfo.API).openConnection();
            connection.setConnectTimeout(10000);connection.setReadTimeout(10000);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("Accept","application/vnd.github+json");
            connection.setRequestProperty("X-GitHub-Api-Version","2022-11-28");
            connection.setRequestProperty("User-Agent","Varjoaika-Android/"+installedVersion(app));
            int code=connection.getResponseCode();
            if(code==404){p.edit().putLong("checked",now).putString("message","GitHubissa ei ole vielä julkaistua versiota.").remove("version").remove("url").remove("digest").apply();return;}
            if(code==403||code==429)throw new IOException("GitHubin tarkistusraja täyttyi. Yritä myöhemmin.");
            if(code!=200)throw new IOException("GitHubin versiotarkistus epäonnistui (HTTP "+code+").");
            ByteArrayOutputStream data=new ByteArrayOutputStream();
            try(InputStream in=connection.getInputStream()) {
                byte[] buffer=new byte[4096];int n;
                while((n=in.read(buffer))!=-1){data.write(buffer,0,n);if(data.size()>2097152)throw new IOException("GitHubin vastaus on liian suuri.");}
            }
            ReleaseInfo release=ReleaseInfo.parse(new String(data.toByteArray(),StandardCharsets.UTF_8));
            if(Thread.currentThread().isInterrupted()||(automatic&&!p.getBoolean("auto_check",true)))return;
            boolean newer=ReleaseInfo.compare(release.version,installedVersion(app))>0;
            String message=!newer?"Käytössä on uusin saatavilla oleva versio.":release.url.isEmpty()
                ?"Versio "+release.version+" on julkaistu, mutta APK ei ole vielä saatavilla."
                :"Uusi versio "+release.version+" on saatavilla.";
            p.edit().putLong("checked",now).putString("message",message).putString("version",release.version).putString("url",release.url).putString("digest",release.digest).apply();
            if(newer&&!release.url.isEmpty()) {
                if(automatic&&p.getBoolean("auto_download",true))download(app,true);
                if(!release.version.equals(p.getString("notified",""))&&notifyUpdate(app,false))
                    p.edit().putString("notified",release.version).apply();
            }
        }catch(Exception error) {
            if(!Thread.currentThread().isInterrupted())p.edit().putString("message",error instanceof IOException?error.getMessage():"Päivitystietoja ei voitu lukea. Yritä myöhemmin.").apply();
        }finally{if(connection!=null)connection.disconnect();}
    }
    private static DownloadManager downloads(Context c){return (DownloadManager)c.getSystemService(Context.DOWNLOAD_SERVICE);}
    static synchronized void download(Context c,boolean wifiOnly) {
        SharedPreferences p=prefs(c);if(!available(c)||wifiOnly&&(!p.getBoolean("auto_check",true)||!p.getBoolean("auto_download",true)))return;
        long previous=p.getLong("download_id",-1);
        if(previous!=-1) {
            reconcile(c,previous);
            if(p.getString("version","").equals(p.getString("download_version",""))&&
                ("downloading".equals(p.getString("download_status",""))||ready(c)))return;
            downloads(c).remove(previous);
        }
        String version=p.getString("version",""),url=p.getString("url","");
        Uri uri=Uri.parse(url);String name=uri.getLastPathSegment();
        if(name==null||!ReleaseInfo.allowedUrl(url,"v"+version,name)&&!ReleaseInfo.allowedUrl(url,version,name))return;
        File target=apkFile(c,version);
        if(target==null){fail(c,"Päivityksen tallennustila ei ole käytettävissä.");return;}
        if(target.exists()&&!target.delete()){fail(c,"Vanhaa latausta ei voitu korvata.");return;}
        try {
            DownloadManager.Request request=new DownloadManager.Request(uri)
                .setTitle("Varjoaika "+version).setDescription("Kalenterisovelluksen päivitys")
                .setMimeType("application/vnd.android.package-archive")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setDestinationInExternalFilesDir(c,Environment.DIRECTORY_DOWNLOADS,target.getName());
            if(wifiOnly)request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI).setAllowedOverRoaming(false);
            long id=downloads(c).enqueue(request);
            p.edit().putLong("download_id",id).putString("download_version",version)
                .putString("download_digest",p.getString("digest","")).putBoolean("download_automatic",wifiOnly).putString("download_status","downloading").remove("download_error").commit();
        }catch(RuntimeException unavailable){fail(c,"Latausta ei voitu aloittaa. Tarkista Androidin Lataukset-sovellus ja tallennustila.");}
    }
    static synchronized void cancelAutomaticDownload(Context c) {
        SharedPreferences p=prefs(c);
        if(p.getBoolean("download_automatic",false)&&"downloading".equals(p.getString("download_status",""))) {
            long id=p.getLong("download_id",-1);if(id>=0)downloads(c).remove(id);
            p.edit().remove("download_id").remove("download_status").remove("download_version").remove("download_digest").remove("download_automatic").apply();
        }
    }
    static File apkFile(Context c,String version) {
        try{ReleaseInfo.versionParts(version);}catch(IllegalArgumentException invalid){return null;}
        File folder=c.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        return folder==null?null:new File(folder,"Varjoaika-"+version+".apk");
    }
    static boolean sameSigners(Signature[] current,Signature[] update) {
        return current!=null&&update!=null&&current.length>0&&
            new HashSet<>(Arrays.asList(current)).equals(new HashSet<>(Arrays.asList(update)));
    }
    static long versionCode(PackageInfo info){return Build.VERSION.SDK_INT>=28?info.getLongVersionCode():info.versionCode;}
    static void verifyApk(Context c,File file,String digest) throws Exception {
        if(file==null||!file.isFile())throw new IOException("Ladattua APK:ta ei löytynyt. Lataa päivitys uudelleen.");
        if(!digest.isEmpty()) {
            if(!digest.matches("sha256:[a-f0-9]{64}"))throw new IOException("Päivityksen tarkistussumma ei kelpaa.");
            MessageDigest hash=MessageDigest.getInstance("SHA-256");
            try(InputStream in=new FileInputStream(file)){byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1)hash.update(buffer,0,n);}
            StringBuilder actual=new StringBuilder("sha256:");
            for(byte b:hash.digest())actual.append(String.format(Locale.ROOT,"%02x",b&255));
            if(!digest.equals(actual.toString()))throw new IOException("APK:n tarkistussumma ei täsmää. Lataa päivitys uudelleen.");
        }
        PackageManager pm=c.getPackageManager();int flags=Build.VERSION.SDK_INT>=28?PackageManager.GET_SIGNING_CERTIFICATES:PackageManager.GET_SIGNATURES;
        PackageInfo update=pm.getPackageArchiveInfo(file.getAbsolutePath(),flags),current=pm.getPackageInfo(c.getPackageName(),flags);
        if(update==null||!c.getPackageName().equals(update.packageName))throw new IOException("APK ei ole Varjoajan päivitys.");
        if(versionCode(update)<=versionCode(current))throw new IOException("APK ei ole asennettua sovellusta uudempi.");
        Signature[] oldSigners=Build.VERSION.SDK_INT>=28?(current.signingInfo==null?null:current.signingInfo.getApkContentsSigners()):current.signatures;
        Signature[] newSigners=Build.VERSION.SDK_INT>=28?(update.signingInfo==null?null:update.signingInfo.getApkContentsSigners()):update.signatures;
        if(!sameSigners(oldSigners,newSigners))throw new IOException("APK:n allekirjoitus ei vastaa asennettua sovellusta. Päivitys tarvitsee saman julkaisuavaimen.");
    }
    private static void fail(Context c,String message){prefs(c).edit().putString("download_status","error").putString("download_error",message).apply();}
    static synchronized void reconcile(Context c,long id) {
        SharedPreferences p=prefs(c);if(id<0||id!=p.getLong("download_id",-1))return;
        try(Cursor cursor=downloads(c).query(new DownloadManager.Query().setFilterById(id))) {
            if(cursor==null||!cursor.moveToFirst()){fail(c,"Lataus poistettiin. Lataa päivitys uudelleen.");return;}
            int state=cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
            if(state==DownloadManager.STATUS_FAILED){fail(c,"Päivityksen lataus epäonnistui. Tarkista yhteys ja tallennustila.");return;}
            if(state!=DownloadManager.STATUS_SUCCESSFUL)return;
            if("ready".equals(p.getString("download_status","")))return;
            verifyApk(c,apkFile(c,p.getString("download_version","")),p.getString("download_digest",""));
            p.edit().putString("download_status","ready").remove("download_error").apply();notifyUpdate(c,true);
        }catch(Exception error){fail(c,error.getMessage()==null?"Päivityksen tarkistus epäonnistui.":error.getMessage());}
    }
    static Intent installerIntent(Uri uri) {
        Intent intent=new Intent(Intent.ACTION_VIEW).setDataAndType(uri,"application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setClipData(ClipData.newRawUri("Varjoaika APK",uri));return intent;
    }
    static void install(Activity activity) {
        if(!ready(activity)){android.widget.Toast.makeText(activity,"Lataa päivitys ensin",android.widget.Toast.LENGTH_SHORT).show();return;}
        if(Build.VERSION.SDK_INT>=26&&!activity.getPackageManager().canRequestPackageInstalls()) {
            activity.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+activity.getPackageName())));
            android.widget.Toast.makeText(activity,"Salli päivitysten asentaminen ja palaa Asenna päivitys -painikkeeseen",android.widget.Toast.LENGTH_LONG).show();return;
        }
        fileTask(()->{
            String error=null;Uri uri=null;
            try {
                SharedPreferences p=prefs(activity);
                verifyApk(activity,apkFile(activity,p.getString("download_version","")),p.getString("download_digest",""));
                uri=downloads(activity).getUriForDownloadedFile(p.getLong("download_id",-1));
                if(uri==null)throw new IOException("Ladattua päivitystä ei löytynyt.");
            }catch(Exception invalid){error=invalid.getMessage();}
            final String message=error;final Uri apk=uri;
            activity.runOnUiThread(()->{
                if(activity.isFinishing()||activity.isDestroyed())return;
                if(message!=null){fail(activity,message);android.widget.Toast.makeText(activity,message,android.widget.Toast.LENGTH_LONG).show();return;}
                try{activity.startActivity(installerIntent(apk));}
                catch(ActivityNotFoundException unavailable){android.widget.Toast.makeText(activity,"Androidin APK-asentajaa ei löytynyt",android.widget.Toast.LENGTH_LONG).show();}
                catch(SecurityException denied){android.widget.Toast.makeText(activity,"Android esti asennuksen. Tarkista sovelluksen asennuslupa.",android.widget.Toast.LENGTH_LONG).show();}
            });
        });
    }
    static boolean notifyUpdate(Context c,boolean downloaded) {
        NotificationManager manager=ReminderReceiver.manager(c);
        if(Build.VERSION.SDK_INT>=33&&c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return false;
        if(Build.VERSION.SDK_INT>=24&&!manager.areNotificationsEnabled())return false;
        if(Build.VERSION.SDK_INT>=26)manager.createNotificationChannel(new NotificationChannel(CHANNEL,"Varjoajan päivitykset",NotificationManager.IMPORTANCE_DEFAULT));
        PendingIntent open=PendingIntent.getActivity(c,7702,new Intent(c,SettingsActivity.class).setAction("fi.varjoaika.android.UPDATES"),ReminderReceiver.flags());
        Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(c,CHANNEL):new Notification.Builder(c);
        b.setSmallIcon(R.drawable.icon).setContentTitle(downloaded?"Varjoajan päivitys ladattu":"Uusi Varjoaika-versio saatavilla")
            .setContentText(downloaded?"Avaa asetukset ja asenna päivitys.":"Versio "+prefs(c).getString("version","")+" · avaa päivitysasetukset")
            .setContentIntent(open).setAutoCancel(true);
        try{manager.notify("varjoaika-update",7702,b.build());return true;}catch(SecurityException denied){return false;}
    }
}
