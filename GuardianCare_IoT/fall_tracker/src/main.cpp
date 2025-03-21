#include <Application.h>

Application *application;

void setup()
{
    Serial.begin(115200);
    M5.begin();
    M5.Lcd.setRotation(3);
    M5.Lcd.fillScreen(BLACK);

    application = new Application();
    application->begin();
}

void loop()
{
    application->loop();
}
