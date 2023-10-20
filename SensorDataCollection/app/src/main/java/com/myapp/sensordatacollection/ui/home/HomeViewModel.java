package com.myapp.sensordatacollection.ui.home;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.Properties;

public class HomeViewModel extends ViewModel {

    public MutableLiveData<Properties> liveProperties;

    public HomeViewModel() {

        liveProperties = new MutableLiveData<>();
    }

    public void  setLiveProperties(Properties properties){

        liveProperties.setValue(properties);
    }

    public Properties getProperties(){

        return  liveProperties.getValue();
    }


}