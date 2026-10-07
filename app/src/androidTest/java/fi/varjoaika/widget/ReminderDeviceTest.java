package fi.varjoaika.widget;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.util.Map;
import static org.junit.Assert.*;

/** Verify the actual Android notification configuration, not only the database claim. */
public class ReminderDeviceTest {
    private Context c;
    private Map<String,?> original;
    @Before public void setup() {
        c=InstrumentationRegistry.getInstrumentation().getTargetContext();
        original=Sound.prefs(c).getAll();
        Sound.prefs(c).edit().putBoolean("enabled",true).putInt("volume",65).remove("uri").commit();
        if(Build.VERSION.SDK_INT>=33)InstrumentationRegistry.getInstrumentation().getUiAutomation()
            .grantRuntimePermission(c.getPackageName(),android.Manifest.permission.POST_NOTIFICATIONS);
    }
    @After public void restore() {
        ReminderReceiver.manager(c).cancel("varjoaika-test",0);
        SharedPreferences.Editor edit=Sound.prefs(c).edit().clear();
        for(Map.Entry<String,?> entry:original.entrySet()) {
            Object value=entry.getValue();
            if(value instanceof String)edit.putString(entry.getKey(),(String)value);
            else if(value instanceof Integer)edit.putInt(entry.getKey(),(Integer)value);
            else if(value instanceof Boolean)edit.putBoolean(entry.getKey(),(Boolean)value);
        }
        edit.commit();
    }
    private EntryStore.Entry entry() {
        EntryStore.Entry e=new EntryStore.Entry();e.id=900001;e.day=Dates.today();
        e.title="Muistutustesti";e.body="Ääni ja ponnahdus";return e;
    }
    @Test public void audibleReminderHasHeadsUpPriorityAndActions() {
        Notification n=ReminderReceiver.notification(c,entry(),true);
        assertEquals(Notification.PRIORITY_HIGH,n.priority);
        assertEquals(Notification.CATEGORY_REMINDER,n.category);
        assertNotNull(n.contentIntent);assertEquals(2,n.actions.length);
        if(Build.VERSION.SDK_INT>=26) {
            NotificationChannel channel=ReminderReceiver.manager(c).getNotificationChannel(n.getChannelId());
            assertEquals(NotificationManager.IMPORTANCE_HIGH,channel.getImportance());
            assertEquals(Sound.defaultUri(c),channel.getSound());
            assertTrue(channel.shouldVibrate());
            assertEquals(android.media.AudioAttributes.USAGE_NOTIFICATION,channel.getAudioAttributes().getUsage());
            assertNotEquals(ReminderReceiver.CHANNEL,n.getChannelId());
        }else {
            assertEquals(Sound.defaultUri(c),n.sound);assertNotNull(n.vibrate);
        }
    }
    @Test public void muteUsesSeparateChannelAndRestoringSoundReusesOriginal() {
        String audible=ReminderReceiver.channel(c);
        Sound.prefs(c).edit().putBoolean("enabled",false).commit();
        Notification n=ReminderReceiver.notification(c,entry(),true);
        if(Build.VERSION.SDK_INT>=26) {
            assertNotEquals(audible,n.getChannelId());
            NotificationChannel muted=ReminderReceiver.manager(c).getNotificationChannel(n.getChannelId());
            assertNull(muted.getSound());assertEquals(NotificationManager.IMPORTANCE_HIGH,muted.getImportance());
        }else assertNull(n.sound);
        Sound.prefs(c).edit().putBoolean("enabled",true).commit();
        assertEquals(audible,ReminderReceiver.channel(c));
    }
    @Test public void unavailableCustomFileFallsBackToBundledSound() {
        Sound.prefs(c).edit().putString("uri","content://missing.varjoaika.test/sound").commit();
        assertEquals(Sound.defaultUri(c),Sound.notificationUri(c));
        Sound.prefs(c).edit().putInt("volume",0).commit();
        assertNull(Sound.notificationUri(c));
    }
    @Test public void differentSoundsHaveStableSeparateChannelIds() {
        Uri one=Uri.parse("content://sound.test/one"),two=Uri.parse("content://sound.test/two");
        assertEquals(ReminderReceiver.channelId(one),ReminderReceiver.channelId(one));
        assertNotEquals(ReminderReceiver.channelId(one),ReminderReceiver.channelId(two));
        assertNotEquals(ReminderReceiver.channelId(one),ReminderReceiver.channelId(null));
    }
    @Test public void testNotificationDoesNotClaimRealReminderOrExposeSnooze() {
        EntryStore db=new EntryStore(c);EntryStore.Entry e=entry();e.id=0;e.due=System.currentTimeMillis()-1000;db.save(e);
        try {
            ReminderReceiver.testNotification(c);
            assertFalse(db.get(e.id).delivered);
            Notification n=ReminderReceiver.notification(c,e,false);
            assertTrue(n.actions==null||n.actions.length==0);
        }finally{db.remove(e.id);db.close();}
    }
    @Test public void dueReminderPostsOnceWithSystemSound() {
        assertTrue(ReminderReceiver.canNotify(c));
        EntryStore db=new EntryStore(c);EntryStore.Entry e=entry();e.id=0;e.due=System.currentTimeMillis()-1000;db.save(e);
        try {
            ReminderReceiver.deliver(c);assertTrue(db.get(e.id).delivered);
            if(Build.VERSION.SDK_INT>=23) {
                android.service.notification.StatusBarNotification posted=null;
                long deadline=android.os.SystemClock.elapsedRealtime()+3000;
                while(posted==null&&android.os.SystemClock.elapsedRealtime()<deadline) {
                    for(android.service.notification.StatusBarNotification n:ReminderReceiver.manager(c).getActiveNotifications())
                        if("varjoaika".equals(n.getTag())&&n.getId()==(int)e.id)posted=n;
                    if(posted==null)android.os.SystemClock.sleep(50);
                }
                assertNotNull(posted);
                long firstPost=posted.getPostTime();
                assertEquals(e.title,posted.getNotification().extras.getString(Notification.EXTRA_TITLE));
                ReminderReceiver.deliver(c);
                for(android.service.notification.StatusBarNotification n:ReminderReceiver.manager(c).getActiveNotifications())
                    if("varjoaika".equals(n.getTag())&&n.getId()==(int)e.id)assertEquals(firstPost,n.getPostTime());
            }else ReminderReceiver.deliver(c);
            assertTrue(db.get(e.id).delivered);
        }finally{ReminderReceiver.cancelNotification(c,e.id);db.remove(e.id);db.close();}
    }
}
