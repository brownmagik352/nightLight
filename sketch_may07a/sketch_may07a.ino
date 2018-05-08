SYSTEM_MODE(MANUAL); 


#define COMMON_ANODE 1

const int POT_INPUT_PIN = A0;
const int RGB_RED_PIN = D0;
const int RGB_GREEN_PIN  = D1;
const int RGB_BLUE_PIN  = D2;
const int DELAY = 1000; // delay between changing colors

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
  // as you would expect with a power of two--e.g., 2^12 or 12 bits). need to map all 16^6 HEX colors 
  int ledVal; 
  ledVal = map(potVal, 0, 4092, 0, 255);

  setColor(ledVal, 0, 0);
  
  delay(1000);

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

