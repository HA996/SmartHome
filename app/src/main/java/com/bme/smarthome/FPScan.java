package com.bme.smarthome;

import static com.bme.smarthome.MainActivity.*;

import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;

import com.futronictech.Scanner;
import com.futronictech.UsbDeviceDataExchangeImpl;

import java.io.File;

public class FPScan {
    private final Handler mHandler;
    private ScanThread mScanThread;
    private UsbDeviceDataExchangeImpl ctx = null;
    private File mDirSync;

    public FPScan(UsbDeviceDataExchangeImpl context, File dirSync, Handler handler ) {
        mHandler = handler;
        ctx = context;
        mDirSync = dirSync;
    }

    public synchronized void start() {
        if (mScanThread == null) {
            mScanThread = new ScanThread();
            mScanThread.start();
        }
    }

    public synchronized void stop() {
        if (mScanThread != null) {mScanThread.cancel(); mScanThread = null;}
    }

    private class ScanThread extends Thread {
        private boolean bGetInfo;
        private Scanner devScan = null;
        private String strInfo;
        private int mask, flag;
        private int errCode;
        private boolean bRet;
        private int nNfiq = 0;

        public ScanThread() {
            bGetInfo=false;
            devScan = new Scanner();
            // set the GlobalSyncDir to 'getExternalFilesDir'
            // *** Returns the absolute path to the directory on the primary shared/external storage device where the application can place persistent files it owns. *** //
            if( !devScan.SetGlobalSyncDir(mDirSync.toString()) )
            {
                mHandler.obtainMessage(MainActivity.MESSAGE_SHOW_MSG, -1, -1, devScan.GetErrorMessage()).sendToTarget();
                mHandler.obtainMessage(MainActivity.MESSAGE_ERROR).sendToTarget();
                devScan = null;
                return;
            }
            /******************************************
             // By default, a directory of "/mnt/sdcard/Android/" is necessary for libftrScanAPI.so to work properly
             // in case you want to change it, you can set it by calling the below function
             String SyncDir =  "/mnt/sdcard/test/";  // YOUR DIRECTORY
             if( !devScan.SetGlobalSyncDir(SyncDir) )
             {
             mHandler.obtainMessage(FtrScanDemoActivity.MESSAGE_SHOW_MSG, -1, -1, devScan.GetErrorMessage()).sendToTarget();
             mHandler.obtainMessage(FtrScanDemoActivity.MESSAGE_ERROR).sendToTarget();
             devScan = null;
             return;
             }
             *******************************************/
        }

        public void run() {
            while (!MainActivity.mStop)
            {
                if(!bGetInfo)
                {
                    Log.i("FUTRONIC", "Run fp scan");
                    boolean bRet;
                    if( mUsbHostMode )
                        bRet = devScan.OpenDeviceOnInterfaceUsbHost(ctx);
                    else
                        bRet = devScan.OpenDevice();
                    if( !bRet )
                    {
                        //Toast.makeText(this, strInfo, Toast.LENGTH_LONG).show();
                        if(mUsbHostMode)
                            ctx.CloseDevice();
                        mHandler.obtainMessage(MainActivity.MESSAGE_SHOW_MSG, -1, -1, devScan.GetErrorMessage()).sendToTarget();
                        mHandler.obtainMessage(MainActivity.MESSAGE_ERROR).sendToTarget();
                        return;
                    }

                    if( !devScan.GetImageSize() )
                    {
                        mHandler.obtainMessage(MainActivity.MESSAGE_SHOW_MSG, -1, -1, devScan.GetErrorMessage()).sendToTarget();
                        if( mUsbHostMode )
                            devScan.CloseDeviceUsbHost();
                        else
                            devScan.CloseDevice();
                        mHandler.obtainMessage(MainActivity.MESSAGE_ERROR).sendToTarget();
                        return;
                    }

                    MainActivity.InitFingerPictureParameters(devScan.GetImageWidth(), devScan.GetImaegHeight());

                    strInfo = devScan.GetVersionInfo();
                    mHandler.obtainMessage(MainActivity.MESSAGE_SHOW_SCANNER_INFO, -1, -1, strInfo).sendToTarget();
                    bGetInfo = true;
                }
                //set options
                flag = 0;
                mask = devScan.FTR_OPTIONS_DETECT_FAKE_FINGER | devScan.FTR_OPTIONS_INVERT_IMAGE;
                if(MainActivity.mLFD)
                    flag |= devScan.FTR_OPTIONS_DETECT_FAKE_FINGER;
                if( MainActivity.mInvertImage)
                    flag |= devScan.FTR_OPTIONS_INVERT_IMAGE;
                if( !devScan.SetOptions(mask, flag) )
                    mHandler.obtainMessage(MainActivity.MESSAGE_SHOW_MSG, -1, -1, devScan.GetErrorMessage()).sendToTarget();
                // get frame / image2
                long lT1 = SystemClock.uptimeMillis();
                if( MainActivity.mFrame )
                    bRet = devScan.GetFrame(MainActivity.mImageFP);
                else
                    bRet = devScan.GetImage2(4,MainActivity.mImageFP);
                if( !bRet )
                {
                    mHandler.obtainMessage(MainActivity.MESSAGE_SHOW_MSG, -1, -1, devScan.GetErrorMessage()).sendToTarget();
                    errCode = devScan.GetErrorCode();
                    if( errCode != devScan.FTR_ERROR_EMPTY_FRAME && errCode != devScan.FTR_ERROR_MOVABLE_FINGER &&  errCode != devScan.FTR_ERROR_NO_FRAME )
                    {
                        if( mUsbHostMode )
                            devScan.CloseDeviceUsbHost();
                        else
                            devScan.CloseDevice();
                        mHandler.obtainMessage(MainActivity.MESSAGE_ERROR).sendToTarget();
                        return;
                    }
                }
                else
                {
                    if( MainActivity.mNFIQ )
                    {
                        if( devScan.GetNfiqFromImage(MainActivity.mImageFP, MainActivity.mImageWidth, MainActivity.mImageHeight))
                            nNfiq = devScan.GetNIFQValue();
                    }
                    if( MainActivity.mFrame )
                        strInfo = String.format("OK",  SystemClock.uptimeMillis()-lT1);
                    else
                        strInfo = String.format("OK",  SystemClock.uptimeMillis()-lT1);
                    if( MainActivity.mNFIQ )
                    {
                        strInfo = strInfo + String.format("NFIQ=%d", nNfiq);
                    }
                    mHandler.obtainMessage(MainActivity.MESSAGE_SHOW_MSG, -1, -1, strInfo ).sendToTarget();
                }
                synchronized (MainActivity.mSyncObj)
                {
                    //show image
                    mHandler.obtainMessage(MainActivity.MESSAGE_SHOW_IMAGE).sendToTarget();
                    try {
                        MainActivity.mSyncObj.wait(2000);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
            //close device
            if( mUsbHostMode )
                devScan.CloseDeviceUsbHost();
            else
                devScan.CloseDevice();
        }

        public void cancel() {
            MainActivity.mStop = true;
            try {
                synchronized (MainActivity.mSyncObj)
                {
                    MainActivity.mSyncObj.notifyAll();
                }
                this.join();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
    }

//    public static void AutoScan(){
//        if (mFPScan != null) {
//            mStop = true;
//            mFPScan.stop();
//
//        }
//        mStop = false;
//        if (mUsbHostMode) {
//            usb_host_ctx.CloseDevice();
//            if (usb_host_ctx.OpenDevice(0, true)) {
//                if (StartScan()) {
//                    mButtonScan.setEnabled(false);
//                    mButtonSave.setEnabled(false);
//                    mCheckUsbHostMode.setEnabled(false);
//                    mButtonStop.setEnabled(true);
//                }
//            } else {
//                if (!usb_host_ctx.IsPendingOpen()) {
//                    mMessage.setText("Can not start scan operation.\nCan't open scanner device");
//                }
//            }
//        } else {
//            if (StartScan()) {
//                mButtonScan.setEnabled(false);
//                mButtonSave.setEnabled(false);
//                mCheckUsbHostMode.setEnabled(false);
//                mButtonStop.setEnabled(true);
//            }
//        }
//    };
}

