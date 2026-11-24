package database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {
    public static final String TABLE_USERS="usuarios";
    private static final String DB_NAME="usuarios.db";
    private static final int DB_VERSION=2;

    public DBHelper(Context context){super(context,DB_NAME,null,DB_VERSION);}

    @Override public void onCreate(SQLiteDatabase db){
        db.execSQL("CREATE TABLE usuarios ("+
            "id INTEGER PRIMARY KEY AUTOINCREMENT,"+
            "email TEXT NOT NULL UNIQUE COLLATE NOCASE,"+
            "password_hash TEXT NOT NULL,"+
            "is_verified INTEGER NOT NULL DEFAULT 0,"+
            "request_id TEXT,"+
            "client_secret TEXT)");
        // NO demo accounts. Users must register and verify their own email.
    }

    @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion){
        if(oldVersion < 2){
            // Deliberately reset old v1 accounts: they used plaintext passwords
            // and included four unsafe demo accounts. Existing v1 user data is lost.
            db.execSQL("DROP TABLE IF EXISTS usuarios");
            onCreate(db);
        }
    }
}
