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

  // pot value -> hex color -> rgb values
  // pot value
  int potVal = analogRead(POT_INPUT_PIN);

  struct RGB ledVal = potToHexToRGB(potVal); 

  setColor(ledVal.r, ledVal.g, ledVal.b);
 
  delay(DELAY);
}

// function that goes from a single pot value to hex color value to rgb
struct RGB potToHexToRGB(int potVal) {

  // ledVal to return
  struct RGB ledVal; 

  // hex color
  unsigned int hexVal;
  hexVal = map(potVal, 0, 4092, 0, 256 * 256 * 256);
  // try this for hex to rgb: https://stackoverflow.com/questions/3723846/convert-from-hex-color-to-rgb-struct-in-c
  ledVal.r = (hexVal >> 16) & 0xFF;
  ledVal.g = (hexVal >> 8) & 0xFF;
  ledVal.b = (hexVal) & 0xFF;

  // read in photovoltaic cell
  int photoVal = analogRead(PHOTO_INPUT_PIN);
  // use brightness as the cap on intensity for each of RGB; inverted so darker lighting => brighter LED
  // based on http://forum.arduino.cc/index.php?topic=272862.0
  int brightness = map(photoVal, 0, 4096, 255, 0);
  // now modify RGB with brightness
  ledVal.r = map(ledVal.r, 0, 255, 0, brightness);
  ledVal.g = map(ledVal.g, 0, 255, 0, brightness);
  ledVal.b = map(ledVal.b, 0, 255, 0, brightness);

  return ledVal;
  
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


