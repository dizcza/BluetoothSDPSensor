package com.kyivaigroup.bluetoothsdpsensor.record;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SerialParser {
    private final StringBuilder mCommand = new StringBuilder();
    private final List<RecordDP> mRecordsDP = new ArrayList<>();
    private final List<RecordP> mRecordsP = new ArrayList<>();
    private final List<Float> mTemperatures = new ArrayList<>();

    private final static Pattern patternDP = Pattern.compile("D(\\d+)t(\\d+)");
    private final static Pattern patternP = Pattern.compile("P(\\d+)H(\\d+\\.\\d)");
    private final static Pattern patternT = Pattern.compile("T(\\d+\\.\\d)");

    public boolean receive(byte[] data, int size) {
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
        return hasRecords();
    }

    public boolean hasRecords() {
        return mRecordsDP.size() > 0 || mRecordsP.size() > 0 || mTemperatures.size() > 0;
    }

    public RecordCollection consumeRecords() {
        RecordCollection collection = new RecordCollection(mRecordsDP, mRecordsP, mTemperatures);
        mRecordsDP.clear();
        mRecordsP.clear();
        mTemperatures.clear();
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
        }
    }
}
