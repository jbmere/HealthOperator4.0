package com.upm.jgp.healthywear.ui.main.DataModule;

import java.io.Serializable;

/**
 * This Class contains a serializable favourite object.
 *
 * It is a custom object type to store and compare the favourite devices
 * Currently only two values are used for device types.
 * 1 is related with SmartBand devices
 * 2 is related with MMR devices
 * 3 is related with MMR2 devices
 * New values should be added for future devices
 *
 * @author Jorge Garcia Paredes (yoryidan)
 * @version 210
 * @since 2020
 */
public class FavouriteObject implements Serializable {
    private static final long serialVersionUID = 1L;

    private int mWearableType;      //value 1 for smartBand
                                    //value 2 for mmr
                                    //value 3 for mmr
                                    //TODO value 4 for others
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
            case 3:
                device += "MMR2";
                break;
            //TODO case 4... for other devices' type
            default:
                device += "";
                break;
        }
        return device;
    }
}
