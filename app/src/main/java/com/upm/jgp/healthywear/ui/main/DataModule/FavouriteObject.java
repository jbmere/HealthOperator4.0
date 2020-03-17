package com.upm.jgp.healthywear.ui.main.DataModule;

import java.io.Serializable;

public class FavouriteObject implements Serializable {
    private static final long serialVersionUID = 1L;

    private int mWearableType;      //value 1 for smartBand
                                    //value 2 for mmr
                                    //TODO value 3 for others
    private String mWearableName;
    private String mWearableMac;

    public FavouriteObject(String mac, int type){
        this.mWearableMac = mac;
        this.mWearableName = "no given name";
        this.mWearableType = type;
    }

    public FavouriteObject(String mac, String name, int type){
            this.mWearableMac = mac;
            this.mWearableName = name;
            this.mWearableType = type;
    }

    //Getters and Setters
    public int getmWearableType() {
        return mWearableType;
    }

    public void setmWearableType(int mWearableType) {
        this.mWearableType = mWearableType;
    }

    public String getmWearableName() {
        return mWearableName;
    }

    public void setmWearableName(String mWearableName) {
        this.mWearableName = mWearableName;
    }

    public String getmWearableMac() {
        return mWearableMac;
    }

    public void setmWearableMac(String mWearableMac) {
        this.mWearableMac = mWearableMac;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this){
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass())
        {
            return false;
        }
        FavouriteObject b = (FavouriteObject) obj;
        return this.mWearableMac == b.mWearableMac;
    }

    @Override
    public int  hashCode(){
        return mWearableMac.hashCode();
    }
    public String toString(){
        String device = "MAC: " + mWearableMac + "     Type: ";
        switch (mWearableType) {
            case 1:
                device += "SmartBand";
                break;
            case 2:
                device += "MMR";
                break;
            default:
                device += "";
                break;
        }
        return device;
    }
}
