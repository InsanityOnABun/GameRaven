package com.ioabsoftware.gameraven.views.rowview;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.LevelListDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.os.AsyncTask;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.ArrowKeyMovementMethod;
import android.text.style.BulletSpan;
import android.text.style.QuoteSpan;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.text.HtmlCompat;
import androidx.preference.PreferenceManager;

import com.ioabsoftware.gameraven.AllInOneV2;
import com.ioabsoftware.gameraven.R;
import com.ioabsoftware.gameraven.util.CharacterLevelTagHandler;
import com.ioabsoftware.gameraven.util.Theming;
import com.ioabsoftware.gameraven.views.BaseRowData;
import com.ioabsoftware.gameraven.views.BaseRowView;
import com.ioabsoftware.gameraven.views.ClickableLinksTextView;
import com.ioabsoftware.gameraven.views.RowType;
import com.ioabsoftware.gameraven.views.rowdata.MessageRowData;
import com.ioabsoftware.gameraven.views.spans.GRBulletSpan;
import com.ioabsoftware.gameraven.views.spans.GRQuoteSpan;
import com.joanzapata.iconify.IconDrawable;
import com.joanzapata.iconify.fonts.MaterialIcons;
import com.koushikdutta.ion.Ion;

public class MessageRowView extends BaseRowView implements View.OnClickListener {

    View topWrapper;

    ImageView avatar;
    TextView overflowIcon;

    TextView user;
    TextView post;

    LinearLayout pollWrapper;

    ClickableLinksTextView message;

    private static float userTextSize = 0;
    private static float postTextSize, messageTextSize;

    MessageRowData myData;

    MessageHeaderDrawable headerSelector;

    boolean isHighlighted = false;
    boolean isShowingPoll = false;
    boolean isUsingAvatars = false;
    boolean isDeleted = false;

    public MessageRowView(Context context) {
        super(context);
    }

    public MessageRowView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MessageRowView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void init(Context context) {
        myType = RowType.MESSAGE;
        LayoutInflater.from(context).inflate(R.layout.msgview, this, true);

        topWrapper = findViewById(R.id.mvTopWrapper);

        avatar = findViewById(R.id.mvAvatar);
        overflowIcon = findViewById(R.id.mvMessageMenuIcon);

        user = findViewById(R.id.mvUser);
        post = findViewById(R.id.mvPostNumber);

        pollWrapper = findViewById(R.id.mvPollWrapper);

        message = findViewById(R.id.mvMessage);

        if (userTextSize == 0) {
            userTextSize = user.getTextSize();
            postTextSize = post.getTextSize();
            messageTextSize = message.getTextSize();
        }

        ShapeDrawable d = new ShapeDrawable();
        d.getPaint().setColor(Theming.colorPrimary());

        headerSelector = new MessageHeaderDrawable(new Drawable[]{d});
        topWrapper.setBackgroundDrawable(headerSelector);
        topWrapper.setOnClickListener(this);
    }

    @Override
    protected void retheme() {
        user.setTextSize(PX, userTextSize * myScale);
        post.setTextSize(PX, postTextSize * myScale);
        message.setTextSize(PX, messageTextSize * myScale);

        message.setLinkTextColor(Theming.colorAccent());
    }

    @Override
    public void showView(BaseRowData data) {
        if (data.getRowType() != myType)
            throw new IllegalArgumentException("data RowType does not match myType");

        myData = (MessageRowData) data;

        if (isDeleted != myData.isDeleted()) {
            isDeleted = myData.isDeleted();
            if (isDeleted) {
                findViewById(R.id.mvDeletedMessageWrapper).setVisibility(View.VISIBLE);
                findViewById(R.id.mvDMSep).setBackgroundColor(Theming.colorPrimary());
                topWrapper.setVisibility(View.GONE);
                pollWrapper.setVisibility(View.GONE);
                message.setVisibility(View.GONE);
            } else {
                findViewById(R.id.mvDeletedMessageWrapper).setVisibility(View.GONE);
                topWrapper.setVisibility(View.VISIBLE);
                pollWrapper.setVisibility(View.VISIBLE);
                message.setVisibility(View.VISIBLE);
            }
        }

        if (isDeleted) {
            ((TextView)findViewById(R.id.mvDMNum)).setText(myData.getPostNum());
            return;
        }

        topWrapper.setClickable(myData.topClickable());
        if (myData.topClickable())
            overflowIcon.setVisibility(View.VISIBLE);
        else
            overflowIcon.setVisibility(View.INVISIBLE);

        user.setText(myData.getUser() + myData.getUserTitles());
        post.setText(myData.getPostNum() + ", Posted " + myData.getPostTime());

        if (myData.hasPoll()) {
            isShowingPoll = true;
            pollWrapper.removeAllViews();
            pollWrapper.addView(myData.getPoll());
            pollWrapper.setVisibility(View.VISIBLE);
        } else if (isShowingPoll) {
            isShowingPoll = false;
            pollWrapper.setVisibility(View.GONE);
            pollWrapper.removeAllViews();
        }

        if (myData.getHLColor() == 0) {
            if (isHighlighted) {
                isHighlighted = false;
                headerSelector.clearHighlightColor();
            }
        } else {
            isHighlighted = true;
            headerSelector.setHighlightColor(myData.getHLColor());
        }

        if (isUsingAvatars != globalIsUsingAvatars) {
            isUsingAvatars = globalIsUsingAvatars;
            if (isUsingAvatars) {
                avatar.setVisibility(View.VISIBLE);
            } else {
                avatar.setVisibility(View.GONE);
            }
        }

        if (isUsingAvatars)
            Ion.with(avatar)
                    .placeholder(R.drawable.avatar_placeholder)
                    .error(R.drawable.avatar_default)
                    .load(myData.getAvatarUrl());

//        message.setText(myData.getSpannedMessage());
        Spanned spanned = HtmlCompat.fromHtml(myData.getMessageTextForDisplay(),
                HtmlCompat.FROM_HTML_MODE_LEGACY,
                source -> {
                    LevelListDrawable d = new LevelListDrawable();
                    Drawable empty = new IconDrawable(getContext(), MaterialIcons.md_cloud_download).color(Theming.colorPrimary()).sizeDp(16);
                    d.addLevel(0, 0, empty);
                    d.setBounds(0, 0, empty.getIntrinsicWidth(), empty.getIntrinsicHeight());
                    new ImageGetterAsyncTask(getContext(), source, d).execute(message);

                    return d;
                }, new CharacterLevelTagHandler());

        // Replace block-level Bulletspans that fromHtml doesn't make look good
        SpannableStringBuilder spanReplacer = new SpannableStringBuilder(spanned);
        for (BulletSpan b : spanReplacer.getSpans(0, spanReplacer.length(), BulletSpan.class)) {
            int start = spanReplacer.getSpanStart(b);
            int end = spanReplacer.getSpanEnd(b);
            spanReplacer.removeSpan(b);
            spanReplacer.setSpan(new GRBulletSpan(), start, end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        }

        // Replace block-level Quotespans that fromHtml doesn't make look good
        for (QuoteSpan q : spanReplacer.getSpans(0, spanReplacer.length(), QuoteSpan.class)) {
            int start = spanReplacer.getSpanStart(q);
            int end = spanReplacer.getSpanEnd(q);
            spanReplacer.removeSpan(q);
            spanReplacer.setSpan(new GRQuoteSpan(), start, end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        }

        message.setText(spanReplacer);

        message.setMovementMethod(ArrowKeyMovementMethod.getInstance());
        message.setTextIsSelectable(true);
        // the autoLink attribute must be removed, if you hasn't set it then ok, otherwise call textView.setAutoLink(0);

        headerSelector.invalidateDrawable(headerSelector);
    }

    /**
     * @return selected text, or null if no text is selected
     */
    public String getSelection() {
        int start = message.getSelectionStart();
        int end = message.getSelectionEnd();
        if (start != end) {
            if (start > end) {
                int temp = end;
                end = start;
                start = temp;
            }
            return message.getText().subSequence(start, end).toString();
        } else
            return null;
    }

    @Override
    public void onClick(View v) {
        AllInOneV2.get().messageMenuClicked(this);
    }

    public String getMessageDetailLink() {
        return myData.getMessageDetailLink();
    }

    public String getBoardActionLink() {
        return myData.getBoardActionLink();
    }

    public String getUserDetailLink() {
        return myData.getUserDetailLink();
    }

    public boolean isEdited() {
        return myData.isEdited();
    }

    public String getMessageID() {
        return myData.getMessageID();
    }

    public String getUserID() {
        return myData.getUserID();
    }

    public String getUser() {
        return myData.getUser();
    }

    public String getPostNum() {
        return myData.getPostNum();
    }

    public String getMessageForQuotingOrEditing() {
        return myData.getMessageForQuotingOrEditing();
    }

    private static boolean globalIsUsingAvatars;

    public static void setUsingAvatars(boolean set) {
        globalIsUsingAvatars = set;
    }

    public boolean canReport() {
        return myData.canReport();
    }

    public boolean canDelete() {
        return myData.canDelete();
    }

    public boolean canEdit() {
        return myData.canEdit();
    }

    public boolean canQuote() {
        return myData.canQuote();
    }

    public boolean canUpdateFlair() {
        return myData.canUpdateFlair();
    }







    public class MessageHeaderDrawable extends LayerDrawable {

        private int myColor, myClickedColor;

        public MessageHeaderDrawable(Drawable[] layers) {
            super(layers);
            clearHighlightColor();
        }

        public void setHighlightColor(int myColorIn) {
            myColor = myColorIn;

            float[] hsv = new float[3];
            Color.colorToHSV(myColor, hsv);
            hsv[2] *= 0.8f;
            myClickedColor = Color.HSVToColor(hsv);

            onStateChange(getState());
        }

        public void clearHighlightColor() {
            myColor = Theming.colorPrimary();
            myClickedColor = Theming.colorPrimaryDark();

            onStateChange(getState());
        }

        @Override
        protected boolean onStateChange(int[] states) {
            boolean isClicked = false;
            for (int state : states) {
                if (state == android.R.attr.state_focused || state == android.R.attr.state_pressed) {
                    isClicked = true;
                }
            }

            if (isClicked) {
                super.setColorFilter(myClickedColor, PorterDuff.Mode.SRC);
            } else {
                super.setColorFilter(myColor, PorterDuff.Mode.SRC);
            }

            return super.onStateChange(states);
        }

        @Override
        public boolean isStateful() {
            return true;
        }

    }

    class ImageGetterAsyncTask extends AsyncTask<TextView, Void, Bitmap> {
        private LevelListDrawable levelListDrawable;
        private Context context;
        private String source;
        private TextView t;

        public ImageGetterAsyncTask(Context context, String source, LevelListDrawable levelListDrawable) {
            this.context = context;
            this.source = source;
            this.levelListDrawable = levelListDrawable;
        }

        @Override
        protected Bitmap doInBackground(TextView... params) {
            t = params[0];
            try {
                while (PreferenceManager.getDefaultSharedPreferences(context)
                        .getInt("contentListScrollState", AbsListView.OnScrollListener.SCROLL_STATE_IDLE)
                        != AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
                    // Effectively pause until no scrolling
                }
                return Ion.with(context).load(source).asBitmap().get();
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(final Bitmap bitmap) {
            try {
                Drawable d = new BitmapDrawable(context.getResources(), bitmap);
                Point size = new Point();
                ((Activity) context).getWindowManager().getDefaultDisplay().getSize(size);
                // Lets calculate the ratio according to the screen width in px
                int multiplier = size.x / bitmap.getWidth();
                levelListDrawable.addLevel(1, 1, d);
                // Set bounds width  and height according to the bitmap resized size
                levelListDrawable.setBounds(0, 0, bitmap.getWidth() * multiplier, bitmap.getHeight() * multiplier);
                levelListDrawable.setLevel(1);
                t.setText(t.getText()); // invalidate() doesn't work correctly...
            } catch (Exception e) { /* Like a null bitmap, etc. */ }
        }
    }

}
