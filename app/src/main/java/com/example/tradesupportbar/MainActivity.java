package com.example.tradesupportbar;

import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.renderscript.ScriptGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Time;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {
    Timer mTimer = null;
    SoundPlayer soundPlayer = null;
    Boolean playingSound = false;

    EditText upEditText = null;
    EditText downEditText = null;
    Button saveButton = null;
    SharedPreferences data = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        soundPlayer = new SoundPlayer(this);

        upEditText = findViewById(R.id.upEditText);
        downEditText = findViewById(R.id.downEditText);

        data = getSharedPreferences("Data", MODE_PRIVATE);

        upEditText.setText(data.getString("DataUpNumber", "0"));
        downEditText.setText(data.getString("DataDownNumber", "0"));

        saveButton = findViewById(R.id.saveButton);
        saveButton.setOnClickListener(new View.OnClickListener() {
              @Override
              public void onClick(View v) {
                  soundPlayer.stop();
                  SharedPreferences.Editor editor = data.edit();
                  editor.putString("DataUpNumber", upEditText.getText().toString());
                  editor.putString("DataDownNumber", downEditText.getText().toString());
                  editor.apply();
              }
         });

        mTimer = new Timer(true);
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                String urlString = "https://api.coin.z.com/public/v1/ticker?symbol=BTC";
                HttpURLConnection urlConnection = null;
                int responseCode = 0;
                String responseData = "";
                try {
                    //ステップ1:接続URLを決める。
                    URL url = null;
                    try {
                        url = new URL(urlString);
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                    //ステップ2:URLへのコネクションを取得する。
                    urlConnection = (HttpURLConnection) url.openConnection();
                    //ステップ3:接続設定（メソッドの決定,タイムアウト値,ヘッダー値等）を行う。
                    //接続タイムアウトを設定する。
                    urlConnection.setConnectTimeout(100000);
                    //レスポンスデータ読み取りタイムアウトを設定する。
                    urlConnection.setReadTimeout(100000);
                    //ヘッダーにUser-Agentを設定する。
                    urlConnection.setRequestProperty("User-Agent", "Android");
                    //ヘッダーにAccept-Languageを設定する。
                    urlConnection.setRequestProperty("Accept-Language", Locale.getDefault().toString());
                    //HTTPのメソッドをGETに設定する。
                    urlConnection.setRequestMethod("GET");
                    //リクエストのボディ送信を許可しない
                    urlConnection.setDoOutput(false);
                    //レスポンスのボディ受信を許可する
                    urlConnection.setDoInput(true);
                    //ステップ4:コネクションを開く
                    urlConnection.connect();
                    //ステップ6:レスポンスボディの読み出しを行う。
                    responseCode = urlConnection.getResponseCode();
                    responseData = convertToString(urlConnection.getInputStream(), 2);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (urlConnection != null) {
                        //ステップ7:コネクションを閉じる。
                        urlConnection.disconnect();
                    }
                }

                try {
                    createNotify(responseData);
                    checkAlert(
                        responseData,
                        data.getString("DataUpNumber", "0"),
                        data.getString("DataDownNumber", "0")
                    );
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }, 0, 5000);

    }

    public String convertToString(InputStream stream, int num) throws IOException {
        StringBuffer sb = new StringBuffer();
        String line = "";
        BufferedReader br = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        try {
            stream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    void createNotify (String response) throws IOException {
        // 通知を作成するビルダーの生成
        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                MainActivity.this,
                "my_channel_01");

        // 通知のアイコン
        builder.setSmallIcon(android.R.drawable.ic_dialog_info);
        ObjectMapper mapper = new ObjectMapper();

        JsonNode root = mapper.readTree(response);

        // get()で指定したキーに対応するJsonNodeを取得できる
        JsonNode last = root.get("data").get(0).get("last");
        JsonNode ask = root.get("data").get(0).get("ask");
        JsonNode bid = root.get("data").get(0).get("bid");


        // 通知のタイトル
        builder.setContentTitle("[BTC] 最終値: " + last + "円  " + getNowDate());
        builder.setContentText("買い気配値: " + bid + "円 売り気配値: " + ask + "円");
        builder.setTicker("BTC " + ask.toString());

        // 通知の内容
        //builder.setContentText("by CoinTradeSupporter");
        //builder.setContentTitle("BTC " + ask.toString());

        // 通知の作成
        Notification notification = builder.build();

        // 通知サービスで通知を実行する
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            String CHANNEL_ID = "my_channel_01";
            CharSequence name = "my_channel";
            String Description = "This is my channel";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
            mChannel.setDescription(Description);
            mChannel.enableLights(true);
            mChannel.setLightColor(Color.RED);
            mChannel.enableVibration(true);
            mChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
            mChannel.setShowBadge(false);
            manager.createNotificationChannel(mChannel);
        }
        manager.notify((int) (Math.random()), notification); // 0は識別子。何でも良い
    }

    public static String getNowDate(){
        final DateFormat df = new SimpleDateFormat("HH:mm:ss");
        final Date date = new Date(System.currentTimeMillis());
        return df.format(date);
    }

    void checkAlert (String response, String upNumberText, String downNumberText) throws IOException {
        // 通知を作成するビルダーの生成
        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                MainActivity.this,
                "my_channel_01");

        // 通知のアイコン
        builder.setSmallIcon(android.R.drawable.ic_dialog_info);
        ObjectMapper mapper = new ObjectMapper();

        JsonNode root = mapper.readTree(response);

        // get()で指定したキーに対応するJsonNodeを取得できる
        JsonNode last = root.get("data").get(0).get("last");
        double upNumber = 0;
        try {
            upNumber = Double.parseDouble(upNumberText);
        } catch (Exception e) {
            upNumber = 0;
        }
        double downNumber = 0;
        try {
            downNumber = Double.parseDouble(downNumberText);
        } catch (Exception e) {
            downNumber = 0;
        }
        Log.d("s", "checkAlert: " + last.asDouble() + ">=" + upNumber );
        if (!Double.isNaN(upNumber) && upNumber > 0 && last.asDouble() >= upNumber) {
            //if (!playingSound) {
                playingSound = true;
                soundPlayer.playUpSound();
            //}
        }
        Log.d("sa", "checkAlert: " + last.asDouble() + "<=" + downNumber );
        if (!Double.isNaN(downNumber) && downNumber > 0 && last.asDouble() <= downNumber) {
            //if (!playingSound) {
                playingSound = true;
                soundPlayer.playDownSound();
            //}
        }

    }



        private static JsonNode readJsonNode(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        // JSON文字列を読み込み、JsonNodeオブジェクトに変換（Fileやbyte[]も引数に取れる）
        JsonNode root = mapper.readTree(json);

        // get()で指定したキーに対応するJsonNodeを取得できる
        return root.get("data").get(0).get("ask"); // {"fuga":1,"piyo":2}

        // 階層的にアクセス可能
        //System.out.println(root.get("hoge").get("fuga")); // 1

        // 配列にアクセスするときは添字をわたす
        //System.out.println(root.get("foo").get(0)); // "bar"

        // 値を特定の基本型に変換して取得可能
        //System.out.println(root.get("hoge").get("piyo").asInt()); // 2
        //System.out.println(root.get("hoge").get("piyo").asDouble()); // 2.0
        //System.out.println(root.get("hoge").get("piyo").asBoolean()); // true

        // toString()でJSON全体を文字列として取得
        //System.out.println(root.toString()); // {"hoge":{"fuga":1,"piyo":2},"foo":["bar","bow"]}

    }

}
