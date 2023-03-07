package com.bme.smarthome;

import static com.bme.smarthome.DBHelper.connection;
import static com.bme.smarthome.MainActivity.*;
import static com.bme.smarthome.SelectFileFormatActivity.mEid;

import android.view.View;

import com.machinezoo.sourceafis.FingerprintTemplate;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class UserDetails {
    public String name;
    public FingerprintTemplate template;
    public Byte fpdata;
    public static MainActivity scan;

    public static void CheckEInfo() {
        try {
            Statement statement = connection.createStatement();

            ResultSet resultSet = statement.executeQuery("SELECT idE FROM Fingerprint WHERE namefp=N'" + mNameFP.getText().toString() + "';");
            resultSet.next();
            mIDe.setText(resultSet.getString(1));
            if (mIDe != null) {
                ResultSet resultSet1 = statement.executeQuery("SELECT * FROM EmployeeInfo WHERE id=" + resultSet.getString(1) + ";");
                resultSet1.next();
                mName.setText(resultSet1.getString(2));
//                if (mName != null) {
//                    ResultSet resultSet2 = statement.executeQuery("SELECT permission FROM EmployeeInfo WHERE name=N'" + mName.getText().toString() + "';");
//                    resultSet2.next();
//                    if (resultSet2.getInt(1) == 1) {
//                        mButtonStop.setVisibility(View.VISIBLE);
//                        mButtonSave.setVisibility(View.VISIBLE);
//                        mButtonScan.setVisibility(View.VISIBLE);
//                        mTB.setText("Quyền Admin");
//                    } else {
//                        WS.setVisibility(View.GONE);
//                        mButtonStop.setVisibility(View.GONE);
//                        mButtonSave.setVisibility(View.GONE);
//                        mButtonGate1.setVisibility(View.VISIBLE);
//                        mButtonGate2.setVisibility(View.VISIBLE);
//                        mButtonGate3.setVisibility(View.VISIBLE);
//                        mButtonGate4.setVisibility(View.VISIBLE);
//                        mButtonCheckin.setVisibility(View.VISIBLE);
//                        mButtonCheckout.setVisibility(View.VISIBLE);
//                    }
//                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void Checkinout(){
        Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
        String strDate = sdf.format(c.getTime());
        SimpleDateFormat sdf1 = new SimpleDateFormat("MM");
        String strMonth = sdf1.format(c.getTime());
        SimpleDateFormat sdf2 = new SimpleDateFormat("HH:mm:ss");
        String strTime = sdf2.format(c.getTime());
        mTB.setText("Check in/out Thành Công Lúc " + strTime + "!!!");

        try {
            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO CheckinoutInfo (name, timecheckinout, datecheckinout, month) VALUES(?,?,?,?)");
            preparedStatement.setString(1, mName.getText().toString());
            preparedStatement.setString(2, strTime);
            preparedStatement.setString(3, strDate);
            preparedStatement.setString(4, strMonth);

            preparedStatement.execute();
            preparedStatement.close();
        } catch (Exception ex) {
            ex.printStackTrace();

        }
//        try {
//            connection.close();
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
    }

    public static void TimeOut(){
        mWS.setVisibility(View.VISIBLE);
        mName.setText("");
        mTB.setText("");
    }
}
