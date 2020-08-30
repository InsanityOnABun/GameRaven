package com.ioabsoftware.gameraven.views.spans;

import android.text.Spannable;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.View;

import com.ioabsoftware.gameraven.views.ClickableLinksTextView;

import org.jetbrains.annotations.NotNull;

public class SpoilerClickSpan extends ClickableSpan {

    SpoilerBackgroundSpan myBackground;

    public SpoilerClickSpan(SpoilerBackgroundSpan spoilerBack) {
        myBackground = spoilerBack;
    }

    @Override
    public void onClick(View widget) {
        myBackground.reveal();
        ((Spannable) ((ClickableLinksTextView) widget).getText()).removeSpan(this);
    }

    @Override
    public void updateDrawState(@NotNull TextPaint ds) {
        // this makes no changes to draw state
    }

}
