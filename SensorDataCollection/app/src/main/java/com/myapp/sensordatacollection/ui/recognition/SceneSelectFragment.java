package com.myapp.sensordatacollection.ui.recognition;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.myapp.sensordatacollection.R;
import com.myapp.sensordatacollection.ui.home.HomeViewModel;
import com.myapp.sensordatacollection.ui.recognition.scene.SceneSelectAdapter;

import java.util.Properties;


public class SceneSelectFragment extends Fragment {
    private SceneSelectAdapter sceneSelectAdapter;
    private  ViewPager2 viewPager;
    private static final String[] tab_title = {"xxx申请场景","xxx场景"};
    private HomeViewModel homeViewModel;
    private Properties properties;

    public static SceneSelectFragment newInstance() {
        SceneSelectFragment fragment = new SceneSelectFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public SceneSelectFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        return inflater.inflate(R.layout.fragment_scene_select, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sceneSelectAdapter = new SceneSelectAdapter(this);
        viewPager = view.findViewById(R.id.pager);
        viewPager.setAdapter(sceneSelectAdapter);

        TabLayout tabLayout = view.findViewById(R.id.tab_layout);
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> tab.setText(tab_title[position])).attach();
    }
}