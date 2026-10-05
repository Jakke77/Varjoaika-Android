package fi.varjoaika.widget;

import org.junit.Test;
import static org.junit.Assert.*;

public class ReleaseInfoTest {
    private String asset(String name,String url) {
        return "{\"name\":\""+name+"\",\"browser_download_url\":\""+url+"\",\"size\":12000,\"state\":\"uploaded\"}";
    }
    private String release(String assets) {return "{\"tag_name\":\"v1.0.2\",\"draft\":false,\"prerelease\":false,\"assets\":["+assets+"]}";}
    private String url(String name) {return "https://github.com/"+ReleaseInfo.REPOSITORY+"/releases/download/v1.0.2/"+name;}
    @Test public void comparesNumericVersionsIncludingMultiDigitComponents() {
        assertTrue(ReleaseInfo.compare("v1.0.10","1.0.2")>0);
        assertTrue(ReleaseInfo.compare("1.1.0","1.0.99")>0);
        assertEquals(0,ReleaseInfo.compare("v1.0","1.0.0"));
        assertTrue(ReleaseInfo.compare("1.0.0","1.0.1")<0);
    }
    @Test public void signedReleaseApkIsSelected() throws Exception {
        ReleaseInfo r=ReleaseInfo.parse(release(asset("Varjoaika-1.0.2.apk",url("Varjoaika-1.0.2.apk"))));
        assertEquals("1.0.2",r.version);assertEquals(url("Varjoaika-1.0.2.apk"),r.url);
    }
    @Test public void sourceOnlyReleaseHasNoInstallableUpdate() throws Exception {
        assertEquals("",ReleaseInfo.parse(release("")).url);
        assertEquals("",ReleaseInfo.parse(release(asset("source.zip",url("source.zip")))).url);
    }
    @Test public void debugUnsignedAndExternalApksAreExcluded() throws Exception {
        for(String name:new String[]{"Varjoaika-debug.apk","Varjoaika-test.apk","Varjoaika-unsigned.apk","Other.apk"})
            assertEquals("",ReleaseInfo.parse(release(asset(name,url(name)))).url);
        assertEquals("",ReleaseInfo.parse(release(asset("Varjoaika-1.0.2.apk","https://example.com/Varjoaika.apk"))).url);
    }
    @Test public void rejectsLookalikeHostsOtherRepositoriesAndUrlCredentials() {
        String path="/"+ReleaseInfo.REPOSITORY+"/releases/download/v1.0.2/Varjoaika-1.0.2.apk";
        for(String host:new String[]{"github.com.evil.test","evil.test","user@github.com","github.com:444"})
            assertFalse(ReleaseInfo.allowedUrl("https://"+host+path,"v1.0.2","Varjoaika-1.0.2.apk"));
        assertFalse(ReleaseInfo.allowedUrl("http://github.com"+path,"v1.0.2","Varjoaika-1.0.2.apk"));
        assertFalse(ReleaseInfo.allowedUrl(url("Varjoaika-1.0.2.apk")+"?redirect=evil","v1.0.2","Varjoaika-1.0.2.apk"));
    }
    @Test(expected=IllegalArgumentException.class) public void excludesPrereleases() throws Exception {
        ReleaseInfo.parse("{\"tag_name\":\"v2.0.0\",\"prerelease\":true,\"assets\":[]}");
    }
    @Test(expected=IllegalArgumentException.class) public void rejectsAmbiguousApks() throws Exception {
        ReleaseInfo.parse(release(asset("Varjoaika-one.apk",url("Varjoaika-one.apk"))+","+asset("Varjoaika-two.apk",url("Varjoaika-two.apk"))));
    }
    @Test(expected=IllegalArgumentException.class) public void rejectsUnrecognizedVersion() {ReleaseInfo.compare("1.0-beta","1.0.1");}
}
