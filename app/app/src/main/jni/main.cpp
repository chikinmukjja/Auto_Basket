#include <jni.h>
#include "android_webcrawler_osori_opencvhog_MainActivity.h"
#include "opencv2/core/core.hpp"
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/video/tracking.hpp>
#include <opencv2/objdetect/objdetect.hpp>

using namespace std;
using namespace cv;

const int MAX_CORNERS = 40;
CascadeClassifier cascade;
bool loaded = false;

int absoluteValue(int x);
void goodFeaturesToTrack_Demo(Mat& prevImage, vector<Point2f>& prevPoints);

extern "C" {

JNIEXPORT jint JNICALL Java_android_webcrawler_osori_opencvhog_MainActivity_loadCascade(JNIEnv*, jobject);
JNIEXPORT jint JNICALL Java_android_webcrawler_osori_opencvhog_MainActivity_calcOpticalFlow(JNIEnv *, jobject, jlong, jlong, jlong);
JNIEXPORT jint JNICALL Java_android_webcrawler_osori_opencvhog_MainActivity_hogDetection(JNIEnv *, jobject, jlong, jlong);

JNIEXPORT jint JNICALL Java_android_webcrawler_osori_opencvhog_MainActivity_loadCascade(JNIEnv*, jobject) {

    loaded = cascade.load("/sdcard/hogcascade_pedestrians.xml" );
    if(loaded == true){
        return (jint)1;
    }else{
        return (jint)0;
    }

}

JNIEXPORT jint JNICALL Java_android_webcrawler_osori_opencvhog_MainActivity_calcOpticalFlow(JNIEnv *, jobject, jlong prev, jlong curr, jlong frame)
{
    Mat& prevImage   = *(Mat*)prev;
    Mat& currImage   = *(Mat*)curr;
    Mat& frameImage  = *(Mat*)frame;

    int average = 0;

    vector<uchar>   status(MAX_CORNERS);
    vector<float>  error(MAX_CORNERS);
    vector<Point2f> prevPoints(MAX_CORNERS);
    vector<Point2f> currPoints(MAX_CORNERS);

    goodFeaturesToTrack_Demo(prevImage, prevPoints);

    if (prevPoints.size() < 5)
    return 0;

    Size winSize(20, 20);							// 윈도우 size
    int max_level = 3;								// pyramid max level

    calcOpticalFlowPyrLK(
            prevImage,
            currImage,
            prevPoints,
            currPoints,
            status,
            error,
            winSize,
            3
    );

    for (int i = 0; i < currPoints.size(); i++)
    {
        if (status[i] == 1)
        {
            CvPoint p1, p2;
            // 화면을 90도 회전하기 때문에 x값이 같도록 해주어야 한다.
            p1.x = (int) currPoints[i].x;
            p1.y = (int) currPoints[i].y;
            p2.x = (int) prevPoints[i].x;
            p2.y = (int) prevPoints[i].y;
            if(absoluteValue(p1.y - p2.y) < absoluteValue(p1.x - p2.x)){
                average += p2.x - p1.x;
                line(frameImage, p1, p2, CV_RGB(0, 255, 0));
           }
        }
    }


    return average;
}

JNIEXPORT jint JNICALL Java_android_webcrawler_osori_opencvhog_MainActivity_hogDetection(JNIEnv *, jobject, jlong curr, jlong frame)
{
    Mat& currImage   = *(Mat*)curr;
    Mat& frameImage  = *(Mat*)frame;
    vector<Rect> found;

    int minNeighbors = 1;            // Parameter specifying how many neighbors each candidate rectangle should have to retain it.
    double scale_step = 1.1;        // Parameter specifying how much the image size is reduced at each image scale.
    Size min_obj_sz(50, 50);         // Minimum possible object size. Objects smaller than that are ignored.
    Size max_obj_sz(200, 200);       // Maximum possible object size. Objects larger than that are ignored.

    if(loaded){
        cascade.detectMultiScale(currImage, found, scale_step, minNeighbors, 0, min_obj_sz, max_obj_sz);
    }

    for(int i=0; i<(int)found.size(); i++)
        rectangle(frameImage, found[i], Scalar(0,255,0), 2);

    return (jint)found.size();
}

}   // extern "C"


int absoluteValue(int x){
    if(x >= 0)
        return x;
    else
        return -x;
}

void goodFeaturesToTrack_Demo(Mat& prevImage, vector<Point2f>& prevPoints)
{

    /// Parameters for Shi-Tomasi algorithm
    double qualityLevel = 0.15;	// 보통 0.1에서 0.01 정도의 값을 가진다.
    // 코너 판별을 위해 사용되는 임계값은 영상에서 구한 고유값들 중에서의 최대값과 quality_level을 곱하여 결정한다.
    double minDistance  = 10;		// 반환되는 코너 점들 사이의 최소거리
    int blockSize = 3;
    bool useHarrisDetector = false;
    double k = 0.04;

    /// Apply corner detection
    goodFeaturesToTrack(prevImage,
                        prevPoints,
                        MAX_CORNERS,
                        qualityLevel,
                        minDistance,
                        Mat(),
                        blockSize,
                        useHarrisDetector,
                        k );

    Size subPixWinSize(5,5);
    int max_iter = 20;
    double epsilon = 0.03;

    TermCriteria termcrit(CV_TERMCRIT_ITER|CV_TERMCRIT_EPS, max_iter, epsilon);
    cornerSubPix(prevImage, prevPoints, subPixWinSize, Size(-1,-1), termcrit);
}
