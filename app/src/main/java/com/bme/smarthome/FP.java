package com.bme.smarthome;

import static com.bme.smarthome.DBHelper.connection;
import static com.bme.smarthome.MainActivity.candidates;
import static com.bme.smarthome.MainActivity.mTB;

import android.os.Build;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.machinezoo.sourceafis.FingerprintImage;
import com.machinezoo.sourceafis.FingerprintImageOptions;
import com.machinezoo.sourceafis.FingerprintTemplate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.Statement;

public class FP {
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void CacheFP() {
        String path = Environment.getExternalStorageDirectory().toString() + "/Android/data/com.bme.smarthome/files/FPfile";
//        Log.d("Files", "Path: " + path);
        File directory = new File(path);
        File[] files = directory.listFiles();
        if (files != null) {
//            Log.d("Files", "Size: " + files.length);
            UserDetails[] candidates = new UserDetails[files.length];
            //FingerprintTemplate candidates[] = new FingerprintTemplate[files.length];

            for (int i = 0; i < files.length; i++) {
                try {
                    candidates[i] = new UserDetails();
                    candidates[i].name = files[i].getName();
                    candidates[i].template = new FingerprintTemplate(
                            new FingerprintImage(
                                    Files.readAllBytes(Paths.get(files[i].getAbsolutePath())),
                                    new FingerprintImageOptions()
                                            .dpi(500)));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                String fname = files[i].getName();
                File storagePath = new File(Environment.getExternalStorageDirectory().toString() + "/Android/data/com.bme.smarthome/files/FPcache");
                storagePath.mkdirs();
                File f = new File(storagePath, fname.substring(0, fname.lastIndexOf(".")) + ".cbor");
                if (f.exists()) {

                } else {
                    try {
                        f.createNewFile();
                        FileOutputStream out = new FileOutputStream(f);
                        byte[] serialized = candidates[i].template.toByteArray();
                        out.write(serialized);
                        out.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void LoadFP() {
        String path = Environment.getExternalStorageDirectory().toString() + "/Android/data/com.bme.smarthome/files/FPcache";
//        Log.d("Files", "Path: " + path);
        File directory = new File(path);
        File[] files = directory.listFiles();
        if (files != null) {
//            Log.d("Files", "Size: " + files.length);
            candidates = new UserDetails[files.length];

            for (int i = 0; i < files.length; i++) {
                try {
                    candidates[i] = new UserDetails();
                    String Uname = files[i].getName();
                    candidates[i].name = Uname.substring(0, Uname.lastIndexOf("."));
                    byte[] serialized = Files.readAllBytes(Paths.get(files[i].getAbsolutePath()));
                    candidates[i].template = new FingerprintTemplate(serialized);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void UpdateInfo() {
        try {
            Statement statement = connection.createStatement();

            ResultSet rs = statement.executeQuery("SELECT * FROM Fingerprint");
            for(int i = 0; i<=rs.getRow();i++){
                rs.next();
                byte[] img = rs.getBytes(3);
                String path = Environment.getExternalStorageDirectory().toString() + "/Android/data/com.bme.smarthome/files/FPfile/";
                File image = new File(path, rs.getString(2) + ".bmp");
                try {
                    FileOutputStream out = new FileOutputStream(image);
                    MyBitmapFile fileBMP = new MyBitmapFile(320, 480, img);
                    out.write(fileBMP.toBytes());
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                CacheFP();
                mTB.setText("Update thành công!");
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }
}
