package fi.varjoaika.widget;
import android.graphics.*;

final class WidgetArt {
    static int ink(int style) { return style==1?0xFFE9E4F4:0xFFF5E8DF; }
    static int accent(int style) { return style==1?0xFFBBB6ED:0xFFE9A77F; }
    static Bitmap background(int style,int opacity) {
        Bitmap b=Bitmap.createBitmap(256,256,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        if(style==2||opacity<=0)return b;
        p.setShader(new LinearGradient(0,0,256,256,style==1?0xFF24213B:0xFF352330,0xFF0D0C16,Shader.TileMode.CLAMP));p.setAlpha(Math.max(0,Math.min(255,opacity*255/100)));
        c.drawRoundRect(new RectF(1,1,255,255),18,18,p);p.setShader(null);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1);p.setColor(accent(style));p.setAlpha(opacity*80/100);c.drawRoundRect(new RectF(1,1,255,255),18,18,p);
        p.setAlpha(opacity*35/100);c.drawCircle(235,28,96,p);c.drawCircle(235,28,128,p);return b;
    }
    private WidgetArt() {}
}
