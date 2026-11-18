package app.expressiveclock.installer;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import org.json.JSONObject;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Tokens are encrypted in internal preferences. The AES key never leaves Android Keystore. */
final class CredentialStore {
    private static final String ALIAS="clock-installer-auth-v1";
    private final Context context;
    private long revision;
    synchronized long revision(){return revision;}
    CredentialStore(Context c) { context=c.getApplicationContext(); }
    private SecretKey key() throws Exception {
        KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);
        if(!ks.containsAlias(ALIAS)) {
            KeyGenerator gen=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
            gen.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
            gen.generateKey();
        }
        return (SecretKey)ks.getKey(ALIAS,null);
    }
    synchronized void save(JSONObject value,long expectedRevision) throws Exception {
        if(expectedRevision!=revision)throw new SecurityException("Authorization was disconnected");
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key());
        String iv=Base64.encodeToString(cipher.getIV(),Base64.NO_WRAP);
        String blob=Base64.encodeToString(cipher.doFinal(value.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)),Base64.NO_WRAP);
        if(!context.getSharedPreferences("auth",0).edit().putString("iv",iv).putString("blob",blob).commit())throw new java.io.IOException("Secure credential storage failed");
    }
    boolean hasCredentials(){return context.getSharedPreferences("auth",0).contains("blob");}
    synchronized JSONObject load() throws Exception {
        var p=context.getSharedPreferences("auth",0);String blob=p.getString("blob",null);
        if(blob==null)return null;
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.decode(p.getString("iv",""),Base64.NO_WRAP)));
        return new JSONObject(new String(cipher.doFinal(Base64.decode(blob,Base64.NO_WRAP)),java.nio.charset.StandardCharsets.UTF_8));
    }
    // Synchronous revocation; a disconnect must not be lost across immediate process death.
    @android.annotation.SuppressLint("ApplySharedPref")
    synchronized void clear() { revision++;context.getSharedPreferences("auth",0).edit().clear().commit(); }
}
