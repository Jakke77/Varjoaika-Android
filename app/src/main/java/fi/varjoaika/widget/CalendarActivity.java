package fi.varjoaika.widget;
import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlarmManager;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.widget.*;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class CalendarActivity extends Activity {
    private long month,selected;
    private EntryStore store;
    private LinearLayout root;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);store=new EntryStore(this);NtpJob.schedule(this);
        selected=state==null?Math.max(0,getIntent().getLongExtra("day",Dates.today())):state.getLong("day");
        selected=Math.min(Dates.offset(7000,12,29),selected);
        month=state==null?selected/30:state.getLong("month");render();
    }
    @Override protected void onNewIntent(Intent intent) { super.onNewIntent(intent);setIntent(intent);selected=Math.max(0,Math.min(Dates.offset(7000,12,29),intent.getLongExtra("day",Dates.today())));month=selected/30;render(); }
    @Override protected void onSaveInstanceState(Bundle state) { super.onSaveInstanceState(state);state.putLong("day",selected);state.putLong("month",month); }
    @Override protected void onResume() { super.onResume();if(store!=null){ReminderReceiver.deliver(this);ReminderReceiver.schedule(this);render();} }
    @Override protected void onDestroy() { if(store!=null)store.close();super.onDestroy(); }
    private void render() {
        root=Ui.root(this);
        Ui.heading(root,"Varjoaika","Pieni paikka merkinnöille, suurillekin ajatuksille.");
        root.addView(Ui.text(this,VarjoDate.official(Calendar.getInstance()),14,Ui.MUTED));
        LinearLayout tools=new LinearLayout(this);
        Button today=Ui.button(this,"Tänään"),agenda=Ui.button(this,"Merkinnät"),settings=Ui.button(this,"Asetukset");
        for(Button b:new Button[]{today,agenda,settings})tools.addView(b,new LinearLayout.LayoutParams(0,-2,1));root.addView(tools);
        today.setOnClickListener(v->{selected=Math.max(0,Dates.today());month=selected/30;render();});
        agenda.setOnClickListener(v->showAll());settings.setOnClickListener(v->startActivity(new Intent(this,SettingsActivity.class)));
        LinearLayout nav=new LinearLayout(this);nav.setGravity(Gravity.CENTER_VERTICAL);
        Button previous=Ui.button(this,"‹"),next=Ui.button(this,"›");
        TextView heading=Ui.text(this,VarjoDate.MONTHS[(int)(month%13)]+"\nVuosi "+(month/13+1),20,Ui.INK);heading.setGravity(Gravity.CENTER);
        nav.addView(previous,new LinearLayout.LayoutParams(Ui.dp(this,48),Ui.dp(this,48)));nav.addView(heading,new LinearLayout.LayoutParams(0,-2,1));nav.addView(next,new LinearLayout.LayoutParams(Ui.dp(this,48),Ui.dp(this,48)));root.addView(nav);
        previous.setEnabled(month>0);previous.setOnClickListener(v->{month--;render();});
        next.setEnabled(month<7000L*13-1);next.setOnClickListener(v->{month++;render();});
        LinearLayout week=new LinearLayout(this);
        for(String name:new String[]{"Va","Ka","Ru","Ka","Kr","No","Ho"}) { TextView t=Ui.text(this,name,12,Ui.MUTED);t.setGravity(Gravity.CENTER);week.addView(t,new LinearLayout.LayoutParams(0,-2,1)); }root.addView(week);
        int start=(int)(month*30%7);
        java.util.Set<Long> marked=new java.util.HashSet<>();for(EntryStore.Entry e:store.all())if(e.day/30==month)marked.add(e.day);
        int rows=(start+30+6)/7;
        for(int row=0;row<rows;row++) {
            LinearLayout line=new LinearLayout(this);
            for(int col=0;col<7;col++) {
                int day=row*7+col-start;
                if(day<0||day>=30) { line.addView(new TextView(this),new LinearLayout.LayoutParams(0,Ui.dp(this,52),1));continue; }
                long offset=month*30+day;Button tile=Ui.button(this,""+day+(marked.contains(offset)?" •":""));tile.setTextSize(14);tile.setPadding(0,0,0,0);
                tile.setContentDescription(Dates.label(offset)+(marked.contains(offset)?", merkintöjä":""));
                Ui.buttonBackground(tile,offset==selected?Ui.COPPER:offset==Dates.today()?0xFF46354D:Ui.PANEL);
                if(offset==selected)tile.setTextColor(Ui.BG);
                tile.setOnClickListener(v->{selected=offset;render();});line.addView(tile,new LinearLayout.LayoutParams(0,Ui.dp(this,52),1));
            }root.addView(line);
        }
        root.addView(Ui.text(this,Dates.label(selected),17,Ui.INK));root.addView(Ui.text(this,VarjoDate.official(Dates.civil(selected)),12,Ui.MUTED));
        Button add=Ui.button(this,"＋ Lisää merkintä tai muistutus");root.addView(add);add.setOnClickListener(v->edit(null,selected));
        List<EntryStore.Entry> entries=store.day(selected);
        if(entries.isEmpty())root.addView(Ui.text(this,"Päivä on vielä tyhjä.",14,Ui.MUTED));
        for(EntryStore.Entry e:entries) {
            Button b=Ui.button(this,e.title+(e.due>0?"  ◷ "+time(e.due)+(e.delivered?" · ilmoitettu":""):""));root.addView(b);b.setOnClickListener(v->entryActions(e));
            if(!e.body.isEmpty())root.addView(Ui.text(this,e.body,14,Ui.MUTED));
        }
    }
    static String time(long utc) { Calendar c=Calendar.getInstance();c.setTimeInMillis(utc);return String.format(Locale.ROOT,"%02d:%02d",c.get(Calendar.HOUR_OF_DAY),c.get(Calendar.MINUTE)); }
    private void showAll() {
        List<EntryStore.Entry> entries=store.all();String[] titles=new String[entries.size()];
        for(int i=0;i<titles.length;i++)titles[i]=entries.get(i).title+"\n"+Dates.label(entries.get(i).day);
        new AlertDialog.Builder(this).setTitle("Kaikki merkinnät").setItems(titles,(d,i)->{selected=entries.get(i).day;month=selected/30;render();entryActions(entries.get(i));}).setNegativeButton("Sulje",null).show();
    }
    private void entryActions(EntryStore.Entry e) {
        new AlertDialog.Builder(this).setTitle(e.title).setMessage(e.body).setPositiveButton("Muokkaa",(d,w)->edit(e,e.day)).setNeutralButton("Poista",(d,w)->new AlertDialog.Builder(this).setTitle("Poistetaanko merkintä?").setMessage(e.title).setPositiveButton("Poista",(x,y)->{store.remove(e.id);ReminderReceiver.cancelNotification(this,e.id);changed();}).setNegativeButton("Peruuta",null).show()).setNegativeButton("Sulje",null).show();
    }
    private void edit(EntryStore.Entry original,long day) {
        EntryStore.Entry entry=new EntryStore.Entry();entry.day=day;entry.body="";entry.title="";
        if(original!=null){entry.id=original.id;entry.title=original.title;entry.body=original.body;entry.due=original.due;entry.delivered=original.delivered;}
        LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(Ui.dp(this,20),Ui.dp(this,8),Ui.dp(this,20),0);
        EditText title=Ui.input(this,"Otsikko",entry.title),body=Ui.input(this,"Muistiinpano",entry.body);title.setSingleLine(true);body.setMinLines(2);form.addView(title);form.addView(body);
        form.addView(Ui.text(this,"Varjovuosi · kuukausi · päivä",12,Ui.MUTED));
        LinearLayout dateRow=new LinearLayout(this);EditText year=Ui.input(this,"Vuosi",""+(day/390+1));year.setInputType(InputType.TYPE_CLASS_NUMBER);
        Spinner monthPicker=Ui.spinner(this,VarjoDate.MONTHS,(int)(day%390/30));Spinner dayPicker=Ui.spinner(this,dayLabels(),(int)(day%30));
        dateRow.addView(year,new LinearLayout.LayoutParams(0,-2,1));dateRow.addView(monthPicker,new LinearLayout.LayoutParams(0,-2,2));dateRow.addView(dayPicker,new LinearLayout.LayoutParams(0,-2,1));form.addView(dateRow);
        CheckBox alarm=new CheckBox(this);alarm.setText("Muistuta ilmoituksella ja huhuilulla");alarm.setChecked(entry.due>0);form.addView(alarm);
        Calendar now=Calendar.getInstance();if(entry.due>0)now.setTimeInMillis(entry.due);int[] clock={now.get(Calendar.HOUR_OF_DAY),now.get(Calendar.MINUTE)};
        Button choose=Ui.button(this,String.format(Locale.ROOT,"Kellonaika %02d:%02d",clock[0],clock[1]));form.addView(choose);
        choose.setOnClickListener(v->new TimePickerDialog(this,(p,h,m)->{clock[0]=h;clock[1]=m;choose.setText(String.format(Locale.ROOT,"Kellonaika %02d:%02d",h,m));},clock[0],clock[1],true).show());
        ScrollView scroll=new ScrollView(this);scroll.addView(form);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(original==null?"Uusi merkintä":"Muokkaa merkintää").setView(scroll).setPositiveButton("Tallenna",null).setNegativeButton("Peruuta",null).create();
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            try {
                long offset=Dates.offset(Long.parseLong(year.getText().toString()),monthPicker.getSelectedItemPosition(),dayPicker.getSelectedItemPosition());
                long due=alarm.isChecked()?Dates.due(offset,clock[0],clock[1]):0;
                if(due>0&&due!=entry.due&&due<=System.currentTimeMillis())throw new IllegalArgumentException("Valitse tuleva muistutusaika");
                boolean changedDue=due!=entry.due;entry.day=offset;entry.title=title.getText().toString();entry.body=body.getText().toString();entry.due=due;if(changedDue)entry.delivered=false;
                store.save(entry);selected=offset;month=offset/30;changed();dialog.dismiss();if(alarm.isChecked())requestReminders();
            } catch(IllegalArgumentException error) { Toast.makeText(this,error.getMessage()==null?"Tarkista päivä ja kellonaika":error.getMessage(),Toast.LENGTH_LONG).show(); }
        }));dialog.show();
    }
    private String[] dayLabels() { String[] labels=new String[30];for(int i=0;i<30;i++)labels[i]=""+i;return labels; }
    void requestReminders() {
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},1);
        else if(Build.VERSION.SDK_INT>=31&&!((AlarmManager)getSystemService(ALARM_SERVICE)).canScheduleExactAlarms())new AlertDialog.Builder(this).setTitle("Tarkat muistutukset").setMessage("Android voi viivästyttää muistutusta virransäästössä. Voit sallia tarkat hälytykset sovelluksen asetuksissa.").setPositiveButton("Avaa asetus",(d,w)->startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+getPackageName())))).setNegativeButton("Myöhemmin",null).show();
    }
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results) { super.onRequestPermissionsResult(request,permissions,results);ReminderReceiver.deliver(this);ReminderReceiver.schedule(this); }
    private void changed() { VarjoWidget.updateAll(this);ReminderReceiver.schedule(this);render(); }
}
