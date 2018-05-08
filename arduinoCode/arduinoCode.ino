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

const int POT_INPUT_PIN = A0;
const int RGB_RED_PIN = D0;
const int RGB_GREEN_PIN  = D1;
const int RGB_BLUE_PIN  = D2;
const int DELAY = 500; // delay between changing colors


void setup() {
  pinMode(POT_INPUT_PIN, INPUT);
  pinMode(RGB_RED_PIN, OUTPUT);
  pinMode(RGB_GREEN_PIN, OUTPUT);
  pinMode(RGB_BLUE_PIN, OUTPUT);
  Serial.begin(9600);
}

void loop() {

  // read the potentiometer value
  int potVal = analogRead(POT_INPUT_PIN);

  
  // the analogRead on the RedBear Duo seems to go from 0 to 4092 (and not 4095
  // as you would expect with a power of two--e.g., 2^12 or 12 bits). 
  // now need to map all 16^6 HEX colors 

  
  unsigned int hexVal;
  hexVal = map(potVal, 0, 4092, 0, 256 * 256 * 256);
  
  Serial.print(potVal);
  Serial.print(",");
  Serial.print(hexVal);

  // try this for rgb: https://stackoverflow.com/questions/3723846/convert-from-hex-color-to-rgb-struct-in-c
  
  int r = (hexVal >> 16) & 0xFF;
  int g = (hexVal >> 8) & 0xFF;
  int b = (hexVal) & 0xFF;
  setColor(r,g,b);
  

  //hexToRGB(hexVal);
  
  delay(DELAY);
}


// algo taken from http://scanftree.com/programs/c/c-code-to-convert-decimal-to-hexadecimal/
void hexToRGB(unsigned int d)
{
  int digits[6] = {0, 0, 0, 0, 0, 0};
  unsigned int quotient = d;

  for (int i = 0; i < 6; i++) {

    if (quotient == 0) {
      continue;
    } 

    digits[i] = quotient % 16;
    quotient = quotient / 16;
  }

  int red = 16 * digits[5] + digits[4];
  int green = 16 * digits[3] + digits[2];
  int blue = 16 * digits[1] + digits[0];

  setColor(red, green, blue);
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


