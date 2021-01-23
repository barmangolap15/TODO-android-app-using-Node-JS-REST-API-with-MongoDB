package com.codewithgolap.snapshot;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.ServerError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.codewithgolap.snapshot.UtilsService.SharedPreferenceClass;
import com.codewithgolap.snapshot.UtilsService.UtilService;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private TextView loginBtn;
    private EditText name_ET, email_ET, password_ET;
    private Button registerBtn;
    private CheckBox showHideBtn;
    ProgressBar progressBar;
    private String name, email, password;
    UtilService utilService;
    SharedPreferenceClass sharedPreferenceClass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        showHideBtn = findViewById(R.id.showHideBtn);
        showHideBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean value) {
                if (value)
                {
                    // Show Password
                    password_ET.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                }
                else
                {
                    // Hide Password
                    password_ET.setTransformationMethod(PasswordTransformationMethod.getInstance());
                }
            }
        });

        loginBtn = findViewById(R.id.singBtn);
        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), LoginActivity.class));
                finish();
            }
        });

        progressBar = findViewById(R.id.progress_bar);
        name_ET = findViewById(R.id.name_ET);
        email_ET = findViewById(R.id.email_ET);
        password_ET = findViewById(R.id.password_ET);
        utilService = new UtilService();
        sharedPreferenceClass =  new SharedPreferenceClass(this);

        registerBtn = findViewById(R.id.registerBtn);
        registerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                utilService.hideKeyboard(view, RegisterActivity.this);
                name = name_ET.getText().toString();
                email = email_ET.getText().toString();
                password = password_ET.getText().toString();

                if (validate(view)){
                    registerUser(view);
                }
            }
        });
    }

    private void registerUser(View view) {
        progressBar.setVisibility(View.VISIBLE);

        final HashMap<String, String> params = new HashMap<>();
        params.put("username", name);
        params.put("email", email);
        params.put("password", password);

        String apiKey= "https://snapshotproject.herokuapp.com/api/snapshot/auth/register";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST,
                apiKey, new JSONObject(params), new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    if (response.getBoolean("success")){
                        String token = response.getString("token");
                        sharedPreferenceClass.setValue_string("token", token);

                        Toast.makeText(RegisterActivity.this, token, Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(RegisterActivity.this, ExtraActivity.class));
                        finish();
                    }
                    progressBar.setVisibility(View.GONE);
                } catch (JSONException e) {
                    e.printStackTrace();
                    progressBar.setVisibility(View.GONE);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                NetworkResponse response = error.networkResponse;
                if (error instanceof ServerError && response != null){
                    try {
                        String res = new String(response.data, HttpHeaderParser.parseCharset(response.headers, "utf-8"));

                        JSONObject obj = new JSONObject(res);
                        Toast.makeText(RegisterActivity.this, obj.getString("msg"), Toast.LENGTH_SHORT).show();

                        progressBar.setVisibility(View.GONE);

                    }catch (JSONException | UnsupportedEncodingException je){
                        je.printStackTrace();
                        progressBar.setVisibility(View.GONE);
                    }
                }
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Content-Type","application/json");

                return params;
            }
        };

        //set retry policy
        int socketTime = 3000;
        RetryPolicy policy = new DefaultRetryPolicy(socketTime, DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        jsonObjectRequest.setRetryPolicy(policy);

        //request add
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(jsonObjectRequest);
    }

    public boolean validate(View view){
        boolean isValid;
        if (!TextUtils.isEmpty(name)){
            if (!TextUtils.isEmpty(email)){
                if (!TextUtils.isEmpty(password)){
                    isValid = true;
                }else {
                    utilService.showSnackbar(view, "Please enter password");
                    isValid = false;
                }
            }else {
                utilService.showSnackbar(view, "Please enter email");
                isValid = false;
            }
        }else {
            utilService.showSnackbar(view, "Please enter name");
            isValid = false;
        }
        return isValid;
    }


    @Override
    protected void onStart() {
        super.onStart();

        SharedPreferences snapshot_pref = getSharedPreferences("user_snapshot", MODE_PRIVATE);
        if (snapshot_pref.contains("token")){
            startActivity(new Intent(RegisterActivity.this, ExtraActivity.class));
            finish();
        }
    }
}