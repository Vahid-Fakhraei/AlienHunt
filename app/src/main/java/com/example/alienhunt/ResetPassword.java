package com.example.alienhunt;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputLayout;
import org.json.JSONObject;
import database.UserManager;

public class ResetPassword extends AppCompatActivity {
    private Button sendButton,verifyButton,saveButton;
    private EditText emailView,codeView,newPassView,repeatPassView;
    private TextInputLayout newPassLayout,repeatPassLayout;
    private UserManager users;
    private String email,requestId,resetGrant;
    @Override protected void onCreate(Bundle savedState){
        super.onCreate(savedState);
        setContentView(R.layout.activity_reset_password);
        users=new UserManager(this);
        emailView=findViewById(R.id.userEmail);
        codeView=findViewById(R.id.verificationCode);
        newPassView=findViewById(R.id.newPass);
        repeatPassView=findViewById(R.id.repNewPass);
        sendButton=findViewById(R.id.btn_resetPass);
        verifyButton=findViewById(R.id.btn_validate);
        saveButton=findViewById(R.id.btn_save);
        newPassLayout=findViewById(R.id.layout_newpass);
        repeatPassLayout=findViewById(R.id.layout_newpassrepeat);
        sendButton.setOnClickListener(v->requestCode());
        verifyButton.setOnClickListener(v->verifyCode());
        saveButton.setOnClickListener(v->saveNewPassword());
    }
    private void show(int id){Toast.makeText(this,id,Toast.LENGTH_LONG).show();}
    private void requestCode(){
        String entered=emailView.getText().toString().trim();
        if(!users.existeUsuario(entered) || !users.estaVerificado(entered)){
            show(R.string.auth_account_not_available);return;
        }
        try{
            String language=getResources().getConfiguration().getLocales().get(0).toLanguageTag();
            JSONObject json=new JSONObject().put("email",entered).put("language",language);
            sendButton.setEnabled(false);
            AuthApi.post("/api/auth/reset/request",json,(response,error)->{
                if(isFinishing()||isDestroyed())return;
                sendButton.setEnabled(true);
                if(error!=null||response==null){show(R.string.auth_connection_error);return;}
                if(!response.optBoolean("success")){show(R.string.auth_send_failed);return;}
                email=entered;
                requestId=response.optString("requestId");
                emailView.setEnabled(false);
                sendButton.setVisibility(View.GONE);
                codeView.setVisibility(View.VISIBLE);
                verifyButton.setVisibility(View.VISIBLE);
                show(R.string.auth_otp_sent);
            });
        }catch(Exception e){sendButton.setEnabled(true);show(R.string.auth_local_error);}
    }
    private void verifyCode(){
        String code=codeView.getText().toString().trim();
        if(!code.matches("[0-9]{6}")){show(R.string.auth_code_format);return;}
        try{
            JSONObject json=new JSONObject().put("requestId",requestId).put("code",code);
            verifyButton.setEnabled(false);
            AuthApi.post("/api/auth/reset/verify",json,(response,error)->{
                if(isFinishing()||isDestroyed())return;
                verifyButton.setEnabled(true);
                if(error!=null||response==null){show(R.string.auth_connection_error);return;}
                if(!response.optBoolean("success")){show(R.string.auth_otp_invalid);return;}
                resetGrant=response.optString("resetGrant");
                if(resetGrant.isEmpty()){show(R.string.auth_local_error);return;}
                codeView.setVisibility(View.GONE);
                verifyButton.setVisibility(View.GONE);
                newPassLayout.setVisibility(View.VISIBLE);
                repeatPassLayout.setVisibility(View.VISIBLE);
                saveButton.setVisibility(View.VISIBLE);
            });
        }catch(Exception e){verifyButton.setEnabled(true);show(R.string.auth_local_error);}
    }
    private void saveNewPassword(){
        String password=newPassView.getText().toString();
        String repeat=repeatPassView.getText().toString();
        if(password.length()<8){show(R.string.auth_password_short);return;}
        if(!password.equals(repeat)){show(R.string.auth_password_mismatch);return;}
        if(resetGrant==null){show(R.string.auth_otp_invalid);return;}
        try{
            JSONObject json=new JSONObject().put("resetGrant",resetGrant);
            saveButton.setEnabled(false);
            AuthApi.post("/api/auth/reset/consume",json,(response,error)->{
                if(isFinishing()||isDestroyed())return;
                saveButton.setEnabled(true);
                if(error!=null||response==null){show(R.string.auth_connection_error);return;}
                if(!response.optBoolean("success")){show(R.string.auth_reset_expired);return;}
                if(!email.equalsIgnoreCase(response.optString("email"))){show(R.string.auth_local_error);return;}
                resetGrant=null;
                if(!users.actualizarPassword(email,password)){show(R.string.auth_local_error);return;}
                show(R.string.auth_password_updated);
                finish(); // Return to existing Login activity.
            });
        }catch(Exception e){saveButton.setEnabled(true);show(R.string.auth_local_error);}
    }
}
