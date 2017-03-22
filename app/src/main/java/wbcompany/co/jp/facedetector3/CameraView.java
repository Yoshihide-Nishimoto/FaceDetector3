package wbcompany.co.jp.facedetector3;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Scalar;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CameraView extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {
    private static final String TAG = "CameraView";

    private int degrees;
    private Camera camera;
    private int[] rgb;
    private Bitmap bitmap;
    private Mat image;
    private CascadeClassifier detector_face;
    private CascadeClassifier detector_eye;
    private MatOfRect objects_face;
    private MatOfRect objects_eye;
    private List<RectF> faces = new ArrayList<RectF>();
    private List<RectF> eyes = new ArrayList<RectF>();
    private MediaRecorder myRecorder;
    private boolean isrecord = false;

    public CameraView(Context context, int displayOrientationDegrees) {
        super(context);
        setWillNotDraw(false);
        getHolder().addCallback(this);

        String filename_face = context.getFilesDir().getAbsolutePath() + "/haarcascades/haarcascade_frontalface_alt.xml";
        String filename_eye = context.getFilesDir().getAbsolutePath() + "/haarcascades/haarcascade_eye.xml";
        Log.d("haarcascade:","" + filename_eye);
        detector_face = new CascadeClassifier(filename_face);
        detector_eye = new CascadeClassifier(filename_eye);
        objects_face = new MatOfRect();
        objects_eye = new MatOfRect();
        degrees = displayOrientationDegrees;

    }

	/*
	 * SurfaceHolder.Callback
	 */

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated: holder=" + holder);

        if(camera == null) {
            camera = Camera.open(0);
        }
        camera.setDisplayOrientation(degrees);
        camera.setPreviewCallback(this);
        try {
            camera.setPreviewDisplay(holder);
        } catch(IOException e) {
            e.printStackTrace();
        }

        Camera.Parameters params = camera.getParameters();
        for(Camera.Size size : params.getSupportedPreviewSizes()) {
            Log.i(TAG, "preview size: " + size.width + "x" + size.height);
        }
        for(Camera.Size size : params.getSupportedPictureSizes()) {
            Log.i(TAG, "picture size: " + size.width + "x" + size.height);
        }
        params.setPreviewSize(640, 480);
        camera.setParameters(params);
        myRecorder = new MediaRecorder();
        myRecorder.setCamera(camera);
        //initializeMediaRecorder();
        try {
            myRecorder.prepare(); // 録画準備
        } catch (Exception e) {
            Log.e("recMovie", e.getMessage());
        }
        initializeMediaRecorder();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "surfaceChanged: holder=" + holder + ", format=" + format + ", width=" + width + ", height=" + height);

        if(image != null) {
            image.release();
            image = null;
        }
        if(bitmap != null) {
            if(!bitmap.isRecycled()) {
                bitmap.recycle();
            }
            bitmap = null;
        }
        if(rgb != null) {
            rgb = null;
        }
        faces.clear();
        eyes.clear();
        camera.startPreview();

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "surfaceDestroyed: holder=" + holder);

        if(camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
        if(image != null) {
            image.release();
            image = null;
        }
        if(bitmap != null) {
            if(!bitmap.isRecycled()) {
                bitmap.recycle();
            }
            bitmap = null;
        }
        if(rgb != null) {
            rgb = null;
        }
        faces.clear();
        eyes.clear();
    }

	/*
	 * SurfaceHolder.Callback
	 */

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        //Log.d(TAG, "onPreviewFrame: ");

        int width = camera.getParameters().getPreviewSize().width;
        int height = camera.getParameters().getPreviewSize().height;
        Log.d(TAG, "onPreviewFrame: width=" + width + ", height=" + height);

        Bitmap bitmap = decode(data, width, height, degrees);
        if(degrees == 90) {
            int tmp = width;
            width = height;
            height = tmp;
        }

        if(image == null) {
            image = new Mat(height, width, CvType.CV_8U, new Scalar(4));
        }
        Utils.bitmapToMat(bitmap, image);

        detector_face.detectMultiScale(image, objects_face);

        faces.clear();
        eyes.clear();
        for(org.opencv.core.Rect rect : objects_face.toArray()) {

            detector_eye.detectMultiScale(image.submat(rect),objects_eye);

            float face_left = (float) (1.0 * rect.x / width);
            float face_top = (float) (1.0 * rect.y / height);
            float face_right = face_left + (float) (1.0 * rect.width / width);
            float face_bottom = face_top + (float) (1.0 * rect.height / height);
            faces.add(new RectF(face_left, face_top, face_right, face_bottom));

            if (objects_eye.toArray().length == 2){

                float left = 0.0f;
                float top = 0.0f;
                float right = 0.0f;
                float bottom = 0.0f;

                for(org.opencv.core.Rect rect_eye : objects_eye.toArray()) {

                    float tmp_left = (float) (1.0 * rect_eye.x / width) + (float) (1.0 * rect.x / width);
                    float tmp_top = (float) (1.0 * rect_eye.y / height) + (float) (1.0 * rect.y / height);
                    float tmp_right = tmp_left + (float) (1.0 * rect_eye.width / width);
                    float tmp_bottom = tmp_top + (float) (1.0 * rect_eye.height / height);

                    if (left == 0.0f) {
                        left = tmp_right;
                        top = tmp_top;
                        right = tmp_right;
                        bottom = tmp_bottom;
                    } else {
                        if (tmp_left < left){
                            left = tmp_left;
                        }
                        if (tmp_right > right){
                            right = tmp_right;
                        }
                        if (tmp_top > top){
                            top = tmp_top;
                        }
                        if (tmp_bottom < bottom){
                            bottom = tmp_bottom;
                        }
                    }

                }

                eyes.add(new RectF(left, top, right, bottom));

            }

        }
        invalidate();
    }

	/*
	 * View
	 */

    @Override
    protected void onDraw(Canvas canvas) {
        //もともとSurfaceView は setWillNotDraw(true) なので super.onDraw(canvas) を呼ばなくてもよい。
        //super.onDraw(canvas);

        Paint paint_face = new Paint();
        paint_face.setColor(Color.BLUE);
//        paint.setStyle(Paint.Style.STROKE);
        paint_face.setStyle(Paint.Style.STROKE);
        paint_face.setStrokeWidth(1);

        Paint paint_eye = new Paint();
        paint_eye.setColor(Color.GREEN);
//        paint.setStyle(Paint.Style.STROKE);
        paint_eye.setStyle(Paint.Style.FILL);
        paint_eye.setStrokeWidth(1);

        int width = getWidth();
        int height = getHeight();

        for(RectF eye : eyes) {
            RectF r = new RectF(width * eye.left, height * eye.top, width * eye.right, height * eye.bottom);
            canvas.drawRect(r, paint_eye);
        }
        for(RectF face : faces) {
            RectF r = new RectF(width * face.left, height * face.top, width * face.right, height * face.bottom);
            canvas.drawRect(r, paint_face);
        }
    }

	/*
	 *
	 */

    /** Camera.PreviewCallback.onPreviewFrame で渡されたデータを Bitmap に変換します。
     *
     * @param data
     * @param width
     * @param height
     * @param degrees
     * @return
     */
    private Bitmap decode(byte[] data, int width, int height, int degrees) {
        if (rgb == null) {
            rgb = new int[width * height];
        }

        final int frameSize = width * height;
        for (int j = 0, yp = 0; j < height; j++) {
            int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
            for (int i = 0; i < width; i++, yp++) {
                int y = (0xff & ((int) data[yp])) - 16;
                if (y < 0) y = 0;
                if ((i & 1) == 0) {
                    v = (0xff & data[uvp++]) - 128;
                    u = (0xff & data[uvp++]) - 128;
                }

                int y1192 = 1192 * y;
                int r = (y1192 + 1634 * v);
                int g = (y1192 - 833 * v - 400 * u);
                int b = (y1192 + 2066 * u);

                if (r < 0) r = 0; else if (r > 262143) r = 262143;
                if (g < 0) g = 0; else if (g > 262143) g = 262143;
                if (b < 0) b = 0; else if (b > 262143) b = 262143;

                rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
            }
        }

        if(degrees == 90) {
            int[] rotatedData = new int[rgb.length];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    rotatedData[x * height + height - y - 1] = rgb[x + y * width];
                }
            }
            int tmp = width;
            width = height;
            height = tmp;
            rgb = rotatedData;
        }

        if(bitmap == null) {
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        }

        bitmap.setPixels(rgb, 0, width, 0, 0, width, height);
        return bitmap;
    }

    // MediaRecorderの初期設定
    public void initializeMediaRecorder() {

        myRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA); // 録画の入力ソースを指定
        //myRecorder.setOutputFile("/sdcard/sample.3gp"); // 動画の出力先となるファイルパスを指定
        myRecorder.setOutputFile("/Movies/sample.3gp");
        myRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        myRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP);
        myRecorder.setVideoFrameRate(30); // 動画のフレームレートを指定
        myRecorder.setVideoSize(320, 240); // 動画のサイズを指定
        myRecorder.setPreviewDisplay(getHolder().getSurface()); // 録画中のプレビューに利用するサーフェイスを指定する


    }

    public void onClick_record(){
        if(isrecord){
            myRecorder.stop();
            myRecorder.release();
            isrecord = false;
        } else {
            try {

                myRecorder.start();
                Log.d("onClick_record","正常");
                isrecord = true;
            } catch (Exception e) {
                Log.d("onClick_record","異常");
                e.printStackTrace();
            }

        }
    }
}