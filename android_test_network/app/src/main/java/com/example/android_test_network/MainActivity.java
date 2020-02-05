package com.example.android_test_network;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {
    TextView tv, textView;
    Handler han;
    int count;

    ProgressBar progress;
    //백그라운드Task
    ImageDownload task;
    int value;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        tv = (TextView)findViewById(R.id.textView);

        han = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                tv.setText(msg.arg1+"");
            }
        };

        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {
                count =0;
                while (count<10) {
                    Message msg = han.obtainMessage();
                    count++;
                    msg.arg1 = count;
                    han.sendMessage(msg);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                han.post(new Runnable() {
                    @Override
                    public void run() {
                        tv.setTextColor(Color.RED);
                    }

                });

            }
        });
        th.start();


        //AsyncTask
        textView = (TextView) findViewById(R.id.textView2);
        progress = (ProgressBar) findViewById(R.id.progress);

        // 실행 버튼 이벤트
        Button executeButton = (Button) findViewById(R.id.executeButton);
        executeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // 백그라운드 task 생성
                task = new ImageDownload();
                //excute를 통해 백그라운드 task를 실행시킨다
                //여기선 100을 매개변수로 보내는데 여기 예제에서는 이 매개변수를 doInBackGround에서 사용을 안했다.
                task.execute("https://66.media.tumblr.com/c15cc4f94fefadea557540b2795e22db/tumblr_ow85exCnMx1vkaa0ko1_400.png");


            }
        });

        // 취소 버튼 이벤트
        Button cancelButton = (Button) findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //task종료한다. BackgrounTask의 onCancled()호출될것이다.
                //이미 스레드가 종료된 뒤에 cancel하면 아무일도 안일어난다. 이미 종료되었기 때문에
                task.cancel(true);
            }
        });
    }


    //새로운 TASK정의 (AsyncTask)
    // < >안에 들은 자료형은 순서대로 doInBackground, onProgressUpdate, onPostExecute의 매개변수 자료형을 뜻한다.(내가 사용할 매개변수타입을 설정하면된다)
    class BackgroundTask extends AsyncTask<Integer , Integer , Integer> {
        //초기화 단계에서 사용한다. 초기화관련 코드를 작성했다.
        protected void onPreExecute() {
            value = 0;
            progress.setProgress(value);
        }

        //스레드의 백그라운드 작업 구현
        //여기서 매개변수 Intger ... values란 values란 이름의 Integer배열이라 생각하면된다.
        //배열이라 여러개를 받을 수 도 있다. ex) excute(100, 10, 20, 30); 이런식으로 전달 받으면 된다.
        protected Integer doInBackground(Integer ... values) {
            //isCancelled()=> Task가 취소되었을때 즉 cancel당할때까지 반복
            while (isCancelled() == false) {
                value++;
                //위에 onCreate()에서 호출한 excute(100)의 100을 사용할려면 이런식으로 해줘도 같은 결과가 나온다.
                //밑 대신 이렇게해도됨 if (value >= values[0].intValue())
                if (value >= 100) {
                    break;
                } else {
                    //publishProgress()는 onProgressUpdate()를 호출하는 메소드(그래서 onProgressUpdate의 매개변수인 int즉 Integer값을 보냄)
                    //즉, 이 메소드를 통해 백그라운드 스레드작업을 실행하면서 중간중간  UI에 업데이트를 할 수 있다.

                    //백그라운드에서는 UI작업을 할 수 없기 때문에 사용
                    publishProgress(value);
                }

                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {}
            }

            return value;
        }

        //UI작업 관련 작업 (백그라운드 실행중 이 메소드를 통해 UI작업을 할 수 있다)
        //publishProgress(value)의 value를 값으로 받는다.values는 배열이라 여러개 받기가능
        protected void onProgressUpdate(Integer ... values) {
            progress.setProgress(values[0].intValue());
            textView.setText("현재 진행 값 : " + values[0].toString());
        }


        //이 Task에서(즉 이 스레드에서) 수행되던 작업이 종료되었을 때 호출됨
        protected void onPostExecute(Integer result) {
            progress.setProgress(0);
            textView.setText("완료되었습니다");
        }

        //Task가 취소되었을때 호출
        protected void onCancelled() {
            progress.setProgress(0);
            textView.setText("취소되었습니다");
        }
    }

    public class ImageDownload extends AsyncTask<String, Integer, String> {        //이미지 다운로드

        private String fileName;
        String savePath = Environment.getExternalStorageDirectory() + File.separator + "temp/";

        @Override
        protected void onPreExecute() {

            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... params) {

            File dir = new File(savePath);
            //상위 디렉토리가 존재하지 않을 경우 생성
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String fileUrl = params[0];

            String localPath = savePath + "/" + fileName + ".jpg";

            try {
                URL imgUrl = new URL(fileUrl);
                //서버와 접속하는 클라이언트 객체 생성
                HttpURLConnection conn = (HttpURLConnection) imgUrl.openConnection();
                int response = conn.getResponseCode();

                File file = new File(localPath);

                InputStream is = conn.getInputStream();
                OutputStream outStream = new FileOutputStream(file);

                byte[] buf = new byte[1024];
                int len = 0;

                while ((len = is.read(buf)) > 0) {
                    outStream.write(buf, 0, len);
                }

                outStream.close();
                is.close();
                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String obj) {

            //저장한 이미지 열기
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            String targetDir = Environment.getExternalStorageDirectory().toString();

            File file = new File(targetDir + "/" + fileName + ".jpg");

            //type 지정 (이미지)
            i.setDataAndType(Uri.fromFile(file), "image/*");
            getApplicationContext().startActivity(i);

            super.onPostExecute(obj);

        }
    }
}





