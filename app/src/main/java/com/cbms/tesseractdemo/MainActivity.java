package com.cbms.tesseractdemo;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.print.PrinterId;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.util.SparseArray;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import  com.googlecode.tesseract.android.*;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class MainActivity extends AppCompatActivity {


    public static final String lang = "OcrB";
    public static final String PACKAGE_NAME = "com.cbms.tesseractdemo";
    public static final String DATA_PATH = Environment
            .getExternalStorageDirectory().toString() + "/CBMSMRZ/";
    private static final int MRZR_REQUEST = 25;

    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    private static final String TAG = "MRZMainApplication";
    private static final int SELECT_IMAGE = 124;
    TextView textView;
    ImageView imageView;

    long start;

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    private TextRecognizer textRecognizer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        moveTrainingFile();
        // Example of a call to a native method
        textView = (TextView) findViewById(R.id.sample_text);
        imageView=(ImageView)findViewById(R.id.imageView);
        textView.setText(stringFromJNI());
    }


    private  void recognizeImage(Bitmap bitmap)
    {

        Log.v(TAG, "Before baseApi");
        Log.v(TAG, DATA_PATH);
        TessBaseAPI baseApi = new TessBaseAPI();
        baseApi.setDebug(true);
        baseApi.init(DATA_PATH, lang);
        baseApi.setImage(bitmap);

        String recognizedText = baseApi.getUTF8Text();

        textView.setText(baseApi.getUTF8Text());
        baseApi.end();

        Log.v(TAG, "OCRED TEXT: " + recognizedText);

    }
private  void initMobileVision()
{
    // TODO: Create the TextRecognizer


     textRecognizer = new TextRecognizer.Builder(this).build();
    if(!textRecognizer.isOperational())
    {

        Log.w(TAG, "Detector dependencies are not yet available.");

        // Check for low storage.  If there is low storage, the native library will not be
        // downloaded, so detection will not become operational.
        IntentFilter lowstorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
        boolean hasLowStorage = registerReceiver(null, lowstorageFilter) != null;

        if (hasLowStorage) {
            Toast.makeText(this,"Low Storage", Toast.LENGTH_LONG).show();
            Log.w(TAG, "Low Storage");
        }
    }
    // TODO: Set the TextRecognizer's Processor.

    // TODO: Check if the TextRecognizer is operational.

    // TODO: Create the mCameraSource using the TextRecognizer.
}
    public  void tessOCR(View v)
    {

        Bitmap bitmap = ((BitmapDrawable)imageView.getDrawable()).getBitmap();

        this.textView.setText("");
        this.recognizeImage(bitmap);
    }
    public  void clearText(View v)
    {
        this.textView.setText("");
    }
    public void openCVImage(View v)
    {


//       Mat src =BitmapToByte(bitmap);
//       Mat outPut=new  Mat();
        //Log.e(TAG,"MAt  Start");
        Runnable r = new Runnable() {


            public void run() {
                Bitmap bitmap = ((BitmapDrawable)imageView.getDrawable()).getBitmap();
                Mat src =BitmapToByte(bitmap);
                Mat outPut=new  Mat();
                Log.e(TAG,"MAt  Start");
                detectPassportZone(src.getNativeObjAddr());
                Log.e(TAG,"MAt  End");
                Bitmap bmp = Bitmap.createBitmap(src.cols(), src   .rows(), Bitmap.Config.ARGB_8888);

                Utils.matToBitmap(src,bmp   );
                SetBitmap(bmp);


            }
        };

        mBackgroundHandler.post(r);

//        Toast.makeText(this, "MAT Available", Toast.LENGTH_SHORT).show();
        //Log.e(TAG,"MAt  End");



    }

    public  void OpenCamera2(View v)
    {

        Intent intent=new Intent(this,CameraActivity.class);
        start = System.currentTimeMillis();
        startActivityForResult(intent,MRZR_REQUEST);
    }

    private void SetBitmap(final Bitmap bmp)
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                imageView.setImageBitmap(bmp  );
            }
        });

    }

    private class PhotoDecodeRunnable implements Runnable {

        @Override
        public void run() {
            /*
             * Code you want to run on the thread goes here
             */

        }

    }

    private Mat BitmapToByte(Bitmap bmp){
        //Camera.Parameters parameters = mCamera.getParameters();
        //YuvImage yuv = new YuvImage(data, parameters.getPreviewFormat(), cameraWidth, cameraHeight, null);
        //ByteArrayOutputStream out = new ByteArrayOutputStream();
        //yuv.compressToJpeg(new Rect(0, 0, cameraWidth, cameraHeight), 100, out);
        //byte[] bytes = out.toByteArray();
        //final Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
//final Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
        if (bmp != null){
            Mat orig = new Mat(bmp.getHeight(),bmp.getWidth(), CvType.CV_8UC1);
//Bitmap myBitmap32 = bmp.copy(Bitmap.Config.ARGB_8888, true);
            Utils.bitmapToMat(bmp,orig);
           // Imgproc.resize(orig,orig,new Size(newWidth, NEW_HEIGHT));
            return orig;
        }
        return null;
    }
    public  void visionOCR(View v)
    {
        if(textRecognizer==null    )
        {
            initMobileVision();
        }

        Bitmap bitmap = ((BitmapDrawable)imageView.getDrawable()).getBitmap();

        Frame imageFrame = new Frame.Builder()
                .setBitmap(bitmap)
                .build();
    String readit="";
        SparseArray<TextBlock> textBlocks = textRecognizer.detect(imageFrame);
        for (int i = 0; i < textBlocks.size(); i++) {
            TextBlock textBlock = textBlocks.get(textBlocks.keyAt(i));

            Log.i(TAG, textBlock.getValue());
            readit=readit+" "+textBlock.getValue();

            // Do something with value
        }
        this.textView.setText(readit);
    }
    public  void openGallery(View v)
    {

        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);//
        startActivityForResult(Intent.createChooser(intent, "Select Picture"),SELECT_IMAGE);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SELECT_IMAGE)
        {
            if (resultCode == Activity.RESULT_OK)
            {
                if (data != null)
                {
                    try
                    {

                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), data.getData());
                        imageView.setImageBitmap(bitmap);
                       // recognizeImage(bitmap);

                    } catch (IOException e)
                    {
                        e.printStackTrace();
                    }

                }
            } else if (resultCode == Activity.RESULT_CANCELED)
            {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show();
            }
        }
        else if(requestCode==MRZR_REQUEST)


        {  long timeSpend = System.currentTimeMillis() - start;

            Log.e("OCR1","Total Time for MRZ "+timeSpend);
            if(resultCode==Activity.RESULT_OK)
            {
                String code=data.getStringExtra("MRZ_RESULT");
                this.textView.setText(code);

                Log.e(TAG,"Result Recieved");
        }
        else
            {
                Log.e(TAG,"Result not Recieved");
            }
        }
    }
    private  void moveTrainingFile()
    {
        String[] paths = new String[] { DATA_PATH, DATA_PATH + "tessdata/" };

        for (String path : paths) {
            File dir = new File(path);
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    Log.v(TAG, "ERROR: Creation of directory " + path + " on sdcard failed");
                    return;
                } else {
                    Log.v(TAG, "Created directory " + path + " on sdcard");
                }
            }

        }

        // lang.traineddata file with the app (in assets folder)
        // You can get them at:
        // http://code.google.com/p/tesseract-ocr/downloads/list
        // This area needs work and optimization
        if (!(new File(DATA_PATH  + "tessdata/" + lang + ".traineddata")).exists()) {
            try {

                AssetManager assetManager = getAssets();
                InputStream in = assetManager.open( lang + ".traineddata");
                //GZIPInputStream gin = new GZIPInputStream(in);
                OutputStream out = new FileOutputStream(DATA_PATH
                        + "tessdata/" +  lang + ".traineddata");

                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                //while ((lenf = gin.read(buff)) > 0) {
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                in.close();
                //gin.close();
                out.close();

                Log.v(TAG, "Copied " + lang + " traineddata");
            } catch (IOException e) {
                Log.e(TAG, "Was unable to copy " + lang + " traineddata " + e.toString());
            }
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        startBackgroundThread();

    }

    @Override
    protected void onPause() {
        super.onPause();
        stopBackgroundThread();
    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    public native static void detectPassportZone(long image);
}
