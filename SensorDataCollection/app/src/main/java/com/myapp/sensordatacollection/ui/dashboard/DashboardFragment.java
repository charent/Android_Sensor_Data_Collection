package com.myapp.sensordatacollection.ui.dashboard;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.myapp.sensordatacollection.databinding.FragmentDashboardBinding;
import com.myapp.sensordatacollection.utils.ToastUtils;

import java.io.File;

public class DashboardFragment extends Fragment {

    private DashboardViewModel dashboardViewModel;
    private FragmentDashboardBinding binding;

    private TextView collectDataCountView;
    private TextView showDataNameView;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        dashboardViewModel =
                new ViewModelProvider(this).get(DashboardViewModel.class);

        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        collectDataCountView = binding.collectDataCountView;
        showDataNameView = binding.showDataNameView;

        intView();
        return root;
    }

    private void intView(){
        PackageManager packageManager = getActivity().getPackageManager();
        boolean storagePermission = (PackageManager.PERMISSION_GRANTED == packageManager.checkPermission("android.permission.READ_EXTERNAL_STORAGE", "com.myapp.sensordatacollection"));
        if (!storagePermission){
            ToastUtils.show(getContext(),"没有授予App读取外部存储的权限");
            return;
        }

        String filePath = getSaveFilePath();

        File file = new File(filePath);
        if (!file.exists()){
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        File files[] = file.listFiles();

        for (int i = 0; i < files.length; ++i){
            stringBuilder.append(i + 1 + " : " + files[i].getName() + "\n");
        }
        collectDataCountView.setText(String.format("已经收集的数据（共%d条）：", files.length));
        showDataNameView.setText(stringBuilder.toString());

    }

    public String getSaveFilePath(){
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
//            Log.d("path",Environment.getExternalStorageDirectory().getAbsolutePath() + "/Collection/");
            return  Environment.getExternalStorageDirectory().getAbsolutePath() + "/Collection/";
        }
        return null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}