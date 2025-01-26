package com.realme.modxposed.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.realme.modxposed.R;
import com.realme.modxposed.activity.ApplicationList;
import com.realme.modxposed.activity.ClassesListActivity;
import com.realme.modxposed.dao.ApplicationListDao;

import java.util.ArrayList;

public class ApplicationListAdapter extends RecyclerView.Adapter<ApplicationListAdapter.ApplicationViewHolder> {

    private ArrayList<ApplicationListDao> applicationListArray;
    private Context context;

    public ApplicationListAdapter(ArrayList<ApplicationListDao> apps, Context context){
        this.applicationListArray = apps;
        this.context = context;
    }

    @NonNull
    @Override
    public ApplicationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.app_row,parent,false);
        return new ApplicationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ApplicationViewHolder holder, int position) {
        ApplicationListDao app = applicationListArray.get(position);
        holder.name.setText(app.getName());
        holder.icon.setImageDrawable(app.getIcon());
        holder.enabledSwitch.setEnabled(app.isEnabled());
        holder.cardView.setOnClickListener(v -> {
//            Toast.makeText(context,"Clicked "+position,Toast.LENGTH_SHORT).show();
            context.startActivity(new Intent(context, ClassesListActivity.class));
        });
    }

    @Override
    public int getItemCount() {
        return applicationListArray.size();
    }

   public class ApplicationViewHolder extends RecyclerView.ViewHolder{
        public ImageView icon;
        public TextView name;
        public CardView cardView;
        public SwitchMaterial enabledSwitch;
        public ApplicationViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.icon);
            name = itemView.findViewById(R.id.name);
            enabledSwitch = itemView.findViewById(R.id.enabled);
            cardView = itemView.findViewById(R.id.listbox);

        }
    }


}
