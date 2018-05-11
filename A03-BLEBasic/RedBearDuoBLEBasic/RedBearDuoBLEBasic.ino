#include "ble_config.h"

/*
 * Simple Bluetooth Demo
 * This code shows that the user can send simple digital write data from the
 * Android app to the Duo board.
 * Created by Liang He, April 27th, 2018
 * 
 * The Library is created based on Bjorn's code for RedBear BLE communication: 
 * https://github.com/bjo3rn/idd-examples/tree/master/redbearduo/examples/ble_led
 * 
 * Our code is created based on the provided example code (Simple Controls) by the RedBear Team:
 * https://github.com/RedBearLab/Android
 */

#if defined(ARDUINO) 
SYSTEM_MODE(SEMI_AUTOMATIC); 
#endif

#define RECEIVE_MAX_LEN    5
#define BLE_SHORT_NAME_LEN 0x08 // must be in the range of [0x01, 0x09]
#define BLE_SHORT_NAME 'a','p','s','u','m','a','n'  // define each char but the number of char should be BLE_SHORT_NAME_LEN-1

/* Define the pins on the Duo board */
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

// global to supress loop when phone being used
bool PHONE_VALUES = false; 

// specific for step counting
const int RED_LED_PIN = D3;
int stepTimer = 0;


// UUID is used to find the device by other BLE-abled devices
static uint8_t service1_uuid[16]    = { 0x71,0x3d,0x00,0x00,0x50,0x3e,0x4c,0x75,0xba,0x94,0x31,0x48,0xf1,0x8d,0x94,0x1e };
static uint8_t service1_tx_uuid[16] = { 0x71,0x3d,0x00,0x03,0x50,0x3e,0x4c,0x75,0xba,0x94,0x31,0x48,0xf1,0x8d,0x94,0x1e };

// Define the configuration data
static uint8_t adv_data[] = {
  0x02,
  BLE_GAP_AD_TYPE_FLAGS,
  BLE_GAP_ADV_FLAGS_LE_ONLY_GENERAL_DISC_MODE, 
  
  BLE_SHORT_NAME_LEN,
  BLE_GAP_AD_TYPE_SHORT_LOCAL_NAME,
  BLE_SHORT_NAME, 
  
  0x11,
  BLE_GAP_AD_TYPE_128BIT_SERVICE_UUID_COMPLETE,
  0x1e,0x94,0x8d,0xf1,0x48,0x31,0x94,0xba,0x75,0x4c,0x3e,0x50,0x00,0x00,0x3d,0x71 
};

// Define the receive and send handlers
static uint16_t receive_handle = 0x0000; // recieve

static uint8_t receive_data[RECEIVE_MAX_LEN] = { 0x01 };

/**
 * @brief Callback for writing event.
 *
 * @param[in]  value_handle  
 * @param[in]  *buffer       The buffer pointer of writting data.
 * @param[in]  size          The length of writting data.   
 *
 * @retval 
 */
int bleWriteCallback(uint16_t value_handle, uint8_t *buffer, uint16_t size) {
  Serial.print("Write value handler: ");
  Serial.println(value_handle, HEX);

  if (receive_handle == value_handle) {
    memcpy(receive_data, buffer, RECEIVE_MAX_LEN);
    Serial.print("Write value: ");
    for (uint8_t index = 0; index < RECEIVE_MAX_LEN; index++) {
      Serial.print(receive_data[index], HEX);
      Serial.print(" ");
    }
    Serial.println(" ");
    
    /* Process the data */
    if (receive_data[0] == 0x01) { // command is to set RGB LED color
      PHONE_VALUES = true;
      setColor(receive_data[1], receive_data[2], receive_data[3]);
    } else if(receive_data[0] == 0x02) { // command is to set RED_LED step color
      PHONE_VALUES = true;
      stepTimer = DELAY * 2;
    } else {
      PHONE_VALUES = false;
    }
  }
  return 0;
}

void setup() {
  Serial.begin(115200);
  delay(5000);

  // Initialize ble_stack.
  ble.init();
  configureBLE(); //lots of standard initialization hidden in here - see ble_config.cpp
  // Set BLE advertising data
  ble.setAdvertisementData(sizeof(adv_data), adv_data);

  // Register BLE callback functions
  ble.onDataWriteCallback(bleWriteCallback);

  // Add user defined service and characteristics
  ble.addService(service1_uuid);
  receive_handle = ble.addCharacteristicDynamic(service1_tx_uuid, ATT_PROPERTY_NOTIFY|ATT_PROPERTY_WRITE|ATT_PROPERTY_WRITE_WITHOUT_RESPONSE, receive_data, RECEIVE_MAX_LEN);
  
  // BLE peripheral starts advertising now.
  ble.startAdvertising();
  Serial.println("BLE start advertising.");

  /* initialize all peripheral/pin modes */
  pinMode(POT_INPUT_PIN, INPUT);

  pinMode(PHOTO_INPUT_PIN, INPUT);
  
  pinMode(RGB_RED_PIN, OUTPUT);
  pinMode(RGB_GREEN_PIN, OUTPUT);
  pinMode(RGB_BLUE_PIN, OUTPUT);

  pinMode(RED_LED_PIN, OUTPUT);
}

void loop() {
  // output to RED LED for steps
  if (stepTimer > 0) {
    analogWrite(RED_LED_PIN, 255);
    stepTimer = stepTimer - DELAY;
  } else {
    analogWrite(RED_LED_PIN, 0);
  }
  
  // pot value
  int potVal = analogRead(POT_INPUT_PIN);
  // read in photovoltaic cell
  int photoVal = analogRead(PHOTO_INPUT_PIN);
  
  unsigned int hexVal = potToHex(potVal);
  struct RGB ledVal = hexToRGB(hexVal);
  struct RGB ledValBrightnessAdjusted = rgbLedBrightnessAdjuster(ledVal, photoVal);

  if (!PHONE_VALUES) setColor(ledValBrightnessAdjusted.r, ledValBrightnessAdjusted.g, ledValBrightnessAdjusted.b);
 
  delay(DELAY);
}

// *** HELPER/PROCESSING FUNCTIONS *** //

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
