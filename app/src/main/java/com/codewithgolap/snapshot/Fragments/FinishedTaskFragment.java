package com.codewithgolap.snapshot.Fragments;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.codewithgolap.snapshot.Interface.RecyclerviewClickListener;
import com.codewithgolap.snapshot.LoginActivity;
import com.codewithgolap.snapshot.R;
import com.codewithgolap.snapshot.UtilsService.SharedPreferenceClass;
import com.codewithgolap.snapshot.adapter.FinishedTaskAdapter;
import com.codewithgolap.snapshot.model.TodoModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FinishedTaskFragment extends Fragment implements RecyclerviewClickListener {

    SharedPreferenceClass sharedPreferenceClass;
    String token;
    FinishedTaskAdapter finishedTaskAdapter;
    RecyclerView recyclerView;
    TextView emptyTv;
    ProgressBar progressBar;
    ArrayList<TodoModel> arrayList;
    Button backToAddTask;

    public FinishedTaskFragment() {
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_finished_task, container, false);

        sharedPreferenceClass = new SharedPreferenceClass(getContext());
        token = sharedPreferenceClass.getValue_string("token");

        recyclerView = view.findViewById(R.id.finished_recyclerView);
        emptyTv = view.findViewById(R.id.empty_text);
        progressBar = view.findViewById(R.id.progressBar);
        backToAddTask  = view.findViewById(R.id.back_to_addTask);
        backToAddTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
                fragmentTransaction.replace(R.id.content, new HomeFragment());
                fragmentTransaction.commit();
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(true);

        getTask();
        return view;
    }

    public void getTask() {
        arrayList = new ArrayList<>();
        progressBar.setVisibility(View.VISIBLE);

        String url = "https://snapshotproject.herokuapp.com/api/snapshot/finished";

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
                            backToAddTask.setVisibility(View.VISIBLE);
                        }else {
                            emptyTv.setVisibility(View.GONE);
                            backToAddTask.setVisibility(View.GONE);
                            for(int i = 0; i < jsonArray.length(); i ++) {
                                JSONObject jsonObject = jsonArray.getJSONObject(i);

                                TodoModel todoModel = new TodoModel(
                                        jsonObject.getString("_id"),
                                        jsonObject.getString("title"),
                                        jsonObject.getString("description")
                                );
                                arrayList.add(todoModel);
                            }

                            finishedTaskAdapter = new FinishedTaskAdapter(getActivity(), arrayList, FinishedTaskFragment.this);
                            recyclerView.setAdapter(finishedTaskAdapter);
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
                    e.printStackTrace();
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



    public void showDeleteDialog(final String id, final int position) {
        final AlertDialog alertDialog = new AlertDialog.Builder(getActivity(), R.style.AlertDialogTheme)
                .setTitle("Are you want to delete the finished task ?")
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

    private void deleteTask(final String id,final int position) {
        String url = "https://snapshotproject.herokuapp.com/api/snapshot/"+id;

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.DELETE, url, null
                , new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    if (response.getBoolean("success")){
                        Toast.makeText(getActivity(), response.getString("msg"), Toast.LENGTH_SHORT).show();
                        arrayList.remove(position);
                        finishedTaskAdapter.notifyItemRemoved(position);
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


    @Override
    public void onItemClick(int position) {

    }

    @Override
    public void onLongItemClick(int position) {

    }

    @Override
    public void onEditButtonClick(int position) {

    }

    @Override
    public void onDeleteButtonClick(int position) {
        Toast.makeText(getActivity(), "Position "+ arrayList.get(position).getId(), Toast.LENGTH_SHORT).show();
        showDeleteDialog(arrayList.get(position).getId(), position);
    }


    @Override
    public void onDoneButtonClick(int position) {

    }
}