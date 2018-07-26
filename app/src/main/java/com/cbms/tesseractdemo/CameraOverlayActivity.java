package com.cbms.tesseractdemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

public class CameraOverlayActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2   {


    private static final String    TAG = "CameraActivity";

    private Mat mRgba;
    private Mat                    mIntermediateMat;
    private Mat mGray;

    private  MyJavaCamera2View mOpenCvCameraView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_overlay);

        mOpenCvCameraView = (MyJavaCamera2View) findViewById(R.id.HelloOpenCvView);

        //mOpenCvCameraView = (JavaCameraView) findViewById(R.id.HelloOpenCvView);
        mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }



private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
@Override
public void onManagerConnected(int status) {
        switch (status) {
        case LoaderCallbackInterface.SUCCESS:
        {
        Log.i(TAG, "OpenCV loaded successfully");

        // Load native library after(!) OpenCV initialization
        //System.loadLibrary("mixed_sample");

        mOpenCvCameraView.enableView();
        } break;
default:
        {
        super.onManagerConnected(status);
        } break;
        }
        }
        };
@Override
public void onPause()
        {
        super.onPause();
        if (mOpenCvCameraView != null)
        mOpenCvCameraView.disableView();
        }

@Override
public void onResume()
        {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
        Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
        Log.d(TAG, "OpenCV library found inside package. Using it!");
        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        }

public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
        mOpenCvCameraView.disableView();
        }



public void onCameraViewStarted(int width, int height) {
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

        mRgba= inputFrame.rgba();

        Log.e(TAG,"Rgba Size:"+mRgba.size().width + " H "+mRgba.size().width);

        // processImage(mRgba.nativeObj);
        return mRgba;
        }

        }
