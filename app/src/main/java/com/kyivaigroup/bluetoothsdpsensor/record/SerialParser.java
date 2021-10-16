package com.kyivaigroup.bluetoothsdpsensor.record;

import android.util.Log;

import com.kyivaigroup.bluetoothsdpsensor.Constants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SerialParser {
    private final StringBuilder mCommand = new StringBuilder();
    private final StringBuilder mLog = new StringBuilder();

    private final List<RecordDP> mRecordsDP = new ArrayList<>();
    private final List<RecordP> mRecordsP = new ArrayList<>();
    private final List<Float> mTemperatures = new ArrayList<>();
    private final List<RecordStatus> mRecordsStatus = new ArrayList<>();
    private SensorInfo mSensorInfo;
    private long mSDCardFreeMB = 0;

    private final static Pattern patternDP = Pattern.compile("D(-?\\d+)t(\\d+)");
    private final static Pattern patternP = Pattern.compile("P(\\d+)H(\\d+\\.\\d)");
    private final static Pattern patternT = Pattern.compile("T(\\d+\\.\\d)");
    private final static Pattern patternStatus = Pattern.compile("S(\\d+)m(\\d+)r(\\d+)");
    private final static Pattern patternInfo = Pattern.compile("I(\\d+)r(\\d+)s(\\d+)m(\\d+)");
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
        return mRecordsDP.size() > 0;
    }

    public RecordCollection consumeRecords() {
        String logStr = mLog.toString().replaceAll("\\u001B\\[0;3\\dm", "");
        String[] logs = patternLog.split(logStr);
        mLog.setLength(0);
        if (!logStr.endsWith(Constants.ANSI_RESET)) {
            mLog.append(logs[logs.length - 1]);
            logs = Arrays.copyOf(logs, logs.length - 1);
        }
        RecordCollection collection = new RecordCollection(mRecordsDP, mRecordsP, mTemperatures,
                mRecordsStatus, logs);
        if (mSensorInfo != null) {
            collection.sensorInfo = mSensorInfo;
            collection.sdcardFreeMB = mSDCardFreeMB;
            mSensorInfo = null;  // obtain the info only once
        }
        mRecordsDP.clear();
        mRecordsP.clear();
        mTemperatures.clear();
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
                Matcher matcher = patternDP.matcher(mCommand);
                if (matcher.matches()) {
                    short dp = Short.parseShort(matcher.group(1));
                    long time = Long.parseLong(matcher.group(2));
                    mRecordsDP.add(new RecordDP(dp, time));
                }
                break;
            }
            case 'T': {
                // Temperature: T<float>
                Matcher matcher = patternT.matcher(mCommand);
                if (matcher.matches()) {
                    float temperature = Float.parseFloat(matcher.group(1));
                    mTemperatures.add(temperature);
                }
                break;
            }
            case 'P': {
                // Atm. pressure, humidity: P<float>H<float>
                Matcher matcher = patternP.matcher(mCommand);
                if (matcher.matches()) {
                    float pressure = Float.parseFloat(matcher.group(1));
                    float humidity = Float.parseFloat(matcher.group(2));
                    mRecordsP.add(new RecordP(pressure, humidity));
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
                    if (mRecordsDP.size() > 0) {
                        RecordDP currRecord = mRecordsDP.get(mRecordsDP.size() - 1);
                        currRecord.setClockTick(clock);
                    }
                }
                break;
            }
            case 'I': {
                // Sensor info: I<model_number:int>r<range_pa:int>s<pressure_scale:int>
                Matcher matcher = patternInfo.matcher(mCommand);
                if (matcher.matches()) {
                    int modelNum = Integer.parseInt(matcher.group(1));
                    int rangePa = Integer.parseInt(matcher.group(2));
                    int pressureScale = Integer.parseInt(matcher.group(3));
                    mSDCardFreeMB = Long.parseLong(matcher.group(4)) / (1 << 20);
                    mSensorInfo = new SensorInfo(modelNum, rangePa, pressureScale);
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
