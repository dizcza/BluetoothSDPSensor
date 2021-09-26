package com.kyivaigroup.bluetoothsdpsensor;

import android.content.Context;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.kyivaigroup.bluetoothsdpsensor.record.RecordCollection;
import com.kyivaigroup.bluetoothsdpsensor.record.RecordDP;

import java.util.ArrayList;
import java.util.List;

public class SensorLineChart extends LineChart implements OnChartGestureListener {
    private static final int MAX_POINTS_KEEP = 10_000;
    private static final String CHART_LABEL = "Differential pressure, Pa";

    private List<Entry> mChartEntries = new ArrayList<>();
    private int mPressureScale = 60;
    private boolean mActive = true;

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
        setOnChartGestureListener(this);
    }

    public void update(RecordCollection collection) {
        if (collection.sensorInfo != null) {
            Description description = new Description();
            description.setText(collection.sensorInfo.toString());
            setDescription(description);
            int prScale = collection.sensorInfo.pressureScale;
            if (prScale != mPressureScale) {
                // apply new scaling factor
                final float rescale = ((float) mPressureScale) / prScale;
                for (Entry entry : mChartEntries) {
                    entry.setY(entry.getY() * rescale);
                }
                mPressureScale = prScale;
            }
        }
        for (RecordDP record : collection.recordsDP) {
            Entry entry = new Entry(record.time / 1e6f, record.diffPressureRaw / (float) mPressureScale);
            mChartEntries.add(entry);
        }
        if (mChartEntries.size() > MAX_POINTS_KEEP) {
            // truncate
            mChartEntries = mChartEntries.subList(mChartEntries.size() / 2,
                    mChartEntries.size() - 1);
        }
        if (mActive) {
            LineDataSet dataset = new LineDataSet(mChartEntries, CHART_LABEL);
            LineData data = new LineData(dataset);
            setData(data);
            invalidate();
        }
    }

    @Override
    public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {

    }

    @Override
    public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {

    }

    @Override
    public void onChartLongPressed(MotionEvent me) {

    }

    @Override
    public void onChartDoubleTapped(MotionEvent me) {

    }

    @Override
    public void onChartSingleTapped(MotionEvent me) {
        mActive = !mActive;
    }

    @Override
    public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {

    }

    @Override
    public void onChartScale(MotionEvent me, float scaleX, float scaleY) {

    }

    @Override
    public void onChartTranslate(MotionEvent me, float dX, float dY) {

    }
}
