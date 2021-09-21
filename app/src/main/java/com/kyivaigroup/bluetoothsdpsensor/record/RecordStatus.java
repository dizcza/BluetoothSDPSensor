package com.kyivaigroup.bluetoothsdpsensor.record;

public class RecordStatus {

    public int messagesCurr;
    public int messagesMax;
    public int readsFailed;
    public long readDurationMax;


    public RecordStatus(int messagesCurr, int messagesMax,
                        int readsFailed, long readDurationMax) {
        this.messagesCurr = messagesCurr;
        this.messagesMax = messagesMax;
        this.readsFailed = readsFailed;
        this.readDurationMax = readDurationMax;
    }
}
