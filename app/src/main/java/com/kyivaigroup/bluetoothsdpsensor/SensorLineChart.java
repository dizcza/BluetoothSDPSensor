package com.kyivaigroup.bluetoothsdpsensor;

import android.content.Context;
import android.graphics.Paint;
import android.util.AttributeSet;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.kyivaigroup.bluetoothsdpsensor.record.RecordCollection;
import com.kyivaigroup.bluetoothsdpsensor.record.RecordDP;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SensorLineChart extends LineChart {
    private static final int MAX_POINTS_KEEP = 10_000;
    private static final String CHART_LABEL = "Differential pressure, Pa";

    private List<Entry> mChartEntries = new ArrayList<>();

    public SensorLineChart(Context context) {
        super(context);
        prepare(context);
    }

    public SensorLineChart(Context context, AttributeSet attrs) {
        super(context, attrs);
        prepare(context);
    }

    public SensorLineChart(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        prepare(context);
    }

    public void clear() {
        super.clear();
        mChartEntries.clear();
    }

    public void prepare(Context context) {
        setNoDataText("Waiting for sensor data...");
        setDescription(null);
        int appColor = context.getResources().getColor(R.color.ic_launcher_background);
        Paint paint = getPaint(LineChart.PAINT_INFO);
        paint.setColor(appColor);
    }

    public void update(RecordCollection collection) {
        if (collection.sensorInfo != null) {
            Description description = new Description();
            description.setText(collection.sensorInfo.toString());
            setDescription(description);
        }
        for (RecordDP record : collection.recordsDP) {
            Entry entry = new Entry(record.time / 1e6f, record.diffPressureRaw);
            mChartEntries.add(entry);
        }
        if (mChartEntries.size() > MAX_POINTS_KEEP) {
            // truncate
            mChartEntries = mChartEntries.subList(mChartEntries.size() / 2,
                    mChartEntries.size() - 1);
        }
        LineDataSet dataset = new LineDataSet(mChartEntries, CHART_LABEL);
        LineData data = new LineData(dataset);
        setData(data);
        invalidate();
    }
}
