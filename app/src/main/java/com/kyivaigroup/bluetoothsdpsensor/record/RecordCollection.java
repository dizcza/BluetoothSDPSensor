package com.kyivaigroup.bluetoothsdpsensor.record;

import java.util.List;

public class RecordCollection {

    public final RecordDP[] recordsDP;
    public final RecordP pressureHumidity;
    public final Float temperature;

    public RecordCollection(List<RecordDP> recordsDP, List<RecordP> recordsP, List<Float> temperatures) {
        this.recordsDP = recordsDP.toArray(new RecordDP[0]);
        this.pressureHumidity = recordsP.size() > 0 ? recordsP.get(recordsP.size() - 1) : null;
        this.temperature = temperatures.size() > 0 ? temperatures.get(temperatures.size() - 1) : null;
    }
}
