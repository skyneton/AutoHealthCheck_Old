package net.mpoisv.autohealth;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Calendar;
import java.util.Objects;

public class DeviceBootReceiver extends BroadcastReceiver {

    private String name, code, url;
    private int getHour, getMinute;

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        if(Objects.equals(intent.getAction(), "android.intent.action.BOOT_COMPLETED")) {

            ServiceManager.alarmManager = (AlarmManager) context.getApplicationContext().getSystemService(Context.ALARM_SERVICE);
            ServiceManager.intent = new Intent(context.getApplicationContext(), ServiceManager.class);
            ServiceManager.alarmIntent = PendingIntent.getService(context.getApplicationContext(), 0, ServiceManager.intent, PendingIntent.FLAG_UPDATE_CURRENT);

//            DontAppClose(context.getApplicationContext());
            Calendar calendar = Calendar.getInstance();

            readData(new File(context.getApplicationContext().getExternalCacheDir(), "config"));

            calendar.set(Calendar.HOUR_OF_DAY, getHour);
            calendar.set(Calendar.MINUTE, getMinute);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            if(calendar.getTimeInMillis() <= System.currentTimeMillis())
                calendar.add(Calendar.DATE, 1);


            ServiceManager.userDataSetting(name, code, url);
            ServiceManager.timeDataSetting(getHour, getMinute);

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                context.getApplicationContext().startForegroundService(ServiceManager.intent);
            else
                context.getApplicationContext().startService(ServiceManager.intent);
        }
    }


    private byte[] key = { 0x13, 0x31, 0x21, 0x11, 0x33, 0x14, 0x12, 0x22, 0x06, 0x09, 0x19, 0x02, 0x32 };

    private void readData(File file) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            int num = 0;
            while((line = reader.readLine()) != null) {
                num++;
                if(num == 1) {
                    name = line;
                    continue;
                } else if(num == 3) {
                    getHour = Integer.parseInt(line.split(":")[0]);
                    getMinute = Integer.parseInt(line.split(":")[1]);
                    continue;
                } else if(num == 4) {
                    url = line;
                    break;
                }
                byte[] buf = line.getBytes();
                byte[] result = new byte[buf.length];

                for(int i = 0; i < buf.length; i++)
                    result[i] = (byte) (buf[i] ^ key[i % key.length]);

                if(num == 2) {
                    code = new String(result);
                }
            }

            reader.close();
        }catch(Exception e) { }
    }
}
