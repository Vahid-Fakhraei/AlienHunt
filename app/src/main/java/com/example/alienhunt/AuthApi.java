package com.example.alienhunt;

import android.os.Handler;
import android.os.Looper;
import org.json.JSONObject;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Local development only. Android Emulator -> Windows host: 10.0.2.2. */
public final class AuthApi {
    private static final String BASE_URL = "http://127.0.0.1:3000";
    private static final ExecutorService executor=Executors.newSingleThreadExecutor();
    private static final Handler main=new Handler(Looper.getMainLooper());
    private AuthApi(){}

    public interface Callback {void onResult(JSONObject response,String networkError);}
    public static void post(String path,JSONObject payload,Callback callback){
        executor.execute(()->{
            JSONObject result=null;
            String error=null;
            HttpURLConnection conn=null;
            try {
                conn=(HttpURLConnection)new URL(BASE_URL+path).openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(15000);
                conn.setRequestProperty("Content-Type","application/json; charset=utf-8");
                conn.setRequestProperty("Accept","application/json");
                conn.setDoOutput(true);
                byte[] data=payload.toString().getBytes(StandardCharsets.UTF_8);
                try(OutputStream out=conn.getOutputStream()){out.write(data);}
                int status=conn.getResponseCode();
                InputStream stream=status>=400?conn.getErrorStream():conn.getInputStream();
                if(stream!=null){
                    try(InputStream in=stream){
                        byte[] bytes=readAll(in);
                        result=new JSONObject(new String(bytes,StandardCharsets.UTF_8));
                    }
                } else error="Empty server response (HTTP "+status+")";
            }catch(Exception e){error=e.getMessage();}
            finally {if(conn!=null)conn.disconnect();}
            JSONObject finalResult=result;
            String finalError=error;
            main.post(()->callback.onResult(finalResult,finalError));
        });
    }
    private static byte[] readAll(InputStream in)throws java.io.IOException{
        java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();
        byte[] buf=new byte[2048];int n;
        while((n=in.read(buf))!=-1)out.write(buf,0,n);
        return out.toByteArray();
    }
}
