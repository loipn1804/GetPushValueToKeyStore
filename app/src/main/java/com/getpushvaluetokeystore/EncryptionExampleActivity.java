package com.getpushvaluetokeystore;

/**
 * Created by stacck on 11/28/16.
 */

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import io.realm.Realm;

public class EncryptionExampleActivity extends Activity {

    public static final String TAG = "SimpleKeystoreApp";
    private TextView tvKey;


    private Realm realm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Open the Realm with encryption enabled
        realm = Realm.getInstance(MyApplication.realmConfiguration);

        // Everything continues to work as normal except for that the file is encrypted on disk
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                Person person = realm.createObject(Person.class);
                person.setName("Happy Person");
                person.setAge(14);
            }
        });

        Person person = realm.where(Person.class).findFirst();
        Log.i(TAG, String.format("Person name: %s", person.getName()));

        tvKey = (TextView) findViewById(R.id.tv_key);
        tvKey.setText(person.getName());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        realm.close(); // Remember to close Realm when done.
    }
}
