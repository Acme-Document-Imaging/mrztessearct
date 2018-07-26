#include <jni.h>
#include <string>
#include <opencv2/core.hpp>
#include <opencv2/opencv.hpp>
#include "opencv2/imgproc/imgproc.hpp"
#include "opencv2/highgui/highgui.hpp"
////#include <opencv2/highgui/highgui.hpp>
#include <opencv2/imgproc.hpp>
#include <opencv2/features2d.hpp>
#include <vector>
#include <iostream>

using namespace std;
using namespace cv;


extern "C" JNIEXPORT jstring

JNICALL
Java_com_cbms_tesseractdemo_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}


extern "C" {

JNIEXPORT void JNICALL
Java_com_cbms_tesseractdemo_CameraActivity_processImage
        (JNIEnv *env, jclass, jlong frameAddress) {

    Mat &image = *(Mat *) frameAddress;
    //Mat &outMat=*(Mat *) outImage;
//argc != 2 ||
    if (image.rows <= 0 || image.empty() || !image.data) {
        return ;
    }

    //cv::rotate(image,image,ROTATE_90_CLOCKWISE);
    cv::Mat gray;
    cv::Mat blurG;

    cv::GaussianBlur(image,blurG, cv::Size(3, 3), 0);
    cv::cvtColor(blurG, gray, cv::COLOR_BGR2GRAY);
    cv::adaptiveThreshold(gray,image,255,cv::ADAPTIVE_THRESH_MEAN_C, cv::THRESH_BINARY,51,15);
}
}

extern "C" {

JNIEXPORT void JNICALL
Java_com_cbms_tesseractdemo_MainActivity_detectPassportZone
        (JNIEnv *env, jclass, jlong frameAddress) {

    cv::Mat rectKernel = cv::getStructuringElement(cv::MORPH_RECT, cv::Size(13, 5));
    cv::Mat sqKernel = cv::getStructuringElement(cv::MORPH_RECT, cv::Size(34, 34));

    Mat &image = *(Mat *) frameAddress;
    //Mat &outMat=*(Mat *) outImage;
//argc != 2 ||
    if (image.rows <= 0 || image.empty() || !image.data) {
        return ;
    }

    cv::Mat gray;
    cv::cvtColor(image, gray, cv::COLOR_BGR2GRAY);
//cv::resize(image, outimage, cv__So)
    cv::GaussianBlur(gray, gray, cv::Size(3, 3), 0);
    cv::Mat blackHat;
    cv::morphologyEx(gray, blackHat, cv::MORPH_BLACKHAT, rectKernel);

//compute the Scharr gradient of the blackhat image and scale the
//result into the range[0, 255]
    cv::Mat gradX;
    Mat abs_grad_x;
    int scale = 1;
    int delta = 0;
    cv::Sobel(blackHat, gradX, CV_8UC1, 1, 0, -1, scale, delta, BORDER_DEFAULT);
    cv::convertScaleAbs(gradX, abs_grad_x);

//apply a closing operation using the rectangular kernel to close
// gaps in between letters — then apply Otsu’s thresholding method
    double threshValue = 0;
    double maxValue = 255;
    cv::Mat thresh;
    cv::morphologyEx(gradX, gradX, cv::MORPH_CLOSE, rectKernel);
    cv::threshold(gradX, thresh, threshValue, maxValue, cv::THRESH_BINARY | cv::THRESH_OTSU);
// perform another closing operation, this time using the square
// kernel to close gaps between lines of the MRZ, then perform a
// serieso of erosions to break apart connected components
    cv::Mat thresh2;
    cv::morphologyEx(thresh, thresh2, cv::MORPH_CLOSE, sqKernel);
    cv::erode(thresh2, thresh, NULL, cv::Point(-1, -1), 4);

// find contours in the thresholded image and sort them by their
// sizes
    vector<vector<Point> > cnts;
    try {
        cv::findContours(thresh, cnts, CV_RETR_EXTERNAL, CV_CHAIN_APPROX_SIMPLE);
    }
    catch (exception &e) {
        cout << "Exception" << e.what() << endl;
    }

    cv::Mat roi;
// loop over the contours
    for (size_t i = 0; i < cnts.size(); i++) {
        CvRect rect = cv::boundingRect(cv::Mat(cnts[i]));
        float ar = rect.width / float(rect.height);
        float crWidth = rect.width / float(gray.size[1]);

// check to see if the aspect ratio and coverage width are within
// acceptable criteria
        if (ar > 5 && crWidth > 0.75) {
// pad the bounding box since we applied erosions and now need
// to re – grow it
            int pX = int((rect.x + rect.width) * 0.03);
            int pY = int((rect.y + rect.height) * 0.03);
            int x = rect.x - pX;
            int y = rect.y - pY;
            int w = rect.width + (pX * 2);
            int h = rect.height + (pY * 2);

// extract the ROI from the image and draw a bounding box
// surrounding the MRZ
            try {
                image(cv::Range(y, y + h), cv::Range(x, x + w)).copyTo(roi);
                cv::rectangle(image, Point(x, y), Point(x + w, y + h), Scalar(0, 255, 0), 2, LINE_8,
                              0);
            }
            catch (exception &e) {
                cout << "Exception" << e.what() << endl;
            }
            break;
        }
    }

//return to java crop image
    if (!roi.empty() && roi.cols > 0) {
          cv::Mat outImg;
//0.75
        cv::resize(roi, image, cv::Size(roi.cols * 0.99, roi.rows * 0.99), 0, 0, CV_INTER_LINEAR);

       // return NULL;
    }
    /*
//        vector<unsigned char> imageDesV;
//        imencode(".bmp", outImg, imageDesV);
//        return NULL;
//    }

    vector<unsigned char> imageDesV;
    //cv::imencode(".bmp",outImg,imageDesV)
    imencode(".bmp", outImg, imageDesV);
    //imencode(".bmp", outImg, imageDesV);
//convert vector<char> to jbyteArray
    jbyte *result_e = new jbyte[imageDesV.size()];
    jbyteArray result = env->NewByteArray(imageDesV.size());
    for (int i = 0; i < imageDesV.size(); i++) {
        result_e[i] = (jbyte) imageDesV[i];
    }
    env->SetByteArrayRegion(result, 0, imageDesV.size(), result_e);
    return (jbyteArray) result;
} else {
    return NULL;
}
*/
}
}

