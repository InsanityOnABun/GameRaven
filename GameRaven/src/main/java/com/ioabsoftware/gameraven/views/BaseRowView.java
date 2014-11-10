package com.ioabsoftware.gameraven.views;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RRDWSBackgroundHack;
import android.util.AttributeSet;
import android.util.TypedValue;

import com.ioabsoftware.gameraven.util.Theming;

public abstract class BaseRowView extends CardView {

    protected RowType myType = null;

    protected int myColor = 0;
    protected float myScale = 0;

    protected final int PX = TypedValue.COMPLEX_UNIT_PX;

    private Drawable background = null;

    public BaseRowView(Context context) {
        super(context);
        preInit(context);
    }

    public BaseRowView(Context context, AttributeSet attrs) {
        super(context, attrs);
        preInit(context);
    }

    public BaseRowView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        preInit(context);
    }

    private void preInit(Context c) {
        setCardElevation(Theming.convertDPtoPX(c, 2));
        setRadius(Theming.convertDPtoPX(c, 4));

        if (background == null)
            background = new RRDWSBackgroundHack(getResources(), Theming.cardBackgroundColor(), getRadius(), Theming.convertDPtoPX(c, 1), Theming.convertDPtoPX(c, 1));

        setBackgroundDrawable(background);

        init(c);

        switch (myType) {
            case GAME_SEARCH:
            case BOARD:
            case TOPIC:
            case AMP_TOPIC:
            case TRACKED_TOPIC:
            case MESSAGE:
            case PM:
            case PM_DETAIL:
//                setBackgroundDrawable(new SelectorItemDrawable(getContext()));
                break;
            case USER_DETAIL:
            case HIGHLIGHTED_USER:
            case HEADER:
                break;
        }

        preRetheme();
    }

    private void preRetheme() {
        myColor = Theming.accentColor();
        myScale = Theming.textScale();
        retheme();
    }

    public void beginShowingView(BaseRowData data) {
        if (Theming.accentColor() != myColor || Theming.textScale() != myScale) {
            SelectorItemDrawable.rebuildColorFilter();
            preRetheme();
        }
        showView(data);
    }

    protected abstract void init(Context context);

    protected abstract void retheme();

    protected abstract void showView(BaseRowData data);

}
