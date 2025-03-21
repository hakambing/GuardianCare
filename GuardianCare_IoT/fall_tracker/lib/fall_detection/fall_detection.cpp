#include <fall_detection.h>

FallDetection::FallDetection() : pos(0), freefallDetected(false), freefallStartTime(0)
{
    for (int i = 0; i < ACCL_BUFFER_SIZE; i++)
    {
        accelBuffer[i] = 1.0;
    }
}

void FallDetection::setupFallDetection()
{
    M5.IMU.Init();
    Serial.println("Enhanced fall detection system initialized...");
}

float FallDetection::calculateSD(float *values, int size)
{
    float sum = 0.0, mean, SD = 0.0;

    for (int i = 0; i < size; i++)
    {
        sum += values[i];
    }
    mean = sum / size;

    for (int i = 0; i < size; i++)
    {
        SD += pow(values[i] - mean, 2);
    }
    return sqrt(SD / size);
}

void FallDetection::getLastNSamples(float *output, int N)
{
    for (int i = 0; i < N; i++)
    {
        int idx = (pos - N + i + ACCL_BUFFER_SIZE) % ACCL_BUFFER_SIZE;
        output[i] = accelBuffer[idx];
    }
}

bool FallDetection::checkFreefall()
{
    // The below code is commented out because it checks whether the user is walking or not which is ultra specific
    // // Reorganizing last 10 acceleration values in time order
    // float orderedSamples[ACCL_BUFFER_SIZE];
    // getLastNSamples(orderedSamples, ACCL_BUFFER_SIZE);

    // // Calculating SD of acceleration
    // float sd = calculateSD(orderedSamples, ACCL_BUFFER_SIZE);

    // // Check if SD is in valid range (0.1 < SD < 0.5)
    // if (sd >= WALK_MAX_SD)
    // {
    //     Serial.println("User was not walking, skipping free fall check");
    //     return false;
    // }
    // Serial.println("User was walking, checking for freefall...");

    // Finding freefall from last 5 samples
    float lastOrderedSamples[ACCL_BUFFER_SIZE / 2];
    int numOfFreeFallSamples = 0;
    getLastNSamples(lastOrderedSamples, 5);

    // Check if freefall is found in any of last 5 samples
    for (int i = 0; i < ACCL_BUFFER_SIZE / 2; i++)
    {
        if (lastOrderedSamples[i] < FREEFALL_TRIGGER_THRESHOLD)
        {
            numOfFreeFallSamples++;
            Serial.println("Free fall sample detected");
        }
    }

    if (numOfFreeFallSamples < NUM_FREEFALL_SAMPLES)
    {
        Serial.println("Not enough free fall samples detected");
        return false;
    }

    // // Check if continuous freefall is found in any of last 5 samples
    // const int noOfFreefallSamples = min(ACCL_BUFFER_SIZE, FREEFALL_TIME / FALL_SAMPLE_RATE);
    // Serial.printf("Checking for continuous freefall in last %d samples...\n", noOfFreefallSamples);
    // int freefallCount = 0;
    // for (int i = ACCL_BUFFER_SIZE - 1; i >= 0; i--)
    // {
    //     freefallCount = lastOrderedSamples[i] < FREEFALL_TRIGGER_THRESHOLD ? freefallCount + 1 : 0;
    //     if (freefallCount >= noOfFreefallSamples)
    //     {
    //         Serial.println("Free fall detected!");
    //         return true;
    //     }
    // }

    Serial.println("Free fall detected!");
    return true;
}

bool FallDetection::checkStillState()
{
    float orderedSamples[ACCL_BUFFER_SIZE];
    getLastNSamples(orderedSamples, ACCL_BUFFER_SIZE);

    if (orderedSamples[ACCL_BUFFER_SIZE - 1] <= STILL_MIN_G || orderedSamples[ACCL_BUFFER_SIZE - 1] >= STILL_MAX_G)
    {
        Serial.println("Latest sample was not still.");
        return false;
    }

    int p = 0;
    for (int i = ACCL_BUFFER_SIZE - 1; i >= 0; i--)
    {
        if (orderedSamples[i] < ACTIVITY_THRESHOLD)
        {
            p++;
        }
        else
        {
            break;
        }
    }

    if (p <= ACCL_BUFFER_SIZE / 2)
    {
        Serial.println("Not enough continuous low activity samples.");
        return false;
    }

    float sdValues[ACCL_BUFFER_SIZE];
    for (int i = 0; i < p; i++)
    {
        sdValues[i] = orderedSamples[ACCL_BUFFER_SIZE - 1 - i];
    }
    float sdp = calculateSD(sdValues, p);

    if (sdp < STILL_SD_THRESHOLD)
    {
        Serial.println("Still state detected, fall confirmed!");
        return true;
    }

    Serial.println("Still state not detected because SD does not indicate still.");
    return false;
}

bool FallDetection::detectFall()
{
    M5.IMU.getAccelData(&ax, &ay, &az);
    float accMagnitude = sqrt(ax * ax + ay * ay + az * az);

    // Store in circular buffer
    accelBuffer[pos] = accMagnitude;
    pos = (pos + 1) % ACCL_BUFFER_SIZE;

    if (!freefallDetected)
    {
        if (accMagnitude > IMPACT_TRIGGER_THRESHOLD)
        {
            Serial.println("Impact detected, checking for freefall...");
            if (checkFreefall())
            {
                freefallDetected = true;
                freefallStartTime = millis();
                return false;
            }
        }
    }
    else
    {
        if (millis() - freefallStartTime >= NO_ACTVITY_WAIT_TIME)
        {
            Serial.println("Checking if user is still...");
            freefallDetected = false;
            return checkStillState();
        }
    }

    return false;
}
