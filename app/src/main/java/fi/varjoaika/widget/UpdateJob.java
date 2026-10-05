package fi.varjoaika.widget;

import android.app.job.*;
import android.content.*;
import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.HashMap;
import java.util.Map;

/** Daily persisted checks, plus a network-aware first check when the app opens. */
public class UpdateJob extends JobService {
    static final int PERIODIC=7702,IMMEDIATE=7703;
    private static final class Task {final AtomicBoolean stopped=new AtomicBoolean(false);Future<?> work;}
    private final Map<Integer,Task> tasks=new HashMap<>();
    static void schedule(Context c,boolean checkNow) {
        JobScheduler scheduler=(JobScheduler)c.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if(!Updates.prefs(c).getBoolean("auto_check",true)){scheduler.cancel(PERIODIC);scheduler.cancel(IMMEDIATE);return;}
        boolean scheduled=false;for(JobInfo info:scheduler.getAllPendingJobs())if(info.getId()==PERIODIC)scheduled=true;
        ComponentName component=new ComponentName(c,UpdateJob.class);
        if(!scheduled)scheduler.schedule(new JobInfo.Builder(PERIODIC,component).setPeriodic(Updates.DAY)
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPersisted(true).build());
        if(checkNow&&System.currentTimeMillis()-Updates.prefs(c).getLong("attempt",0)>=Updates.DAY)
            scheduler.schedule(new JobInfo.Builder(IMMEDIATE,component).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setMinimumLatency(0).build());
    }
    @Override public boolean onStartJob(JobParameters job) {
        Task task=new Task();tasks.put(job.getJobId(),task);
        task.work=Updates.run(()->{
            try {if(!task.stopped.get()) {Updates.reconcile(this,Updates.prefs(this).getLong("download_id",-1));Updates.check(this,true);}}
            finally{new Handler(Looper.getMainLooper()).post(()->{if(tasks.get(job.getJobId())==task)tasks.remove(job.getJobId());if(!task.stopped.get())jobFinished(job,false);});}
        });return true;
    }
    @Override public boolean onStopJob(JobParameters job){Task task=tasks.remove(job.getJobId());if(task!=null){task.stopped.set(true);if(task.work!=null)task.work.cancel(true);}return true;}
}
