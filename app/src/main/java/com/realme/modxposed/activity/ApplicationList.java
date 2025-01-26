package com.realme.modxposed.activity;

import android.app.ProgressDialog;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.realme.modxposed.R;
import com.realme.modxposed.adapters.ApplicationListAdapter;
import com.realme.modxposed.dao.ApplicationListDao;

import java.util.ArrayList;
import java.util.List;

public class ApplicationList extends AppCompatActivity {

    TextView progressText;
    ProgressDialog dialog;
    ArrayList<ApplicationListDao> apps = new ArrayList<>();
    ApplicationListAdapter listAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showProgressBar();

//        if(true) return;
        setContentView(R.layout.activity_application_list);


        RecyclerView recyclerView = findViewById(R.id.recycle);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        listAdapter = new ApplicationListAdapter(apps,this);
        recyclerView.setAdapter(listAdapter);

        loadApps();





    }//onCreate

    public void loadApps(){
        new Thread(() -> {
            List< ApplicationInfo> applicationInfos = getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);
            int count = 0;
            int length = applicationInfos.size();
            runOnUiThread(() -> {
                dialog.setMax(length);
            });
            for( ApplicationInfo appinfo :applicationInfos){
                count++;
                String name = getPackageManager().getApplicationLabel(appinfo).toString();
                Drawable icon = getPackageManager().getApplicationIcon(appinfo);
                apps.add(new ApplicationListDao(icon,name,true) );

                int finalCount1 = count;
                runOnUiThread(() -> {
//                    progressText.setText(String.format("%d/%d", finalCount, length));
                    dialog.setProgress(finalCount1);
                });
            }
            runOnUiThread(()->{
                listAdapter.notifyDataSetChanged();
                dialog.dismiss();
            });
        }).start();
    }

    public void showProgressBar(){
        dialog = new ProgressDialog(this);
        dialog.setTitle("Apps");
        dialog.setMessage("Loading...");
        dialog.setProgressStyle(ProgressBar.SCROLL_AXIS_HORIZONTAL);
        dialog.setContentView(R.layout.loading_packages);
        dialog.setCancelable(false);

        progressText = dialog.findViewById(R.id.pac_count);
        dialog.show();

    }

}