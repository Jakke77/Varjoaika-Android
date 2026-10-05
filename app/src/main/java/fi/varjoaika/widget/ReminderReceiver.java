package fi.varjoaika.widget;
import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.media.AudioAttributes;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

public class ReminderReceiver extends BroadcastReceiver {
    static final String CHANNEL="varjoaika_reminders", FIRE="fi.varjoaika.android.REMIND", SNOOZE="fi.varjoaika.android.SNOOZE", ACK="fi.varjoaika.android.ACK";
    static int flags() { return PendingIntent.FLAG_UPDATE_CURRENT|(Build.VERSION.SDK_INT>=23?PendingIntent.FLAG_IMMUTABLE:0); }
    static NotificationManager manager(Context c) { return (NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE); }
    static AudioAttributes audioAttributes() {
        return new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
    }
    static String channelId(Uri sound) {
        if(sound==null)return CHANNEL+"_v2_silent";
        // Android channel sound and importance cannot be changed after creation.
        // A stable ID per sound also preserves the user's settings when switching back.
        try {
            byte[] hash=MessageDigest.getInstance("SHA-256").digest(sound.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder id=new StringBuilder(CHANNEL+"_v2_");
            for(int i=0;i<8;i++)id.append(String.format(java.util.Locale.ROOT,"%02x",hash[i]&255));
            return id.toString();
        }catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
    }
    static String channel(Context c) {
        Uri sound=Sound.notificationUri(c);String id=channelId(sound);
        if(Build.VERSION.SDK_INT>=26&&manager(c).getNotificationChannel(id)==null) {
            String previous=Sound.prefs(c).getString("reminder_channel",CHANNEL);
            NotificationChannel old=manager(c).getNotificationChannel(previous);
            int importance=NotificationManager.IMPORTANCE_HIGH;
            // Migrate the original silent/default channel, but keep an explicit block.
            if(old!=null&&(old.getImportance()==NotificationManager.IMPORTANCE_NONE||!CHANNEL.equals(previous)))
                importance=old.getImportance();
            NotificationChannel ch=new NotificationChannel(id,"Varjoajan muistutukset",importance);
            ch.setDescription("Kalenterin muistutukset: ääni, värinä ja ponnahdusilmoitus.");
            ch.setSound(sound,audioAttributes());ch.enableVibration(true);
            ch.setVibrationPattern(new long[]{0,250,150,250});
            manager(c).createNotificationChannel(ch);
        }
        Sound.prefs(c).edit().putString("reminder_channel",id).apply();
        return id;
    }
    static boolean canNotify(Context c) {
        String id=channel(c);
        if(Build.VERSION.SDK_INT>=33&&c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return false;
        if(Build.VERSION.SDK_INT>=24&&!manager(c).areNotificationsEnabled())return false;
        return Build.VERSION.SDK_INT<26||manager(c).getNotificationChannel(id).getImportance()!=NotificationManager.IMPORTANCE_NONE;
    }
    public static void schedule(Context c) {
        AlarmManager alarms=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        PendingIntent alarm=PendingIntent.getBroadcast(c,0,new Intent(c,ReminderReceiver.class).setAction(FIRE),flags());
        EntryStore db=new EntryStore(c);long next;try{next=db.nextDue();}finally{db.close();}
        if(next==0||!canNotify(c)){alarms.cancel(alarm);return;}
        long when=Math.max(next,System.currentTimeMillis()+1000);
        if(Build.VERSION.SDK_INT<31||alarms.canScheduleExactAlarms()) {
            try { if(Build.VERSION.SDK_INT>=23)alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,alarm);else alarms.setExact(AlarmManager.RTC_WAKEUP,when,alarm);return; }
            catch(SecurityException revoked) { /* Permission may have changed since the check. */ }
        }
        if(Build.VERSION.SDK_INT>=23)alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,alarm);else alarms.set(AlarmManager.RTC_WAKEUP,when,alarm);
    }
    public static void cancelNotification(Context c,long id) { manager(c).cancel("varjoaika",(int)(id&0x7fffffff)); }
    static Notification notification(Context c,EntryStore.Entry e,boolean actions) {
        String id=channel(c);
        PendingIntent open=PendingIntent.getActivity(c,(int)e.id,new Intent(c,CalendarActivity.class).setData(Uri.parse("varjoaika://entry/"+e.id)).putExtra("day",e.day),flags());
        Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(c,id):new Notification.Builder(c);
        b.setSmallIcon(R.drawable.icon).setContentTitle(e.title).setContentText(e.body.isEmpty()?Dates.label(e.day):e.body)
            .setStyle(new Notification.BigTextStyle().bigText(e.body+"\n"+Dates.label(e.day))).setContentIntent(open).setAutoCancel(true)
            .setCategory(Notification.CATEGORY_REMINDER).setVisibility(Notification.VISIBILITY_PRIVATE)
            .setPriority(Notification.PRIORITY_HIGH);
        if(Build.VERSION.SDK_INT<26) {
            b.setSound(Sound.notificationUri(c),audioAttributes()).setVibrate(new long[]{0,250,150,250});
        }
        if(actions) {
            PendingIntent snooze=PendingIntent.getBroadcast(c,(int)e.id,new Intent(c,ReminderReceiver.class).setAction(SNOOZE).setData(Uri.parse("varjoaika://snooze/"+e.id)).putExtra("id",e.id),flags());
            PendingIntent ack=PendingIntent.getBroadcast(c,(int)e.id,new Intent(c,ReminderReceiver.class).setAction(ACK).setData(Uri.parse("varjoaika://ack/"+e.id)).putExtra("id",e.id),flags());
            b.addAction(0,"Siirrä 10 min",snooze).addAction(0,"Kuittaa",ack);
        }
        return b.build();
    }
    static boolean testNotification(Context c) {
        if(!canNotify(c))return false;
        EntryStore.Entry e=new EntryStore.Entry();e.id=0;e.day=Dates.today();
        e.title="Varjoajan testi-ilmoitus";e.body="Tarkista ääni ja ponnahdusilmoitus. Tämä testi ei muuta muistutuksia.";
        try {manager(c).notify("varjoaika-test",0,notification(c,e,false));return true;}
        catch(SecurityException revoked){return false;}
    }
    private static boolean deliverNotifications(Context c) {
        if(!canNotify(c))return false;
        EntryStore db=new EntryStore(c);boolean sent=false;
        try {
            List<EntryStore.Entry> entries=db.due(System.currentTimeMillis());
            for(EntryStore.Entry e:entries) {
                if(!db.claim(e.id))continue;
                try { manager(c).notify("varjoaika",(int)(e.id&0x7fffffff),notification(c,e,true));sent=true; }
                catch(SecurityException revoked){db.retry(e.id);}
            }
        } finally{db.close();}
        return sent;
    }
    public static void deliver(Context c) { deliverNotifications(c); }
    @Override public void onReceive(Context c,Intent intent) {
        String action=intent.getAction();long id=intent.getLongExtra("id",-1);
        if(ACK.equals(action)){cancelNotification(c,id);return;}
        if(SNOOZE.equals(action)) {
            EntryStore db=new EntryStore(c);try {
                EntryStore.Entry e=db.get(id);
                if(e!=null){e.due=System.currentTimeMillis()+600000;e.delivered=false;java.util.Calendar when=java.util.Calendar.getInstance();when.setTimeInMillis(e.due);VarjoDate d=VarjoDate.from(when);if(d!=null)e.day=Dates.offset(d.year,d.month,d.day);db.save(e);}
            }finally{db.close();}cancelNotification(c,id);schedule(c);VarjoWidget.updateAll(c);return;
        }
        PendingResult pending=goAsync();
        try {
            deliverNotifications(c);schedule(c);VarjoWidget.updateAll(c);NtpJob.schedule(c);
        }finally{pending.finish();}
    }
}
