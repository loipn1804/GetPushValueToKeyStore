package com.getpushvaluetokeystore;

/**
 * Created by stacck on 11/28/16.
 */

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.security.KeyPairGeneratorSpec;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.security.auth.x500.X500Principal;

import io.realm.Realm;
import io.realm.RealmConfiguration;

public class MyApplication extends Application {
    public static RealmConfiguration realmConfiguration;
    private static final boolean IS_M = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;

    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";
    private static final String RSA_MODE =  "RSA/ECB/PKCS1Padding";
    private static final String SHARED_PREFENCE_NAME = "SHARED_PREFENCE_NAME";
    private static final String ENCRYPTED_KEY = "ENCRYPTED_KEY";

    private KeyStore keyStore;

    @Override
    public void onCreate() {
        super.onCreate();
        Realm.init(this);

        initKeyStore();
    }
    @SuppressWarnings("NewApi")
    private void initKeyStore() {
        byte[] encryptedKey = new byte[64];
        try {
            keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);

            Log.i("johan", "Not containsAlias: " + !keyStore.containsAlias(getString(R.string.app_name)));
            if (!keyStore.containsAlias(getString(R.string.app_name))) {
                // Create new key and save to KeyStore
                KeyPairGenerator kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEY_STORE);
                if (IS_M) {
                    KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(getString(R.string.app_name),
                            KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
                            .setRandomizedEncryptionRequired(false)
                            .build();

                    kpg.initialize(spec);
                } else {
                    // Generate a key pair for encryption
                    Calendar start = Calendar.getInstance();
                    Calendar end = Calendar.getInstance();
                    end.add(Calendar.YEAR, 30);
                    KeyPairGeneratorSpec spec = new KeyPairGeneratorSpec.Builder(getApplicationContext())
                            .setAlias(getString(R.string.app_name))
                            .setSubject(new X500Principal("CN=" + getString(R.string.app_name)))
                            .setSerialNumber(BigInteger.TEN)
                            .setStartDate(start.getTime())
                            .setEndDate(end.getTime())
                            .build();

                    kpg.initialize(spec);
                }
                kpg.generateKeyPair();
                encryptedKey = setSecretKey();
            } else {
                // Get key from KeyStore
                encryptedKey = getSecretKey();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.i("johan", "encryptedKey: " + encryptedKey.length + " -- " + Arrays.toString(encryptedKey));

        initRealm(encryptedKey);
    }

    private void initRealm(byte[] key) {
        realmConfiguration = new RealmConfiguration.Builder()
                .encryptionKey(key)
                .build();

        // Start with a clean slate every time
        Realm.deleteRealm(realmConfiguration);
    }

    private byte[] getSecretKey() {
        SharedPreferences pref = getApplicationContext().getSharedPreferences(SHARED_PREFENCE_NAME, Context.MODE_PRIVATE);
        String enryptedKeyB64 = pref.getString(ENCRYPTED_KEY, null);
        byte[] key = new byte[64];
        try {
            byte[] encryptedKey = Base64.decode(enryptedKeyB64, Base64.DEFAULT);
            key = rsaDecrypt(encryptedKey);

        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.i("johan", "getSecretKey string: " + enryptedKeyB64);

        Log.i("johan", "getSecretKey key: " + Arrays.toString(key));
        return key;
    }

    private byte[] setSecretKey() {
        byte[] key = new byte[64];
        try {
            SharedPreferences pref = getApplicationContext().getSharedPreferences(SHARED_PREFENCE_NAME, Context.MODE_PRIVATE);
            String enryptedKeyB64 = pref.getString(ENCRYPTED_KEY, null);
            if (enryptedKeyB64 == null) {
                SecureRandom secureRandom = new SecureRandom();
                secureRandom.nextBytes(key);
                byte[] encryptedKey = rsaEncrypt(key);
                enryptedKeyB64 = Base64.encodeToString(encryptedKey, Base64.DEFAULT);
                SharedPreferences.Editor edit = pref.edit();
                edit.putString(ENCRYPTED_KEY, enryptedKeyB64);
                edit.commit();
                Log.i("johan", "setSecretKey string: " + enryptedKeyB64);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.i("johan", "setSecretKey key: " + Arrays.toString(key));
        return key;
    }

    private byte[] rsaEncrypt(byte[] secret) throws Exception {
        KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(getString(R.string.app_name), null);
        Cipher inputCipher = Cipher.getInstance(RSA_MODE);
        inputCipher.init(Cipher.ENCRYPT_MODE, privateKeyEntry.getCertificate().getPublicKey());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CipherOutputStream cipherOutputStream = new CipherOutputStream(outputStream, inputCipher);
        cipherOutputStream.write(secret);
        cipherOutputStream.close();

        byte[] vals = outputStream.toByteArray();
        return vals;
    }

    private byte[] rsaDecrypt(byte[] encrypted) throws Exception {
        KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(getString(R.string.app_name), null);
        Cipher output = Cipher.getInstance(RSA_MODE);
        output.init(Cipher.DECRYPT_MODE, privateKeyEntry.getPrivateKey());
        CipherInputStream cipherInputStream = new CipherInputStream(
                new ByteArrayInputStream(encrypted), output);
        ArrayList<Byte> values = new ArrayList<>();
        int nextByte;
        while ((nextByte = cipherInputStream.read()) != -1) {
            values.add((byte)nextByte);
        }

        byte[] bytes = new byte[values.size()];
        for(int i = 0; i < bytes.length; i++) {
            bytes[i] = values.get(i).byteValue();
        }
        return bytes;
    }
}
