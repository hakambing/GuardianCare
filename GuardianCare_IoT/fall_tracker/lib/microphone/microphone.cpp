#include "microphone.h"

void i2sReaderTask(void *param)
{
    Microphone *sampler = (Microphone *)param;
    while (true)
    {
        i2s_event_t evt;
        if (xQueueReceive(sampler->m_i2sQueue, &evt, portMAX_DELAY) != pdPASS)
        {
            continue;
        }

        if (evt.type != I2S_EVENT_RX_DONE)
        {
            continue;
        }

        size_t bytesRead = 0;
        do
        {
            if (!sampler->isRecording)
            {
                vTaskDelay(pdMS_TO_TICKS(100));
                continue;
            }
            else
            {
                int16_t i2sData[2048];
                i2s_read(I2S_NUM_0, i2sData, 4096, &bytesRead, 10);
                for (int i = 0; i < bytesRead / 2; i += 4)
                {
                    sampler->addSample((i2sData[i] + i2sData[i + 1] + i2sData[i + 2] + i2sData[i + 3]) / 4);
            }
            }
        } while (bytesRead > 0);
    }
}

void Microphone::i2sInit()
{
    i2s_config_t i2s_config = {
        .mode = (i2s_mode_t)(I2S_MODE_MASTER | I2S_MODE_RX | I2S_MODE_PDM),
        .sample_rate = SAMPLE_RATE,
        .bits_per_sample = I2S_BITS_PER_SAMPLE_16BIT,
        .channel_format = I2S_CHANNEL_FMT_ALL_RIGHT,
        #if ESP_IDF_VERSION > ESP_IDF_VERSION_VAL(4, 1, 0)
            .communication_format = I2S_COMM_FORMAT_STAND_I2S,
        #else
            .communication_format = I2S_COMM_FORMAT_I2S,
        #endif
        .intr_alloc_flags = ESP_INTR_FLAG_LEVEL1,
        .dma_buf_count = 4,
        .dma_buf_len = 1024,
    };

    i2s_pin_config_t i2s_pins;
    #if (ESP_IDF_VERSION > ESP_IDF_VERSION_VAL(4, 3, 0))
        i2s_pins.mck_io_num = I2S_PIN_NO_CHANGE;
    #endif
    i2s_pins.bck_io_num = I2S_PIN_NO_CHANGE;
    i2s_pins.ws_io_num = PIN_CLK;
    i2s_pins.data_out_num = I2S_PIN_NO_CHANGE;
    i2s_pins.data_in_num = PIN_DATA;

    i2s_driver_install(I2S_NUM_0, &i2s_config, 4, &m_i2sQueue);
    i2s_set_pin(I2S_NUM_0, &i2s_pins);
    i2s_set_clk(I2S_NUM_0, i2s_config.sample_rate, i2s_config.bits_per_sample, I2S_CHANNEL_MONO);

    xTaskCreatePinnedToCore(i2sReaderTask, "i2s Reader Task", 8192, this, 1, &m_recordingTaskHandle, 0);
}

void Microphone::addSample(int16_t sample)
{
    // Serial.printf("Sample: %d\n", sample);
    m_currentAudioBuffer[m_audioBufferPos] = sample;
    m_audioBufferPos++;
    if (m_audioBufferPos == m_bufferSizeInSamples)
    {
        std::swap(m_currentAudioBuffer, m_capturedAudioBuffer);
        m_audioBufferPos = 0;
        xTaskNotify(m_processorTaskHandle, 1, eIncrement);
    }
}

void Microphone::setupMicrophone(TaskHandle_t processorTaskHandle)
{
    m_processorTaskHandle = processorTaskHandle;
    m_bufferSizeInSamples = WINDOW_SIZE;
    m_bufferSizeInBytes = WINDOW_SIZE * sizeof(int16_t);
    m_audioBuffer1 = (int16_t *)malloc(m_bufferSizeInBytes);
    m_audioBuffer2 = (int16_t *)malloc(m_bufferSizeInBytes);

    m_currentAudioBuffer = m_audioBuffer1;
    m_capturedAudioBuffer = m_audioBuffer2;

    i2sInit();
}

void Microphone::startRecording()
{
    isRecording = true;
    recordStartTime = millis();
}

void Microphone::stopRecording()
{
    memset(m_capturedAudioBuffer, 0, m_bufferSizeInBytes);
    isRecording = false;
    recordStartTime = 0;
}
