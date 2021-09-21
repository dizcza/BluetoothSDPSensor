package com.kyivaigroup.bluetoothsdpsensor.record;

import androidx.annotation.NonNull;

import java.util.Locale;

public class SensorInfo {
    public final int modelNum;
    public final int rangePa;
    public final int pressureScale;

    public SensorInfo(int modelNum, int rangePa, int pressureScale) {
        this.modelNum = modelNum;
        this.rangePa = rangePa;
        this.pressureScale = pressureScale;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format(Locale.getDefault(), "SDP%d %dPa", modelNum, rangePa);
    }
}
