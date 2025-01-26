package com.realme.modxposed.adapters;

import android.content.Context;
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
import com.realme.modxposed.dao.ApplicationListDao;
import com.realme.modxposed.dao.ClassInfo;

import java.util.ArrayList;

public class ClassListAdapter extends RecyclerView.Adapter<ClassListAdapter.ApplicationViewHolder> {

    private final ArrayList<ClassInfo> classList;
    private final Context context;

    public ClassListAdapter(ArrayList<ClassInfo> apps, Context context){
        this.classList = apps;
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
        ClassInfo app = classList.get(position);
        holder.name.setText(app.getName());
//        holder.icon.setImageDrawable(app.getIcon());
        holder.enabledSwitch.setEnabled(app.isActive());
        holder.cardView.setOnClickListener(v -> {
            Toast.makeText(context,"Clicked "+position,Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        return classList.size();
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
