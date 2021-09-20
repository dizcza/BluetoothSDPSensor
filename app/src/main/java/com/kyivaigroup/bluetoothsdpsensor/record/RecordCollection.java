package com.kyivaigroup.bluetoothsdpsensor.record;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RecordCollection {

    public final List<RecordDP> recordsDP;
    public final List<RecordP> recordsP;
    public final List<Float> temperatures;

    public RecordCollection(List<RecordDP> recordsDP, List<RecordP> recordsP, List<Float> temperatures) {
        this.recordsDP = new ArrayList<>(recordsDP);
        this.recordsP = new ArrayList<>(recordsP);
        this.temperatures = new ArrayList<>(temperatures);
    }

    public String summary() {
        return String.format(Locale.getDefault(),
                "%d Diff Pressure, %d Atm. Pressure, %d Temperature records",
                recordsDP.size(), recordsP.size(), temperatures.size());
    }
}
