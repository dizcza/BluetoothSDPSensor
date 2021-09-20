package com.kyivaigroup.bluetoothsdpsensor.record;

import androidx.annotation.NonNull;

import java.util.Locale;

public class RecordP {
    public float pressure;
    public float humidity;

    public RecordP(float pressure, float humidity) {
        this.pressure = pressure;
        this.humidity = humidity;
    }

    @NonNull
    public String toString() {
        return String.format(Locale.getDefault(),
                "%.0f atm. Pa, %.1f humidity",
                this.pressure, this.humidity);
    }
}
