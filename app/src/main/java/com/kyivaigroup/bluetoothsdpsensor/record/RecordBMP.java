package com.kyivaigroup.bluetoothsdpsensor.record;

import androidx.annotation.NonNull;

import java.util.Locale;

public class RecordBMP {
    public final float pressure;
    public final float temperature;
    public final float humidity;

    public RecordBMP(float pressure, float temperature, float humidity) {
        this.pressure = pressure;
        this.temperature = temperature;
        this.humidity = humidity;
    }

    @NonNull
    public String toString() {
        return String.format(Locale.getDefault(),
                "%.0f atm. Pa, %.1f humidity",
                this.pressure, this.humidity);
    }
}
