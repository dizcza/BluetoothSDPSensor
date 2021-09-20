package com.kyivaigroup.bluetoothsdpsensor;

import android.content.Context;
import android.util.AttributeSet;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.kyivaigroup.bluetoothsdpsensor.record.RecordDP;

import java.util.ArrayList;
import java.util.List;

public class SensorLineChart extends LineChart {
    private static final long INVALIDATE_PERIOD = 500;  // ms
    private static final String CHART_LABEL = "Differential pressure, Pa";

    private long mLastInvalidate = 0;

    private final List<Entry> mChartEntries = new ArrayList<>();

    public SensorLineChart(Context context) {
        super(context);
    }

    public SensorLineChart(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SensorLineChart(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void clear() {
        super.clear();
        mChartEntries.clear();
    }

    public void update(RecordDP[] recordsDP) {
        for (RecordDP record : recordsDP) {
            Entry entry = new Entry(record.time / 1e6f, record.diffPressureRaw);
            mChartEntries.add(entry);
        }
        long tick = System.currentTimeMillis();
        if (tick - mLastInvalidate > INVALIDATE_PERIOD) {
            LineDataSet dataset = new LineDataSet(mChartEntries, CHART_LABEL);
            LineData data = new LineData(dataset);
            data.setDrawValues(true);
            setData(data);
            invalidate();
            mLastInvalidate = tick;
        }
    }
}
