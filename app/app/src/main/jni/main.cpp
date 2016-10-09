#include <jni.h>
#include "android_webcrawler_osori_opencvhog_MainActivity.h"
#include "opencv2/core/core.hpp"
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/video/tracking.hpp>
#include <opencv2/objdetect/objdetect.hpp>
#include <main.h>
#include <stdlib.h>

using namespace std;
using namespace cv;

const int MAX_CORNERS = 50;
const double cos45   = 0.52532198881;
const double sin45   = 0.85090352453;

/* Cascade 관련 변수들 */
CascadeClassifier cascade;
bool loaded = false;

/* 화면 분할 관련 변수들 */

void goodFeaturesToTrack_Demo(Mat& prevImage, vector<Point2f>& prevPoints);

extern "C" {
JNIEXPORT jint JNICALL Java_android_webcrawler_osori_opencvhog_MainActivity_init(JNIEnv *, jobject);
JNIEXPORT jstring JNICALL Java_android_webcrawler_osori_opencvhog_MainActivity_calcOpticalFlow(JNIEnv *env, jobject obj, jlong, jlong, jlong);
JNIEXPORT jint JNICALL Java_android_webcrawler_osori_opencvhog_MainActivity_hogDetection(JNIEnv *, jobject, jlong, jlong);

JNIEXPORT jint JNICALL Java_android_webcrawler_osori_opencvhog_MainActivity_init(JNIEnv *, jobject){

    loaded = cascade.load("/sdcard/hogcascade_pedestrians.xml" );

    if(loaded == true){
        return (jint)1;
    }else{
        return (jint)0;
    }

}

JNIEXPORT jstring JNICALL Java_android_webcrawler_osori_opencvhog_MainActivity_calcOpticalFlow(JNIEnv *env, jobject obj,
                                                                                               jlong prev, jlong curr, jlong frame)
{
    Mat& prevImage   = *(Mat*)prev;
    Mat& currImage   = *(Mat*)curr;
    Mat& frameImage  = *(Mat*)frame;

    int sumX = 0;
    int sumY = 0;
    int diagonal1 = 0;
    int diagonal2 = 0;

    vector<uchar>   status(MAX_CORNERS);
    vector<float>  error(MAX_CORNERS);
    vector<Point2f> prevPoints(MAX_CORNERS);
    vector<Point2f> currPoints(MAX_CORNERS);

    goodFeaturesToTrack_Demo(prevImage, prevPoints);

    if (prevPoints.size() < 3)
        return env->NewStringUTF("/0/0/0/0");

    Size winSize(20, 20);							// 윈도우 size
    int max_level = 3;								// pyramid max level
    int minX      = 3;                             // 최소 X값
    calcOpticalFlowPyrLK(
            prevImage,
            currImage,
            prevPoints,
            currPoints,
            status,
            error,
            winSize,
            max_level
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

            sumX        += p2.x - p1.x;
            sumY        += p2.y - p1.y;
            diagonal1   += (cos45*(double)(p2.x) + sin45*(double)(p2.y)) - (cos45*(double)(p1.x) + sin45*(double)(p1.y));
            diagonal2   += (-sin45*(double)(p2.x) + cos45*(double)(p2.y)) - (-sin45*(double)(p1.x) + cos45*(double)(p1.y));
            // line(frameImage, p1, p2, CV_RGB(0, 255, 0));
        }
    }

    char msg[1000];
    int len = 0;

    len =  sprintf(msg + len, "%d/", sumX);
    len += sprintf(msg + len, "%d/", sumY);
    len += sprintf(msg + len, "%d/", diagonal1);
    len += sprintf(msg + len, "%d", diagonal2);

    return env->NewStringUTF(msg);
}

JNIEXPORT jint JNICALL Java_android_webcrawler_osori_opencvhog_MainActivity_hogDetection(JNIEnv *, jobject, jlong curr, jlong frame)
{
    Mat& currImage   = *(Mat*)curr;
    Mat& frameImage  = *(Mat*)frame;
    vector<Rect> found;

    int minNeighbors = 1;            // Parameter specifying how many neighbors each candidate rectangle should have to retain it.
    double scale_step = 1.1;        // Parameter specifying how much the image size is reduced at each image scale.
    Size min_obj_sz(50, 50);         // Minimum possible object size. Objects smaller than that are ignored.
    Size max_obj_sz(150, 150);       // Maximum possible object size. Objects larger than that are ignored.

    if(loaded){
        cascade.detectMultiScale(currImage, found, scale_step, minNeighbors, 0, min_obj_sz, max_obj_sz);
    }

    /*
    for(int i=0; i<(int)found.size(); i++)
        rectangle(frameImage, found[i], Scalar(0,255,0), 2);
    */

    return (jint)found.size();
}

}   // extern "C"


void goodFeaturesToTrack_Demo(Mat& prevImage, vector<Point2f>& prevPoints)
{

    /// Parameters for Shi-Tomasi algorithm
    double qualityLevel = 0.15;	// 보통 0.1에서 0.01 정도의 값을 가진다.
    // 코너 판별을 위해 사용되는 임계값은 영상에서 구한 고유값들 중에서의 최대값과 quality_level을 곱하여 결정한다.
    double minDistance  = 5;		// 반환되는 코너 점들 사이의 최소거리
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