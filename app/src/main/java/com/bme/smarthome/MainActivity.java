package com.bme.smarthome;

import static com.bme.smarthome.DBHelper.ConnectDB;
import static com.bme.smarthome.DBHelper.connection;
import static com.bme.smarthome.FP.UpdateInfo;
import static com.bme.smarthome.UserDetails.CheckEInfo;
import static com.bme.smarthome.UserDetails.Checkinout;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.futronictech.Scanner;
import com.futronictech.UsbDeviceDataExchangeImpl;
import com.futronictech.ftrWsqAndroidHelper;
import com.machinezoo.sourceafis.FingerprintImage;
import com.machinezoo.sourceafis.FingerprintImageOptions;
import com.machinezoo.sourceafis.FingerprintMatcher;
import com.machinezoo.sourceafis.FingerprintTemplate;

import java.io.File;
import java.io.FileOutputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class MainActivity extends AppCompatActivity {
    public static final int MESSAGE_SHOW_MSG = 1;
    public static final int MESSAGE_SHOW_SCANNER_INFO = 2;
    public static final int MESSAGE_SHOW_IMAGE = 3;
    public static final int MESSAGE_ERROR = 4;
    public static final int MESSAGE_TRACE = 5;
    // Intent request codes
    private static final int REQUEST_FILE_FORMAT = 1;
    public static boolean mStop = false;
    public static boolean mFrame = true;
    public static boolean mLFD = false;
    public static boolean mInvertImage = false;
    public static boolean mNFIQ = false;
    public static byte[] mImageFP = null;
    public static Object mSyncObj = new Object();
    public static int mImageWidth = 0;
    public static int mImageHeight = 0;
    //
    public static boolean mUsbHostMode = true;
    /**
     * Called when the activity is first created.
     */
    public static Button mButtonScan;
    public static Button mButtonStop;
    public static TextView mMessage;
    public static TextView mScannerInfo;
    public static ImageView mFingerImage;
    public static ImageView mWS;
    public static int[] mPixels = null;
    public static Bitmap mBitmapFP = null;
    public static Canvas mCanvas = null;
    public static Paint mPaint = null;
    public static Button mButtonSave;
    public static CheckBox mCheckUsbHostMode;
    public static FPScan mFPScan = null;
    public static UsbDeviceDataExchangeImpl usb_host_ctx = null;
    private static File SyncDir = null;
    public CheckBox mCheckFrame;
    public CheckBox mCheckLFD;
    public CheckBox mCheckInvertImage;
    public CheckBox mCheckNFIQ;
    public static TextView mName;
    public static TextView mNameFP;
    public static TextView mIDe;

    public static UserDetails[] candidates;
    public static TextView mTB;
    MyAsyncTask myAsyncTask;
    UserDetails result;

    private static int _ok = 0;
    static Handler mhandler = new Handler();

    public static void InitFingerPictureParameters(int wight, int height) {
        mImageWidth = wight;
        mImageHeight = height;

        mImageFP = new byte[MainActivity.mImageWidth * MainActivity.mImageHeight];
        mPixels = new int[MainActivity.mImageWidth * MainActivity.mImageHeight];

        mBitmapFP = Bitmap.createBitmap(wight, height, Bitmap.Config.RGB_565);

        mCanvas = new Canvas(mBitmapFP);
        mPaint = new Paint();

        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        mPaint.setColorFilter(f);
    }

    private static void ShowBitmap() {
        for (int i = 0; i < mImageWidth * mImageHeight; i++) {
            mPixels[i] = Color.rgb(mImageFP[i], mImageFP[i], mImageFP[i]);
        }

        mCanvas.drawBitmap(mPixels, 0, mImageWidth, 0, 0, mImageWidth, mImageHeight, false, mPaint);

        mFingerImage.setImageBitmap(mBitmapFP);
        mFingerImage.invalidate();

        synchronized (mSyncObj) {
            mSyncObj.notifyAll();
        }
    }

    public static UserDetails find(FingerprintTemplate probe, UserDetails[] candidates)  {
        FingerprintMatcher matcher = new FingerprintMatcher(probe);
        UserDetails match = null;
        double high = 0;
        for (UserDetails candidate : candidates) {
            double score = matcher.match(candidate.template);
            if (score > high) {
                high = score;
                match = candidate;
            }
        }
        double threshold = 60;
        return high >= threshold ? match : null;
    }

//    public static boolean StartScan() {
//        mFPScan = new FPScan(usb_host_ctx, SyncDir, mHandler);
//        mFPScan.start();
//        return true;
//    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mFrame = true;
        mUsbHostMode = true;
        mLFD = mInvertImage = false;
        mButtonScan = findViewById(R.id.btnScan);
        mButtonStop = findViewById(R.id.btnStop);
        mButtonSave = findViewById(R.id.btnSave);
        mMessage = findViewById(R.id.tvMessage);
        mScannerInfo = findViewById(R.id.tvScannerInfo);
        mFingerImage = findViewById(R.id.imageFinger);
        mCheckFrame = findViewById(R.id.cbFrame);
        mCheckLFD = findViewById(R.id.cbLFD);
        mCheckInvertImage = findViewById(R.id.cbInvertImage);
        mCheckUsbHostMode = findViewById(R.id.cbUsbHostMode);
        mCheckNFIQ = findViewById(R.id.cbNFIQ);
        mTB = findViewById(R.id.tvTB);
        mName = findViewById(R.id.tvName);
        mNameFP = findViewById(R.id.tvNameFP);
        mIDe = findViewById(R.id.tvIDE);
        Button mUpdate = findViewById(R.id.btnUpdate);
        mWS = findViewById(R.id.imgWS);
        myAsyncTask = new MyAsyncTask(MainActivity.this);

        usb_host_ctx = new UsbDeviceDataExchangeImpl(this, mHandler);
        SyncDir = this.getExternalFilesDir(null);

        if (!isStoragePermissionGranted()) {
            mButtonScan.setEnabled(false);
        }

        ConnectDB();

        myAsyncTask.execute();


        mButtonScan.setOnClickListener(v -> {
            if (mFPScan != null) {
                mStop = true;
                mFPScan.stop();

            }
            mStop = false;
            if (mUsbHostMode) {
                usb_host_ctx.CloseDevice();
                if (usb_host_ctx.OpenDevice(0, true)) {
                    if (StartScan()) {
                        mButtonScan.setEnabled(false);
                        mButtonSave.setEnabled(false);
                        mCheckUsbHostMode.setEnabled(false);
                        mButtonStop.setEnabled(true);
                    }
                } else {
                    if (!usb_host_ctx.IsPendingOpen()) {
                        mMessage.setText("Can not start scan operation.\nCan't open scanner device");
                    }
                }
            } else {
                if (StartScan()) {
                    mButtonScan.setEnabled(false);
                    mButtonSave.setEnabled(false);
                    mCheckUsbHostMode.setEnabled(false);
                    mButtonStop.setEnabled(true);
                }
            }
        });

        mButtonStop.setOnClickListener(v -> {
            mStop = true;
            if (mFPScan != null) {
                mFPScan.stop();
                mFPScan = null;

            }
            mButtonScan.setEnabled(true);
            mButtonSave.setEnabled(true);
            mCheckUsbHostMode.setEnabled(true);
            mButtonStop.setEnabled(false);
        });

        mButtonSave.setOnClickListener(v -> {
            if (mImageFP != null)
                SaveImage();
        });


        mCheckFrame.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isChecked())
                mFrame = true;
            else {
                mFrame = false;
                mCheckLFD.setChecked(false);
                mLFD = false;
            }
        });


        mCheckLFD.setOnCheckedChangeListener((buttonView, isChecked) -> mLFD = buttonView.isChecked());

        mCheckInvertImage.setOnCheckedChangeListener((buttonView, isChecked) -> mInvertImage = buttonView.isChecked());

        mCheckUsbHostMode.setOnCheckedChangeListener((buttonView, isChecked) -> mUsbHostMode = buttonView.isChecked());

        mCheckNFIQ.setOnCheckedChangeListener((buttonView, isChecked) -> mNFIQ = buttonView.isChecked());

        mUpdate.setOnClickListener(v -> UpdateInfo());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mStop = true;
        if (mFPScan != null) {
            mFPScan.stop();
            mFPScan = null;
        }
        usb_host_ctx.CloseDevice();
        usb_host_ctx.Destroy();
        usb_host_ctx = null;
    }

    public boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                //Log.v(TAG,"Permission is granted");
                return true;
            } else {
                //Log.v(TAG,"Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            //Log.v(TAG,"Permission is granted");
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            //Log.v("FtrScanDemoUsbHost","Permission: "+permissions[0]+ "was "+grantResults[0]);
            //Log.v("FtrScanDemoUsbHost","Permission: "+permissions[1]+ "was "+grantResults[1]);
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //resume tasks needing this permission
                mButtonScan.setEnabled(true);
            }
        }
    }

    private boolean StartScan() {
        mFPScan = new FPScan(usb_host_ctx, SyncDir, mHandler);
        mFPScan.start();
        return true;
    }

    private void SaveImage() {
        Intent serverIntent = new Intent(this, SelectFileFormatActivity.class);
        startActivityForResult(serverIntent, REQUEST_FILE_FORMAT);
    }

    private void SaveImageByFileFormat(String fileFormat, String fileName) {
        if (fileFormat.compareTo("WSQ") == 0)    //save wsq file
        {
            Scanner devScan = new Scanner();
            boolean bRet;
            if (mUsbHostMode)
                bRet = devScan.OpenDeviceOnInterfaceUsbHost(usb_host_ctx);
            else
                bRet = devScan.OpenDevice();
            if (!bRet) {
                mMessage.setText(devScan.GetErrorMessage());
                return;
            }
            byte[] wsqImg = new byte[mImageWidth * mImageHeight];
            long hDevice = devScan.GetDeviceHandle();
            ftrWsqAndroidHelper wsqHelper = new ftrWsqAndroidHelper();
            if (wsqHelper.ConvertRawToWsq(hDevice, mImageWidth, mImageHeight, 2.25f, mImageFP, wsqImg)) {
                File file = new File(fileName);
                try {
                    FileOutputStream out = new FileOutputStream(file);
                    out.write(wsqImg, 0, wsqHelper.mWSQ_size);    // save the wsq_size bytes data to file
                    out.close();
                    mMessage.setText("Image is saved as " + fileName);
                } catch (Exception e) {
                    mMessage.setText("Exception in saving file");
                }
            } else
                mMessage.setText("Failed to convert the image!");
            if (mUsbHostMode)
                devScan.CloseDeviceUsbHost();
            else
                devScan.CloseDevice();
            return;
        }
        // 0 - save bitmap file
        File file = new File(fileName);
        try {
            FileOutputStream out = new FileOutputStream(file);
            //mBitmapFP.compress(Bitmap.CompressFormat.PNG, 90, out);
            MyBitmapFile fileBMP = new MyBitmapFile(mImageWidth, mImageHeight, mImageFP);
            out.write(fileBMP.toBytes());
            out.close();
            mMessage.setText("Image is saved as " + fileName);
        } catch (Exception e) {
            mMessage.setText("Exception in saving file");
        }
    }    // The Handler that gets information back from the FPScan

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_FILE_FORMAT:
                if (resultCode == Activity.RESULT_OK) {
                    // Get the file format
                    String[] extraString = data.getExtras().getStringArray(SelectFileFormatActivity.EXTRA_FILE_FORMAT);
                    String fileFormat = extraString[0];
                    String fileName = extraString[1];
                    SaveImageByFileFormat(fileFormat, fileName);

                    String fName = extraString[2];
                    String mName = extraString[3];
                    try {
                        Statement statement = connection.createStatement();

                        ResultSet resultSet = statement.executeQuery("SELECT id FROM EmployeeInfo WHERE name=N'" + mName + "';");
                        resultSet.next();

                        PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO Fingerprint (namefp,fp,idE) VALUES (?,?,?)");
                        preparedStatement.setString(1, fName);
                        preparedStatement.setBytes(2, mImageFP);
                        preparedStatement.setString(3, resultSet.getString(1));

                        preparedStatement.execute();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else
                    mMessage.setText("Cancelled!");
                break;
        }
    }    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_SHOW_MSG:
                    String showMsg = (String) msg.obj;
                    mMessage.setText(showMsg);
                    if (showMsg.equals("OK")) {
                        _ok = 1;
//                        WS.setVisibility(View.GONE);
                    }

                    if (showMsg.equals("Empty Frame")) {
                        if (_ok == 1) {
                            _ok = 0;
//                            GB.setVisibility(View.VISIBLE);
//                            GB.startAnimation(animation2);
                            mTB.setText("tìm van tay");
                            mName.setText("");
                            if (mImageFP != null) {
                                ConnectDB();
                                mWS.setVisibility(View.GONE);
                                MatchFP matchFP = new MatchFP();
                                matchFP.execute();

//                                MyBitmapFile fileBMP = new MyBitmapFile(mImageWidth, mImageHeight, mImageFP);
//
//                                FingerprintTemplate probe = new FingerprintTemplate(new FingerprintImage(fileBMP.toBytes(), new FingerprintImageOptions().dpi(500)));
//                                String path = Environment.getExternalStorageDirectory().toString() + "/Android/data/com.bme.smarthome/files/FPfile/";
//                                Log.d("Files", "Path: " + path);
//                                File directory = new File(path);
//                                if(directory.exists()){
//                                    UserDetails result = find(probe, candidates);
//
//                                    if (result != null) {
//                                        mNameFP.setText(result.name);
//                                        CheckEInfo();
//                                        Checkinout();
//                                        mWS.setVisibility(View.GONE);
//                                        mhandler.removeCallbacks(UserDetails::TimeOut);
//                                        mhandler.postDelayed(UserDetails::TimeOut,15000);
//                                    }
//                                    if (result == null) {
////                                        mHandler.removeCallbacks(mRunnable);
//                                        mTB.setText("khong thay van tay");
//                                        mWS.setVisibility(View.GONE);
//                                        mhandler.removeCallbacks(UserDetails::TimeOut);
//                                        mhandler.postDelayed(UserDetails::TimeOut,15000);
//                                    }
//                                }
                            }
                        }
                    }
                    break;
                case MESSAGE_SHOW_SCANNER_INFO:
                    String showInfo = (String) msg.obj;
                    mScannerInfo.setText(showInfo);
                    break;
                case MESSAGE_SHOW_IMAGE:
                    ShowBitmap();
                    break;
                case MESSAGE_ERROR:
                    //mFPScan = null;
                    mButtonScan.setEnabled(true);
                    mCheckUsbHostMode.setEnabled(true);
                    mButtonStop.setEnabled(false);
                    break;
                case UsbDeviceDataExchangeImpl.MESSAGE_ALLOW_DEVICE:
                    if (usb_host_ctx.ValidateContext()) {
                        if (StartScan()) {
                            mButtonScan.setEnabled(false);
                            mButtonSave.setEnabled(false);
                            mCheckUsbHostMode.setEnabled(false);
                            mButtonStop.setEnabled(true);
                        }
                    } else
                        mMessage.setText("Can't open scanner device");
                    break;
                case UsbDeviceDataExchangeImpl.MESSAGE_DENY_DEVICE:
                    mMessage.setText("User deny scanner device");
                    break;
            }
        }
    };

    public class MatchFP extends AsyncTask<UserDetails,Void,Void>{

        @Override
        protected Void doInBackground(UserDetails... params) {
            MyBitmapFile fileBMP = new MyBitmapFile(mImageWidth, mImageHeight, mImageFP);

            FingerprintTemplate probe = new FingerprintTemplate(new FingerprintImage(fileBMP.toBytes(), new FingerprintImageOptions().dpi(500)));
            String path = Environment.getExternalStorageDirectory().toString() + "/Android/data/com.bme.smarthome/files/FPcache";
            Log.d("Files", "Path: " + path);
            File directory = new File(path);
            if (directory.exists()) {
                result = find(probe, candidates);

                if (result != null) {
                    mNameFP.setText(result.name);
                    CheckEInfo();
                    Checkinout();

                    mhandler.removeCallbacks(UserDetails::TimeOut);
                    mhandler.postDelayed(UserDetails::TimeOut,15000);
                }
                if (result == null) {
                    mTB.setText("khong thay van tay");

                    mhandler.removeCallbacks(UserDetails::TimeOut);
                    mhandler.postDelayed(UserDetails::TimeOut,15000);
                }
            }
            return null;
        }
    }
}