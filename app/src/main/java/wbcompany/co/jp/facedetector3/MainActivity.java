package wbcompany.co.jp.facedetector3;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends Activity {

    static {
        System.loadLibrary("opencv_java3");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            // assetsの内容を /data/data/*/files/ にコピーします。
            copyAssets("haarcascades");
        } catch (IOException e) {
            e.printStackTrace();
        }

        final CameraView cameraView = new CameraView(this, 90);

        ViewGroup activityMain = (ViewGroup)findViewById(R.id.activity_main);
        activityMain.addView(cameraView);

        final Button record_button = new Button(this);
        record_button.setText("録画する");

        activityMain.addView(record_button);

        // リスナーをボタンに登録
        record_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // flagがtrueの時
                cameraView.onClick_record();
                record_button.setBackgroundColor(Color.BLUE);
            }
        });

    }

    private void copyAssets(String dir) throws IOException {
        byte[] buf = new byte[8192];
        int size;

        File dst = new File(getFilesDir(), dir);
        if(!dst.exists()) {
            dst.mkdirs();
            dst.setReadable(true, false);
            dst.setWritable(true, false);
            dst.setExecutable(true, false);
        }

        for(String filename : getAssets().list(dir)) {
            File file = new File(dst, filename);
            OutputStream out = new FileOutputStream(file);
            InputStream in = getAssets().open(dir + "/" + filename);
            while((size = in.read(buf)) >= 0) {
                if(size > 0) {
                    out.write(buf, 0, size);
                }
            }
            in.close();
            out.close();
            file.setReadable(true, false);
            file.setWritable(true, false);
            file.setExecutable(true, false);
        }
    }
}