package app.expressiveclock.installer;

import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.zip.ZipInputStream;

/** No cookies/WebView, no arbitrary API endpoints, no credentials on storage redirects, no logs. */
final class GitHub {
    static final class UserError extends IOException {UserError(String message){super(message);}}
    interface Progress { void update(long bytes,long total); }
    private final CredentialStore store;
    GitHub(CredentialStore s) { store=s; }
    static JSONObject oauth(String endpoint, JSONObject fields) throws Exception {
        if(!endpoint.equals("device/code") && !endpoint.equals("oauth/access_token"))throw new SecurityException();
        HttpURLConnection c=(HttpURLConnection)new URL("https://github.com/login/"+endpoint).openConnection();
        c.setInstanceFollowRedirects(false);c.setConnectTimeout(20000);c.setReadTimeout(20000);
        c.setRequestMethod("POST");c.setRequestProperty("Accept","application/json");c.setRequestProperty("Content-Type","application/json");
        c.setDoOutput(true);
        try {
            try(OutputStream out=c.getOutputStream()){out.write(fields.toString().getBytes(StandardCharsets.UTF_8));}
            if(c.getResponseCode()!=200)throw new UserError("GitHub authentication unavailable. Try again.");
            return new JSONObject(new String(read(c.getInputStream(),65536,null,-1),StandardCharsets.UTF_8));
        } finally {c.disconnect();}
    }
    String token() throws Exception {
        long revision=store.revision();JSONObject auth=store.load();
        if(auth==null)throw new UserError("Connect GitHub first.");
        if(auth.getLong("expires_at") < System.currentTimeMillis()/1000+60) {
            JSONObject next=oauth("oauth/access_token",new JSONObject().put("client_id",auth.getString("client_id"))
                .put("grant_type","refresh_token").put("refresh_token",auth.getString("refresh_token")));
            saveTokens(next,auth.getString("client_id"),revision);auth=store.load();
        }
        return auth.getString("access_token");
    }
    void saveTokens(JSONObject result,String client,long revision) throws Exception {
        // Require expiring device-flow credentials. Do not accept PATs or non-expiring tokens.
        if(!result.has("access_token") || !result.has("refresh_token") || result.optLong("expires_in",0)<=0)
            throw new UserError("Authorization failed or expired. Enable expiring user tokens on the GitHub App and reconnect.");
        result.put("client_id",client).put("expires_at",System.currentTimeMillis()/1000+result.getLong("expires_in"));
        store.save(result,revision);
    }
    JSONObject api(String path) throws Exception {
        return new JSONObject(new String(fetch(path,2*1024*1024,null),StandardCharsets.UTF_8));
    }
    byte[] fetch(String path,long max,Progress progress) throws Exception {
        ByteArrayOutputStream out=new ByteArrayOutputStream();transfer(path,max,progress,out);return out.toByteArray();
    }
    void download(String path,File file,Progress progress) throws Exception {
        try(OutputStream out=new FileOutputStream(file)){transfer(path,Policy.MAX_BYTES,progress,out);}
    }
    private void transfer(String path,long max,Progress progress,OutputStream out) throws Exception {
        if(!path.startsWith("repos/"+Policy.REPO+"/") && !path.equals("repos/"+Policy.REPO))throw new SecurityException();
        if(Thread.currentThread().isInterrupted())throw new InterruptedException();
        String bearer=token();URL url=new URL("https://api.github.com/"+path);
        for(int redirects=0;redirects<4;redirects++) {
            if(Thread.currentThread().isInterrupted())throw new InterruptedException();
            HttpURLConnection c=(HttpURLConnection)url.openConnection();
            c.setInstanceFollowRedirects(false);c.setConnectTimeout(20000);c.setReadTimeout(20000);
            c.setRequestProperty("Accept","application/vnd.github+json");c.setRequestProperty("User-Agent","Expressive-Island-Installer");
            if(url.getHost().equals("api.github.com"))c.setRequestProperty("Authorization","Bearer "+bearer);
            try {
                int status=c.getResponseCode();
                if(status>=300 && status<400) {
                    URL next=new URL(url,c.getHeaderField("Location"));
                    if(!next.getProtocol().equals("https") || next.getUserInfo()!=null || !Policy.storageHost(next.getHost()))
                        throw new SecurityException("Unexpected download redirect");
                    url=next;continue;
                }
                if(status==401) { store.clear();throw new UserError("GitHub authorization expired. Connect GitHub again."); }
                if(status==404 || status==410)throw new UserError("Build expired or unavailable. Check repository access or request a new build.");
                if(status!=200)throw new UserError("GitHub request failed (HTTP "+status+"). Try again later.");
                long total=c.getContentLengthLong();if(total>max)throw new UserError("Download exceeds size limit");
                try(InputStream in=c.getInputStream()){
                    byte[] buffer=new byte[32768];int n;long count=0,last=0;
                    while((n=in.read(buffer))!=-1){
                        if(Thread.currentThread().isInterrupted())throw new InterruptedException();
                        count+=n;if(count>max)throw new UserError("Download exceeds size limit");out.write(buffer,0,n);
                        if(progress!=null && System.nanoTime()-last>100_000_000){progress.update(count,total);last=System.nanoTime();}
                    }
                }
                return;
            } finally {c.disconnect();}
        }
        throw new UserError("Too many download redirects");
    }
    static byte[] read(InputStream stream,long max,Progress progress,long total) throws Exception {
        if(total>max)throw new UserError("Download exceeds size limit");
        try(InputStream in=stream;ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            byte[] buffer=new byte[32768];int n;long last=0;
            while((n=in.read(buffer))!=-1) {
                if(Thread.currentThread().isInterrupted())throw new InterruptedException();
                if((long)out.size()+n>max)throw new UserError("Download exceeds size limit");
                out.write(buffer,0,n);
                if(progress!=null && System.nanoTime()-last>100_000_000){progress.update(out.size(),total);last=System.nanoTime();}
            }
            return out.toByteArray();
        }
    }
    JSONObject evidence(Policy.Request req) throws Exception {
        JSONObject repo=api("repos/"+Policy.REPO);
        if(!Policy.REPO.equals(repo.getString("full_name")))throw new SecurityException("Unexpected repository");
        JSONObject run=api("repos/"+Policy.REPO+"/actions/runs/"+req.run());
        if(!Policy.REPO.equals(run.getJSONObject("head_repository").getString("full_name")))
            throw new SecurityException("Unexpected source repository");
        Policy.checkRun(run.getString("event"),run.getString("status"),run.getString("conclusion"),run.getString("head_branch"),repo.getString("default_branch"),
            run.getString("path"),run.getString("display_title"),req.build(),run.getJSONObject("repository").getString("full_name"));
        JSONArray artifacts=api("repos/"+Policy.REPO+"/actions/runs/"+req.run()+"/artifacts?per_page=100").getJSONArray("artifacts");
        boolean apk=false,manifest=false;
        for(int i=0;i<artifacts.length();i++) {
            JSONObject a=artifacts.getJSONObject(i);
            if(a.getBoolean("expired"))continue;
            if(a.getLong("id")==req.artifact() && a.getString("name").equals("ExpressiveClock-debug.apk"))apk=true;
            if(a.getLong("id")==req.manifest() && a.getString("name").equals("build-manifest.json"))manifest=true;
        }
        if(!apk || !manifest)throw new SecurityException("Artifacts do not belong to the requested build");
        byte[] data=Transport.unwrap(fetch("repos/"+Policy.REPO+"/actions/artifacts/"+req.manifest()+"/zip",131072,null),"build-manifest.json",65536);
        JSONObject m=new JSONObject(new String(data,StandardCharsets.UTF_8));
        long age=Instant.now().getEpochSecond()-Instant.parse(m.getString("built_at")).getEpochSecond();
        Policy.checkManifest(req,m.getInt("schema"),m.getString("build_id"),m.getLong("run_id"),m.getLong("artifact_id"),m.getString("sha256"),
            m.getString("package_name"),m.getString("certificate_sha256"),age);
        if(m.getLong("version_code") <= 0 || m.getString("version_name").isBlank()
            || m.getLong("apk_bytes") <= 0 || m.getLong("apk_bytes") > Policy.MAX_BYTES)
            throw new SecurityException("Invalid build metadata");
        return m;
    }
}
