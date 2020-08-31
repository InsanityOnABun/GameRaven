package com.ioabsoftware.gameraven.views.spans;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Layout;
import android.text.Spanned;
import android.text.style.BulletSpan;

import androidx.annotation.NonNull;

public class GRBulletSpan extends BulletSpan {
    public GRBulletSpan() {
        super(15);
    }

    @Override
    public void drawLeadingMargin(@NonNull Canvas canvas, @NonNull Paint paint, int x, int dir,
                                  int top, int baseline, int bottom,
                                  @NonNull CharSequence text, int start, int end,
                                  boolean first, Layout layout) {
        int bulletRadius = 7;

        if (((Spanned) text).getSpanStart(this) == start) {
            Paint.Style style = paint.getStyle();
            paint.setStyle(Paint.Style.FILL);
            float yPosition;
            if (layout != null) {
                yPosition = layout.getLineBaseline(layout.getLineForOffset(start)) - bulletRadius * 2f;
            } else {
                yPosition = (top + bottom) / 2f;
            }
            final float xPosition = x + dir * bulletRadius;
            canvas.drawCircle(xPosition, yPosition, bulletRadius, paint);
            paint.setStyle(style);
        }
    }
}
