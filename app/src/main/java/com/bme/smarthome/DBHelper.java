package com.bme.smarthome;

import static com.bme.smarthome.MainActivity.mTB;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.StrictMode;

import androidx.core.app.ActivityCompat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBHelper {
    private static final String ip = "192.168.1.201";
    private static final String port = "19445";
    private static final String Classes = "net.sourceforge.jtds.jdbc.Driver";
    private static final String database = "QLGL";
    private static final String username = "adminAPP";
    private static final String password = "admin@123Abc";
    private static final String url = "jdbc:jtds:sqlserver://" + ip + ":" + port + ";databaseName=" + database + ";";
    public static Connection connection = null;


    public static void ConnectDB(){
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        try {
            Class.forName(Classes);
            connection = DriverManager.getConnection(url, username, password);
//            mTB.setText("Success");
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
//            mTB.setText(e.toString());
        }
    }
}
