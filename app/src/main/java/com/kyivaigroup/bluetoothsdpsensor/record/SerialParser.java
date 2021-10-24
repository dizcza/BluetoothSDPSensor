package com.kyivaigroup.bluetoothsdpsensor.record;

import com.kyivaigroup.bluetoothsdpsensor.Constants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SerialParser {
    private final StringBuilder mCommand = new StringBuilder();
    private final StringBuilder mLog = new StringBuilder();

    private final List<RecordSDP> mRecordsSDP = new ArrayList<>();
    private final List<RecordBMP> mRecordsBMP = new ArrayList<>();
    private final List<RecordStatus> mRecordsStatus = new ArrayList<>();
    private DeviceInfo mDeviceInfo;

    private final static Pattern patternSDP = Pattern.compile("D(-?\\d+)t(\\d+)");
    private final static Pattern patternBMP = Pattern.compile("P(\\d+)T(\\d+\\.\\d)H(\\d+\\.\\d)");
    private final static Pattern patternStatus = Pattern.compile("S(\\d+) m(\\d+) r(\\d+)");
    private final static Pattern patternInfo = Pattern.compile("I(\\d+) r(\\d+) s(\\d+) i(\\d+) m(\\d+)");
    private final static Pattern patternClock = Pattern.compile("C(\\d+)");
    private final static Pattern patternLog = Pattern.compile("\\u001B\\[0m");

    public void receive(byte[] data, int size) {
        for (int i = 0; i < size; i++) {
            char item = (char) data[i];
            switch (item) {
                case '\r':
                    // ignore \r
                    break;
                case '\n':
                    process();
                    mCommand.setLength(0);
                    break;
                default:
                    mCommand.append(item);
                    break;
            }
        }
    }

    public boolean hasRecords() {
        return mRecordsSDP.size() > 0;
    }

    public RecordCollection consumeRecords() {
        String logStr = mLog.toString().replaceAll("\\u001B\\[0;3\\dm", "");
        String[] logs = patternLog.split(logStr);
        mLog.setLength(0);
        if (!logStr.endsWith(Constants.ANSI_RESET)) {
            mLog.append(logs[logs.length - 1]);
            logs = Arrays.copyOf(logs, logs.length - 1);
        }
        RecordCollection collection = new RecordCollection(mRecordsSDP, mRecordsBMP, mRecordsStatus, logs);
        if (mDeviceInfo != null) {
            collection.deviceInfo = mDeviceInfo;
            mDeviceInfo = null;  // obtain the info only once
        }
        mRecordsSDP.clear();
        mRecordsBMP.clear();
        mRecordsStatus.clear();
        return collection;
    }

    private void process() {
        if (mCommand.length() == 0) {
            return;
        }
        switch (mCommand.charAt(0)) {
            case 'D': {
                // Diff pressure, time: D<int16>t<int64>
                Matcher matcher = patternSDP.matcher(mCommand);
                if (matcher.matches()) {
                    short dp = Short.parseShort(matcher.group(1));
                    long time = Long.parseLong(matcher.group(2));
                    mRecordsSDP.add(new RecordSDP(dp, time));
                }
                break;
            }
            case 'P': {
                // Atm. pressure, temperature, humidity: P<float>T<float>H<float>
                Matcher matcher = patternBMP.matcher(mCommand);
                if (matcher.matches()) {
                    float pressure = Float.parseFloat(matcher.group(1));
                    float temperature = Float.parseFloat(matcher.group(2));
                    float humidity = Float.parseFloat(matcher.group(3));
                    mRecordsBMP.add(new RecordBMP(pressure, temperature, humidity));
                }
                break;
            }
            case 'S': {
                /*
                 * Status:
                 *   - S<int> current queue size
                 *   - m<int> max queue size
                 *   - r<long> max read duration
                 */
                Matcher matcher = patternStatus.matcher(mCommand);
                if (matcher.matches()) {
                    mRecordsStatus.add(new RecordStatus(
                            Integer.parseInt(matcher.group(1)),
                            Integer.parseInt(matcher.group(2)),
                            Long.parseLong(matcher.group(3))
                    ));
                }
                break;
            }
            case 'C': {
                // Clock: C<clock_time:long>
                Matcher mather = patternClock.matcher(mCommand);
                if (mather.matches()) {
                    long clock = Long.parseLong(mather.group(1));
                    if (mRecordsSDP.size() > 0) {
                        RecordSDP currRecord = mRecordsSDP.get(mRecordsSDP.size() - 1);
                        currRecord.setClockTick(clock);
                    }
                }
                break;
            }
            case 'I': {
                // Sensor info: I<model_number:int> r<range_pa:int> s<pressure_scale:int> i<record_id:int> m<memory_free:long>
                Matcher matcher = patternInfo.matcher(mCommand);
                if (matcher.matches()) {
                    int modelNum = Integer.parseInt(matcher.group(1));
                    int rangePa = Integer.parseInt(matcher.group(2));
                    int pressureScale = Integer.parseInt(matcher.group(3));
                    int recordId = Integer.parseInt(matcher.group(4));
                    long sdcardFree = Long.parseLong(matcher.group(5)) / (1 << 20);
                    SensorInfo sensorInfo = new SensorInfo(modelNum, rangePa, pressureScale);
                    mDeviceInfo = new DeviceInfo(sensorInfo, recordId, sdcardFree);
                    break;
                }
                break;
            }
            default: {
                mLog.append(mCommand);
                break;
            }
        }
    }
}
