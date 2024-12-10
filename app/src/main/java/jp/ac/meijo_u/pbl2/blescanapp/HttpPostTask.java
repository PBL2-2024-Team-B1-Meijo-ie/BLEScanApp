package jp.ac.meijo_u.pbl2.blescanapp;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpPostTask extends AsyncTask<String, Void, String> {

    private final Context context;

    public HttpPostTask(Context context) {
        this.context = context;
    }

    private static final String TAG = "BeaconDetection";
    @Override
    protected String doInBackground(String... params) {
        String serverUrl = params[0]; // サーバのURL
        String dataToSend = params[1]; // 送信する文字(

        try {
            // URLを設定
            Log.d(TAG, "call");
            URL url = new URL(serverUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // POSTリクエストの設定
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; utf-8");
            connection.setDoOutput(true);

            // データ送信
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = dataToSend.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // レスポンスコードを取得
            int responseCode = connection.getResponseCode();
            Log.d(TAG,"wait response");
            //Log.d(TAG,responseCode);
            if (responseCode == HttpURLConnection.HTTP_CREATED) {
                Log.d(TAG, "good");
                return "送信成功";
            } else {
                Log.d(TAG, "bad");
                return "送信失敗: " + responseCode;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "エラー: " + e.getMessage();
        }
    }
    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);

        // 結果をToastで表示
        Toast.makeText(context, result, Toast.LENGTH_LONG).show();
    }

}
