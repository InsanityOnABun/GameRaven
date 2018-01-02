package com.ioabsoftware.gameraven.views.rowdata;

import com.ioabsoftware.gameraven.views.BaseRowData;
import com.ioabsoftware.gameraven.views.RowType;


public class TopicRowData extends BaseRowData {

    public enum TopicFlavor {BOARD, AMP, TRACKED}

    public enum TopicType {NORMAL, POLL, LOCKED, ARCHIVED, PINNED}

    private String title, tcOrBoard, lastPost, mCount, url, lPostUrl, untrackUrl;

    public String getTitle() {
        return title;
    }

    public String getTCOrBoard() {
        return tcOrBoard;
    }

    public String getLastPost() {
        return lastPost;
    }

    public String getMCount() {
        return mCount;
    }

    public String getUrl() {
        return url;
    }

    public String getLastPostUrl() {
        return lPostUrl;
    }

    public String getUntrackUrl() {
        return untrackUrl;
    }

    private TopicType type;

    public TopicType getType() {
        return type;
    }

    private TopicFlavor flavor;

    public TopicFlavor getFlavor() {
        return flavor;
    }

    private ReadStatus status;

    public ReadStatus getStatus() {
        return status;
    }

    private int hlColor;

    public int getHLColor() {
        return hlColor;
    }

    @Override
    public RowType getRowType() {
        return RowType.TOPIC;
    }

    public TopicRowData(String titleIn, String tcOrBoardIn, String lastPostIn, String mCountIn,
                        String urlIn, String lPostUrlIn, String untrackUrlIn, TopicType typeIn,
                        ReadStatus statusIn, int hlColorIn, TopicFlavor flavorIn) {
        title = titleIn;
        tcOrBoard = tcOrBoardIn;
        lastPost = lastPostIn;
        mCount = mCountIn;
        url = urlIn;
        lPostUrl = lPostUrlIn;
        untrackUrl = untrackUrlIn;

        type = typeIn;

        status = statusIn;

        hlColor = hlColorIn;

        flavor = flavorIn;
    }

    @Override
    public String toString() {
        return "title: " + title +
                "\ntcOrBoard: " + tcOrBoard +
                "\nhlColor: " + hlColor +
                "\nlastPost: " + lastPost +
                "\nmCount: " + mCount +
                "\nurl: " + url +
                "\nlPostUrl: " + lPostUrl +
                "\nuntrackUrl: " + untrackUrl +
                "\ntype: " + type.name() +
                "\nflavor: " + flavor.name() +
                "\nstatus: " + status.name();
    }

}
