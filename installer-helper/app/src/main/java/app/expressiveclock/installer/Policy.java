package app.expressiveclock.installer;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

/** Pure policy: all notification input is untrusted. No arbitrary repository, URL or package. */
public final class Policy {
    public static final String REPO = "samuelbeckman/expressive-clock";
    public static final String PACKAGE = BuildConfig.EXPECTED_PACKAGE;
    // Public fingerprint from the existing verified Mac debug APK. Never trust a link-supplied pin.
    public static final String CERT = BuildConfig.EXPECTED_CERT;
    public static final long MAX_BYTES = 100L * 1024 * 1024;
    public record Request(long run, long artifact, long manifest, String build, String sha) {}
    public static Request parse(String text) {
        URI uri = URI.create(text);
        if (!"expressive-clock-install".equals(uri.getScheme()) || !"artifact".equals(uri.getHost())
                || uri.getUserInfo()!=null || uri.getPort()!=-1 || uri.getFragment()!=null
                || !(uri.getPath()==null || uri.getPath().isEmpty())) throw new IllegalArgumentException("Invalid install link");
        Map<String,String> values = new HashMap<>();
        if(uri.getRawQuery()==null) throw new IllegalArgumentException("Missing build identity");
        for(String pair:uri.getRawQuery().split("&")) {
            String[] parts=pair.split("=",2);
            if(parts.length!=2 || values.put(parts[0],decode(parts[1]))!=null)
                throw new IllegalArgumentException("Duplicate or missing link fields");
        }
        if(values.size()!=5 || !hex(values.get("sha"),64) || !hex(values.get("build"),32))
            throw new IllegalArgumentException("Invalid build identity");
        return new Request(id(values.get("run")),id(values.get("artifact")),id(values.get("manifest")),values.get("build"),values.get("sha"));
    }
    private static String decode(String value) {
        try {return URLDecoder.decode(value,"UTF-8");} catch(java.io.UnsupportedEncodingException e){throw new IllegalStateException(e);}
    }
    public static void checkRun(String event,String status,String conclusion,String branch,String defaultBranch,String path,String title,String build,String repo) {
        if(!"workflow_dispatch".equals(event) || !"completed".equals(status) || !"success".equals(conclusion)
            || !branch.equals(defaultBranch) || !".github/workflows/local-debug-apk.yml".equals(path)
            || !("Local APK "+build).equals(title) || !REPO.equals(repo))throw new SecurityException("Untrusted workflow run");
    }
    public static void checkManifest(Request r,int schema,String build,long run,long artifact,String sha,String name,String cert,long age) {
        if(schema!=1 || !r.build().equals(build) || r.run()!=run || r.artifact()!=artifact || !r.sha().equals(sha)
            || !PACKAGE.equals(name) || !CERT.equals(cert) || age < -60 || age>7*86400)throw new SecurityException("Untrusted build manifest");
    }
    public static void checkUpdate(long candidate, long installed, boolean sameApk, long builtMillis, long installedMillis) {
        if (candidate < installed) throw new SecurityException("Older version: downgrades are not allowed");
        if (candidate == installed && !sameApk && builtMillis < installedMillis)
            throw new SecurityException("An older build cannot replace the installed build");
    }
    private static long id(String value) {
        if(value==null || !value.matches("[1-9][0-9]{0,18}")) throw new IllegalArgumentException("Invalid artifact ID");
        return Long.parseLong(value);
    }
    public static boolean hex(String s,int length) { return s!=null && s.matches("[a-f0-9]{"+length+"}"); }
    public static String digest(byte[] data) {
        try { byte[] b=MessageDigest.getInstance("SHA-256").digest(data); StringBuilder s=new StringBuilder();
            for(byte v:b)s.append(String.format(java.util.Locale.ROOT,"%02x",v & 255)); return s.toString();
        } catch(Exception e) { throw new IllegalStateException(e); }
    }
    public static String digest(java.io.File file) throws Exception {
        MessageDigest digest=MessageDigest.getInstance("SHA-256");
        try(var in=new java.io.FileInputStream(file)){byte[] buffer=new byte[32768];int n;
            while((n=in.read(buffer))!=-1){if(Thread.currentThread().isInterrupted())throw new InterruptedException();digest.update(buffer,0,n);}}
        StringBuilder result=new StringBuilder();for(byte b:digest.digest())result.append(String.format(java.util.Locale.ROOT,"%02x",b & 255));return result.toString();
    }
    public static void checkPackage(String name,String[] certificates) {
        if(!PACKAGE.equals(name) || certificates.length!=1 || !CERT.equals(certificates[0]))
            throw new SecurityException("APK package or signing certificate is not trusted");
    }
    public static boolean storageHost(String host) {
        return "productionresultssa0.blob.core.windows.net".equals(host)
            || host.endsWith(".blob.core.windows.net") || host.endsWith(".githubusercontent.com");
    }
}
