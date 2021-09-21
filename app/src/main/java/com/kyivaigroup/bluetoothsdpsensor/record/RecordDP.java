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
                    "%02d %02d:%02d.%03d.%03d",
                    hh, mm, (ss % 60), ms, (us % 1000));
        } else {
            timeDelta = String.format(Locale.getDefault(),
                    "%02d:%02d.%03d.%03d",
                    mm, (ss % 60), ms, (us % 1000));
        }
        return timeDelta;
    }
}


public class RecordDP {

    public final short diffPressureRaw;
    public final long time;

    public RecordDP(short diffPressureRaw, long time) {
        this.diffPressureRaw = diffPressureRaw;
        this.time = time;
    }

    @NonNull
    public String toString() {
        return String.format(Locale.getDefault(),
                "[%s] %d",
                TimeDeltaFormatter.format(time),
                diffPressureRaw);
    }
}
