package fi.varjoaika.widget;
import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.*;

public class SettingsActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);LinearLayout root=Ui.root(this);
        Ui.heading(root,"Huuhkajan ääni","Huhuilu on sovellusta varten luotu jäljitelmä. Kalenteri, widgetit ja muistutukset toimivat ilman verkkoyhteyttä.");
        CheckBox enabled=new CheckBox(this);enabled.setText("Toista ilmoitusääni");enabled.setChecked(Sound.prefs(this).getBoolean("enabled",true));root.addView(enabled);
        TextView title=Ui.text(this,"",16,Ui.INK);root.addView(title);SeekBar volume=new SeekBar(this);volume.setMax(100);volume.setProgress(Sound.prefs(this).getInt("volume",65));title.setText("Voimakkuus "+volume.getProgress()+" %");root.addView(volume);
        volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}public void onProgressChanged(SeekBar b,int p,boolean user){title.setText("Voimakkuus "+p+" %");}});
        root.addView(Ui.text(this,"Puhelimen ilmoitusäänen voimakkuus ja Älä häiritse -tila vaikuttavat myös ääneen. Omat äänet toistetaan enintään 8 sekunnin ajan.",14,Ui.MUTED));
        Button save=Ui.button(this,"Tallenna ääniasetukset"),test=Ui.button(this,"Kuuntele ääni"),pick=Ui.button(this,"Valitse oma äänitiedosto"),reset=Ui.button(this,"Käytä huuhkajan huhuilua"),permissions=Ui.button(this,"Ilmoitus- ja hälytysluvat"),done=Ui.button(this,"Takaisin kalenteriin");
        for(Button b:new Button[]{save,test,pick,reset,permissions,done})root.addView(b);
        Runnable persist=()->Sound.prefs(this).edit().putBoolean("enabled",enabled.isChecked()).putInt("volume",volume.getProgress()).apply();
        save.setOnClickListener(v->{persist.run();Toast.makeText(this,"Ääniasetukset tallennettu",Toast.LENGTH_SHORT).show();});
        test.setOnClickListener(v->{persist.run();Sound.play(this,null);});
        pick.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("audio/*").addCategory(Intent.CATEGORY_OPENABLE);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);startActivityForResult(i,1);});
        reset.setOnClickListener(v->{Sound.prefs(this).edit().remove("uri").apply();Toast.makeText(this,"Huuhkajan ääni valittu",Toast.LENGTH_SHORT).show();});
        permissions.setOnClickListener(v->{
            if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},2);
            else if(Build.VERSION.SDK_INT>=31&&!((AlarmManager)getSystemService(ALARM_SERVICE)).canScheduleExactAlarms())startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+getPackageName())));
            else startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName())));
        });done.setOnClickListener(v->finish());
        Ui.heading(root,"Kotinäytön widgetit","Paina kotinäytön tyhjää kohtaa pitkään → Widgetit → Varjoaika.");
        root.addView(Ui.text(this,"Kello 2×2 · Varjokupari 2×3\nKalenteri 3×3 ja 4×4 · Merkinnät 2×3\n\nJokaisella widgetillä on oma teema, peittävyys ja tekstikoko. ⚙ avaa sen asetukset. Kotinäyttö päättää tarkan ruutukoon.",15,Ui.MUTED));
        Ui.heading(root,"Suomen aika","time.mikes.fi · VTT MIKES");
        CheckBox weekly=new CheckBox(this);weekly.setText("Tarkista aika kerran viikossa MIKESiltä");weekly.setChecked(NtpJob.prefs(this).getBoolean("enabled",true));root.addView(weekly);
        TextView status=Ui.text(this,NtpJob.status(this),14,Ui.MUTED);root.addView(status);
        weekly.setOnCheckedChangeListener((b,value)->{NtpJob.prefs(this).edit().putBoolean("enabled",value).apply();NtpJob.schedule(this);status.setText(NtpJob.status(this));});
        root.addView(Ui.text(this,"Vain ajan tarkistus käyttää verkkoa (NTP/UDP 123). Android voi viivästyttää viikoittaista työtä virransäästössä. Kello käyttää Androidin järjestelmäaikaa; sovellus ei voi asettaa puhelimen aikaa. Pidä puhelimen automaattinen päivä ja aika käytössä.",14,Ui.MUTED));
        Button timeSettings=Ui.button(this,"Avaa puhelimen aika-asetukset");root.addView(timeSettings);timeSettings.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_DATE_SETTINGS)));
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if(request==1&&result==RESULT_OK&&data!=null&&data.getData()!=null) {
            Uri uri=data.getData();
            try { getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);Sound.prefs(this).edit().putString("uri",uri.toString()).apply();Toast.makeText(this,"Oma ääni valittu",Toast.LENGTH_SHORT).show(); }
            catch(SecurityException e){Toast.makeText(this,"Äänitiedoston käyttöoikeutta ei saatu",Toast.LENGTH_LONG).show();}
        }
    }
    @Override protected void onPause(){Sound.stop();super.onPause();}
}
