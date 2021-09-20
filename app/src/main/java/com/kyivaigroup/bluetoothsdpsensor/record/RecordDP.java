package com.kyivaigroup.bluetoothsdpsensor.record;

import androidx.annotation.NonNull;

import java.util.Locale;


class TimeDeltaFormatter {
    public static String format(long us) {
        long ms = (us % 1_000_000) / 1000;
        long ss = us / 1_000_000;
        long mm = (ss % 3600) / 60;
        long hh = ss / 3600;
        String timeDelta;
        if (hh > 0) {
            timeDelta = String.format(Locale.getDefault(),
                    "%02d %02d:%02d.%03d %03d",
                    hh, mm, (ss % 60), ms, (us % 1000));
        } else {
            timeDelta = String.format(Locale.getDefault(),
                    "%02d:%02d.%03d %03d",
                    mm, (ss % 60), ms, (us % 1000));
        }
        return timeDelta;
    }
}


public class RecordDP {
    private static final int PRESSURE_SCALE = 60;

    public short diffPressureRaw;
    public long time;

    public RecordDP(short diffPressureRaw, long time) {
        this.diffPressureRaw = diffPressureRaw;
        this.time = time;
    }

    public float diffPressure() {
        return ((float) diffPressureRaw) / PRESSURE_SCALE;
    }

    @NonNull
    public String toString() {
        return String.format(Locale.getDefault(),
                "[%s] %.2f Pa",
                TimeDeltaFormatter.format(time),
                diffPressure());
    }
}
