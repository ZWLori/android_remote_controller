package com.example.wanluzhang.mdp_17;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.renderscript.ScriptGroup;
import android.view.Window;
import android.view.inputmethod.InputMethod;
import android.widget.Button;
import android.widget.EditText;
import android.view.View;
import android.widget.Toast;
import android.view.inputmethod.InputMethodManager;

/**
 * Created by ZWL on 6/9/16.
 */
public class reconfigurationActivity extends Activity{

    private Button SavenUpdate;
    private Button cancel;
    private EditText editText_f1;
    private EditText editText_f2;
    private String content_f1;
    private String content_f2;

    SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_reconfiguration);

        SavenUpdate = (Button)findViewById(R.id.save_btn);
        SavenUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                save();
                finish();
            }
        });

        cancel = (Button)findViewById(R.id.cancel_reconfiguration);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        editText_f1 = (EditText)findViewById(R.id.f1_editText);
        editText_f1.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocused) {
                if(!hasFocused)
                    hideKeyboard(v);
            }
        });

        editText_f2 = (EditText)findViewById(R.id.f2_editText);
        editText_f2.setOnFocusChangeListener(new View.OnFocusChangeListener(){
            @Override
            public void onFocusChange(View v, boolean hasFocused){
                if(!hasFocused)
                    hideKeyboard(v);
            }
        });

        loadPreferences();

    }

    private void loadPreferences(){
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        content_f1 = preferences.getString("F1Command","");
        content_f2 = preferences.getString("F2Command","");

        editText_f1.setText(content_f1);
        editText_f2.setText(content_f2);
    }

    private void save(){
        super.onPause();
        android.content.SharedPreferences.Editor editor = preferences.edit();
        editor.putString("F1Command", editText_f1.getText().toString());
        editor.putString("F2Command", editText_f2.getText().toString());

        editor.commit();
        Toast.makeText(getApplicationContext(), "Commands Updated", Toast.LENGTH_SHORT).show();
    }

    private void hideKeyboard(View v){
        if(v != null){
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromInputMethod(v.getWindowToken(), 0);
        }
    }

}
