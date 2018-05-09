/* A3 Breadboard code for nightlight */
/* Apurv Suman */
/* This samples heavily from Professor's stock code at https://github.com/jonfroehlich/CSE590Sp2018 */

SYSTEM_MODE(MANUAL); 


#define COMMON_ANODE 1


const int POT_INPUT_PIN = A0;

const int PHOTO_INPUT_PIN = A4;
const float MAX_VOLTAGE = 3.3;
const int MAX_ADC_VALUE = 4096;

const int RGB_RED_PIN = D0;
const int RGB_GREEN_PIN  = D1;
const int RGB_BLUE_PIN  = D2;
const int DELAY = 500; // delay between changing colors

// struct to store rgb values
struct RGB {
  int r, g, b;
};


void setup() {
  pinMode(POT_INPUT_PIN, INPUT);

  pinMode(PHOTO_INPUT_PIN, INPUT);
  
  pinMode(RGB_RED_PIN, OUTPUT);
  pinMode(RGB_GREEN_PIN, OUTPUT);
  pinMode(RGB_BLUE_PIN, OUTPUT);
  Serial.begin(9600);
}

void loop() {
  // pot value
  int potVal = analogRead(POT_INPUT_PIN);
  // read in photovoltaic cell
  int photoVal = analogRead(PHOTO_INPUT_PIN);
  
  unsigned int hexVal = potToHex(potVal);
  struct RGB ledVal = hexToRGB(hexVal);
  struct RGB ledValBrightnessAdjusted = rgbLedBrightnessAdjuster(ledVal, photoVal);

  setColor(ledValBrightnessAdjusted.r, ledValBrightnessAdjusted.g, ledValBrightnessAdjusted.b);
 
  delay(DELAY);
}



//function that goes from single pot value to hex color
unsigned int potToHex(int potVal) {
  unsigned int hexVal;
  hexVal = map(potVal, 0, 4092, 0, 256 * 256 * 256);
  return hexVal;
}

// function that goes from hex color value to rgb
struct RGB hexToRGB(unsigned int hexVal) {
  struct RGB ledVal; 

  // try this for hex to rgb: https://stackoverflow.com/questions/3723846/convert-from-hex-color-to-rgb-struct-in-c
  ledVal.r = (hexVal >> 16) & 0xFF;
  ledVal.g = (hexVal >> 8) & 0xFF;
  ledVal.b = (hexVal) & 0xFF;

  return ledVal;
}

// function that takes an RGB LED value and modifies brightness based on photovoltaic cell
struct RGB rgbLedBrightnessAdjuster(struct RGB original, int photoVal) {
  struct RGB brightnessAdjusted;

  // use brightness as the cap on intensity for each of RGB; inverted so darker lighting => brighter LED
  // based on http://forum.arduino.cc/index.php?topic=272862.0
  int brightness = map(photoVal, 0, 4096, 255, 0);
  
  // now modify RGB with brightness
  brightnessAdjusted.r = map(original.r, 0, 255, 0, brightness);
  brightnessAdjusted.g = map(original.g, 0, 255, 0, brightness);
  brightnessAdjusted.b = map(original.b, 0, 255, 0, brightness);
  
  return brightnessAdjusted;
}

void setColor(int red, int green, int blue) {
  #ifdef COMMON_ANODE
    red = 255 - red;
    green = 255 - green;
    blue = 255 - blue;
  #endif
  analogWrite(RGB_RED_PIN, red);
  analogWrite(RGB_GREEN_PIN, green);
  analogWrite(RGB_BLUE_PIN, blue);  
}


