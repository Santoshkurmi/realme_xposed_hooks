package com.realme.modxposed.activity;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.realme.modxposed.R;
import com.realme.modxposed.adapters.ApplicationListAdapter;
import com.realme.modxposed.adapters.ClassListAdapter;
import com.realme.modxposed.dao.ApplicationListDao;
import com.realme.modxposed.dao.ClassInfo;

import java.util.ArrayList;

public class ClassesListActivity extends AppCompatActivity {

    ArrayList<ClassInfo> apps = new ArrayList<>();
    ClassListAdapter listAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_classes_list);
        RecyclerView recyclerView = findViewById(R.id.recycle);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        ClassInfo one = new ClassInfo();
        one.setName("com.esewa.main");
        apps.add(one);

        listAdapter = new ClassListAdapter(apps,this);
        recyclerView.setAdapter(listAdapter);

    }
}