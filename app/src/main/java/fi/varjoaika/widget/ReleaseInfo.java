package fi.varjoaika.widget;

import org.json.JSONArray;
import org.json.JSONObject;
import java.net.URI;
import java.util.Locale;

/** Only stable, installable assets from this application's public GitHub repository. */
final class ReleaseInfo {
    static final String REPOSITORY="Jakke77/Varjoaika-Android";
    static final String API="https://api.github.com/repos/"+REPOSITORY+"/releases/latest";
    final String version,url,digest;
    ReleaseInfo(String version,String url,String digest){this.version=version;this.url=url;this.digest=digest;}
    static long[] versionParts(String text) {
        if(text==null||!text.matches("v?[0-9]{1,9}(\\.[0-9]{1,9}){0,2}"))
            throw new IllegalArgumentException("Tuntematon versionumero");
        String[] parts=(text.startsWith("v")?text.substring(1):text).split("\\.");
        long[] result=new long[3];for(int i=0;i<parts.length;i++)result[i]=Long.parseLong(parts[i]);return result;
    }
    static int compare(String first,String second) {
        long[] a=versionParts(first),b=versionParts(second);
        for(int i=0;i<3;i++){int cmp=Long.compare(a[i],b[i]);if(cmp!=0)return cmp;}return 0;
    }
    static boolean allowedUrl(String url,String tag,String name) {
        try {
            URI uri=new URI(url);
            return "https".equals(uri.getScheme())&&"github.com".equalsIgnoreCase(uri.getHost())
                &&uri.getPort()==-1&&uri.getUserInfo()==null&&uri.getQuery()==null&&uri.getFragment()==null
                &&("/"+REPOSITORY+"/releases/download/"+tag+"/"+name).equals(uri.getPath());
        }catch(Exception invalid){return false;}
    }
    static ReleaseInfo parse(String json) throws Exception {
        JSONObject release=new JSONObject(json);
        if(release.optBoolean("draft",false)||release.optBoolean("prerelease",false))
            throw new IllegalArgumentException("Julkaisu on testiversio");
        String tag=release.getString("tag_name");versionParts(tag);
        String version=tag.startsWith("v")?tag.substring(1):tag;
        JSONArray assets=release.optJSONArray("assets");ReleaseInfo result=null;
        if(assets!=null)for(int i=0;i<assets.length();i++) {
            JSONObject asset=assets.getJSONObject(i);String name=asset.optString("name","");
            String lower=name.toLowerCase(Locale.ROOT),url=asset.optString("browser_download_url","");
            if(!name.startsWith("Varjoaika-")||!lower.endsWith(".apk")||lower.contains("debug")
                ||lower.contains("unsigned")||lower.contains("test")||asset.optLong("size",0)<=0
                ||!"uploaded".equals(asset.optString("state",""))||!allowedUrl(url,tag,name))continue;
            String digest=asset.optString("digest","");
            if(!digest.isEmpty()&&!digest.matches("sha256:[a-fA-F0-9]{64}"))continue;
            // Ambiguous APKs must be resolved by the release maintainer, not guessed.
            if(result!=null)throw new IllegalArgumentException("Julkaisussa on useita päivitys-APK:ita");
            result=new ReleaseInfo(version,url,digest.toLowerCase(Locale.ROOT));
        }
        return result==null?new ReleaseInfo(version,"",""):result;
    }
}
