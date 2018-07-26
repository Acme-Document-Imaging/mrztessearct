package com.cbms.tesseractdemo;

import android.annotation.TargetApi;
import android.app.DownloadManager;
import android.content.Context;
import android.graphics.Camera;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.nfc.Tag;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.app.NotificationCompatSideChannelService;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.ViewGroup.LayoutParams;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * This class is an implementation of the Bridge View between OpenCV and Java Camera.
 * This class relays on the functionality available in base class and only implements
 * required functions:
 * connectCamera - opens Java camera and sets the PreviewCallback to be delivered.
 * disconnectCamera - closes the camera and stops preview.
 * When frame is delivered via callback from Camera - it processed via OpenCV to be
 * converted to RGBA32 and then passed to the external callback for modifications if required.
 */

@TargetApi(21)
public class MyJavaCamera2View extends CameraBridgeViewBase {

	private static final String LOGTAG = "JavaCamera2View";

	private ImageReader mImageReader;
	private int mPreviewFormat = ImageFormat.YUV_420_888;

	private CameraDevice mCameraDevice;
	private CameraCaptureSession mCaptureSession;
	private CaptureRequest.Builder mPreviewRequestBuilder;
	private String mCameraID;
	private android.util.Size mPreviewSize = new android.util.Size(-1, -1);

	private HandlerThread mBackgroundThread;
	private Handler mBackgroundHandler;

	private int mState=0;
	public boolean areWeFocused;
	//public  boolean  mIsOcrProcessing=false;
	private static final int STATE_PREVIEW = 0;
	private static final int STATE_WAIT_LOCK = 1;
	private static final int STATE_WAIT_CLOSE = 10;
	public Integer mAfState=0;

	public MyJavaCamera2View(Context context, int cameraId) {
		super(context, cameraId);
	}

	public MyJavaCamera2View(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	private void startBackgroundThread() {
		Log.i(LOGTAG, "startBackgroundThread");
		stopBackgroundThread();
		mBackgroundThread = new HandlerThread("OpenCVCameraBackground");
		mBackgroundThread.start();
		mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
	}

	private void stopBackgroundThread() {
		Log.i(LOGTAG, "stopBackgroundThread");
		if (mBackgroundThread == null)
			return;
		mBackgroundThread.quitSafely();
		try {
			mBackgroundThread.join();
			mBackgroundThread = null;
			mBackgroundHandler = null;
		} catch (InterruptedException e) {
			Log.e(LOGTAG, "stopBackgroundThread", e);
		}
	}

	protected boolean initializeCamera() {
		Log.i(LOGTAG, "initializeCamera");
		CameraManager manager = (CameraManager) getContext().getSystemService(Context.CAMERA_SERVICE);
		try {
			String camList[] = manager.getCameraIdList();
			if (camList.length == 0) {
				Log.e(LOGTAG, "Error: camera isn't detected.");
				return false;
			}
			if (mCameraIndex == CameraBridgeViewBase.CAMERA_ID_ANY) {
				mCameraID = camList[0];
			} else {
				for (String cameraID : camList) {
					CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraID);
					if ((mCameraIndex == CameraBridgeViewBase.CAMERA_ID_BACK &&
							characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) ||
							(mCameraIndex == CameraBridgeViewBase.CAMERA_ID_FRONT &&
									characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT)
							) {
						mCameraID = cameraID;
						break;
					}
				}
			}
			if (mCameraID != null) {
				Log.i(LOGTAG, "Opening camera: " + mCameraID);
				manager.openCamera(mCameraID, mStateCallback, mBackgroundHandler);
			}
			return true;
		} catch (CameraAccessException e) {
			Log.e(LOGTAG, "OpenCamera - Camera Access Exception", e);
		} catch (IllegalArgumentException e) {
			Log.e(LOGTAG, "OpenCamera - Illegal Argument Exception", e);
		} catch (SecurityException e) {
			Log.e(LOGTAG, "OpenCamera - Security Exception", e);
		}
		return false;
	}

	private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

		@Override
		public void onOpened(CameraDevice cameraDevice) {
			mCameraDevice = cameraDevice;

			createCameraPreviewSession();
		}

		@Override
		public void onDisconnected(CameraDevice cameraDevice) {
			cameraDevice.close();
			mCameraDevice = null;
		}

		@Override
		public void onError(CameraDevice cameraDevice, int error) {
			cameraDevice.close();
			mCameraDevice = null;
		}

	};

	private  void unLockFocus()

	{
		try {

			mState = STATE_PREVIEW;
			mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
					CaptureRequest.CONTROL_AF_TRIGGER_IDLE);
//			mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
//					CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

			mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback, mBackgroundHandler);
		}catch (CameraAccessException e)
		{
			e.printStackTrace();
		}
		catch (Exception e) {
			//Log.e(TAG,"ERROR :"e.getMessage());
			e.printStackTrace();
		}

	}
	private  void lockFocus()

	{
try {

	mState = STATE_WAIT_LOCK;
	mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
			CaptureRequest.CONTROL_AF_TRIGGER_START);
//	mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
//			CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

	mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback, mBackgroundHandler);
}catch (CameraAccessException e)
{
	e.printStackTrace();
}
catch (Exception e) {
	//Log.e(TAG,"ERROR :"e.getMessage());
	e.printStackTrace();
}

	}
	public static boolean checkControlAfState(CaptureResult result) {
		boolean missing = result.get(CaptureResult.CONTROL_AF_STATE) == null;
		if (missing) {
			// throw new IllegalStateException("CaptureResult missing CONTROL_AF_STATE.");
		//    Log.e(TAG, "\n!!!! TotalCaptureResult missing CONTROL_AF_STATE. !!!!\n ");
		}
		return !missing;
	}




	private CameraCaptureSession.CaptureCallback mCaptureCallback
			= new CameraCaptureSession.CaptureCallback() {

		private void process(CaptureResult result) {
			switch (mState) {
				case STATE_PREVIEW: {

					int afState = result.get(CaptureResult.CONTROL_AF_STATE);
					if (CaptureResult.CONTROL_AF_TRIGGER_START == afState) {
						if (areWeFocused) {
							//Run specific task here
							 mAfState=CameraMetadata.CONTROL_AF_STATE_FOCUSED_LOCKED;
						}
					}
					if (CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED == afState) {
						areWeFocused = true;
					} else {
						areWeFocused = false;
						mAfState=CameraMetadata.CONTROL_AF_STATE_ACTIVE_SCAN;
					}

					break;
				}
			}
		}

		@Override
		public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request,
										CaptureResult partialResult) {
			process(partialResult);
		}

		@Override
		public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request,
									   TotalCaptureResult result) {
			process(result);
		}
	};
//	private CameraCaptureSession.CaptureCallback mCaptureCallback
//			= new CameraCaptureSession.CaptureCallback() {
//
//		private void process(CaptureResult result) {
//			switch (mState) {
//				case STATE_WAIT_CLOSE:
//
//					break;
//				case STATE_PREVIEW:
//					if(result.get(CaptureResult.CONTROL_AF_STATE) == null) {
//						Log.e("FOCUS","Preview AF State is empty " );
//						return;
//					}
//					Log.e("FOCUS","Preview Lock Focus " );
//
//							lockFocus();
//
//
////				    if(result.get(CaptureResult.CONTROL_AF_STATE) == null)
////					{
////						Log.e(LOGTAG,"Focus State is empty");
////						return  ;
////					}
////
////                    int afState = result.get(CaptureResult.CONTROL_AF_STATE);
////                    if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState) {
////                        Log.e("FOCUS","Focus is Locked");
////                    }
////                    else
////                    {
////                        Log.e("FOCUS","Focus is not  Locked " + afState);
////                    }
//
//
//					break;
//				case STATE_WAIT_LOCK:
//					if(result.get(CaptureResult.CONTROL_AF_STATE) == null) {
//						return;
//					}
//					 mAfState = result.get(CaptureResult.CONTROL_AF_STATE);
//                    if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == mAfState) {
//						Log.e("FOCUS","Focused now un lock focus " + mAfState);
//                    	unLockFocus();
//					}
//					else
//                {
//                        Log.e("FOCUS","Focus is not  Locked " + mAfState);
//                    }
//
//
////					int afState = result.get(CaptureResult.CONTROL_AF_STATE);
////					if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState) {
////						Log.e(LOGTAG,"FOCUS LOCKED");
////					}
//					//DO nothing
//					break;
////                case STATE_PREVIEW: {
////
////                    Log.e(LOGTAG,"PREVIEW");
////                    if(result==null )
////                    {
////                        Log.e(LOGTAG,"PREVIEW NULL");
////                        return;
////                    }
////                    if(result.get(CaptureResult.CONTROL_AF_STATE) == null)
////                    //if(checkControlAfState(result))
////                    {
////                        Log.e(LOGTAG,"Focus State is empty");
////                        return  ;
////                    }
////                    int afState = result.get(CaptureResult.CONTROL_AF_STATE);
////                    Log.e(LOGTAG,"PREVIEW AF State"+afState);
////                    if (CaptureResult.CONTROL_AF_TRIGGER_START == afState) {
////                        if (areWeFocused) {
////                            //Run specific task here
////                        }
////                    }
////                    if (CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED == afState) {
////                        areWeFocused = true;
////                    } else {
////                        areWeFocused = false;
////                    }
////
////                    break;
////                }
//			}
//		}
//
//		@Override
//		public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request,
//										CaptureResult partialResult) {
//			process(partialResult);
//		}
//
//		@Override
//		public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request,
//
//									   TotalCaptureResult result) {
///*
//
//		int afMode =result.get(CaptureResult.CONTROL_AF_MODE);
//			Log.e("FOCUS","AF Mode is "+afMode);
//	if(afMode== CameraMetadata.CONTROL_AF_MODE_AUTO)
//	{
//		Log.e("FOCUS","AF Mode is Auto");
//
//		mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);
//
//		try {
//			Log.e("FOCUS","BF Capture ");
//			mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback, mBackgroundHandler);
//			Log.e("FOCUS","Capture ");
//		} catch (CameraAccessException e) {
//			e.printStackTrace();
//		}
//		mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_IDLE);
//		Log.e("FOCUS","AF  Idle");
//		try {
//
//			Log.e("FOCUS","Bef Repeating");
//			mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), mCaptureCallback, mBackgroundHandler);
//			Log.e("FOCUS","Repeating");
//		} catch (CameraAccessException e) {
//			e.printStackTrace();
//		}
//
//	}
//	else
//	{
//		Log.e(LOGTAG,"Capture Request "+afMode);
//	}
//*/
//
//			process(result);
//		}
//	};
	private void createCameraPreviewSession() {
		final int w = mPreviewSize.getWidth(), h = mPreviewSize.getHeight();
		Log.i(LOGTAG, "createCameraPreviewSession(" + w + "x" + h + ")");
		if (w < 0 || h < 0)
			return;
		try {
			if (null == mCameraDevice) {
				Log.e(LOGTAG, "createCameraPreviewSession: camera isn't opened");
				return;
			}
			if (null != mCaptureSession) {
				Log.e(LOGTAG, "createCameraPreviewSession: mCaptureSession is already started");
				return;
			}

			mImageReader = ImageReader.newInstance(w, h, mPreviewFormat, 2);
			mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
				@Override
				public void onImageAvailable(ImageReader reader) {
					Image image = reader.acquireLatestImage();
					if (image == null )
						return;

					// If not focus then it should close the image and return
//                    if(mAfState!=CameraMetadata.CONTROL_AF_STATE_FOCUSED_LOCKED)
//                    {
//
//                        image.close();
//                        return  ;
//                    }
					Log.e("FOCUS","Image Available  " + mAfState + " With Size "+image.getWidth()+" x "+image.getHeight());
					//Log.e(LOGTAG,"Image Available focus  "+areWeFocused);
					// sanity checks - 3 planes
					Image.Plane[] planes = image.getPlanes();
					assert (planes.length == 3);
					assert (image.getFormat() == mPreviewFormat);

					// see also https://developer.android.com/reference/android/graphics/ImageFormat.html#YUV_420_888
					// Y plane (0) non-interleaved => stride == 1; U/V plane interleaved => stride == 2
					assert (planes[0].getPixelStride() == 1);
					assert (planes[1].getPixelStride() == 2);
					assert (planes[2].getPixelStride() == 2);

					ByteBuffer y_plane = planes[0].getBuffer();
					ByteBuffer uv_plane = planes[1].getBuffer();
					Mat y_mat = new Mat(h, w, CvType.CV_8UC1, y_plane);
					Mat uv_mat = new Mat(h / 2, w / 2, CvType.CV_8UC2, uv_plane);
					JavaCamera2Frame tempFrame = new JavaCamera2Frame(y_mat, uv_mat, w, h);
					deliverAndDrawFrame(tempFrame);
					tempFrame.release();
					image.close();
				}
			}, mBackgroundHandler);
			Surface surface = mImageReader.getSurface();



			mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
			mPreviewRequestBuilder.addTarget(surface);

			mCameraDevice.createCaptureSession(Arrays.asList(surface),
					new CameraCaptureSession.StateCallback() {
						@Override
						public void onConfigured(CameraCaptureSession cameraCaptureSession) {
							Log.i(LOGTAG, "createCaptureSession::onConfigured");
							if (null == mCameraDevice) {
								return; // camera is already closed
							}
							mCaptureSession = cameraCaptureSession;
							try {

                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
//								mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
//										CaptureRequest.CONTROL_AF_MODE_EDOF);
//								mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
//										CaptureRequest.CONTROL_AF_MODE_AUTO);

								mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
										CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
//								mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);



							    mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), null, mBackgroundHandler);
								//mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), mCaptureCallback, mBackgroundHandler);
								//mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback, mBackgroundHandler);
								Log.i(LOGTAG, "CameraPreviewSession has been started");
							} catch (Exception e) {
								Log.e(LOGTAG, "createCaptureSession failed", e);
							}
						}

						@Override
						public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
							Log.e(LOGTAG, "createCameraPreviewSession failed");
						}
					},
					null
			);
		} catch (CameraAccessException e) {
			Log.e(LOGTAG, "createCameraPreviewSession", e);
		}
	}

	@Override
	protected void disconnectCamera() {
		Log.i(LOGTAG, "closeCamera");
		try {
			CameraDevice c = mCameraDevice;
			mCameraDevice = null;
			if (null != mCaptureSession) {
				mCaptureSession.close();
				mCaptureSession = null;
			}
			if (null != c) {
				c.close();
			}
			if (null != mImageReader) {
				mImageReader.close();
				mImageReader = null;
			}
		} finally {
			stopBackgroundThread();
		}
	}

	boolean calcPreviewSize(final int width, final int height) {
		Log.i(LOGTAG, "calcPreviewSize: " + width + "x" + height);
		if (mCameraID == null) {
			Log.e(LOGTAG, "Camera isn't initialized!");
			return false;
		}
		CameraManager manager = (CameraManager) getContext().getSystemService(Context.CAMERA_SERVICE);
		try {
			CameraCharacteristics characteristics = manager.getCameraCharacteristics(mCameraID);
			StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
			int bestWidth = 0, bestHeight = 0;
			float aspect = (float) width / height;
			//android.util.Size[] sizes = map.getOutputSizes(ImageReader.class);
			android.util.Size[] sizes = map.getOutputSizes(ImageReader.class);
			bestWidth = sizes[0].getWidth();
			bestHeight = sizes[0].getHeight();
			for (android.util.Size sz : sizes) {
				int w = sz.getWidth(), h = sz.getHeight();
			  //  Log.d(LOGTAG, "trying size: " + w + "x" + h);
				if (width >= w && height >= h && bestWidth <= w && bestHeight <= h
						&& Math.abs(aspect - (float) w / h) < 0.2) {
					bestWidth = w;
					bestHeight = h;
				}
			//	Log.i(LOGTAG, "size: " + w + "x" + h);

				//if (width >= w && height >= h && bestWidth <= w && bestHeight <= h						) {
//				if ( bestWidth <= w && bestHeight <= h						) {
//					bestWidth = w;
//					bestHeight = h;
//				}
			}
			Log.i(LOGTAG, "best size: " + bestWidth + "x" + bestHeight);
			assert(!(bestWidth == 0 || bestHeight == 0));
			if (mPreviewSize.getWidth() == bestWidth && mPreviewSize.getHeight() == bestHeight)
				return false;
			else {
				mPreviewSize = new android.util.Size(bestWidth, bestHeight);
			//	mPreviewSize=new android.util.Size(3120, 4160);
				return true;
			}
		} catch (CameraAccessException e) {
			Log.e(LOGTAG, "calcPreviewSize - Camera Access Exception", e);
		} catch (IllegalArgumentException e) {
			Log.e(LOGTAG, "calcPreviewSize - Illegal Argument Exception", e);
		} catch (SecurityException e) {
			Log.e(LOGTAG, "calcPreviewSize - Security Exception", e);
		}
		return false;
	}

	//This method call 1st time
	@Override
	protected boolean connectCamera(int width, int height) {
		Log.i(LOGTAG, "setCameraPreviewSize(" + width + "x" + height + ")");
		startBackgroundThread();
		initializeCamera();
		try {
			boolean needReconfig = calcPreviewSize(width, height);
			mFrameWidth = mPreviewSize.getWidth();
			mFrameHeight = mPreviewSize.getHeight();

			if ((getLayoutParams().width == LayoutParams.MATCH_PARENT) && (getLayoutParams().height == LayoutParams.MATCH_PARENT))
				mScale = Math.min(((float)height)/mFrameHeight, ((float)width)/mFrameWidth);
			else
				mScale = 0;

			AllocateCache();

			if (needReconfig) {
				if (null != mCaptureSession) {
					Log.d(LOGTAG, "closing existing previewSession");
					mCaptureSession.close();
					mCaptureSession = null;
				}
				createCameraPreviewSession();
			}
		} catch (RuntimeException e) {
			throw new RuntimeException("Interrupted while setCameraPreviewSize.", e);
		}
		return true;
	}

	private class JavaCamera2Frame implements CvCameraViewFrame {
		@Override
		public Mat gray() {
			return mYuvFrameData.submat(0, mHeight, 0, mWidth);
		}

		@Override
		public Mat rgba() {
			if (mPreviewFormat == ImageFormat.NV21)
				Imgproc.cvtColor(mYuvFrameData, mRgba, Imgproc.COLOR_YUV2RGBA_NV21, 4);
			else if (mPreviewFormat == ImageFormat.YV12)
				Imgproc.cvtColor(mYuvFrameData, mRgba, Imgproc.COLOR_YUV2RGB_I420, 4); // COLOR_YUV2RGBA_YV12 produces inverted colors
			else if (mPreviewFormat == ImageFormat.YUV_420_888) {
				assert (mUVFrameData != null);
				Imgproc.cvtColorTwoPlane(mYuvFrameData, mUVFrameData, mRgba, Imgproc.COLOR_YUV2RGBA_NV21);
			} else
				throw new IllegalArgumentException("Preview Format can be NV21 or YV12");

			return mRgba;
		}

		public JavaCamera2Frame(Mat Yuv420sp, int width, int height) {
			super();
			mWidth = width;
			mHeight = height;
			mYuvFrameData = Yuv420sp;
			mUVFrameData = null;
			mRgba = new Mat();
		}

		public JavaCamera2Frame(Mat Y, Mat UV, int width, int height) {
			super();
			mWidth = width;
			mHeight = height;
			mYuvFrameData = Y;
			mUVFrameData = UV;
			mRgba = new Mat();
		}

		public void release() {
			mRgba.release();
		}

		private Mat mYuvFrameData;
		private Mat mUVFrameData;
		private Mat mRgba;
		private int mWidth;
		private int mHeight;
	};
}