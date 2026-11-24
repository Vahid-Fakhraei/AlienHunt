package com.example.alienhunt;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import org.json.JSONObject;
import database.UserManager;

public class Login extends AppCompatActivity {
    private UserManager users;
    private EditText usuario;
    private TextInputEditText pass;
    private Button loginButton;
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        setContentView(R.layout.activity_login);
        users=new UserManager(this);
        usuario=findViewById(R.id.usuario);
        pass=findViewById(R.id.pass);
        loginButton=findViewById(R.id.btn_iniciar);
        TextView forgot=findViewById(R.id.link_forgotPass);
        Button register=findViewById(R.id.btn_regsiter);
        forgot.setOnClickListener(v->startActivity(new Intent(this,ResetPassword.class)));
        register.setOnClickListener(v->startActivity(new Intent(this,Registerarse.class)));
        loginButton.setOnClickListener(v->attemptLogin());
    }
    private void show(int resource){Toast.makeText(this,resource,Toast.LENGTH_LONG).show();}
    private void enterGame(){startActivity(new Intent(this,TerminosActivity.class));finish();}
    private void attemptLogin(){
        String email=usuario.getText().toString().trim();
        String password=pass.getText()==null?"":pass.getText().toString();
        if(email.isEmpty() || password.isEmpty()){show(R.string.auth_required);return;}
        if(users.validarUsuario(email,password)){enterGame();return;}
        UserManager.PendingRegistration pending=users.getPendingRegistration(email,password);
        if(pending==null){show(R.string.auth_invalid_credentials);return;}
        try{
            JSONObject payload=new JSONObject()
                .put("requestId",pending.requestId).put("clientSecret",pending.clientSecret);
            loginButton.setEnabled(false);
            AuthApi.post("/api/auth/verification-status",payload,(result,error)->{
                if(isFinishing()||isDestroyed())return;
                loginButton.setEnabled(true);
                if(error!=null||result==null){show(R.string.auth_connection_error);return;}
                String status=result.optString("status");
                if("verified".equals(status)){
                    if(users.activarUsuario(email,pending.requestId,pending.clientSecret)) enterGame();
                    else show(R.string.auth_local_error);
                } else if("pending".equals(status)){show(R.string.auth_pending);}
                else if("expired".equals(status)){show(R.string.auth_expired);}
                else show(R.string.auth_pending_retry);
            });
        }catch(Exception ex){loginButton.setEnabled(true);show(R.string.auth_local_error);}
    }
}
