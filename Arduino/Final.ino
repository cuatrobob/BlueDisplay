#include "LCD4884.h"
#include <SoftwareSerial.h>

SoftwareSerial BLE(1, 0); // define SoftwareSerial I/O ports

String str = "";
char charBuf[84]; // define a String and a char array for Serial reading

void setup() {
  
  lcd.LCD_init();
  lcd.LCD_clear(); // LCD initialization
  
  BLE.begin(9600); // set SoftwareSerial communication baud rate
  
}

void loop () {

  if (BLE.available()) {
    
    lcd.LCD_clear();
    str = BLE.readString();
    BLE.print(str); // read input string from SoftwareSerial

    str.toCharArray(charBuf, 84); // convert the string into a char array
  
    lcd.LCD_write_string(0, 0, charBuf, MENU_NORMAL); // display on LCD
  
    delay(50); 
    
}
}
