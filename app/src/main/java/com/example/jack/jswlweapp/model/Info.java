package com.example.jack.jswlweapp.model;

import xiaofei.library.datastorage.annotation.ClassId;
import xiaofei.library.datastorage.annotation.ObjectId;

/**
 * Created by jack on 18-1-18.
 */
@ClassId("Info")
public class Info {
    @ObjectId
    private String mId;
    private String moblie;

    public String getmId() {
        return mId;
    }

    public void setmId(String mId) {
        this.mId = mId;
    }

    public String getMoblie() {
        return moblie;
    }

    public void setMoblie(String moblie) {
        this.moblie = moblie;
    }

    @Override
    public String toString() {
        return "Info{" +
                "mId='" + mId + '\'' +
                ", moblie='" + moblie + '\'' +
                '}';
    }
}
