# BlueDisplay
Simple execution for controlling an LED display with Android devices via Bluetooth LE.

Simple execution for controlling an LED display with Android devices via Bluetooth LE.

The system is controlled by Arduino Uno, the communication is handled by HM-11 Bluetooth 4.0 module, and the display module is DFRobot LCD4884. Since LCD4884 takes up D2 through D7 it will not conflict with the Bluetooth module. In order to monitor serial I/O at the same time, I used SoftwareSerial to redefine the data I/O of BLE communication.

The Android application is capable of running on devices with Android 5.0+ and written with Android Studio 1.2.1.1.

The EAGLE folder includes a idea to merge the HM-11 BLE module with an Arduino Leonardo. The design was not tested though.
