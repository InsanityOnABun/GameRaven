package com.ioabsoftware.gameraven.util;

import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import android.text.style.CharacterStyle;

import com.ioabsoftware.gameraven.views.spans.GRMonospaceSpan;
import com.ioabsoftware.gameraven.views.spans.SpoilerBackgroundSpan;
import com.ioabsoftware.gameraven.views.spans.SpoilerClickSpan;

import org.xml.sax.XMLReader;

public class CharacterLevelTagHandler implements Html.TagHandler {

    public void handleTag(boolean opening, String tag, Editable output, XMLReader xmlReader) {
        // Get span for the tag
        CharacterStyle span = null;
        switch (tag.toLowerCase()) {
            case "code":
                span = new GRMonospaceSpan();
                break;

            case "gr_spoiler":
                span = new SpoilerBackgroundSpan(Theming.colorHiddenSpoiler(), Theming.colorRevealedSpoiler());
                break;
        }

        // If there's a span to use
        if (span != null) {
            int len = output.length();
            if(opening) { // Set a marker span to flag the start for this span
                output.setSpan(span, len, len, Spannable.SPAN_MARK_MARK);
            } else { // Remove the marker for this span and add the final one
                Object obj = getLast(output, span.getClass());
                int where = output.getSpanStart(obj);

                output.removeSpan(obj);

                if (where != len) {
                    output.setSpan(span, where, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                    if (span instanceof SpoilerBackgroundSpan) {
                        output.setSpan(new SpoilerClickSpan((SpoilerBackgroundSpan) span), where, len,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
            }
        }
    }

    private Object getLast(Editable text, Class<?> kind) {
        Object[] objs = text.getSpans(0, text.length(), kind);

        if (objs.length != 0) {
            for (int i = objs.length; i > 0; i--) {
                if (text.getSpanFlags(objs[i - 1]) == Spannable.SPAN_MARK_MARK) {
                    return objs[i - 1];
                }
            }
        }
        return null;
    }
}
