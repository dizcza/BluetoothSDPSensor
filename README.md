# Bluetooth SDPxx sensor plotter

Update 2024.04.22. Moved to https://github.com/dizcza/TcpDataViewer

Android client app to plot Sensirion SDP Sensor data sent by an ESP32 board (the server). The server app is [here](https://github.com/dizcza/esp32-sdpsensor).

Based on the official [BluetoothChat](https://github.com/android/connectivity-samples/tree/master/BluetoothChat) Android app. Powered by the [MPAndroidChart](https://github.com/PhilJay/MPAndroidChart) plotting library.

## Sensors

* SDP3x differential pressure sensor:
   * differential pressure, Pa
   * temperature, C
* BME280 (optional):
   * atmospheric pressure, Pa
   * humidity, %


## Available commands

The user can interact with the ESP32 board by sending the following commands via Bluetooth:

* `info`: get SDPxx sensor info and the SD card available memory
* `restart`: restart the ESP32 board (`esp_restart()` function is called)
* `format`: format the SD card and restart the board


## Screenshots

The plot below shows differential pressure sensor data read at the maximal ~2080 Hz frequency and sent over Bluetooth to the receiver (Android). The sensor used in the project is Sensirion SDP31 500 Pa.

<img src="screenshots/main.png" height="400" alt="Screenshot"/>


