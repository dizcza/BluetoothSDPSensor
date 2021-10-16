package com.kyivaigroup.bluetoothsdpsensor.record;

public class RecordStatus {

    public int messagesCurr;
    public int messagesMax;
    public long readDurationMax;


    public RecordStatus(int messagesCurr, int messagesMax, long readDurationMax) {
        this.messagesCurr = messagesCurr;
        this.messagesMax = messagesMax;
        this.readDurationMax = readDurationMax;
    }
}
