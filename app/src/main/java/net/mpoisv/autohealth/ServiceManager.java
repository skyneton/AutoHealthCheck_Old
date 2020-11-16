package net.mpoisv.autohealth;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Picture;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class ServiceManager extends IntentService {

    public static AlarmManager alarmManager;
    public static Intent intent;
    public static PendingIntent alarmIntent;


    private static String name, code, url;
    private static int getHour, getMinute;

    private static LayoutInflater li;
    private static WindowManager windowManager;
    private static WindowManager.LayoutParams params;
    private static LinearLayout layout;
    private static WebView driver;
    private static WebSettings setting;

    private static int codeInput = 0;

    private boolean Once = true;

    private int ScreenOnce = 0, reNum = 0, alertNum = 0;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public ServiceManager() {
        super("ServiceManager");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        if(Once) { Once = false; return; }


        Calendar nextAlarm = Calendar.getInstance();

        nextAlarm.set(Calendar.HOUR_OF_DAY, getHour);
        nextAlarm.set(Calendar.MINUTE, getMinute);
        nextAlarm.set(Calendar.SECOND, 0);
        nextAlarm.add(Calendar.DATE, 1);


        switch(Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
            case 1: //일요일
                return;
            case 7: //토요일
                return;
        }

//        render(nextAlarm);

        PowerManager pm = (PowerManager) getApplicationContext().getSystemService(POWER_SERVICE);
        PowerManager.WakeLock sCpuWakeLock = pm.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK
                | PowerManager.ACQUIRE_CAUSES_WAKEUP
                | PowerManager.ON_AFTER_RELEASE, "AutoHealth:screenOn");

        sCpuWakeLock.acquire();
        sCpuWakeLock.release();

        new Activity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                WebSetting();

                windowManager.addView(layout, params);

                driver.clearHistory();
                driver.clearCache(true);
                driver.clearView();
                android.webkit.CookieManager.getInstance().removeAllCookie();

                if(url == null | url.replaceAll(" ", "").equalsIgnoreCase("") | url.replaceAll(" ", "").equalsIgnoreCase("https://"))
                    driver.loadUrl("https://eduro.sen.go.kr/hcheck/index.jsp");
                else
                    driver.loadUrl(url);
            }
        });

    }

    public static void userDataSetting(String n, String c, String u) {
        name = n;
        code = c;
        url = u;
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {

        startForegroundService();

        Calendar date = Calendar.getInstance();

        date.set(Calendar.HOUR_OF_DAY, getHour);
        date.set(Calendar.MINUTE, getMinute);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);

        if(date.getTimeInMillis() <= System.currentTimeMillis())
            date.add(Calendar.DATE, 1);

        render(date);


        return super.onStartCommand(intent, flags, START_REDELIVER_INTENT);
    }

    private void WebSetting() {
        li = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.LEFT | Gravity.TOP;

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        layout = (LinearLayout) li.inflate(R.layout.layout, null);

        driver = layout.findViewById(R.id.webview);

        ScreenOnce = reNum = alertNum = 0;

        driver.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if(driver.getProgress() == 100)
                    automaticWeb();
            }

            @Override
            public void onFormResubmission(WebView view, Message dontResend, Message resend) {
                resend.sendToTarget();
                super.onFormResubmission(view, dontResend, resend);
            }
        });

        driver.setWebChromeClient(new WebChromeClient() {

            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                if(alertNum++ > 10) windowManager.removeView(layout);
                else driver.reload();
                return super.onJsAlert(view, url, message, result);
            }
        });

        setting = driver.getSettings();
        setting.setJavaScriptEnabled(true);
        setting.setDomStorageEnabled(true);
        setting.setSupportMultipleWindows(false);
        setting.setJavaScriptCanOpenWindowsAutomatically(false);
        setting.setLoadWithOverviewMode(true);
        setting.setSupportZoom(false);
        setting.setBuiltInZoomControls(false);
    }

    private void automaticWeb() {
        driver.evaluateJavascript(
                "(function() { try {return document.getElementsByClassName('content_box')[0].innerText;} catch { return (document.getElementsByClassName('btn_confirm')[0] != null).toString(); } })();",

                new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String value) {
                        String data = value.replaceAll("\"", "");
                        if (data.equalsIgnoreCase("true"))
                            driver.loadUrl("https://eduro.sen.go.kr/stv_cvd_co00_010.do");
                        else if(data.contains("* 표시된 항목은 필수입력사항입니다.")) {
                            if(codeInput++ > 10) windowManager.removeView(layout);

                            else {
                                driver.evaluateJavascript(
                                        "document.getElementById('pName').value = '" + name + "';" +
                                                "document.getElementById('qstnCrtfcNo').value = '" + code + "';" +
                                                "document.getElementById('btnConfirm').click();",
                                        new ValueCallback<String>() {
                                            @Override
                                            public void onReceiveValue(String value) {
                                            }
                                        });
                            }
                        }
                        else if(data.contains("1. 학생의 몸에 열이 있나요")) {
                            driver.evaluateJavascript(
                                    "document.getElementById('rspns011').checked = true;" +
                                            "document.getElementById('rspns02').checked = true;" +
                                            "document.getElementById('rspns070').checked = true;" +
                                            "document.getElementById('rspns080').checked = true;" +
                                            "document.getElementById('rspns090').checked = true;" +
                                            "document.getElementById('btnConfirm').click();",
                                    new ValueCallback<String>() {
                                        @Override
                                        public void onReceiveValue(String value) {
                                        }
                                    });
                        }
                        else if(data.contains("자가진단 참여를 완료하였습니다.")) {
                            if(ScreenOnce++ < 5) { driver.reload(); }
                            else {
                                savePicture(driver.capturePicture());

                                windowManager.removeView(layout);

                            }
                        }else if(data.contains("본 화면은 비정상적인 접속 시 안내되는 화면입니다.")) {
                            if(url == null | url.replaceAll(" ", "").equalsIgnoreCase("") | url.replaceAll(" ", "").equalsIgnoreCase("https://"))
                                driver.loadUrl("https://eduro.sen.go.kr/hcheck/index.jsp");
                            else
                                driver.loadUrl(url);
                        }else {
                            if(reNum++ > 10) windowManager.removeView(layout);
                            else {
                                driver.reload();
                            }
                        }
                    }
                }
        );
    }

    public void savePicture(Picture picture) {
        if(picture.getHeight() <= 0 | picture.getWidth() <= 0) return;

        Bitmap b = Bitmap.createBitmap(picture.getWidth(), picture.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        picture.draw(c);

        File file = new File(getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES), "HealthScreenShot");
        if(!file.exists()) file.mkdirs();

        String fileName = new SimpleDateFormat("yyyy년 MM월 dd일 hh시 mm분 ss초", Locale.getDefault()).format(System.currentTimeMillis())+".jpg";

        try {
            FileOutputStream fos = new FileOutputStream(file.getAbsolutePath()+"/"+fileName);
            b.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();

            NotificationFile(fileName);
        }catch(Exception e) { }
    }

    public static String getName() {
        return name;
    }

    public static void timeDataSetting(int h, int m) {
        getHour = h;
        getMinute = m;
    }

    void startForegroundService() {
        Calendar date = Calendar.getInstance();

        date.set(Calendar.HOUR_OF_DAY, getHour);
        date.set(Calendar.MINUTE, getMinute);
        date.set(Calendar.SECOND, 0);

        if(date.getTimeInMillis() <= System.currentTimeMillis())
            date.add(Calendar.DATE, 1);

        switch(date.get(Calendar.DAY_OF_WEEK)) {
            case 1: //일요일
                date.add(Calendar.DATE, 1);
                break;
            case 7: //토요일
                date.add(Calendar.DATE, 2);
                break;
        }

        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.activity_main);

        NotificationCompat.Builder builder;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String CHANNEL_ID = "default";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "다음 자동화 시간 알림", NotificationManager.IMPORTANCE_NONE);
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(channel);

            builder = new NotificationCompat.Builder(this, CHANNEL_ID);
        }else
            builder = new NotificationCompat.Builder(this);

        builder.setSmallIcon(R.mipmap.ic_launcher)
                .setContent(remoteViews)
                .setContentTitle("자가검진 자동화 - 다음시간")
                .setContentText(new SimpleDateFormat("MM월 dd일 EE요일 a hh시 mm분", Locale.getDefault()).format(date.getTime()));

        startForeground(1, builder.build());
    }

    public void Notification(Calendar date) {

        switch(date.get(Calendar.DAY_OF_WEEK)) {
            case Calendar.SUNDAY:
                date.add(Calendar.DATE, 1);
                break;
            case Calendar.SATURDAY: //토요일
                date.add(Calendar.DATE, 2);
                break;
        }

        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder builder;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("default", "다음 자동화 시간 알림", NotificationManager.IMPORTANCE_NONE);
            notificationManager.createNotificationChannel(channel);

            builder = new NotificationCompat.Builder(getApplicationContext(), "default");
        }else
            builder = new NotificationCompat.Builder(getApplicationContext());

        PendingIntent notifiPendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, new Intent(getApplicationContext(), MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

        builder.setContentTitle("자가검진 자동화 - 다음 시간")
                .setContentIntent(notifiPendingIntent)
                .setContentText(new SimpleDateFormat("MM월 dd일 EE요일 a hh시 mm분", Locale.getDefault()).format(date.getTime()))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true);

        notificationManager.notify(1234, builder.build());
    }

    public void NotificationFile(String image) {
        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder builder;

        String fileLoc = new File(getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES), "HealthScreenShot").getAbsolutePath();

        Uri uri = Uri.parse("file://"+fileLoc+"/"+image);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "image/*");
//        intent.setDataAndType(uri, "resource/image");

        PendingIntent notifiPendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("date", "자동화 시간", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);

            builder = new NotificationCompat.Builder(getApplicationContext(), "date");
        }else
            builder = new NotificationCompat.Builder(getApplicationContext());


        builder.setContentTitle(new SimpleDateFormat("MM월 dd일 EE요일 a hh시 mm분 ss초 - 실행됨").format(System.currentTimeMillis()))
                .setContentIntent(notifiPendingIntent)
                .setContentText("파일 위치: " + fileLoc)
                .setSmallIcon(R.mipmap.ic_launcher);

        notificationManager.notify(1230, builder.build());
    }

    private void render(Calendar calendar) {
        Notification(calendar);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), alarmIntent);
        else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), alarmIntent);
        else
            alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), alarmIntent);
    }
}
