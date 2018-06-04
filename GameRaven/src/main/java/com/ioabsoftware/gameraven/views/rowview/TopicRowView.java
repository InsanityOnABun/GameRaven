package com.ioabsoftware.gameraven.views.rowview;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.ioabsoftware.gameraven.AllInOneV2;
import com.ioabsoftware.gameraven.R;
import com.ioabsoftware.gameraven.networking.NetDesc;
import com.ioabsoftware.gameraven.util.Theming;
import com.ioabsoftware.gameraven.views.BaseRowData;
import com.ioabsoftware.gameraven.views.BaseRowData.ReadStatus;
import com.ioabsoftware.gameraven.views.BaseRowView;
import com.ioabsoftware.gameraven.views.RowType;
import com.ioabsoftware.gameraven.views.rowdata.TopicRowData;
import com.joanzapata.iconify.Iconify;

public class TopicRowView extends BaseRowView {

    TextView title;
    TextView tcOrBoard;
    TextView msgLP;
    TextView rightButton, leftButton;
    View rightSep, leftSep;

    OnClickListener lastPostListener, untrackListener;

    TopicRowData myData;


    private int defaultTitleColor, defaultTCColor, defaultMsgLPColor, defaultButtonColor;

    private static float titleTextSize = 0;
    private static float tcTextSize, msgLPTextSize, buttonTextSize;

    private ForegroundColorSpan flairForegroundColor = new ForegroundColorSpan(Theming.colorPrimary());
    private StyleSpan flairBold = new StyleSpan(Typeface.BOLD);

    private String typeColor = "#" + Integer.toHexString(Theming.colorTopicTypeIndicator());

    public TopicRowView(Context context) {
        super(context);
    }

    public TopicRowView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TopicRowView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void init(Context context) {
        myType = RowType.TOPIC;
        LayoutInflater.from(context).inflate(R.layout.topicview, this, true);

        title = findViewById(R.id.tvTitle);
        tcOrBoard = findViewById(R.id.tvTC);
        msgLP = findViewById(R.id.tvMsgCountLastPost);
        rightButton = findViewById(R.id.tvRightButton);
        leftButton = findViewById(R.id.tvLeftButton);
        rightSep = findViewById(R.id.tvRightSep);
        leftSep = findViewById(R.id.tvLeftSep);

        defaultTitleColor = title.getCurrentTextColor();
        defaultTCColor = tcOrBoard.getCurrentTextColor();
        defaultMsgLPColor = msgLP.getCurrentTextColor();
        defaultButtonColor = rightButton.getCurrentTextColor();

        if (titleTextSize == 0) {
            titleTextSize = title.getTextSize();
            tcTextSize = tcOrBoard.getTextSize();
            msgLPTextSize = msgLP.getTextSize();
            buttonTextSize = rightButton.getTextSize();
        }

        lastPostListener = v -> {
            AllInOneV2.get().enableGoToUrlDefinedPost();
            AllInOneV2.get().getSession().get(NetDesc.TOPIC, myData.getLastPostUrl());
        };

        untrackListener = v -> AllInOneV2.get().getSession().get(NetDesc.TRACKED_TOPICS, myData.getUntrackUrl());

        setOnClickListener(v -> AllInOneV2.get().getSession().get(NetDesc.TOPIC, myData.getUrl()));

        setOnLongClickListener(v -> {
            String url = myData.getUrl().substring(0, myData.getUrl().lastIndexOf('/'));
            AllInOneV2.get().getSession().get(NetDesc.BOARD, url);
            return true;
        });
    }

    @Override
    protected void retheme() {
        title.setTextSize(PX, titleTextSize * myScale);
        tcOrBoard.setTextSize(PX, tcTextSize * myScale);
        msgLP.setTextSize(PX, msgLPTextSize * myScale);
        rightButton.setTextSize(PX, buttonTextSize * myScale);
        leftButton.setTextSize(PX, buttonTextSize * myScale);
    }

    @Override
    public void showView(BaseRowData data) {
        if (data.getRowType() != myType)
            throw new IllegalArgumentException("data RowType does not match myType");

        myData = (TopicRowData) data;

        TopicRowData.TopicFlavor myFlavor = myData.getFlavor();

        TextView lastPostButton = (Theming.swapTopicViewButtons() ? leftButton : rightButton);
        View lastPostSep = (Theming.swapTopicViewButtons() ? leftSep : rightSep);
        lastPostButton.setOnClickListener(lastPostListener);

        TextView untrackButton = (Theming.swapTopicViewButtons() ? rightButton : leftButton);
        View untrackSep = (Theming.swapTopicViewButtons() ? rightSep : leftSep);
        untrackButton.setOnClickListener(untrackListener);

        setLongClickable(true);

        switch (myFlavor) {
            case BOARD:
                setLongClickable(false);
            case AMP:
                untrackButton.setVisibility(GONE);
                untrackSep.setVisibility(GONE);
                break;

            case TRACKED:
                untrackButton.setVisibility(VISIBLE);
                untrackSep.setVisibility(VISIBLE);
                untrackButton.setText("X");
                break;
        }

        lastPostButton.setVisibility(VISIBLE);
        lastPostSep.setVisibility(VISIBLE);

        if (myData.getFlair() != null && myData.getFlair().length() > 0) {
            int flag = Spannable.SPAN_INCLUSIVE_INCLUSIVE;
            int spanLength = myData.getFlair().length() + 2;
            SpannableStringBuilder titleBuilder = new SpannableStringBuilder();
            titleBuilder.append('[').append(myData.getFlair()).append("] ").append(myData.getTitle());
            titleBuilder.setSpan(flairForegroundColor, 0, spanLength, flag);
            titleBuilder.setSpan(flairBold, 0, spanLength, flag);
            title.setText(titleBuilder);
        } else {
            title.setText(myData.getTitle());
        }

        StringBuilder typeIcon = new StringBuilder();
        switch (myData.getType()) {
            case POLL:
                typeIcon.append("{mdi-poll");
                break;
            case LOCKED:
                typeIcon.append("{md-lock");
                break;
            case ARCHIVED:
                typeIcon.append("{mdi-archive");
                break;
            case PINNED:
                typeIcon.append("{mdi-pin");
                break;
            case NORMAL:
            default:
                typeIcon.setLength(0); // ensure empty
                break;
        }

        if (typeIcon.length() > 0) {
            typeIcon.append(" 18sp ").append(typeColor).append("}  ");
        }

        String spacer = " {mdi-lock 18sp #00000000}";
        tcOrBoard.setText(String.format("%s%s%s",
                typeIcon.toString(), myData.getTCOrBoard(), spacer));

        Iconify.addIcons(tcOrBoard);

        msgLP.setText(String.format("%s Msgs, Last: %s", myData.getMCount(), myData.getLastPost()));

        int hlColor = myData.getHLColor();
        if (myData.getStatus() == ReadStatus.READ) {
            tcOrBoard.setTextColor(Theming.colorReadTopic());
            title.setTextColor(Theming.colorReadTopic());
            msgLP.setTextColor(Theming.colorReadTopic());
            rightButton.setTextColor(Theming.colorReadTopic());
            leftButton.setTextColor(Theming.colorReadTopic());
        } else if (hlColor != 0) {
            tcOrBoard.setTextColor(hlColor);
            title.setTextColor(hlColor);
            msgLP.setTextColor(hlColor);
            rightButton.setTextColor(hlColor);
            leftButton.setTextColor(hlColor);
        } else {
            tcOrBoard.setTextColor(defaultTCColor);
            title.setTextColor(defaultTitleColor);
            msgLP.setTextColor(defaultMsgLPColor);
            rightButton.setTextColor(defaultButtonColor);
            leftButton.setTextColor(defaultButtonColor);
        }

        if (myData.getStatus() == ReadStatus.NEW_POST) {
            tcOrBoard.setTypeface(Typeface.DEFAULT, Typeface.BOLD_ITALIC);
            title.setTypeface(Typeface.DEFAULT, Typeface.BOLD_ITALIC);
            msgLP.setTypeface(Typeface.DEFAULT, Typeface.BOLD_ITALIC);
            rightButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD_ITALIC);
            leftButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD_ITALIC);

            lastPostButton.setText(R.string.first_unread);
        } else {
            tcOrBoard.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
            title.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
            msgLP.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
            rightButton.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
            leftButton.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);

            lastPostButton.setText(R.string.last_post);
        }
    }
}
