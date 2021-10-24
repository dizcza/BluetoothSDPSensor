package com.kyivaigroup.bluetoothsdpsensor.record;

public class DeviceInfo {
    public final SensorInfo sensorInfo;
    public final int recordId;
    public final long sdcardFreeMB;

    public DeviceInfo(SensorInfo sensorInfo, int recordId, long sdcardFreeMB) {
        this.sensorInfo = sensorInfo;
        this.recordId = recordId;
        this.sdcardFreeMB = sdcardFreeMB;
    }
}
