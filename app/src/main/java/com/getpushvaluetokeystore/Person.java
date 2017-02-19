package com.getpushvaluetokeystore;

/**
 * Created by stacck on 11/28/16.
 */
import io.realm.RealmObject;

public class Person extends RealmObject {
    private String name;
    private int age;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }
}
