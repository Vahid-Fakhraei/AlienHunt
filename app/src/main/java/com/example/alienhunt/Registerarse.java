package com.example.alienhunt;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import org.json.JSONObject;
import database.UserManager;

public class Registerarse extends AppCompatActivity {
    private EditText emailView;
    private TextInputEditText passwordView,repeatView;
    private Button registerButton;
    private CheckBox conditions;
    private UserManager users;
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        setContentView(R.layout.activity_registerarse);
        emailView=findViewById(R.id.txt_user);
        passwordView=findViewById(R.id.txt_password);
        repeatView=findViewById(R.id.txt_repeat_password);
        registerButton=findViewById(R.id.btn_reg);
        conditions=findViewById(R.id.checkBox1);
        users=new UserManager(this);
        registerButton.setOnClickListener(v->register());
    }
    private void show(int id){Toast.makeText(this,id,Toast.LENGTH_LONG).show();}
    private void register(){
        String email=emailView.getText().toString().trim();
        String password=passwordView.getText()==null?"":passwordView.getText().toString();
        String repeat=repeatView.getText()==null?"":repeatView.getText().toString();
        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){show(R.string.auth_invalid_email);return;}
        if(password.length()<8){show(R.string.auth_password_short);return;}
        if(!password.equals(repeat)){show(R.string.auth_password_mismatch);return;}
        if(!conditions.isChecked()){show(R.string.auth_accept_conditions);return;}
        boolean exists=users.existeUsuario(email);
        if(exists && users.estaVerificado(email)){show(R.string.auth_user_exists);return;}
        if(exists && users.getPendingRegistration(email,password)==null){
            show(R.string.auth_user_exists);return;
        }
        try{
            String language=getResources().getConfiguration().getLocales().get(0).toLanguageTag();
            JSONObject payload=new JSONObject().put("email",email).put("language",language);
            registerButton.setEnabled(false);
            AuthApi.post("/api/auth/send-verification",payload,(result,error)->{
                if(isFinishing()||isDestroyed())return;
                registerButton.setEnabled(true);
                if(error!=null||result==null){show(R.string.auth_connection_error);return;}
                if(!result.optBoolean("success")){
                    show("Too many requests; try later".equals(result.optString("message"))
                        ? R.string.auth_rate_limit:R.string.auth_send_failed);return;
                }
                String id=result.optString("requestId");
                String proof=result.optString("clientSecret");
                if(id.isEmpty()||proof.isEmpty()){show(R.string.auth_local_error);return;}
                boolean saved=exists
                    ? users.replacePendingRegistration(email,password,id,proof)
                    : users.registrarUsuarioPendiente(email,password,id,proof);
                if(!saved){show(R.string.auth_local_error);return;}
                show(R.string.auth_check_email);
                finish(); // Login below will check the proof after email verification.
            });
        }catch(Exception ex){registerButton.setEnabled(true);show(R.string.auth_local_error);}
    }
}
