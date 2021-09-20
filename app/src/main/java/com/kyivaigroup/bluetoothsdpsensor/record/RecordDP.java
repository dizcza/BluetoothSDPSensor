package com.kyivaigroup.bluetoothsdpsensor.record;

import androidx.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RecordDP {
    private static final int PRESSURE_SCALE = 60;

    public short diffPressureRaw;
    public long time;

    public RecordDP(short diffPressureRaw, long time) {
        this.diffPressureRaw = diffPressureRaw;
        this.time = time;
    }

    public float diffPressure() {
        return diffPressureRaw * 1.f / PRESSURE_SCALE;
    }

    @NonNull
    public String toString() {
        return String.format(Locale.getDefault(),
                "[%s] %.2f Pa",
                (new SimpleDateFormat("hh mm:ss")).format(new Date(time)),
                diffPressure());
    }
}
