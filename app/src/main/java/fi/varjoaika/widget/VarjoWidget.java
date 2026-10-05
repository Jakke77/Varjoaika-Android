package fi.varjoaika.widget;
import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;
import java.util.Calendar;
import java.util.List;

public class VarjoWidget extends AppWidgetProvider {
    static final String REFRESH="fi.varjoaika.android.WIDGET_REFRESH", NAV="fi.varjoaika.android.MONTH_NAV";
    static final Class<?>[] PROVIDERS={VarjoWidget.class,CopperClockWidget.class,CalendarWidget.class,LargeCalendarWidget.class,AgendaWidget.class};
    static SharedPreferences prefs(Context c,int id){return c.getSharedPreferences("widget_"+id,Context.MODE_PRIVATE);}
    static boolean calendar(AppWidgetProviderInfo info){return info.provider.getClassName().endsWith("CalendarWidget");}
    static int defaultStyle(AppWidgetProviderInfo info){return info.provider.getClassName().equals(VarjoWidget.class.getName())?2:0;}
    public static void updateAll(Context c) {
        AppWidgetManager m=AppWidgetManager.getInstance(c);int count=0;
        for(Class<?> provider:PROVIDERS)for(int id:m.getAppWidgetIds(new ComponentName(c,provider))){update(c,m,id);count++;}
        AlarmManager alarms=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        PendingIntent refresh=PendingIntent.getBroadcast(c,0,new Intent(c,VarjoWidget.class).setAction(REFRESH),ReminderReceiver.flags());
        if(count==0){alarms.cancel(refresh);return;}
        Calendar midnight=Calendar.getInstance();midnight.add(Calendar.DAY_OF_MONTH,1);midnight.set(Calendar.HOUR_OF_DAY,0);midnight.set(Calendar.MINUTE,0);midnight.set(Calendar.SECOND,0);midnight.set(Calendar.MILLISECOND,0);
        if(Build.VERSION.SDK_INT>=23)alarms.setAndAllowWhileIdle(AlarmManager.RTC,midnight.getTimeInMillis(),refresh);else alarms.set(AlarmManager.RTC,midnight.getTimeInMillis(),refresh);
    }
    static PendingIntent open(Context c,int id,long day) { return PendingIntent.getActivity(c,id,new Intent(c,CalendarActivity.class).setData(Uri.parse("varjoaika://widget/"+id+"/"+day)).putExtra("day",day),ReminderReceiver.flags()); }
    static RemoteViews render(Context c,AppWidgetProviderInfo info,int id,int width,int height) {
        SharedPreferences p=prefs(c,id);int style=p.getInt("style",defaultStyle(info)),opacity=p.getInt("background",style==2?0:85);
        boolean isCalendar=calendar(info),isAgenda=info.provider.getClassName().endsWith("AgendaWidget");
        RemoteViews v=new RemoteViews(c.getPackageName(),isCalendar?R.layout.widget_calendar:isAgenda?R.layout.widget_agenda:R.layout.widget_clock);
        v.setImageViewBitmap(R.id.background,WidgetArt.background(style,opacity));v.setTextColor(R.id.title,WidgetArt.accent(style));
        Calendar now=Calendar.getInstance();long today=Math.max(0,Dates.today());VarjoDate date=VarjoDate.from(now);
        v.setTextViewText(R.id.gothic_date,date==null?"Ennen Varjoajan alkua":date.dateLabel()+"\n"+date.yearLabel());
        if(date!=null&&height<220)v.setTextViewText(R.id.gothic_date,date.day+". "+VarjoDate.MONTHS[date.month]+"\nVuosi "+date.year);
        v.setInt(R.id.gothic_date,"setMaxLines",height<220?2:4);
        v.setTextViewText(R.id.official,VarjoDate.official(now));v.setTextColor(R.id.gothic_date,WidgetArt.ink(style));v.setTextColor(R.id.official,WidgetArt.ink(style));
        v.setTextViewTextSize(R.id.gothic_date,TypedValue.COMPLEX_UNIT_SP,Math.min(p.getInt("date_size",13),width<180?11:16));
        v.setOnClickPendingIntent(R.id.title,open(c,id,today));
        PendingIntent config=PendingIntent.getActivity(c,id,new Intent(c,WidgetConfigActivity.class).setData(Uri.parse("varjoaika://config/"+id)).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id),ReminderReceiver.flags());
        v.setOnClickPendingIntent(R.id.settings,config);
        if(!isCalendar) {
            String format=p.getBoolean("seconds",false)?"HH:mm:ss":"HH:mm";
            v.setCharSequence(R.id.clock,"setFormat12Hour",format);v.setCharSequence(R.id.clock,"setFormat24Hour",format);v.setTextColor(R.id.clock,WidgetArt.ink(style));
            float size=Math.min(p.getInt("clock_size",isAgenda?25:36),(width-24)/(p.getBoolean("seconds",false)?5.2f:3.3f));
            v.setTextViewTextSize(R.id.clock,TypedValue.COMPLEX_UNIT_SP,Math.max(16,size));v.setOnClickPendingIntent(R.id.clock,open(c,id,today));
        }
        EntryStore store=new EntryStore(c);
        try {
            if(isCalendar) {
                long month=Math.max(0,Math.min(7000L*13-1,today/30+p.getInt("month_offset",0)));
                v.setTextViewText(R.id.title,VarjoDate.MONTHS[(int)(month%13)]+" · "+(month/13+1));v.setOnClickPendingIntent(R.id.title,open(c,id,month*30));
                v.setViewVisibility(R.id.gothic_date,View.GONE);v.removeAllViews(R.id.calendar_rows);
                int start=(int)(month*30%7);int[] cells={R.id.d0,R.id.d1,R.id.d2,R.id.d3,R.id.d4,R.id.d5,R.id.d6};
                java.util.Set<Long> marked=new java.util.HashSet<>();for(EntryStore.Entry e:store.all())if(e.day/30==month)marked.add(e.day);
                for(int row=0;row<(start+30+6)/7;row++) {
                    RemoteViews r=new RemoteViews(c.getPackageName(),R.layout.day_row);
                    for(int col=0;col<7;col++) {
                        int day=row*7+col-start;long offset=month*30+day;
                        r.setTextViewText(cells[col],day<0||day>=30?"":""+day+(marked.contains(offset)?" •":""));
                        r.setTextColor(cells[col],offset==today?WidgetArt.accent(style):WidgetArt.ink(style));
                        r.setTextViewTextSize(cells[col],TypedValue.COMPLEX_UNIT_SP,Math.max(10,Math.min(16,(width-24)/7f/2.4f)));
                        if(day>=0&&day<30){r.setContentDescription(cells[col],Dates.label(offset));r.setOnClickPendingIntent(cells[col],open(c,id,offset));}
                    }v.addView(R.id.calendar_rows,r);
                }
                for(int delta:new int[]{-1,1}) {
                    Intent nav=new Intent(c,VarjoWidget.class).setAction(NAV).setData(Uri.parse("varjoaika://nav/"+id+"/"+delta)).putExtra("widget",id).putExtra("delta",delta);
                    v.setOnClickPendingIntent(delta<0?R.id.previous:R.id.next,PendingIntent.getBroadcast(c,id*2+(delta<0?0:1),nav,ReminderReceiver.flags()));
                }
            } else if(!isAgenda) {
                v.setTextViewText(R.id.title,"VARJOAIKA");
            }
            if(isCalendar||isAgenda) {
                List<EntryStore.Entry> entries=store.upcoming(today);StringBuilder text=new StringBuilder();int max=isAgenda?Math.max(1,Math.min(5,(height-130)/38)):height>=300?3:1;
                for(int n=0;n<Math.min(max,entries.size());n++){EntryStore.Entry e=entries.get(n);if(n>0)text.append("\n");text.append(e.day%30).append(". ").append(e.title);if(e.due>0)text.append(" · ").append(CalendarActivity.time(e.due));}
                v.setTextViewText(R.id.agenda_text,text.length()==0?"Ei tulevia merkintöjä":text.toString());v.setTextColor(R.id.agenda_text,WidgetArt.ink(style));v.setOnClickPendingIntent(R.id.agenda_text,open(c,id,entries.isEmpty()?today:entries.get(0).day));
                if(isAgenda)v.setTextViewText(R.id.title,"TULEVAT MERKINNÄT");
                if(isCalendar&&height<280){v.setViewVisibility(R.id.agenda_text,View.GONE);v.setViewVisibility(R.id.official,View.GONE);}
            }
        }finally{store.close();}
        if(!isCalendar)v.setViewVisibility(R.id.official,height<160?View.GONE:View.VISIBLE);
        return v;
    }
    public static void update(Context c,AppWidgetManager manager,int id) {
        AppWidgetProviderInfo info=manager.getAppWidgetInfo(id);if(info==null)return;
        Bundle size=manager.getAppWidgetOptions(id);manager.updateAppWidget(id,render(c,info,id,size.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,220),size.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,250)));
    }
    @Override public void onUpdate(Context c,AppWidgetManager m,int[] ids){updateAll(c);}
    @Override public void onAppWidgetOptionsChanged(Context c,AppWidgetManager m,int id,Bundle options){update(c,m,id);}
    @Override public void onDeleted(Context c,int[] ids){for(int id:ids)prefs(c,id).edit().clear().apply();updateAll(c);}
    @Override public void onDisabled(Context c){updateAll(c);}
    @Override public void onReceive(Context c,Intent intent) {
        super.onReceive(c,intent);String action=intent.getAction();
        if(REFRESH.equals(action))updateAll(c);
        if(NAV.equals(action)) {
            int id=intent.getIntExtra("widget",-1),delta=intent.getIntExtra("delta",0);AppWidgetManager m=AppWidgetManager.getInstance(c);AppWidgetProviderInfo info=m.getAppWidgetInfo(id);
            if(info==null||!info.provider.getPackageName().equals(c.getPackageName())||!calendar(info)||Math.abs(delta)!=1)return;
            long base=Math.max(0,Dates.today())/30;int next=(int)Math.max(-base,Math.min(7000L*13-1-base,prefs(c,id).getInt("month_offset",0)+delta));
            prefs(c,id).edit().putInt("month_offset",next).apply();update(c,m,id);
        }
    }
}
