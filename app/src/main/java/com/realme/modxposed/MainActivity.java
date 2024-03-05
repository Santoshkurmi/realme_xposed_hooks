package com.realme.modxposed;

import androidx.appcompat.widget.SwitchCompat;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;



import dalvik.system.DexFile;

public class MainActivity extends Activity {

    EditText time,passwordLen,passwordCount,password;
    SwitchCompat switchCompat;
    Button submit;
    SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        preferences = getSharedPreferences("settings",MODE_WORLD_READABLE);

        switchCompat = findViewById(R.id.magic_switch);
        time = findViewById(R.id.time);
        passwordLen = findViewById(R.id.password_len);
        passwordCount = findViewById(R.id.password_count);
        password = findViewById(R.id.password);
        submit = findViewById(R.id.submit);


        loadPreferences();
        
        submit.setOnClickListener(v -> savePreference());
//        listAllClasses();

    }//onCreate


    private void savePreference() {

        try{
            SharedPreferences.Editor editor = preferences.edit();

            int timeVal = Integer.parseInt(time.getText().toString());
            int passwordLenVal = Integer.parseInt(passwordLen.getText().toString());
            int passwordCountVal = Integer.parseInt(passwordCount.getText().toString());
            String passwordVal = password.getText().toString();

            editor.putInt("time",timeVal);
            editor.putBoolean("magic",switchCompat.isChecked());
            editor.putInt("password_len",passwordLenVal);
            editor.putInt("password_count",passwordCountVal);
            editor.putString("password",passwordVal);
            editor.apply();
            Toast.makeText(getApplicationContext(),"Settings successfully updated",Toast.LENGTH_SHORT).show();
        }
        catch (Exception ignored){

        }

    }//savePreference

    public void loadPreferences(){

       int timeVl =  preferences.getInt("time",0);
       if (timeVl !=0) time.setText(timeVl+"");


       int psVal = preferences.getInt("password_count",0);
       if (psVal !=0) passwordCount.setText(psVal+"");
        
       int passLen = preferences.getInt("password_len",0);
       if (passLen !=0) passwordLen.setText(passLen+"");

       password.setText(preferences.getString("password",""));

       switchCompat.setChecked(preferences.getBoolean("magic",false));
    }//

}