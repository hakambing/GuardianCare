#ifndef FALL_DETECT_H
#define FALL_DETECT_H

#include <M5StickCPlus.h>
#include <math.h>

#define FALL_SAMPLE_RATE 100 // 100ms sampling time; use in the main loop() function
#define ACCL_BUFFER_SIZE 10
#define NUM_FREEFALL_SAMPLES 2
#define NO_ACTVITY_WAIT_TIME 2000

// Thresholds from the paper
#define IMPACT_TRIGGER_THRESHOLD 2.0
#define FREEFALL_TRIGGER_THRESHOLD 0.5
#define STILL_MIN_G 0.7
#define STILL_MAX_G 1.3
#define WALK_MIN_SD 0.1
#define WALK_MAX_SD 0.5
#define STILL_SD_THRESHOLD 0.3
#define ACTIVITY_THRESHOLD 1.5

#define NORMAL_ACTIVITY 0
#define FREEFALL_IMPACT_DETECTED 1
#define CHECKING_NO_ACTVITY 1
#define FALL_DETECTED 2
#define RECOVERED 3

class FallDetection {
    private:
        // Circular buffer for acceleration values
        float accelBuffer[ACCL_BUFFER_SIZE];
        int pos; // Current position in circular buffer
        bool freefallDetected;
        unsigned long freefallStartTime;

    public:
        float ax;
        float ay;
        float az;
        int state;
    
        FallDetection();
        void setupFallDetection();
        float calculateSD(float *values, int size);
        void getLastNSamples(float *output, int N);
        bool checkFreefall();
        bool checkStillState();
        bool detectFall();
    };

#endif
