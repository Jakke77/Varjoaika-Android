package fi.varjoaika.widget;
import android.app.job.*;
import android.content.*;
import android.os.Handler;
import android.os.Looper;
import java.net.*;
import java.util.Locale;
import java.text.DateFormat;
import java.util.Date;

/** Weekly optional check only; Android retains ownership of system time. */
public class NtpJob extends JobService {
    static final long WEEK=7*86400000L;
    static final int ID=7701;
    static SharedPreferences prefs(Context c){return c.getSharedPreferences("time",Context.MODE_PRIVATE);}
    private volatile boolean stopped;
    public static void schedule(Context c) {
        JobScheduler jobs=(JobScheduler)c.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if(!prefs(c).getBoolean("enabled",true)){jobs.cancel(ID);return;}
        for(JobInfo info:jobs.getAllPendingJobs())if(info.getId()==ID)return;
        jobs.schedule(new JobInfo.Builder(ID,new ComponentName(c,NtpJob.class)).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPeriodic(WEEK).setPersisted(true).build());
    }
    static String status(Context c) {
        SharedPreferences p=prefs(c);
        if(!p.getBoolean("enabled",true))return "Viikoittainen ajan tarkistus pois päältä";
        long last=p.getLong("checked",0);String message=p.getString("status","Ensimmäinen tarkistus odottaa verkkoyhteyttä");
        return message+(last>0?"\n"+DateFormat.getDateTimeInstance().format(new Date(last)):"");
    }
    @Override public boolean onStartJob(JobParameters job) {
        stopped=false;long now=System.currentTimeMillis();
        if(!prefs(this).getBoolean("enabled",true)||now-prefs(this).getLong("attempt",0)<WEEK)return false;
        new Thread(()->{
            String message;long checked=0;double offset=0;
            try(DatagramSocket socket=new DatagramSocket()) {
                socket.setSoTimeout(5000);socket.connect(InetAddress.getByName("time.mikes.fi"),123);
                long t1=System.currentTimeMillis();byte[] request=NtpPacket.request(t1);socket.send(new DatagramPacket(request,request.length));
                byte[] reply=new byte[512];DatagramPacket packet=new DatagramPacket(reply,reply.length);socket.receive(packet);long t4=System.currentTimeMillis();
                offset=NtpPacket.offset(request,reply,packet.getLength(),t1,t4);checked=t4;
                message=String.format(Locale.ROOT,"MIKES time.mikes.fi · kellon poikkeama %+.3f s",offset);
            }catch(Exception error){message="MIKES ei vastannut. Seuraava yritys viikon kuluttua.";}
            if(!stopped&&prefs(this).getBoolean("enabled",true)) {
                SharedPreferences.Editor editor=prefs(this).edit().putLong("attempt",now).putString("status",message);
                if(checked>0)editor.putLong("checked",checked).putFloat("offset",(float)offset);editor.apply();
                new Handler(Looper.getMainLooper()).post(()->{if(!stopped)jobFinished(job,false);});
            }
        },"Varjoaika-NTP").start();return true;
    }
    @Override public boolean onStopJob(JobParameters job){stopped=true;return true;}
}
