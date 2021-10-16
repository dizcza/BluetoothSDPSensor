package com.kyivaigroup.bluetoothsdpsensor.record;

import java.util.List;

public class RecordCollection {

    public final RecordSDP[] recordsSDP;
    public final RecordBMP recordBMP;
    public final RecordStatus status;
    public SensorInfo sensorInfo;
    public long sdcardFreeMB;
    public final String[] logs;

    public RecordCollection(List<RecordSDP> recordsSDP, List<RecordBMP> recordBMP,
                            List<RecordStatus> statuses, String[] logs) {
        this.recordsSDP = recordsSDP.toArray(new RecordSDP[0]);
        this.recordBMP = recordBMP.size() > 0 ? recordBMP.get(recordBMP.size() - 1) : null;
        this.status = statuses.size() > 0 ? statuses.get(statuses.size() - 1) : null;
        this.logs = logs;
        this.sensorInfo = null;
        this.sdcardFreeMB = 0;
    }
}
