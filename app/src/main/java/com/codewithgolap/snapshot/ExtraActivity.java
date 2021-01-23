package com.codewithgolap.snapshot;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.codewithgolap.snapshot.UtilsService.SharedPreferenceClass;

public class ExtraActivity extends AppCompatActivity {

    Button logoutBtn;
    SharedPreferenceClass sharedPreferenceClass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_extra);

        sharedPreferenceClass = new SharedPreferenceClass(this);

        logoutBtn = findViewById(R.id.logoutBtn);
        logoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sharedPreferenceClass.clear();
                startActivity(new Intent(ExtraActivity.this, LoginActivity.class));
                finish();
            }
        });
    }
}