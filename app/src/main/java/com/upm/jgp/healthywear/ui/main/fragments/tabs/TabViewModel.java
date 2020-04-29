package com.upm.jgp.healthywear.ui.main.fragments.tabs;

import androidx.arch.core.util.Function;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

public class TabViewModel extends ViewModel {

    private MutableLiveData<Integer> mIndex = new MutableLiveData<>();

    //TODO implement the content for the different tabs
    private LiveData<String> mText = Transformations.map(mIndex, new Function<Integer, String>() {
        @Override
        public String apply(Integer input) {
            String aux = null;
            switch (input){
                case 1:
                    aux = "There is no wearable connected  for this tab (" + input + ")\n" + "Press + to add SmartBand.";
                    break;
                case 2:
                    aux = "There is no wearable connected  for this tab (" + input + ")\n" + "Press + to add MMR device.";
                    break;
                case 3:
                    aux = "There is no wearable connected  for this tab (" + input + ")\n" + "Press + to add other device.";
                    break;

                default:
                    aux = "There is no wearable connected  for this tab (" + input + ")\n";
                    break;
            }
            return aux;
        }
    });

    public void setIndex(int index) {
        mIndex.setValue(index);
    }
    public MutableLiveData<Integer> getIndex(){return mIndex;};

    public LiveData<String> getText() {
        return mText;
    }
}