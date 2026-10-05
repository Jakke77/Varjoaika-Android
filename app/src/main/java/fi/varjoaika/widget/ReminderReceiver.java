package fi.varjoaika.widget;
import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import java.util.List;

public class ReminderReceiver extends BroadcastReceiver {
    static final String CHANNEL="varjoaika_reminders", FIRE="fi.varjoaika.android.REMIND", SNOOZE="fi.varjoaika.android.SNOOZE", ACK="fi.varjoaika.android.ACK";
    static int flags() { return PendingIntent.FLAG_UPDATE_CURRENT|(Build.VERSION.SDK_INT>=23?PendingIntent.FLAG_IMMUTABLE:0); }
    static NotificationManager manager(Context c) { return (NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE); }
    static void channel(Context c) {
        if(Build.VERSION.SDK_INT>=26) {
            NotificationChannel ch=new NotificationChannel(CHANNEL,"Varjoajan muistutukset",NotificationManager.IMPORTANCE_DEFAULT);
            ch.setDescription("Kalenterin muistutukset. Huhuilu ja äänen voimakkuus sovelluksen asetuksista.");ch.setSound(null,null);manager(c).createNotificationChannel(ch);
        }
    }
    static boolean canNotify(Context c) {
        channel(c);
        if(Build.VERSION.SDK_INT>=33&&c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return false;
        if(Build.VERSION.SDK_INT>=24&&!manager(c).areNotificationsEnabled())return false;
        return Build.VERSION.SDK_INT<26||manager(c).getNotificationChannel(CHANNEL).getImportance()!=NotificationManager.IMPORTANCE_NONE;
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
    private static boolean deliverNotifications(Context c) {
        if(!canNotify(c))return false;
        EntryStore db=new EntryStore(c);boolean sent=false;
        try {
            List<EntryStore.Entry> entries=db.due(System.currentTimeMillis());
            for(EntryStore.Entry e:entries) {
                if(!db.claim(e.id))continue;
                PendingIntent open=PendingIntent.getActivity(c,(int)e.id,new Intent(c,CalendarActivity.class).setData(Uri.parse("varjoaika://entry/"+e.id)).putExtra("day",e.day),flags());
                PendingIntent snooze=PendingIntent.getBroadcast(c,(int)e.id,new Intent(c,ReminderReceiver.class).setAction(SNOOZE).setData(Uri.parse("varjoaika://snooze/"+e.id)).putExtra("id",e.id),flags());
                PendingIntent ack=PendingIntent.getBroadcast(c,(int)e.id,new Intent(c,ReminderReceiver.class).setAction(ACK).setData(Uri.parse("varjoaika://ack/"+e.id)).putExtra("id",e.id),flags());
                Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(c,CHANNEL):new Notification.Builder(c);
                b.setSmallIcon(R.drawable.icon).setContentTitle(e.title).setContentText(e.body.isEmpty()?Dates.label(e.day):e.body)
                    .setStyle(new Notification.BigTextStyle().bigText(e.body+"\n"+Dates.label(e.day))).setContentIntent(open).setAutoCancel(true)
                    .setCategory(Notification.CATEGORY_REMINDER).setVisibility(Notification.VISIBILITY_PRIVATE)
                    .addAction(0,"Siirrä 10 min",snooze).addAction(0,"Kuittaa",ack);
                try { manager(c).notify("varjoaika",(int)(e.id&0x7fffffff),b.build());sent=true; }
                catch(SecurityException revoked){db.retry(e.id);}
            }
        } finally{db.close();}
        return sent;
    }
    public static void deliver(Context c) { if(deliverNotifications(c))Sound.play(c,null); }
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
            boolean sent=deliverNotifications(c);schedule(c);VarjoWidget.updateAll(c);NtpJob.schedule(c);
            if(sent)Sound.play(c,pending::finish);else pending.finish();
        }catch(RuntimeException error){pending.finish();throw error;}
    }
}
