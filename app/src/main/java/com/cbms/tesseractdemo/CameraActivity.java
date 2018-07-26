package com.cbms.tesseractdemo;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.camera2.CameraMetadata;
import android.media.AudioManager;
import android.media.Image;
import android.media.ToneGenerator;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.app.NotificationCompatSideChannelService;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.googlecode.tesseract.android.ResultIterator;
import com.googlecode.tesseract.android.TessBaseAPI;
import com.googlecode.tesseract.android.TessBaseAPI.PageIteratorLevel;

import org.jmrtd.lds.icao.MRZInfo;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class CameraActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {


    private static final String TAG = "CameraActivity";
    //OCR
    public static final String lang = "OcrB";
    public static final String PACKAGE_NAME = "com.cbms.tesseractdemo";
    public static final String DATA_PATH = Environment
            .getExternalStorageDirectory().toString() + "/CBMSMRZ/";
    private static final int MRZ_RESULT = 25;
    private boolean isOCRProcessing = false;

    private Mat mRgba;
    private Mat mIntermediateMat;
    private Mat mGray;

    private MyJavaCamera2View mOpenCvCameraView;
    //   private  JavaCameraView mOpenCvCameraView;

    private ImageView mImageView;
    private boolean mTakePicture = true;

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    private LinearLayout scannerLayout;
    private Size mSize;
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    private TessBaseAPI baseApi;
    private TextRecognizer textRecognizer;
    private File mFile;
    //private CameraOverlayView scannerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);


        mOpenCvCameraView = (MyJavaCamera2View) findViewById(R.id.HelloOpenCvView);
        mImageView = (ImageView) findViewById(R.id.imageView2);
        scannerLayout = (LinearLayout) findViewById(R.id.scannerLayout);
        //scannerLayout=(CameraOverlayView)findViewById(R.id.scannerLayout);
        //mOpenCvCameraView = (JavaCameraView) findViewById(R.id.HelloOpenCvView);
        mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        moveTrainingFile();
    }

    private void moveTrainingFile() {
        String[] paths = new String[]{DATA_PATH, DATA_PATH + "tessdata/"};

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
        if (!(new File(DATA_PATH + "tessdata/" + lang + ".traineddata")).exists()) {
            try {

                AssetManager assetManager = getAssets();
                InputStream in = assetManager.open(lang + ".traineddata");
                //GZIPInputStream gin = new GZIPInputStream(in);
                OutputStream out = new FileOutputStream(DATA_PATH
                        + "tessdata/" + lang + ".traineddata");

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

    private void recognizeImageMV(Bitmap bitmap) {

        if (textRecognizer == null) {
            initMobileVision();
        }

        // Bitmap bitmap = ((BitmapDrawable)imageView.getDrawable()).getBitmap();

        Frame imageFrame = new Frame.Builder()
                .setBitmap(bitmap)
                .build();
        String readit = "";
        SparseArray<TextBlock> textBlocks = textRecognizer.detect(imageFrame);
        for (int i = 0; i < textBlocks.size(); i++) {
            TextBlock textBlock = textBlocks.get(textBlocks.keyAt(i));

            Log.i("OCR:", textBlock.getValue());
            readit = readit + " " + textBlock.getValue();

            // Do something with value
        }

    }

    //private void recognizeImage(final Bitmap bitmap) {
    private void recognizeImage(final File file) {

//        recognizeImageMV(bitmap);
//        return ;

     Runnable r = new Runnable() {

//
            public void run() {
                isOCRProcessing = true;

                if (baseApi == null)
                    initTesseact();

                Log.e(TAG, "Before baseApi");
                //    Log.v(TAG, DATA_PATH);
                long start = System.currentTimeMillis();
                baseApi.setImage(file);
                long timeRequired = System.currentTimeMillis() - start;


                String recognizedText = baseApi.getUTF8Text();
                //Log.e("OCR", "OCR : " + recognizedText);
                Log.e("OCR1","OCR Process Time is "+timeRequired +" Mean Confidence "+baseApi.meanConfidence() +"With Result "+recognizedText);
                if (recognizedText == null || recognizedText.length() < 1 || baseApi.meanConfidence()<55) {
                    isOCRProcessing = false;
                    return;
                }

                String validmrz = validMRZ();
                if (validmrz.length() <= 1) {
                    isOCRProcessing = false;
                    return;
                }
                Log.e("OCR", "Valid MRZ: " + validmrz);

                if (mOpenCvCameraView != null)
                    mOpenCvCameraView.disableView();

          ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
                toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP,150);




              //  mBackgroundHandler.post(new ImageSaver(bitmap, mFile));

                //Log.e(TAG, "OCRED TEXT: " + recognizedText);
                try {
                    //MRZInfo mrzInfo=new MRZInfo(validmrz);
                    //Log.e(TAG,"PR identifier "+mrzInfo.getPrimaryIdentifier());
                } catch (Exception e) {

                    System.out.println("Error " + e.getMessage());
                }

                //Log.e(TAG,  mrzInfo.getDocumentNumber());
                //textView.setText(baseApi.getUTF8Text());
                // baseApi.end();



                isOCRProcessing = false;
            //mOpenCvCameraView.mIsOcrProcessing=false;

                Intent returnIntent = new Intent();
                returnIntent.putExtra("MRZ_RESULT", validmrz);
                setResult(Activity.RESULT_OK, returnIntent);
                finish();

      }
       };
        mBackgroundHandler.post(r);

    }

    private void initMobileVision() {
        // TODO: Create the TextRecognizer


        textRecognizer = new TextRecognizer.Builder(this).build();
        if (!textRecognizer.isOperational()) {

            Log.w(TAG, "Detector dependencies are not yet available.");

            // Check for low storage.  If there is low storage, the native library will not be
            // downloaded, so detection will not become operational.
            IntentFilter lowstorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
            boolean hasLowStorage = registerReceiver(null, lowstorageFilter) != null;

            if (hasLowStorage) {
                Toast.makeText(this, "Low Storage", Toast.LENGTH_LONG).show();
                Log.w(TAG, "Low Storage");
            }
        }
        // TODO: Set the TextRecognizer's Processor.

        // TODO: Check if the TextRecognizer is operational.

        // TODO: Create the mCameraSource using the TextRecognizer.
    }

    private void finishActivity(final String code) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Intent returnIntent = new Intent();
                returnIntent.putExtra("MRZ_RESULT", code);
                setResult(Activity.RESULT_OK, returnIntent);
                finish();
            }
        });
    }

    private String validMRZ() {

        String validMRZ = "";
        Boolean isStart = false;
        int lineNo = 0;
        ResultIterator iterator = baseApi.getResultIterator();
        iterator.begin();
        do {
            String text = iterator.getUTF8Text(PageIteratorLevel.RIL_TEXTLINE);
            if (text == null) continue;
            ;
            if (!isStart && text.contains("P<")) {
                isStart = true;
                //lineNo=1;
            }
            if (isStart)
                lineNo = lineNo + 1;

            if (lineNo > 2)
                break;
            if (isStart) {

                validMRZ = validMRZ + "" + text;
            }

//            lastBoundingBox = iterator.getBoundingBox(PageIteratorLevel.RIL_TEXTLINE);
//            Rect lastRectBox = new Rect(lastBoundingBox[0], lastBoundingBox[1],
//                    lastBoundingBox[2], lastBoundingBox[3]);
//            charBoxes.add(lastRectBox);
        } while (iterator.next(PageIteratorLevel.RIL_TEXTLINE));

        return validMRZ;
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");

                    // Load native library after(!) OpenCV initialization
                    //System.loadLibrary("mixed_sample");

                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();

        stopBackgroundThread();
        stopTessearct();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        startBackgroundThread();
        initTesseact();
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();


        stopTessearct();
    }


    public void onCameraViewStarted(int width, int height) {
        mSize = new Size(width, height);
        Log.e(TAG, "On Camera View" + width + "h" + height);
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mIntermediateMat = new Mat(height, width, CvType.CV_8UC4);
        mGray = new Mat(height, width, CvType.CV_8UC1);
    }

    public void onCameraViewStopped() {
        mRgba.release();
        mGray.release();
        mIntermediateMat.release();
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

            mRgba=inputFrame.rgba();
            boolean isFocus=false;
        if (mOpenCvCameraView.mAfState != CameraMetadata.CONTROL_AF_STATE_FOCUSED_LOCKED) {
            Log.e("OCR","Focus Locked");
            isFocus=true;
        }
        else
        {
            Log.e("OCR","Focus  Scanning ");
        }

        if(!isFocus)
            return mRgba;



            if (mTakePicture && !isOCRProcessing) {

                long start=System.currentTimeMillis();
                //mTakePicture=false;


                Mat cropMat = CropMat(mRgba);


                long startPro=System.currentTimeMillis();
//                Mat blurG=new  Mat();
//                Mat gray=new  Mat();
//                Imgproc.GaussianBlur(cropMat,blurG,new Size(3,3),0);
//                Imgproc.cvtColor(blurG,gray,Imgproc.COLOR_BayerBG2GRAY);
//                Imgproc.adaptiveThreshold(gray,cropMat,225,Imgproc.ADAPTIVE_THRESH_MEAN_C,Imgproc.THRESH_BINARY,51,15);
                //cv::GaussianBlur(image,blurG, cv::Size(3, 3), 0);
                //cv::cvtColor(blurG, gray, cv::COLOR_BGR2GRAY);
                //cv::adaptiveThreshold(gray,image,255,cv::ADAPTIVE_THRESH_MEAN_C, cv::THRESH_BINARY,51,15);
                processImage(cropMat.nativeObj);
                long timeSpendPro = System.currentTimeMillis() - startPro;
                Log.e("OCR","Image Processing Time "+timeSpendPro);

                //detectPassportZone(cropMat.nativeObj);
                Bitmap bmp = Bitmap.createBitmap(cropMat.cols(), cropMat.rows(), Bitmap.Config.ARGB_8888);

                Utils.matToBitmap(cropMat, bmp);
                mBackgroundHandler.post(new ImageSaver(bmp, mFile));

                recognizeImage(mFile);

                //recognizeImage(bmp);
                //    mImageView.setImageBitmap(bmp);


                setBitmap(bmp);

                long timeRequired = System.currentTimeMillis() - start;
                Log.e("OCR","Total Time Spend on Processing is (ms) "+timeRequired);
            }


            //processImage(mRgba.nativeObj);
            return mRgba;

    }
//    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
//
//        mRgba=inputFrame.rgba();
//        //mRgba=CropMat(mRgba);
//
//        if (mOpenCvCameraView.mAfState != CameraMetadata.CONTROL_AF_STATE_FOCUSED_LOCKED) {
//
//
//            return mRgba;
//        } else {
//
//            Display display = getWindowManager().getDefaultDisplay();
//
//            Point size = new Point();
//            display.getSize(size);
//            //int width = size.x;
//
//            //Log.e(TAG,"Overlay Size:"+scannerLayout.getWidth() + " H "+scannerLayout.getHeight() +" Dislapy w"+size.x +" h "+size.y);
//            Log.e(TAG, "Overlay Measured Size:" + scannerLayout.getMeasuredWidth() + " H " + scannerLayout.getMeasuredHeight());
//
//            int[] customViewPosition = new int[2];
//            ;
//            scannerLayout.getLocationInWindow(customViewPosition);
//            Log.e(TAG, "Rgba Size:" + mRgba.size().width + " H " + mRgba.size().width + " positoin at " + customViewPosition[0] + " " + customViewPosition[1]);
//            Log.e("FOCUS", " on Camera Activity " + mOpenCvCameraView.mAfState + " with picture Status and OCR Status "+mTakePicture+" : "+isOCRProcessing) ;
//            //if(mAfState!=CameraMetadata.CONTROL_AF_STATE_FOCUSED_LOCKED)
//        //    mRgba = CropMat(mRgba);
//
//            if (mTakePicture && !isOCRProcessing) {
//
//                //mTakePicture=false;
//
//
//                Mat cropMat = CropMat(mRgba);
//
//                processImage(cropMat.nativeObj);
//
//                //detectPassportZone(cropMat.nativeObj);
//                Bitmap bmp = Bitmap.createBitmap(cropMat.cols(), cropMat.rows(), Bitmap.Config.ARGB_8888);
//
//                Utils.matToBitmap(cropMat, bmp);
//
//                recognizeImage(bmp);
//                //    mImageView.setImageBitmap(bmp);
//
//
//                setBitmap(bmp);
//
//            }
//
//
//             //processImage(mRgba.nativeObj);
//            return mRgba;
//        }
//    }


    private void setBitmap(final Bitmap bmp) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                mImageView.setImageBitmap(bmp);
            }
        });

    }


    /**
     * Saves a JPEG {@link Image} into the specified {@link File}.
     */
    private static class ImageSaver implements Runnable {

        private  Bitmap bmp;
        /**
         * The JPEG image
         */
        private  Image mImage;
        /**
         * The file we save the image into.
         */
        private final File mFile;

        ImageSaver(Image image, File file) {
            mImage = image;
            mFile = file;
        }

        ImageSaver(Bitmap bitmap, File file) {
            bmp  = bitmap;
            mFile = file;
        }
        public void setDpi(byte[] imageData, int dpi) {
            imageData[13] = 1;
            imageData[14] = (byte) (dpi >> 8);
            imageData[15] = (byte) (dpi & 0xff);
            imageData[16] = (byte) (dpi >> 8);
            imageData[17] = (byte) (dpi & 0xff);
        }

        @Override
        public void run() {

//          //  Log.e(TAG,"Size Image: "+mImage.getWidth()+"x"+mImage.getHeight());
//            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
//            byte[] bytes = new byte[buffer.remaining()];
//            buffer.get(bytes);
            FileOutputStream output = null;
            try {
                output = new FileOutputStream(mFile);
                Log.e(TAG,"Saving File to "+mFile.getAbsolutePath());

                ByteArrayOutputStream imageByteArray = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.JPEG, 100, imageByteArray);
                byte[] imageData = imageByteArray.toByteArray();
                //300 will be the dpi of the bitmap
                setDpi(imageData, 300);
                output.write(imageData);
               // bmp.compress(Bitmap.CompressFormat.PNG, 100, output); // bmp is your Bitmap instance
                //output.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
              //  mImage.close();
                if (null != output) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
//        @Override
//        public void run() {
//
//            Log.e(TAG,"Size Image: "+mImage.getWidth()+"x"+mImage.getHeight());
//            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
//            byte[] bytes = new byte[buffer.remaining()];
//            buffer.get(bytes);
//            FileOutputStream output = null;
//            try {
//                output = new FileOutputStream(mFile);
//                output.write(bytes);
//            } catch (IOException e) {
//                e.printStackTrace();
//            } finally {
//                mImage.close();
//                if (null != output) {
//                    try {
//                        output.close();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        }

    }


    private Mat CropMat(Mat mat) {

        //=mOpenCvCameraView.mPreviewSize;
        long start=System.currentTimeMillis();
        int[] customViewPosition = new int[2];
        int[] surfacePosition = new int[2];
        scannerLayout.getLocationInWindow(customViewPosition);


        //int[] customViewPosition;

        mOpenCvCameraView.getLocationInWindow(surfacePosition);

        float scaley = 1;
        float scalex = 1;
//        if(mat.size().height<=mat.size().width)
//        {
//            scaley=(float) mat.size().height / (float) mOpenCvCameraView.getHeight();
//        }
//        else if(mat.size().width<mat.size().height)
//        {
//            scaley=(float) mat.size().width / (float) mOpenCvCameraView.getWidth();
//        }

        scaley = (float) mat.size().height / (float) mOpenCvCameraView.getHeight();
        scalex = (float) mat.size().width / (float) mOpenCvCameraView.getWidth();
        //float scaley =  (float) mOpenCvCameraView.getWidth()/(float) mat.size().width ;
//        int left = (int) scaley * (customViewPosition[0] + scannerLayout.getWidth() / 6 - surfacePosition[0]);
//        int top = (int) scaley * (customViewPosition[1] + scannerLayout.getHeight() / 6 - surfacePosition[1]);
//        int w = (int) scaley * scannerLayout.getWidth() * 2 / 3;
//        int h = (int) scaley * scannerLayout.getHeight() * 2 / 3;
//
//        Log.e(TAG, scaley+"left "+left +"Top "+top+"w"+w);

        int w = (int) (scannerLayout.getWidth());
        int h = (int) (scannerLayout.getHeight());
        int x = (int) (customViewPosition[0]);
        int y = (int) (customViewPosition[1]);
        //x=(int) x*scalex;
        x = (int) ((float) x * scalex);
        y = (int) ((float) y * scaley  );
        x = x - surfacePosition[0];
        y = y - surfacePosition[1];
        w = (int) ((float) w * scalex);// -surfacePosition[0];
        h = (int) ((float) h * scaley );// -surfacePosition[1];

        int surfacey= (int) (surfacePosition[1]* scaley);
        int surfacex= (int) (surfacePosition[0]* scalex);
//        x=x-surfacex;
//        y=y-surfacey;
        w = w + x;
        h = h + y;
Log.e(TAG,"New Surface " +surfacex+", "+surfacey);
        Log.e(TAG,"Image" +mat.size().width+"X "+mat.size().height);


        //Draw Rectangle
        Imgproc.rectangle(mat,new org.opencv.core.Point(x,y), new org.opencv.core.Point(w,h),new org.opencv.core.Scalar(255,0,255), 3);


       org.opencv.core.Rect roi = new org.opencv.core.Rect(new org.opencv.core.Point(x, y), new org.opencv.core.Point(w, h));
      Mat cropped = new Mat(mat, roi);


        Log.e(TAG,  "x "+scannerLayout.getX() +" y "+scannerLayout.getY()+" w "+scannerLayout.getWidth()+" h " +scannerLayout.getWidth() +" S "+scaley +" Surface View X "+surfacePosition[0]+" Surface y "+surfacePosition[1] +" layout View X "+customViewPosition[0]+" lay y "+customViewPosition[1] );
        long timeSpend=System.currentTimeMillis()-start;
        Log.e("OCR","Crop Spend Time"+timeSpend);
        return cropped;
        //w=w+x-surfacePosition[0];
        //h=h+y-surfacePosition[1];
        //
        //org.opencv.core.Rect roi = new org.opencv.core.Rect(new org.opencv.core.Point(w * 1 / 3 + 1, h * 1 / 3 + 1), new org.opencv.core.Point(w * 2 / 3, h * 2 / 3));
        //org.opencv.core.Rect roi = new org.opencv.core.Rect(new org.opencv.core.Point(customViewPosition[0],customViewPosition[1]), new org.opencv.core.Point(100,150));

        // org.opencv.core.Rect roi = new org.opencv.core.Rect(new org.opencv.core.Point(customViewPosition[0],customViewPosition[1]), new org.opencv.core.Point(100,150));
        //Imgproc.rectangle(mat,);
        //  y=customViewPosition[1];
        //scaley  =(float)0.93333334;
        //Imgproc.rectangle(mat,new org.opencv.core.Point(mat.size().width,y-30), new org.opencv.core.Point(0,y+h-x),new org.opencv.core.Scalar(255,0,255), 3);
        //Working
        //Imgproc.rectangle(mat,new org.opencv.core.Point((x*scalex)-surfacePosition[0],(y*scaley)-surfacePosition[1]), new org.opencv.core.Point((w*scalex)-surfacePosition[0],(h*scaley)-surfacePosition[1]),new org.opencv.core.Scalar(255,0,255), 3);

        //Imgproc.rectangle(mat,new org.opencv.core.Point(0,customViewPosition[1]+453), new org.opencv.core.Point(x+w+x,y+h),new org.opencv.core.Scalar(255,0,255), 10);
        //   Imgproc.rectangle(mat,new org.opencv.core.Point(scannerLayout.getLeft(),772), new org.opencv.core.Point(scannerLayout.getRight(),500),new org.opencv.core.Scalar(250,0,0), 3);
        //Log.e(TAG,"RECT "+roi);
//    int[] customViewPosition;
//    mCustomView.getLocationInWindow(customViewPosition);
//    int[] surfacePosition;
//    mSurfaceView.getLocationInWindow(surfacePosition);
//
//    float scaley = imageOriginal.getWidth()/(float)mSurfaceView.getWidth();
//    int left = (int) scaley*(customViewPosition[0] + mCustomView.getWidth()/6F - surfacePosition[0]);
//    int top = (int) scaley*(customViewPosition[1] + mCustomView.getHeight()/6F - surfacePosition[1]);
//    int width = (int) scaley*mCustomView.getWidth()*2/3;
//    int height = (int) scaley*mCustomView.getHeight()*2/3;}
    }


    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void initTesseact() {

        mFile = new File(this.getExternalFilesDir(null), "pic.jpg");

        if (baseApi != null) return;

        baseApi = new TessBaseAPI();
        baseApi.setDebug(true);

        baseApi.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789<");



        baseApi.init(DATA_PATH, lang,TessBaseAPI.OEM_TESSERACT_ONLY);
    }

    private void stopTessearct() {

        baseApi.end();
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

    public void takePhoto(View v) {
        mTakePicture = true;
    }


    public native static void processImage(long image);

    public native static void detectPassportZone(long image);
}
