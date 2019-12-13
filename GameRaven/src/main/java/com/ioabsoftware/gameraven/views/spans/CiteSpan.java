package com.ioabsoftware.gameraven.views.spans;

import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.style.MetricAffectingSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;

import androidx.annotation.NonNull;

public class CiteSpan extends MetricAffectingSpan {

    private UnderlineSpan underlineSpan = new UnderlineSpan();
    private StyleSpan italicSpan = new StyleSpan(Typeface.ITALIC);

    @Override
    public void updateMeasureState(@NonNull TextPaint textPaint) {
        italicSpan.updateMeasureState(textPaint);
    }

    @Override
    public void updateDrawState(TextPaint tp) {
        underlineSpan.updateDrawState(tp);
        italicSpan.updateDrawState(tp);
    }
}
