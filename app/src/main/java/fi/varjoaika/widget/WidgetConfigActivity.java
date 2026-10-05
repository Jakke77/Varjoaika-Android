package fi.varjoaika.widget;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.*;

public class WidgetConfigActivity extends Activity {
    private int widget;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);setResult(RESULT_CANCELED);
        widget=getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,AppWidgetManager.INVALID_APPWIDGET_ID);
        AppWidgetProviderInfo info=AppWidgetManager.getInstance(this).getAppWidgetInfo(widget);
        if(info==null||!info.provider.getPackageName().equals(getPackageName())){finish();return;}
        SharedPreferences p=VarjoWidget.prefs(this,widget);int initial=VarjoWidget.defaultStyle(info);
        LinearLayout root=Ui.root(this);Ui.heading(root,"Oma pala Varjoaikaa","Nämä asetukset koskevat vain tätä widgetiä.");
        root.addView(Ui.text(this,"Teema",16,Ui.INK));Spinner style=Ui.spinner(this,new String[]{"Varjokupari","Yöhopea","Läpinäkyvä teksti"},p.getInt("style",initial));root.addView(style);
        SeekBar background=slider(root,"Taustan peittävyys",p.getInt("background",initial==2?0:85),100);
        SeekBar clock=slider(root,"Kellon tekstikoko",p.getInt("clock_size",36)-18,36);
        SeekBar date=slider(root,"Päiväyksen tekstikoko",p.getInt("date_size",13)-10,12);
        CheckBox seconds=new CheckBox(this);seconds.setText("Näytä sekunnit");seconds.setChecked(p.getBoolean("seconds",false));root.addView(seconds);
        root.addView(Ui.text(this,"Läpinäkyvässä teemassa paneeli katoaa. Kotinäyttö määrää tarkan koon; widgetiä voi venyttää. Kellonaika tulee puhelimen järjestelmästä.",14,Ui.MUTED));
        Button save=Ui.button(this,"Tallenna widget"),cancel=Ui.button(this,"Peruuta");root.addView(save);root.addView(cancel);
        save.setOnClickListener(v->{p.edit().putInt("style",style.getSelectedItemPosition()).putInt("background",background.getProgress()).putInt("clock_size",clock.getProgress()+18).putInt("date_size",date.getProgress()+10).putBoolean("seconds",seconds.isChecked()).apply();VarjoWidget.updateAll(this);setResult(RESULT_OK,new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,widget));finish();});cancel.setOnClickListener(v->finish());
    }
    private SeekBar slider(LinearLayout root,String label,int value,int max) {
        root.addView(Ui.text(this,label,16,Ui.INK));SeekBar b=new SeekBar(this);b.setMax(max);b.setProgress(Math.max(0,Math.min(max,value)));root.addView(b);return b;
    }
}
