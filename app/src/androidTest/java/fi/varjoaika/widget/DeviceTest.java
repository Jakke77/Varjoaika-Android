package fi.varjoaika.widget;
import android.app.Activity;
import android.appwidget.AppWidgetProviderInfo;
import android.content.*;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RemoteViews;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import static org.junit.Assert.*;

public class DeviceTest {
    private Context context(){return InstrumentationRegistry.getInstrumentation().getTargetContext();}
    @Test public void noLocationOrCalendarPermissions() throws Exception {
        String[] requested=context().getPackageManager().getPackageInfo(context().getPackageName(),android.content.pm.PackageManager.GET_PERMISSIONS).requestedPermissions;
        for(String permission:requested){assertNotEquals("android.permission.READ_CALENDAR",permission);assertNotEquals("android.permission.WRITE_CALENDAR",permission);assertNotEquals("android.permission.ACCESS_FINE_LOCATION",permission);}
    }
    @Test public void persistentNotesAndOneShotReminder() {
        Context c=context();EntryStore db=new EntryStore(c);EntryStore.Entry e=new EntryStore.Entry();e.day=7;e.title="Laitetesti";e.body="Säilyy uudelleen avattaessa";e.due=System.currentTimeMillis()-1000;db.save(e);long id=e.id;db.close();
        db=new EntryStore(c);assertEquals(e.body,db.get(id).body);assertTrue(db.claim(id));assertFalse(db.claim(id));assertTrue(db.get(id).delivered);
        e=db.get(id);e.delivered=false;e.due=System.currentTimeMillis()+600000;db.save(e);assertFalse(db.get(id).delivered);db.remove(id);assertNull(db.get(id));db.close();
    }
    @Test public void everyWidgetInflatesAtSmallAndLargeSizes() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
            Context c=context();int id=8000;
            for(Class<?> provider:VarjoWidget.PROVIDERS) {
                AppWidgetProviderInfo info=new AppWidgetProviderInfo();info.provider=new ComponentName(c,provider);
                for(int[] size:new int[][]{{140,210},{210,210},{300,360}}) {
                    RemoteViews remote=VarjoWidget.render(c,info,id,size[0],size[1]);
                    View view=remote.apply(c,new FrameLayout(c));assertNotNull(view.findViewById(R.id.gothic_date));assertNotNull(view.findViewById(R.id.settings));
                    int width=Math.round(size[0]*c.getResources().getDisplayMetrics().density),height=Math.round(size[1]*c.getResources().getDisplayMetrics().density);
                    view.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));view.layout(0,0,width,height);
                    if(size[0]==300) {
                        Bitmap preview=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);view.draw(new android.graphics.Canvas(preview));
                        java.io.File folder=new java.io.File(c.getExternalFilesDir(null),"previews");assertTrue(folder.isDirectory()||folder.mkdirs());
                        try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(folder,provider.getSimpleName()+".png"))){assertTrue(preview.compress(Bitmap.CompressFormat.PNG,100,out));}catch(java.io.IOException error){throw new AssertionError(error);}finally{preview.recycle();}
                    }
                }id++;
            }
        });
    }
    @Test public void bundledSound() throws Exception {
        try(android.content.res.AssetFileDescriptor fd=context().getResources().openRawResourceFd(R.raw.huuhkaja)) {
            MediaMetadataRetriever media=new MediaMetadataRetriever();try{media.setDataSource(fd.getFileDescriptor(),fd.getStartOffset(),fd.getLength());assertEquals("3200",media.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));}finally{media.release();}
        }
    }
    @Test public void calendarOpensAndResumes() {
        Activity a=InstrumentationRegistry.getInstrumentation().startActivitySync(new Intent(context(),CalendarActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        assertNotNull(a);InstrumentationRegistry.getInstrumentation().runOnMainSync(a::finish);
    }
}
