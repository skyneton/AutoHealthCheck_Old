package net.mpoisv.autohealth;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputFilter;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    public static final int REQUEST_SYSTEM_ALERT_WINDOW = 1;

    Button btnSave, btnAutoStart;
    TimePicker autoTime;
    EditText edtName, edtCode, edtUrl;
    TextView alert;

    File file;

    String name = "", code = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(!Settings.canDrawOverlays(getApplicationContext())) {
                checkOverlayPermission();
            }
        }

        setTitle(R.string.title_name);

        init();

        file = new File(getExternalCacheDir(), "config");

        if(!file.exists()) {
            try{
                file.createNewFile();
                alert.setText("파일이 생성되었습니다. 위치: " + file.getAbsolutePath());
            }catch(Exception e) {
                alert.setText("파일 생성 실패");
            }
        }

        readData();

        if(ServiceManager.getName() != null) {
            Calendar calendar = Calendar.getInstance();

            calendar.set(Calendar.HOUR_OF_DAY, autoTime.getHour());
            calendar.set(Calendar.MINUTE, autoTime.getMinute());
            calendar.set(Calendar.SECOND, 0);

            alert.setText("자가검진 자동화가 시작됩니다.\n시간: " + new SimpleDateFormat("hh시 mm분", Locale.getDefault()).format(calendar.getTime()));
            btnAutoStart.setText("자동화 종료");
        }

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
    }

    private void checkOverlayPermission() {
        Uri uri = Uri.parse("package:" + getPackageName());
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, uri);

        startActivityForResult(intent, REQUEST_SYSTEM_ALERT_WINDOW);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch(requestCode) {
            case REQUEST_SYSTEM_ALERT_WINDOW:
                if(!Settings.canDrawOverlays(getApplicationContext()))
                    finish();
                break;
        }
    }

    private void init() {
        alert = findViewById(R.id.txtAlert);
        btnSave = findViewById(R.id.BtnSave);
        btnAutoStart = findViewById(R.id.BtnAutoStart);

        autoTime = findViewById(R.id.autoTime);

        edtName = findViewById(R.id.EdtName);
        edtCode = findViewById(R.id.EdtCode);
        edtUrl = findViewById(R.id.GoToUrl);

        edtName.setFilters(new InputFilter[] {filterKorea});
        edtCode.setFilters(new InputFilter[] {new InputFilter.LengthFilter(6), filterAlphaNumber});
        edtUrl.setFilters(new InputFilter[] {filterAlphaNumberPlus});

        btnSave.setOnClickListener(btnSaveClickListener);
        btnAutoStart.setOnClickListener(btnAutoClickListener);

        ServiceManager.alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        ServiceManager.intent = new Intent(getApplicationContext(), ServiceManager.class);
        ServiceManager.alarmIntent = PendingIntent.getService(getApplicationContext(), 0, ServiceManager.intent, PendingIntent.FLAG_UPDATE_CURRENT);

        edtName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                name = edtName.getText().toString();
            }
        });

        edtCode.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                code = edtCode.getText().toString();
            }
        });
    }

    Button.OnClickListener btnSaveClickListener = new Button.OnClickListener() {
        @Override
        public void onClick(View view) {
            if(name.length() < 2 | code.length() < 6)
                alert.setText("사용자 정보를 입력해주세요.");
            else {
                SaveData();
            }
        }
    };

    protected void SaveData() {

        byte[] buf = code.getBytes();
        byte[] result = new byte[buf.length];

        for(int i = 0; i < buf.length; i++)
            result[i] = (byte) (buf[i] ^ key[i % key.length]);

        String secCode = new String(result, StandardCharsets.UTF_8);

        String url = edtUrl.getText().toString();
        if(!url.contains("http://") && !url.contains("https://")) url = "https://"+url;

        writeData(name, secCode, url);
    }

    Button.OnClickListener btnAutoClickListener = new Button.OnClickListener() {
        @Override
        public void onClick(View view) {
            if(name.length() < 2 | code.length() < 6)
                alert.setText("사용자 정보를 입력해주세요.");
            else {
                if(btnAutoStart.getText().toString().equalsIgnoreCase("자동화 시작")) {
                    SaveData();


                    Calendar calendar = Calendar.getInstance();

                    calendar.set(Calendar.HOUR_OF_DAY, autoTime.getHour());
                    calendar.set(Calendar.MINUTE, autoTime.getMinute());
                    calendar.set(Calendar.SECOND, 0);
                    calendar.set(Calendar.MILLISECOND, 0);


                    if(calendar.getTimeInMillis() <= System.currentTimeMillis())
                        calendar.add(Calendar.DATE, 1);

                    String url = edtUrl.getText().toString();
                    if(!url.contains("http://") && !url.contains("https://")) url = "https://"+url;
                    ServiceManager.userDataSetting(name, code, url);
                    ServiceManager.timeDataSetting(autoTime.getHour(), autoTime.getMinute());

                    DontAppClose();

                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        startForegroundService(ServiceManager.intent);
                    else
                        startService(ServiceManager.intent);

                    alert.setText("자가검진 자동화가 시작됩니다.\n시간: " + new SimpleDateFormat("hh시 mm분", Locale.getDefault()).format(calendar.getTime()));
                    btnAutoStart.setText("자동화 종료");
                }else {
                    ServiceManager.alarmManager.cancel(ServiceManager.alarmIntent);
                    NotificationRemove();
                    AppClose();

                    ServiceManager.userDataSetting(null, null, null);
                    stopService(ServiceManager.intent);

                    alert.setText("자동화가 종료됩니다.");
                    btnAutoStart.setText("자동화 시작");
                }
            }
        }
    };

    protected void NotificationRemove() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }

    protected void DontAppClose() {
        PackageManager pm = this.getPackageManager();
        ComponentName receiver = new ComponentName(getApplicationContext(), DeviceBootReceiver.class);
        pm.setComponentEnabledSetting(receiver, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
    }

    protected void AppClose() {
        PackageManager pm = this.getPackageManager();
        ComponentName receiver = new ComponentName(getApplicationContext(), DeviceBootReceiver.class);
        pm.setComponentEnabledSetting(receiver, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
    }


    private byte[] key = { 0x13, 0x31, 0x21, 0x11, 0x33, 0x14, 0x12, 0x22, 0x06, 0x09, 0x19, 0x02, 0x32 };

    private void readData() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            int num = 0;
            while((line = reader.readLine()) != null) {
                num++;
                if(num == 1) {
                    name = line;
                    edtName.setText(name);
                    continue;
                } else if(num == 3) {
                    autoTime.setHour(Integer.parseInt(line.split(":")[0]));
                    autoTime.setMinute(Integer.parseInt(line.split(":")[1]));
                    continue;
                } else if(num == 4) {
                    edtUrl.setText(line);
                    break;
                }
                byte[] buf = line.getBytes();
                byte[] result = new byte[buf.length];

                for(int i = 0; i < buf.length; i++)
                    result[i] = (byte) (buf[i] ^ key[i % key.length]);

                if(num == 2) {
                    code = new String(result);
                    edtCode.setText(code);
                }
            }

            reader.close();
        }catch(Exception e) { }
    }

    private void writeData(String name, String code, String url) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(name+"\n"+code+"\n"+autoTime.getHour()+":"+autoTime.getMinute()+"\n"+url);
            writer.close();

            alert.setText("저장되었습니다.");
        }catch(Exception e) {
            alert.setText("저장 실패");
        }
    }

    public InputFilter filterAlphaNumber = new InputFilter() {
        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {

            Pattern ps = Pattern.compile("^[a-zA-Z0-9]+$");

            if (source instanceof SpannableStringBuilder) {
                SpannableStringBuilder sourceAsSpannableBuilder = (SpannableStringBuilder)source;
                for (int i = end - 1; i >= start; i--) {
                    CharSequence currentChar = source.subSequence(i, i+1);
                    if (!ps.matcher(currentChar).matches()) {
                        sourceAsSpannableBuilder.delete(i, i+1);
                    }
                }
                return source;
            } else {
                StringBuilder filteredStringBuilder = new StringBuilder();
                for (int i = start; i < end; i++) {
                    CharSequence currentChar = source.subSequence(i, i+1);
                    if (ps.matcher(currentChar).matches()) {
                        filteredStringBuilder.append(currentChar);
                    }
                }
                return filteredStringBuilder.toString();
            }
        }
    };

    public InputFilter filterAlphaNumberPlus = new InputFilter() {
        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {

            Pattern ps = Pattern.compile("^[a-zA-Z0-9:/._=%?]+$");

            if (source instanceof SpannableStringBuilder) {
                SpannableStringBuilder sourceAsSpannableBuilder = (SpannableStringBuilder)source;
                for (int i = end - 1; i >= start; i--) {
                    CharSequence currentChar = source.subSequence(i, i+1);
                    if (!ps.matcher(currentChar).matches()) {
                        sourceAsSpannableBuilder.delete(i, i+1);
                    }
                }
                return source;
            } else {
                StringBuilder filteredStringBuilder = new StringBuilder();
                for (int i = start; i < end; i++) {
                    CharSequence currentChar = source.subSequence(i, i+1);
                    if (ps.matcher(currentChar).matches()) {
                        filteredStringBuilder.append(currentChar);
                    }
                }
                return filteredStringBuilder.toString();
            }
        }
    };

    public InputFilter filterKorea = new InputFilter() {
        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {

            Pattern ps = Pattern.compile("^[ㄱ-ㅎ가-힣a-zA-Zㅏ-ㅣ]+$");

            if (source instanceof SpannableStringBuilder) {
                SpannableStringBuilder sourceAsSpannableBuilder = (SpannableStringBuilder)source;
                for (int i = end - 1; i >= start; i--) {
                    CharSequence currentChar = source.subSequence(i, i+1);
                    if (!ps.matcher(currentChar).matches()) {
                        sourceAsSpannableBuilder.delete(i, i+1);
                    }
                }
                return source;
            } else {
                StringBuilder filteredStringBuilder = new StringBuilder();
                for (int i = start; i < end; i++) {
                    CharSequence currentChar = source.subSequence(i, i+1);
                    if (ps.matcher(currentChar).matches()) {
                        filteredStringBuilder.append(currentChar);
                    }
                }
                return filteredStringBuilder.toString();
            }
        }
    };

}
