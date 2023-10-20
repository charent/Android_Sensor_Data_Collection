package com.myapp.sensordatacollection.ui.recognition.scene;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class SceneSelectAdapter extends FragmentStateAdapter {
    private static final String TAG = "SceneSelectAdapter";

    public SceneSelectAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
//        Log.d(TAG,String.valueOf(position));
        Fragment fragment = null;
        Bundle bundle = null;
       switch (position){
           case 0:
               fragment = new CardApplicationFragment();
               bundle = new Bundle();
               bundle.putInt(CardApplicationFragment.TAG, position + 1);
               fragment.setArguments(bundle);
               break;
           case 1:
               fragment = new TransferFragment();
               bundle = new Bundle();
               bundle.putInt(TransferFragment.TAG, position + 1);
               fragment.setArguments(bundle);
               break;
           default:
               break;
       }

       return  fragment;
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
