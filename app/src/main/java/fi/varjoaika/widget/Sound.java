package fi.varjoaika.widget;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

final class Sound {
    private static MediaPlayer active;
    private static Runnable finish;
    static SharedPreferences prefs(Context c) { return c.getSharedPreferences("sound",Context.MODE_PRIVATE); }
    static Uri defaultUri(Context c) { return Uri.parse("android.resource://"+c.getPackageName()+"/raw/huuhkaja"); }
    /** The system plays this URI, even when the application's process is stopped. */
    static Uri notificationUri(Context c) {
        SharedPreferences p=prefs(c);
        if(!p.getBoolean("enabled",true)||p.getInt("volume",65)==0)return null;
        String custom=p.getString("uri","");
        if(custom.isEmpty())return defaultUri(c);
        Uri uri=Uri.parse(custom);
        if("content".equals(uri.getScheme())) {
            try(android.content.res.AssetFileDescriptor fd=c.getContentResolver().openAssetFileDescriptor(uri,"r")) {
                if(fd!=null)return uri;
            }catch(Exception unavailable){android.util.Log.w("Varjoaika","Oma ääni ei ole käytettävissä; käytetään huuhkajaa.");}
        }
        return defaultUri(c);
    }
    static void stop() {
        if(active!=null){active.release();active=null;}
        if(finish!=null){Runnable done=finish;finish=null;done.run();}
    }
    static void play(Context c,Runnable completion) {
        stop();SharedPreferences p=prefs(c);
        if(!p.getBoolean("enabled",true)||p.getInt("volume",65)==0||
            (Build.VERSION.SDK_INT>=23&&ReminderReceiver.manager(c).getCurrentInterruptionFilter()!=NotificationManager.INTERRUPTION_FILTER_ALL)) { if(completion!=null)completion.run();return; }
        MediaPlayer player=new MediaPlayer();active=player;finish=completion;
        Runnable cleanup=()->{if(active==player)stop();};
        try {
            String custom=p.getString("uri","");
            Uri uri=custom.isEmpty()?defaultUri(c):Uri.parse(custom);
            if(!custom.isEmpty()&&!"content".equals(uri.getScheme()))throw new IllegalArgumentException("Vain laitteen äänitiedostot");
            player.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
            player.setDataSource(c,uri);float volume=Math.max(0,Math.min(100,p.getInt("volume",65)))/100f;player.setVolume(volume,volume);
            player.setOnCompletionListener(m->cleanup.run());player.setOnErrorListener((m,a,b)->{cleanup.run();return true;});
            player.setOnPreparedListener(m->{if(active==player)m.start();});player.prepareAsync();
            // Finish within BroadcastReceiver.goAsync's background time budget.
            new Handler(Looper.getMainLooper()).postDelayed(cleanup,8000);
        }catch(Exception error){cleanup.run();}
    }
    private Sound() {}
}
