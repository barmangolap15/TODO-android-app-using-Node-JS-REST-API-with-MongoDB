package com.codewithgolap.snapshot.Fragments;



import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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
import com.codewithgolap.snapshot.Interface.RecyclerviewClickListener;
import com.codewithgolap.snapshot.LoginActivity;
import com.codewithgolap.snapshot.R;
import com.codewithgolap.snapshot.UtilsService.SharedPreferenceClass;
import com.codewithgolap.snapshot.adapter.TodoListAdapter;
import com.codewithgolap.snapshot.model.TodoModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class HomeFragment extends Fragment implements RecyclerviewClickListener {


    SharedPreferenceClass sharedPreferenceClass;
    String token;
    FloatingActionButton floatingActionButton;

    TodoListAdapter todoListAdapter;
    RecyclerView recyclerView;
    TextView emptyTv, emptyTv2;
    ImageView emptyIv;
    ProgressBar progressBar;
    ArrayList<TodoModel> arrayList;

    public HomeFragment() {
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        sharedPreferenceClass = new SharedPreferenceClass(getContext());
        token = sharedPreferenceClass.getValue_string("token");


        floatingActionButton = view.findViewById(R.id.add_task_btn);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAlertDialog();
            }
        });

        recyclerView = view.findViewById(R.id.recyclerView);
        emptyTv = view.findViewById(R.id.empty_text);
        emptyTv2 = view.findViewById(R.id.empty_text2);
        emptyIv = view.findViewById(R.id.empty_image);
        progressBar = view.findViewById(R.id.progressBar);


        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(true);

        getTask();
        return view;
    }


    //get task from API
    public void getTask() {
        arrayList = new ArrayList<>();
        progressBar.setVisibility(View.VISIBLE);

        String url = "https://snapshotproject.herokuapp.com/api/snapshot";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET,
                url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    if(response.getBoolean("success")) {
//                        Toast.makeText(getActivity(), response.toString(), Toast.LENGTH_SHORT).show();
                        JSONArray jsonArray = response.getJSONArray("snapshots");

                        if (jsonArray.length() == 0){
                            emptyTv.setVisibility(View.VISIBLE);
                            emptyTv2.setVisibility(View.VISIBLE);
                            emptyIv.setVisibility(View.VISIBLE);
                        }else {
                            emptyTv.setVisibility(View.GONE);
                            emptyTv2.setVisibility(View.GONE);
                            emptyIv.setVisibility(View.GONE);
                            for(int i = 0; i < jsonArray.length(); i ++) {
                                JSONObject jsonObject = jsonArray.getJSONObject(i);

                                TodoModel todoModel = new TodoModel(
                                        jsonObject.getString("_id"),
                                        jsonObject.getString("title"),
                                        jsonObject.getString("description")
                                );
                                arrayList.add(todoModel);
                            }

                            todoListAdapter = new TodoListAdapter(getActivity(), arrayList, HomeFragment.this);
                            recyclerView.setAdapter(todoListAdapter);
                        }

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
                if (error == null || error.networkResponse == null){
                    return;
                }

                String body;
               // final String statusCode = String.valueOf(error.networkResponse.statusCode);
                try {

                    body = new String(error.networkResponse.data, "UTF-8");
                    JSONObject errorObject = new JSONObject(body);

                    if (errorObject.getString("msg").equals("Token not valid")){
                        sharedPreferenceClass.clear();
                        startActivity(new Intent(getActivity(), LoginActivity.class));
                        Toast.makeText(getActivity(), "Session Expired", Toast.LENGTH_SHORT).show();
                    }

                    Toast.makeText(getActivity(), errorObject.getString("msg"), Toast.LENGTH_SHORT).show();

                } catch (UnsupportedEncodingException | JSONException e){
                    //exception
                }

                progressBar.setVisibility(View.GONE);
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("Authorization", token);
                return headers;
            }
        };

        // set retry policy
        int socketTime = 3000;
        RetryPolicy policy = new DefaultRetryPolicy(socketTime,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        jsonObjectRequest.setRetryPolicy(policy);

        // request add
        RequestQueue requestQueue = Volley.newRequestQueue(getContext());
        requestQueue.add(jsonObjectRequest);
    }


    //show create task dialog
    public void showAlertDialog() {
        LayoutInflater inflater = getLayoutInflater();
        View alertLayout = inflater.inflate(R.layout.custom_dialog_layout, null);

        final EditText title_field = alertLayout.findViewById(R.id.title_edittext);
        final EditText description_field = alertLayout.findViewById(R.id.description_edittext);

        final AlertDialog dialog = new AlertDialog.Builder(getActivity(), R.style.AlertDialogTheme)
                .setView(alertLayout)
                .setTitle("Add Task")
                .setPositiveButton("Add",null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String title = title_field.getText().toString();
                        String description = description_field.getText().toString();
                        if(!TextUtils.isEmpty(title)) {
                            addTask(title, description);
                            dialog.dismiss();

                        } else {
                            Toast.makeText(getActivity(), "You need one title.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
        dialog.show();
    }


    //show edit task dialog
    public void showUpdateDialog(final  String  id, String title, String description){
        LayoutInflater inflater = getLayoutInflater();
        View alertLayout = inflater.inflate(R.layout.custom_dialog_layout, null);

        final EditText title_field = alertLayout.findViewById(R.id.title_edittext);
        final EditText description_field = alertLayout.findViewById(R.id.description_edittext);

        title_field.setText(title);
        description_field.setText(description);

        final AlertDialog alertDialog = new AlertDialog.Builder(getActivity(), R.style.AlertDialogTheme)
                .setView(alertLayout)
                .setTitle("Update Task")
                .setPositiveButton("Update", null)
                .setNegativeButton("Cancel", null)
                .create();

        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button button = ((AlertDialog)alertDialog).getButton(AlertDialog.BUTTON_POSITIVE);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String title = title_field.getText().toString();
                        String description = description_field.getText().toString();

                        updateTask(id, title, description);
                        alertDialog.dismiss();
                    }
                });
            }
        });

        alertDialog.show();
    }

    //show delete task dialog
    public void showDeleteDialog(final String id, final int position){

        final AlertDialog alertDialog = new AlertDialog.Builder(getActivity(), R.style.AlertDialogTheme)
                .setTitle("Are you want to delete the task ?")
                .setPositiveButton("Yes", null)
                .setNegativeButton("No", null)
                .create();

        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button button = ((AlertDialog)alertDialog).getButton(AlertDialog.BUTTON_POSITIVE);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        deleteTask(id, position);
                        alertDialog.dismiss();
                    }
                });
            }
        });

        alertDialog.show();
    }

    //done task
    public void showFinishTaskDialog(final String id, final int position){
        final AlertDialog alertDialog = new AlertDialog.Builder(getActivity(), R.style.AlertDialogTheme)
                .setTitle("Move to finished task ?")
                .setPositiveButton("Yes", null)
                .setNegativeButton("No", null)
                .create();

        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button button = ((AlertDialog)alertDialog).getButton(AlertDialog.BUTTON_POSITIVE);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        updateToFinishedTodo(id, position);
                        alertDialog.dismiss();
                    }
                });
            }
        });

        alertDialog.show();
    }


    //done task
    private void updateToFinishedTodo(final String id, final int position){
        String url = "https://snapshotproject.herokuapp.com/api/snapshot/"+id;
        HashMap<String, String> body = new HashMap<>();
        body.put("finished", "true");

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.PUT, url, new JSONObject(body),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            if(response.getBoolean("success")) {
                                arrayList.remove(position);
                                getTask();
                                todoListAdapter.notifyItemRemoved(position);
                                Toast.makeText(getActivity(), response.getString("msg"), Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getActivity(), error.toString(), Toast.LENGTH_SHORT).show();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("Content-Type", "application/json");
                return params;
            }
        };

        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(10000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        RequestQueue requestQueue = Volley.newRequestQueue(getContext());
        requestQueue.add(jsonObjectRequest);
    }

    //delete task
    private void deleteTask(final String id, final int position){
        String url = "https://snapshotproject.herokuapp.com/api/snapshot/"+id;

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.DELETE, url, null
                , new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    if (response.getBoolean("success")){
                        Toast.makeText(getActivity(), response.getString("msg"), Toast.LENGTH_SHORT).show();
                        arrayList.remove(position);
                        todoListAdapter.notifyItemRemoved(position);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getActivity(), error.toString(), Toast.LENGTH_SHORT).show();
            }
        });

        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(10000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        RequestQueue requestQueue = Volley.newRequestQueue(getContext());
        requestQueue.add(jsonObjectRequest);
    }
    //update task
    private void updateTask(String id, String title, String description) {
        String url = "https://snapshotproject.herokuapp.com/api/snapshot/"+id;
        HashMap<String, String> body = new HashMap<>();
        body.put("title", title);
        body.put("description", description);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.PUT, url, new JSONObject(body),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            if(response.getBoolean("success")) {
                                getTask();
                                Toast.makeText(getActivity(), response.getString("msg"), Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getActivity(), error.toString(), Toast.LENGTH_SHORT).show();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("Content-Type", "application/json");
                params.put("Authorization", token);
                return params;
            }
        };

        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(10000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        RequestQueue requestQueue = Volley.newRequestQueue(getContext());
        requestQueue.add(jsonObjectRequest);
    }


    // Add Todo Task Method
    private void addTask(String title, String description) {

        String url = "https://snapshotproject.herokuapp.com/api/snapshot";

        HashMap<String, String> body = new HashMap<>();
        body.put("title", title);
        body.put("description", description);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST,
                url, new JSONObject(body), new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    if(response.getBoolean("success")) {
                        Toast.makeText(getActivity(), "Great!! Your task added successfully", Toast.LENGTH_SHORT).show();
                        getTask();
                    }


                } catch (JSONException e) {
                    e.printStackTrace();

                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                NetworkResponse response = error.networkResponse;
                if(error instanceof ServerError && response != null) {
                    try {
                        String res = new String(response.data, HttpHeaderParser.parseCharset(response.headers,  "utf-8"));
                        JSONObject obj = new JSONObject(res);
                        Toast.makeText(getActivity(), obj.getString("msg"), Toast.LENGTH_SHORT).show();

                    } catch (JSONException | UnsupportedEncodingException je) {
                        je.printStackTrace();
                    }

                }
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("Authorization", token);
                return headers;
            }
        };

        // set retry policy
        int socketTime = 3000;
        RetryPolicy policy = new DefaultRetryPolicy(socketTime,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        jsonObjectRequest.setRetryPolicy(policy);

        // request add
        RequestQueue requestQueue = Volley.newRequestQueue(getContext());
        requestQueue.add(jsonObjectRequest);
    }


    @Override
    public void onItemClick(int position) {
        Toast.makeText(getActivity(), "Position "+ position, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLongItemClick(int position) {
        showUpdateDialog(arrayList.get(position).getId(), arrayList.get(position).getTitle(), arrayList.get(position).getDescription());
        Toast.makeText(getActivity(), "Position "+ position, Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onEditButtonClick(int position) {
        showUpdateDialog(arrayList.get(position).getId(), arrayList.get(position).getTitle(), arrayList.get(position).getDescription());
        Toast.makeText(getActivity(), "Position "+ position, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDeleteButtonClick(int position) {
        showDeleteDialog(arrayList.get(position).getId(), position);
        Toast.makeText(getActivity(), "Position "+ position, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDoneButtonClick(int position) {
        showFinishTaskDialog(arrayList.get(position).getId(), position);
        Toast.makeText(getActivity(), "Position "+ position, Toast.LENGTH_SHORT).show();
    }
}