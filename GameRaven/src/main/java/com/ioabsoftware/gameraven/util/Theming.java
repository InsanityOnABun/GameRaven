package com.ioabsoftware.gameraven.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;

import com.ioabsoftware.gameraven.R;

import de.keyboardsurfer.android.widget.crouton.Configuration;
import de.keyboardsurfer.android.widget.crouton.Style;

public final class Theming {

    private static float dwrHeaderTextBaseSize,
            dwrButtonTextBaseSize,
            pageTitleTextBaseSize,
            pjButtonTextBaseSize,
            pjLabelTextBaseSize;

    private static int colorPrimary;

    public static int colorPrimary() {
        return colorPrimary;
    }

    private static int colorPrimaryDark;

    public static int colorPrimaryDark() {
        return colorPrimaryDark;
    }

    private static int colorAccent;

    public static int colorAccent() {
        return colorAccent;
    }

    private static boolean usingLightTheme;

    public static boolean usingLightTheme() {
        return usingLightTheme;
    }

    private static int cardBackgroundColor;

    public static int cardBackgroundColor() {
        return cardBackgroundColor;
    }

    private static int accentColor;

    public static int accentColor() {
        return accentColor;
    }

    private static int moddedAccentColor;

    public static int moddedAccentColor() {
        return moddedAccentColor;
    }

    private static int accentTextColor;

    public static int accentTextColor() {
        return accentTextColor;
    }

    private static boolean useWhiteAccentText;

    public static boolean useWhiteAccentText() {
        return useWhiteAccentText;
    }

    private static boolean isAccentLight;

    public static boolean isAccentLight() {
        return isAccentLight;
    }

    private static float textScale = 1f;

    public static float textScale() {
        return textScale;
    }

    private static Style croutonStyle;

    public static Style croutonStyle() {
        return croutonStyle;
    }

    private static Configuration croutonShort = new Configuration.Builder().setDuration(2500).build();

    public static void init(Context c, SharedPreferences settings) {
        updateAccentColor(settings.getInt("accentColor", (c.getResources().getColor(R.color.holo_blue))), settings.getBoolean("useWhiteAccentText", false));
        usingLightTheme = settings.getBoolean("useLightTheme", false);
        textScale = settings.getInt("textScale", 100) / 100f;

        // Obtain the styled attributes. 'themedContext' is a context with a
        // theme, typically the current Activity (i.e. 'this')
        TypedArray ta = c.obtainStyledAttributes(new int[] {
                R.attr.themedCardBackgroundColor,
                R.attr.colorPrimary,
                R.attr.colorPrimaryDark,
                R.attr.colorAccent
        });

        // Get the individual values
        cardBackgroundColor = ta.getColor(0, c.getResources().getColor(R.color.card_background_dark));
        colorPrimary = ta.getColor(1, c.getResources().getColor(R.color.gf_blue_dark));
        colorPrimaryDark = ta.getColor(2, c.getResources().getColor(R.color.gf_blue_dark_secondary));
        colorAccent = ta.getColor(3, c.getResources().getColor(R.color.gf_blue_dark_accent));

        // Finally, free the resources used by TypedArray
        ta.recycle();
    }

    public static void setTextSizeBases(float dwrHeader, float dwrButton, float pageTitle, float pjButton, float pjLabel) {
        dwrHeaderTextBaseSize = dwrHeader;
        dwrButtonTextBaseSize = dwrButton;
        pageTitleTextBaseSize = pageTitle;
        pjButtonTextBaseSize = pjButton;
        pjLabelTextBaseSize = pjLabel;
    }

    public static float getScaledDwrHeaderTextSize() {
        return dwrHeaderTextBaseSize * textScale;
    }

    public static float getScaledDwrButtonTextSize() {
        return dwrButtonTextBaseSize * textScale;
    }

    public static float getScaledPageTitleTextSize() {
        return pageTitleTextBaseSize * textScale;
    }

    public static float getScaledPJButtonTextSize() {
        return pjButtonTextBaseSize * textScale;
    }

    public static float getScaledPJLabelTextSize() {
        return pjLabelTextBaseSize * textScale;
    }

    /**
     * returns true if new scale is different from old scale
     */
    public static boolean updateTextScale(float newScale) {
        if (textScale != newScale) {
            textScale = newScale;
            return true;
        } else
            return false;
    }

    /**
     * returns true if new color is different from old color
     */
    public static boolean updateAccentColor(int newAccentColor, boolean newUseWhiteAccentText) {
        if (accentColor != newAccentColor || useWhiteAccentText != newUseWhiteAccentText) {
            accentColor = newAccentColor;
            useWhiteAccentText = newUseWhiteAccentText;

            float[] hsv = new float[3];
            Color.colorToHSV(accentColor, hsv);
            if (useWhiteAccentText) {
                // color is probably dark
                if (hsv[2] > 0)
                    hsv[2] *= 1.2f;
                else
                    hsv[2] = 0.2f;

                accentTextColor = Color.WHITE;
                isAccentLight = false;
            } else {
                // color is probably bright
                hsv[2] *= 0.8f;
                accentTextColor = Color.BLACK;
                isAccentLight = true;
            }

            moddedAccentColor = Color.HSVToColor(hsv);

            croutonStyle = new Style.Builder()
                    .setBackgroundColorValue(accentColor)
                    .setTextColorValue(accentTextColor)
                    .setConfiguration(croutonShort)
                    .build();

            return true;
        } else
            return false;
    }

    /**
     * Colors the overscroll of the activity
     *
     * @param context The context of the activity
     */
    public static void colorOverscroll(Context context) {
        //glow
        int glowDrawableId = context.getResources().getIdentifier("overscroll_glow", "drawable", "android");
        Drawable androidGlow = context.getResources().getDrawable(glowDrawableId);
        androidGlow.setColorFilter(accentColor, PorterDuff.Mode.SRC_IN);
        //edge
        int edgeDrawableId = context.getResources().getIdentifier("overscroll_edge", "drawable", "android");
        Drawable androidEdge = context.getResources().getDrawable(edgeDrawableId);
        androidEdge.setColorFilter(accentColor, PorterDuff.Mode.SRC_IN);
    }

    /**
     * Converts a DP value into a PX value, based on the current device's density
     *
     * @param c  Context needed to find the display density.
     * @param dp The DP value to convert to PX
     * @return
     */
    public static int convertDPtoPX(Context c, float dp) {
        // Get the screen's density scale
        final float scale = c.getResources().getDisplayMetrics().density;
        // Convert the dps to pixels, based on density scale
        return ((int) (dp * scale + 0.5f));
    }

}
