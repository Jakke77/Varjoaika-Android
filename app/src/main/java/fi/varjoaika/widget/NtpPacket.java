package fi.varjoaika.widget;
import java.util.Arrays;

/** RFC 5905 client timestamps and validated public NTP replies. */
final class NtpPacket {
    static final double EPOCH=2208988800.0, ERA=4294967296.0;
    static byte[] request(long now) {
        byte[] p=new byte[48];p[0]=0x23;double ntp=now/1000.0+EPOCH;
        put(p,40,(long)ntp);put(p,44,(long)((ntp-Math.floor(ntp))*ERA));return p;
    }
    private static void put(byte[] p,int at,long value) { for(int i=3;i>=0;i--){p[at+i]=(byte)value;value>>>=8;} }
    private static long word(byte[] p,int at) { long n=0;for(int i=0;i<4;i++)n=(n<<8)|(p[at+i]&255);return n; }
    private static double timestamp(byte[] p,int at,double near) {
        double t=word(p,at)+word(p,at+4)/ERA-EPOCH;
        return t+Math.round((near-t)/ERA)*ERA;
    }
    static double offset(byte[] request,byte[] reply,int length,long started,long ended) {
        if(length<48||length>reply.length||request.length<48||reply.length<48||((reply[0]&255)>>>6)==3||((reply[0]>>>3)&7)<3||(reply[0]&7)!=4||(reply[1]&255)<1||(reply[1]&255)>15||
            !Arrays.equals(Arrays.copyOfRange(request,40,48),Arrays.copyOfRange(reply,24,32)))throw new IllegalArgumentException("Aikapalvelimen vastaus ei kelpaa");
        if((word(reply,32)==0&&word(reply,36)==0)||(word(reply,40)==0&&word(reply,44)==0))throw new IllegalArgumentException("Aikaleima puuttuu");
        double t1=started/1000.0,t4=ended/1000.0,t2=timestamp(reply,32,t1),t3=timestamp(reply,40,t1);
        if(ended<started||ended-started>6000||t3<t2||t3-t2>6)throw new IllegalArgumentException("Aikaleima ei kelpaa");
        return ((t2-t1)+(t3-t4))/2;
    }
    private NtpPacket() {}
}
