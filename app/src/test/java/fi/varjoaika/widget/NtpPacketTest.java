package fi.varjoaika.widget;
import org.junit.Test;
import static org.junit.Assert.*;

public class NtpPacketTest {
    private byte[] reply(byte[] request,long received,long sent) {
        byte[] p=new byte[48];p[0]=0x24;p[1]=2;
        System.arraycopy(request,40,p,24,8);
        System.arraycopy(NtpPacket.request(received),40,p,32,8);
        System.arraycopy(NtpPacket.request(sent),40,p,40,8);return p;
    }
    @Test public void computesOffsetWithRoundTripDelay() {
        long start=1791187200000L;byte[] request=NtpPacket.request(start);
        assertEquals(0.125,NtpPacket.offset(request,reply(request,start+150,start+200),48,start,start+100),0.000001);
    }
    @Test public void worksAcrossNtpEraRolloverIn2036() {
        long start=2085978495900L;byte[] request=NtpPacket.request(start);
        assertEquals(0.0,NtpPacket.offset(request,reply(request,start+50,start+150),48,start,start+200),0.000001);
    }
    @Test public void rejectsUnrelatedOrUnsynchronizedReplies() {
        long start=1791187200000L;byte[] request=NtpPacket.request(start),p=reply(request,start+50,start+60);
        p[24]^=1;assertThrows(IllegalArgumentException.class,()->NtpPacket.offset(request,p,48,start,start+100));
        p[24]^=1;p[0]=(byte)0xe4;assertThrows(IllegalArgumentException.class,()->NtpPacket.offset(request,p,48,start,start+100));
        p[0]=0x24;p[1]=0;assertThrows(IllegalArgumentException.class,()->NtpPacket.offset(request,p,48,start,start+100));
    }
    @Test public void rejectsTruncatedRepliesAndLocalClockChanges() {
        long start=1791187200000L;byte[] request=NtpPacket.request(start),p=reply(request,start+50,start+60);
        assertThrows(IllegalArgumentException.class,()->NtpPacket.offset(request,p,47,start,start+100));
        assertThrows(IllegalArgumentException.class,()->NtpPacket.offset(request,p,48,start,start-1));
        assertThrows(IllegalArgumentException.class,()->NtpPacket.offset(request,p,48,start,start+7000));
    }
}
