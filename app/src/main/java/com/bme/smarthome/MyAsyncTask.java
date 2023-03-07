package com.bme.smarthome;

import static android.content.ContentValues.TAG;
import static com.bme.smarthome.FP.CacheFP;
import static com.bme.smarthome.FP.LoadFP;
import static com.bme.smarthome.MainActivity.mTB;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

public class MyAsyncTask extends AsyncTask<Void,Void,Void> {
    Activity contextParent;

    public MyAsyncTask(Activity contextParent) {
        this.contextParent = contextParent;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected Void doInBackground(Void... voids) {


        CacheFP();

        LoadFP();

        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        //Hàm này được thực hiện khi tiến trình kết thúc
        //Ở đây mình thông báo là đã "Finshed" để người dùng biết
        Toast.makeText(contextParent, "Complete!!!", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Complete");
    }
}
