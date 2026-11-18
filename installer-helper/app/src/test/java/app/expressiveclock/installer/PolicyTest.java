package app.expressiveclock.installer;
import org.junit.Test;
import static org.junit.Assert.*;
import java.io.*;
import java.util.zip.*;

public class PolicyTest {
    private String link="expressive-clock-install://artifact?run=5&artifact=9&manifest=10&build="+"a".repeat(32)+"&sha="+"b".repeat(64);
    private void bad(String value){assertThrows(IllegalArgumentException.class,()->Policy.parse(value));}
    @Test public void exactBuildLink(){var r=Policy.parse(link);assertEquals(5,r.run());assertEquals(9,r.artifact());assertEquals(10,r.manifest());}
    @Test public void rejectWebLink(){bad(link.replace("expressive-clock-install","https"));}
    @Test public void rejectOtherHost(){bad(link.replace("artifact?","evil?"));}
    @Test public void rejectCredentials(){bad(link.replace("://","://token@"));}
    @Test public void rejectDuplicateField(){bad(link+"&run=6");}
    @Test public void rejectMissingEvidence(){bad(link.replace("manifest=10&",""));}
    @Test public void rejectExtraField(){bad(link+"&url=https://evil");}
    @Test public void rejectInvalidDigest(){bad(link.replace("b".repeat(64),"xyz"));}
    @Test public void rejectOverflowId(){bad(link.replace("run=5","run=99999999999999999999"));}
    @Test public void pinnedPackageAndCertificate(){Policy.checkPackage(Policy.PACKAGE,new String[]{Policy.CERT});}
    @Test public void rejectProductionPackage(){assertThrows(SecurityException.class,()->Policy.checkPackage("app.expressiveclock",new String[]{Policy.CERT}));}
    @Test public void rejectOtherSigner(){assertThrows(SecurityException.class,()->Policy.checkPackage(Policy.PACKAGE,new String[]{"0".repeat(64)}));}
    @Test public void rejectMultipleSigners(){assertThrows(SecurityException.class,()->Policy.checkPackage(Policy.PACKAGE,new String[]{Policy.CERT,Policy.CERT}));}
    @Test public void digestKnownVector(){assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",Policy.digest(new byte[0]));}
    @Test public void trustedWorkflow(){Policy.checkRun("workflow_dispatch","completed","success","main","main",".github/workflows/local-debug-apk.yml","Local APK "+"a".repeat(32),"a".repeat(32),Policy.REPO);}
    @Test public void rejectsPullRequest(){assertThrows(SecurityException.class,()->Policy.checkRun("pull_request","completed","success","main","main",".github/workflows/local-debug-apk.yml","Local APK "+"a".repeat(32),"a".repeat(32),Policy.REPO));}
    @Test public void rejectWrongWorkflow(){assertThrows(SecurityException.class,()->Policy.checkRun("workflow_dispatch","completed","success","main","main",".github/workflows/evil.yml","Local APK "+"a".repeat(32),"a".repeat(32),Policy.REPO));}
    @Test public void trustedManifest(){var r=Policy.parse(link);Policy.checkManifest(r,1,r.build(),r.run(),r.artifact(),r.sha(),Policy.PACKAGE,Policy.CERT,0);}
    @Test public void rejectStaleManifest(){var r=Policy.parse(link);assertThrows(SecurityException.class,()->Policy.checkManifest(r,1,r.build(),r.run(),r.artifact(),r.sha(),Policy.PACKAGE,Policy.CERT,604801));}
    @Test public void rejectFutureManifest(){var r=Policy.parse(link);assertThrows(SecurityException.class,()->Policy.checkManifest(r,1,r.build(),r.run(),r.artifact(),r.sha(),Policy.PACKAGE,Policy.CERT,-61));}
    @Test public void rejectReplacedArtifact(){var r=Policy.parse(link);assertThrows(SecurityException.class,()->Policy.checkManifest(r,1,r.build(),r.run(),999,r.sha(),Policy.PACKAGE,Policy.CERT,0));}
    @Test public void rejectWrongManifestHash(){var r=Policy.parse(link);assertThrows(SecurityException.class,()->Policy.checkManifest(r,1,r.build(),r.run(),r.artifact(),"0".repeat(64),Policy.PACKAGE,Policy.CERT,0));}
    @Test public void storageRedirectHost(){assertTrue(Policy.storageHost("x.blob.core.windows.net"));assertFalse(Policy.storageHost("blob.core.windows.net.evil.com"));}
    private byte[] zip(String...names)throws Exception{var bytes=new ByteArrayOutputStream();try(var z=new ZipOutputStream(bytes)){for(String name:names){z.putNextEntry(new ZipEntry(name));z.write(new byte[1024]);z.closeEntry();}}return bytes.toByteArray();}
    @Test public void rawApkIsNotUnwrapped()throws Exception{byte[] b=zip("AndroidManifest.xml","classes.dex");assertArrayEquals(b,Transport.unwrap(b,"ExpressiveClock-debug.apk",4096));}
    @Test public void unwrapSingleTransportFile()throws Exception{assertEquals(1024,Transport.unwrap(zip("ExpressiveClock-debug.apk"),"ExpressiveClock-debug.apk",4096).length);}
    @Test public void rejectTransportExtraFiles(){assertThrows(IOException.class,()->Transport.unwrap(zip("ExpressiveClock-debug.apk","extra"),"ExpressiveClock-debug.apk",4096));}
    @Test public void rejectOversizedExpansion(){assertThrows(IOException.class,()->Transport.unwrap(zip("ExpressiveClock-debug.apk"),"ExpressiveClock-debug.apk",100));}
    @Test public void fileHashMatchesByteHash()throws Exception{var file=java.nio.file.Files.createTempFile("hash-test",".apk");try{java.nio.file.Files.write(file,new byte[4096]);assertEquals(Policy.digest(new byte[4096]),Policy.digest(file.toFile()));}finally{java.nio.file.Files.delete(file);}}
    @Test public void diskRawApkRoundTrip()throws Exception{diskRoundTrip(zip("AndroidManifest.xml","classes.dex"),false);}
    @Test public void diskWrappedApkRoundTrip()throws Exception{diskRoundTrip(zip("ExpressiveClock-debug.apk"),true);}
    private void diskRoundTrip(byte[] bytes,boolean wrapped)throws Exception{
        var folder=java.nio.file.Files.createTempDirectory("artifact-test");var source=folder.resolve("source");var target=folder.resolve("target");
        try{java.nio.file.Files.write(source,bytes);Transport.unpack(source.toFile(),target.toFile(),"ExpressiveClock-debug.apk",4096);
            assertEquals(wrapped?1024:bytes.length,java.nio.file.Files.size(target));}
        finally{java.nio.file.Files.deleteIfExists(target);java.nio.file.Files.deleteIfExists(source);java.nio.file.Files.delete(folder);}
    }
    @Test public void diskRejectsExtraFiles()throws Exception{
        var folder=java.nio.file.Files.createTempDirectory("artifact-test");var source=folder.resolve("source");var target=folder.resolve("target");
        try{java.nio.file.Files.write(source,zip("ExpressiveClock-debug.apk","extra"));assertThrows(IOException.class,()->Transport.unpack(source.toFile(),target.toFile(),"ExpressiveClock-debug.apk",4096));}
        finally{java.nio.file.Files.deleteIfExists(target);java.nio.file.Files.deleteIfExists(source);java.nio.file.Files.delete(folder);}
    }
    @Test public void rejectWrongBranch(){assertThrows(SecurityException.class,()->Policy.checkRun("workflow_dispatch","completed","success","feature","main",".github/workflows/local-debug-apk.yml","Local APK "+"a".repeat(32),"a".repeat(32),Policy.REPO));}

    @Test public void rejectsIslandScheme(){bad(link.replace("expressive-clock-install","expressive-island-install"));}
    @Test public void rejectsIslandPackage(){assertThrows(SecurityException.class,()->Policy.checkPackage("app.expressiveisland.debug",new String[]{Policy.CERT}));}
    @Test public void rejectsOlderVersion(){assertThrows(SecurityException.class,()->Policy.checkUpdate(6,7,false,2000,1000));}
    @Test public void rejectsOlderSameVersionBuild(){assertThrows(SecurityException.class,()->Policy.checkUpdate(7,7,false,999,1000));}
    @Test public void allowsExactInstalledApkRetry(){Policy.checkUpdate(7,7,true,999,1000);}
    @Test public void allowsNewSameVersionBuild(){Policy.checkUpdate(7,7,false,2000,1000);}
    @Test public void allowsNewerVersion(){Policy.checkUpdate(8,7,false,2000,1000);}
}
