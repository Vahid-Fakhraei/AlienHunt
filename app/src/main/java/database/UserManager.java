package database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.Locale;

public class UserManager {
    private final DBHelper helper;
    public UserManager(Context context){helper=new DBHelper(context);}
    private String normalize(String email){return email.trim().toLowerCase(Locale.ROOT);}

    public boolean existeUsuario(String email){
        SQLiteDatabase db=helper.getReadableDatabase();
        try(Cursor c=db.rawQuery("SELECT 1 FROM usuarios WHERE email=? LIMIT 1",
            new String[]{normalize(email)})){
            return c.moveToFirst();
        }
    }
    public boolean estaVerificado(String email){
        SQLiteDatabase db=helper.getReadableDatabase();
        try(Cursor c=db.rawQuery("SELECT is_verified FROM usuarios WHERE email=? LIMIT 1",
            new String[]{normalize(email)})){
            return c.moveToFirst() && c.getInt(0)==1;
        }
    }
    public boolean registrarUsuarioPendiente(String email,String password,String requestId,String clientSecret){
        ContentValues v=new ContentValues();
        v.put("email",normalize(email));
        v.put("password_hash",PasswordUtils.hashPassword(password));
        v.put("is_verified",0);
        v.put("request_id",requestId);
        v.put("client_secret",clientSecret);
        return helper.getWritableDatabase().insert(DBHelper.TABLE_USERS,null,v)!=-1;
    }
    public static final class PendingRegistration {
        public final String requestId;
        public final String clientSecret;
        public PendingRegistration(String requestId,String clientSecret){
            this.requestId=requestId;this.clientSecret=clientSecret;
        }
    }
    public PendingRegistration getPendingRegistration(String email,String password){
        SQLiteDatabase db=helper.getReadableDatabase();
        try(Cursor c=db.rawQuery("SELECT password_hash,is_verified,request_id,client_secret FROM usuarios WHERE email=?",
            new String[]{normalize(email)})){
            if(!c.moveToFirst() || c.getInt(1)!=0 ||
               !PasswordUtils.verifyPassword(password,c.getString(0))) return null;
            String id=c.getString(2),secret=c.getString(3);
            return id==null || secret==null ? null : new PendingRegistration(id,secret);
        }
    }
    public boolean replacePendingRegistration(String email,String password,String requestId,String clientSecret){
        if(getPendingRegistration(email,password)==null) return false;
        ContentValues v=new ContentValues();
        v.put("request_id",requestId);
        v.put("client_secret",clientSecret);
        return helper.getWritableDatabase().update(DBHelper.TABLE_USERS,v,
            "email=? AND is_verified=0",new String[]{normalize(email)})==1;
    }
    public boolean activarUsuario(String email,String requestId,String clientSecret){
        ContentValues v=new ContentValues();
        v.put("is_verified",1);
        v.putNull("request_id");
        v.putNull("client_secret");
        return helper.getWritableDatabase().update(DBHelper.TABLE_USERS,v,
            "email=? AND is_verified=0 AND request_id=? AND client_secret=?",
            new String[]{normalize(email),requestId,clientSecret})==1;
    }
    public boolean validarUsuario(String email,String password){
        SQLiteDatabase db=helper.getReadableDatabase();
        try(Cursor c=db.rawQuery("SELECT password_hash,is_verified FROM usuarios WHERE email=?",
            new String[]{normalize(email)})){
            return c.moveToFirst() && c.getInt(1)==1 &&
                PasswordUtils.verifyPassword(password,c.getString(0));
        }
    }
    public boolean actualizarPassword(String email,String nuevaPassword){
        ContentValues v=new ContentValues();
        v.put("password_hash",PasswordUtils.hashPassword(nuevaPassword));
        return helper.getWritableDatabase().update(DBHelper.TABLE_USERS,v,
            "email=? AND is_verified=1",new String[]{normalize(email)})==1;
    }
}
