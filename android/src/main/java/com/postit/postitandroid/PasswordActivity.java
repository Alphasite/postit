package com.postit.postitandroid;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class PasswordActivity extends AppCompatActivity {

    private AutoCompleteTextView mTitle;
    private AutoCompleteTextView mUsername;
    private EditText mPassword;
    private EditText mComments;
    private Button mSaveButton;
    private Button mDeleteButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Intent myIntent = getIntent();

        String title = myIntent.getStringExtra("title");
        mTitle = (AutoCompleteTextView)findViewById(R.id.etPTitle);
        mTitle.setText(title, TextView.BufferType.NORMAL);

        String username = myIntent.getStringExtra("username");
        mUsername = (AutoCompleteTextView) findViewById(R.id.etPUsername);
        mUsername.setText(username,TextView.BufferType.EDITABLE);

        String password = myIntent.getStringExtra("password");
        mPassword = (EditText) findViewById(R.id.etPPassword);
        mPassword.setText(password,TextView.BufferType.EDITABLE);
        mPassword.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v,boolean hasFocus) {
                if(hasFocus)
                    mPassword.setTransformationMethod(null);
                else
                    mPassword.setTransformationMethod(new PasswordTransformationMethod());

            }
        });

        String comments = myIntent.getStringExtra("comments");
        mComments = (EditText)findViewById(R.id.etPComments);
        mComments.setText(comments,TextView.BufferType.EDITABLE);

        mSaveButton = (Button) findViewById(R.id.bSave);
        mDeleteButton = (Button) findViewById(R.id.bDelete);

    }

}
