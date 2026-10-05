package fi.varjoaika.widget;
import android.content.Context;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

/** Private persistent notes and one-shot reminders. No phone calendar permission. */
public final class EntryStore extends SQLiteOpenHelper {
    public static final class Entry {
        public long id,day,due; public String title,body; public boolean delivered;
    }
    public EntryStore(Context c) { super(c,"varjoaika.db",null,1); }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE entries (id INTEGER PRIMARY KEY AUTOINCREMENT, day INTEGER NOT NULL, title TEXT NOT NULL, body TEXT NOT NULL DEFAULT '', due INTEGER NOT NULL DEFAULT 0, delivered INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE INDEX entry_due ON entries(due,delivered)");
    }
    @Override public void onUpgrade(SQLiteDatabase db,int old,int next) { throw new IllegalStateException("Tuntematon tietokantapäivitys"); }
    private Entry read(Cursor c) {
        Entry e=new Entry();e.id=c.getLong(0);e.day=c.getLong(1);e.title=c.getString(2);e.body=c.getString(3);e.due=c.getLong(4);e.delivered=c.getInt(5)!=0;return e;
    }
    private List<Entry> query(String where,String[] args,String order) {
        List<Entry> out=new ArrayList<>();
        try(Cursor c=getReadableDatabase().query("entries",null,where,args,null,null,order)) { while(c.moveToNext())out.add(read(c)); }
        return out;
    }
    public List<Entry> day(long day) { return query("day=?",new String[]{""+day},"due,id"); }
    public List<Entry> all() { return query(null,null,"day,due,id"); }
    public List<Entry> upcoming(long day) { return query("day>=?",new String[]{""+day},"day,due,id LIMIT 8"); }
    public List<Entry> due(long now) { return query("due>0 AND due<=? AND delivered=0",new String[]{""+now},"due,id"); }
    public Entry get(long id) {
        List<Entry> rows=query("id=?",new String[]{""+id},"id");return rows.isEmpty()?null:rows.get(0);
    }
    public void save(Entry e) {
        String title=e.title.trim();
        if(title.isEmpty()||title.length()>200||e.body.length()>20000)throw new IllegalArgumentException("Anna otsikko (enintään 200 merkkiä)");
        if(e.day<0||e.day>Dates.offset(7000,12,29)||e.due<0)throw new IllegalArgumentException("Päivämäärä ei kelpaa");
        ContentValues v=new ContentValues();v.put("day",e.day);v.put("title",title);v.put("body",e.body);v.put("due",e.due);v.put("delivered",e.delivered?1:0);
        if(e.id==0)e.id=getWritableDatabase().insertOrThrow("entries",null,v);
        else getWritableDatabase().update("entries",v,"id=?",new String[]{""+e.id});
    }
    public boolean claim(long id) {
        ContentValues v=new ContentValues();v.put("delivered",1);
        return getWritableDatabase().update("entries",v,"id=? AND delivered=0",new String[]{""+id})==1;
    }
    public void retry(long id) { ContentValues v=new ContentValues();v.put("delivered",0);getWritableDatabase().update("entries",v,"id=?",new String[]{""+id}); }
    public long nextDue() {
        try(Cursor c=getReadableDatabase().rawQuery("SELECT MIN(due) FROM entries WHERE due>0 AND delivered=0",null)) { return c.moveToFirst()&&!c.isNull(0)?c.getLong(0):0; }
    }
    public void remove(long id) { getWritableDatabase().delete("entries","id=?",new String[]{""+id}); }
}
