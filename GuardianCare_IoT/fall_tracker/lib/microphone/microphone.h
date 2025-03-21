#ifndef MICROPHONE_H
#define MICROPHONE_H

#include <M5StickCPlus.h>
#include <driver/i2s.h>

#define PIN_CLK 0
#define PIN_DATA 34
#define RECORD_TIME 10000
#define SAMPLE_RATE 16000
#define GAIN_FACTOR 10
#define WINDOW_SIZE 4096

class Microphone
{
private:
    int16_t *m_audioBuffer1;
    int16_t *m_audioBuffer2;
    int32_t m_audioBufferPos = 0;
    int16_t *m_currentAudioBuffer;
    int16_t *m_capturedAudioBuffer;
    int32_t m_bufferSizeInBytes;
    int32_t m_bufferSizeInSamples;
    TaskHandle_t m_recordingTaskHandle;
    TaskHandle_t m_processorTaskHandle;
    QueueHandle_t m_i2sQueue;
    
    void i2sInit();
    void addSample(int16_t sample);

public:
    Microphone() : isRecording(false), recordStartTime(0) {}
    bool isRecording;
    uint32_t recordStartTime;
        
    int16_t getBufferSizeInBytes() { return m_bufferSizeInBytes; }
    int16_t *getCapturedAudioBuffer() { return m_capturedAudioBuffer; }
    
    void setupMicrophone(TaskHandle_t processorTaskHandle);
    void startRecording();
    void stopRecording();

    friend void i2sReaderTask(void *param);
};

#endif
