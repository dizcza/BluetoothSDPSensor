package com.kyivaigroup.bluetoothsdpsensor.record;

import com.jjoe64.graphview.series.DataPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RecordCollection {

    public final RecordDP[] recordsDP;
    public final RecordP pressureHumidity;
    public final Float temperature;

    public RecordCollection(List<RecordDP> recordsDP, List<RecordP> recordsP, List<Float> temperatures) {
        this.recordsDP = recordsDP.toArray(new RecordDP[0]);
        this.pressureHumidity = recordsP.size() > 0 ? recordsP.get(recordsP.size() - 1) : null;
        this.temperature = temperatures.size() > 0 ? temperatures.get(temperatures.size() - 1) : null;
    }

    public DataPoint[] toDataPoints() {
        DataPoint[] dataPoints = new DataPoint[recordsDP.length];
        for (int i = 0; i < recordsDP.length; i++) {
            dataPoints[i] = new DataPoint(recordsDP[i].time, recordsDP[i].diffPressureRaw);
        }
        return dataPoints;
    }
}
