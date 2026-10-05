package fi.varjoaika.widget;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.View;
import android.view.WindowInsets;
import android.widget.*;

final class Ui {
    static final int BG=Color.rgb(16,14,24), PANEL=Color.rgb(34,27,43), INK=Color.rgb(245,232,223), MUTED=Color.rgb(198,179,196), COPPER=Color.rgb(233,167,127);
    static int dp(Context c,float n) { return Math.round(n*c.getResources().getDisplayMetrics().density); }
    static GradientDrawable panel(int color) {
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(20);d.setStroke(1,Color.rgb(91,63,82));return d;
    }
    static LinearLayout root(Activity a) {
        ScrollView scroll=new ScrollView(a);scroll.setFillViewport(true);scroll.setBackgroundColor(BG);
        LinearLayout column=new LinearLayout(a);column.setOrientation(LinearLayout.VERTICAL);column.setPadding(dp(a,18),dp(a,16),dp(a,18),dp(a,24));scroll.addView(column);a.setContentView(scroll);
        scroll.setOnApplyWindowInsetsListener((v,insets)->{
            if(Build.VERSION.SDK_INT>=30) { android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());v.setPadding(bars.left,bars.top,bars.right,bars.bottom); }
            else v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());
            return insets;
        });scroll.requestApplyInsets();return column;
    }
    static TextView text(Context c,String value,int size,int color) {
        TextView t=new TextView(c);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setPadding(0,dp(c,6),0,dp(c,6));return t;
    }
    static Button button(Context c,String value) {
        Button b=new Button(c);b.setText(value);b.setTextColor(INK);b.setAllCaps(false);b.setMinHeight(dp(c,48));buttonBackground(b,PANEL);return b;
    }
    static void buttonBackground(Button b,int color) {
        b.setBackgroundTintList(null);
        android.graphics.drawable.RippleDrawable ripple=new android.graphics.drawable.RippleDrawable(
            android.content.res.ColorStateList.valueOf(0x33E9A77F),panel(color),null);
        int gap=dp(b.getContext(),2);
        b.setBackground(new android.graphics.drawable.InsetDrawable(ripple,gap));
    }
    static EditText input(Context c,String hint,String value) {
        EditText e=new EditText(c);e.setHint(hint);e.setHintTextColor(MUTED);e.setTextColor(INK);e.setText(value);return e;
    }
    static Spinner spinner(Context c,String[] choices,int selected) {
        Spinner s=new Spinner(c);s.setAdapter(new ArrayAdapter<>(c,android.R.layout.simple_spinner_dropdown_item,choices));s.setSelection(selected);return s;
    }
    static void heading(LinearLayout root,String title,String description) {
        TextView t=text(root.getContext(),title,28,INK);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);root.addView(t);root.addView(text(root.getContext(),description,13,MUTED));
    }
    private Ui() {}
}
