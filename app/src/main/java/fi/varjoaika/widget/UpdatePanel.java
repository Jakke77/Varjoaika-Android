package fi.varjoaika.widget;

import android.Manifest;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Build;
import android.widget.*;

final class UpdatePanel implements SharedPreferences.OnSharedPreferenceChangeListener {
    private final Activity activity;
    private final SharedPreferences prefs;
    private final TextView status;
    private final Button check,download,install;
    private final CheckBox autoDownload;
    private boolean checking;
    UpdatePanel(Activity activity,LinearLayout root) {
        this.activity=activity;prefs=Updates.prefs(activity);
        Ui.heading(root,"Sovelluksen päivitykset","Varjoaika · GitHub-julkaisut");
        status=Ui.text(activity,Updates.status(activity),14,Ui.MUTED);root.addView(status);
        CheckBox automatic=new CheckBox(activity);automatic.setText("Tarkista uusi versio automaattisesti päivittäin");
        automatic.setChecked(prefs.getBoolean("auto_check",true));root.addView(automatic);
        autoDownload=new CheckBox(activity);autoDownload.setText("Lataa päivitys automaattisesti Wi-Fissä");
        autoDownload.setChecked(prefs.getBoolean("auto_download",true));autoDownload.setEnabled(automatic.isChecked());root.addView(autoDownload);
        automatic.setOnCheckedChangeListener((b,on)->{prefs.edit().putBoolean("auto_check",on).apply();autoDownload.setEnabled(on);UpdateJob.schedule(activity,on);if(!on)Updates.fileTask(()->Updates.cancelAutomaticDownload(activity.getApplicationContext()));});
        autoDownload.setOnCheckedChangeListener((b,on)->{
            prefs.edit().putBoolean("auto_download",on).apply();
            if(on&&automatic.isChecked()&&Updates.available(activity))Updates.fileTask(()->Updates.download(activity.getApplicationContext(),true));
            if(!on)Updates.fileTask(()->Updates.cancelAutomaticDownload(activity.getApplicationContext()));
        });
        root.addView(Ui.text(activity,"Asennus tarvitsee hyväksyntäsi Androidin asennusnäkymässä. Manuaalinen lataus voi käyttää myös mobiilidataa. Voit poistaa automaattitarkistuksen käytöstä ja tarkistaa päivitykset itse.",14,Ui.MUTED));
        check=Ui.button(activity,"Tarkista päivitykset");
        download=Ui.button(activity,"Lataa päivitys");install=Ui.button(activity,"Asenna päivitys");
        root.addView(check);root.addView(download);root.addView(install);
        check.setOnClickListener(v->{
            if(checking)return;checking=true;refresh();
            Updates.run(()->{
                Updates.check(activity.getApplicationContext(),false);
                activity.runOnUiThread(()->{checking=false;if(!activity.isFinishing()&&!activity.isDestroyed())refresh();});
            });
        });
        download.setOnClickListener(v->Updates.fileTask(()->Updates.download(activity.getApplicationContext(),false)));
        install.setOnClickListener(v->Updates.install(activity));
        Button notifications=Ui.button(activity,"Salli päivitysilmoitukset");root.addView(notifications);
        notifications.setOnClickListener(v->{
            if(Build.VERSION.SDK_INT>=33&&activity.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)
                activity.requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},3);
            else {
                android.content.Intent intent=new android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:"+activity.getPackageName()));
                activity.startActivity(intent);
            }
        });
        refresh();
    }
    void resume() {
        prefs.registerOnSharedPreferenceChangeListener(this);UpdateJob.schedule(activity,true);refresh();
        Updates.fileTask(()->Updates.reconcile(activity.getApplicationContext(),prefs.getLong("download_id",-1)));
    }
    void pause(){prefs.unregisterOnSharedPreferenceChangeListener(this);}
    private void refresh() {
        status.setText(checking?"Tarkistetaan GitHubista…":Updates.status(activity));
        check.setEnabled(!checking);check.setText(checking?"Tarkistetaan…":"Tarkista päivitykset");
        boolean available=Updates.available(activity),ready=Updates.ready(activity);
        boolean pending="downloading".equals(prefs.getString("download_status",""))&&prefs.getString("download_version","").equals(prefs.getString("version",""));
        download.setEnabled(available&&!ready&&!pending);install.setEnabled(ready);
    }
    @Override public void onSharedPreferenceChanged(SharedPreferences p,String key){if(!activity.isFinishing()&&!activity.isDestroyed())refresh();}
}
