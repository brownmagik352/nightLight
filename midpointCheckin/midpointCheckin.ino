/* This samples heavily from Professor's stock code at https://github.com/jonfroehlich/CSE590Sp2018 */

/*
 * This example reads in a potentiometer value (from A0) and sets the brightness 
 * of an LED (hooked up to D0).
 * 
 * The analogWrite() function uses PWM, so if you want to change the pin you're
 * using, be sure to use another PWM capable pin. On the Arduino Uno, the PWM pins
 * are identified with a "~" sign, like ~3, ~5, ~6, ~9, ~10 and ~11. On the RedBear
 * Duo, please consult the pin layout data sheet provided in class or the lecture slides.
 *
 * By Jon Froehlich for CSE590
 * http://makeabilitylab.io
 */

/* 
 * IMPORTANT: When working with the RedBear Duo, you must have this line of
 * code at the top of your program. The default state is SYSTEM_MODE(AUTOMATIC);
 * however, this puts the RedBear Duo in a special cloud-based mode that we 
 * are not using. For our purposes, set SYSTEM_MODE(SEMI_AUTOMATIC) or
 * SYSTEM_MODE(MANUAL). See https://docs.particle.io/reference/firmware/photon/#system-modes
 */
SYSTEM_MODE(MANUAL); 


#define COMMON_ANODE 1


const int POT_INPUT_PIN_R = A0;
const int POT_INPUT_PIN_G = A1;
const int POT_INPUT_PIN_B = A2;

const int PHOTO_INPUT_PIN = A4;
const float MAX_VOLTAGE = 3.3;
const int MAX_ADC_VALUE = 4096;

const int RGB_RED_PIN = D0;
const int RGB_GREEN_PIN  = D1;
const int RGB_BLUE_PIN  = D2;
const int DELAY = 500; // delay between changing colors


void setup() {
  pinMode(POT_INPUT_PIN_R, INPUT);
  pinMode(POT_INPUT_PIN_G, INPUT);
  pinMode(POT_INPUT_PIN_B, INPUT);

  pinMode(PHOTO_INPUT_PIN, INPUT);
  
  pinMode(RGB_RED_PIN, OUTPUT);
  pinMode(RGB_GREEN_PIN, OUTPUT);
  pinMode(RGB_BLUE_PIN, OUTPUT);
  Serial.begin(9600);
}

void loop() {
  
  // read in photovoltaic cell
  int photoVal = analogRead(PHOTO_INPUT_PIN);
  // use brightness as the cap on intensity for each of RGB; inverted so darker lighting => brighter LED
  // pulled from http://forum.arduino.cc/index.php?topic=272862.0
  int brightness = map(photoVal, 0, 4096, 255, 0);

  // read the potentiometer value
  int potValR = analogRead(POT_INPUT_PIN_R);
  int ledValR = map(potValR, 0, 4092, 0, brightness);

  int potValG = analogRead(POT_INPUT_PIN_G);
  int ledValG = map(potValG, 0, 4092, 0, brightness);
  
  int potValB = analogRead(POT_INPUT_PIN_B);
  int ledValB = map(potValB, 0, 4092, 0, brightness);

  setColor(ledValR, ledValG, ledValB);
 
  delay(DELAY);
}

void setColor(int red, int green, int blue)
{
  #ifdef COMMON_ANODE
    red = 255 - red;
    green = 255 - green;
    blue = 255 - blue;
  #endif
  analogWrite(RGB_RED_PIN, red);
  analogWrite(RGB_GREEN_PIN, green);
  analogWrite(RGB_BLUE_PIN, blue);  
}


