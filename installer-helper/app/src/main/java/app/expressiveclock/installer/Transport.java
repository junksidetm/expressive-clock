package app.expressiveclock.installer;
import java.io.*;
import java.util.zip.ZipInputStream;
public final class Transport {
    public static byte[] unwrap(byte[] blob,String filename,long max) throws Exception {
        // Raw APK is also ZIP; only unwrap when the sole entry is the exact transport filename.
        if(blob.length>=2 && blob[0]=='P' && blob[1]=='K') {
            try(ZipInputStream zip=new ZipInputStream(new ByteArrayInputStream(blob))) {
                var entry=zip.getNextEntry();
                if(entry!=null && entry.getName().equals(filename)) {
                    byte[] data=readEntry(zip,max);
                    if(zip.getNextEntry()!=null)throw new IOException("Unexpected archive contents");
                    return data;
                }
            }
        }
        if(blob.length>max)throw new IOException("Download exceeds size limit");
        return blob;
    }
    private static byte[] readEntry(InputStream in,long max) throws Exception {
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[32768];int n;
        while((n=in.read(buffer))!=-1){if(Thread.currentThread().isInterrupted())throw new InterruptedException();if((long)out.size()+n>max)throw new IOException("Archive exceeds size limit");out.write(buffer,0,n);}
        return out.toByteArray();
    }
    public static void unpack(java.io.File source,java.io.File target,String filename,long max) throws Exception {
        try(java.util.zip.ZipFile zip=new java.util.zip.ZipFile(source)) {
            if(zip.getEntry("AndroidManifest.xml")!=null) {
                if(source.length()>max)throw new IOException("APK exceeds size limit");
                java.nio.file.Files.copy(source.toPath(),target.toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);return;
            }
            var entry=zip.getEntry(filename);
            if(zip.size()!=1 || entry==null || entry.isDirectory() || entry.getSize()>max)throw new IOException("Unexpected artifact contents");
            try(InputStream in=zip.getInputStream(entry);OutputStream out=new FileOutputStream(target)) {
                byte[] buffer=new byte[32768];int n;long count=0;
                while((n=in.read(buffer))!=-1){if(Thread.currentThread().isInterrupted())throw new InterruptedException();if(Thread.currentThread().isInterrupted())throw new InterruptedException();count+=n;if(count>max)throw new IOException("Expanded APK exceeds size limit");out.write(buffer,0,n);}
            }
        }
    }
}
