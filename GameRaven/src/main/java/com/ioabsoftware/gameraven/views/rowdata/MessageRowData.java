package com.ioabsoftware.gameraven.views.rowdata;

import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ioabsoftware.gameraven.AllInOneV2;
import com.ioabsoftware.gameraven.R;
import com.ioabsoftware.gameraven.networking.GF_URLS;
import com.ioabsoftware.gameraven.networking.NetDesc;
import com.ioabsoftware.gameraven.networking.Session;
import com.ioabsoftware.gameraven.util.Theming;
import com.ioabsoftware.gameraven.views.BaseRowData;
import com.ioabsoftware.gameraven.views.RowType;
import com.ioabsoftware.gameraven.views.rowview.HeaderRowView;

import org.apache.commons.lang3.StringEscapeUtils;
import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class MessageRowData extends BaseRowData {

    private String username, userTitles, avatarUrl, postNum, postTime, messageID, boardID, topicID, userID;
    private final String unprocessedMessageText, messageTextForDisplay;

    private LinearLayout poll = null;

//    private Spannable spannedMessage;

    private int hlColor;

    private boolean topClickable = true;
    private boolean isDeleted = false;

    private boolean canReport = false, canDelete = false,
            canEdit = false, canQuote = false, canUpdateFlair = false;

    @NotNull
    @Override
    public String toString() {
        return "username: " + username +
                "\nuserTitles: " + userTitles +
                "\nhlColor: " + hlColor +
                "\npostNum: " + postNum +
                "\npostTime: " + postTime +
                "\nmessageID: " + messageID +
                "\nboardID: " + boardID +
                "\ntopicID: " + topicID +
                "\nhasPoll: " + hasPoll() +
                "\nunprocessedMessageText: " + unprocessedMessageText;
//                "\nspannedMessage: " + spannedMessage;
    }

    public void disableTopClick() {
        topClickable = false;
    }

    public boolean topClickable() {
        return topClickable;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public String getUser() {
        return username;
    }

    public String getUserTitles() {
        return userTitles;
    }

    public boolean hasTitles() {
        return userTitles != null;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getPostNum() {
        return postNum;
    }

    public String getPostTime() {
        return postTime;
    }

    public String getMessageID() {
        return messageID;
    }

    public boolean hasMsgID() {
        return messageID != null;
    }

    public String getTopicID() {
        return topicID;
    }

    public String getBoardID() {
        return boardID;
    }

    public String getUserID() {
        return userID;
    }

    public boolean canReport() {
        return canReport;
    }

    public boolean canDelete() {
        return canDelete;
    }

    public boolean canEdit() {
        return canEdit;
    }

    public boolean canQuote() {
        return canQuote;
    }

    public boolean canUpdateFlair() {
        return canUpdateFlair;
    }

    public String getUnprocessedMessageText() {
        return unprocessedMessageText;
    }

    public String getMessageTextForDisplay() {
        return messageTextForDisplay;
    }

    public LinearLayout getPoll() {
        if (poll.getParent() != null)
            ((ViewGroup) poll.getParent()).removeView(poll);

        return poll;
    }

    public boolean hasPoll() {
        return poll != null;
    }

//    public Spannable getSpannedMessage() {
//        return spannedMessage;
//    }

    public int getHLColor() {
        return hlColor;
    }

    public String getMessageDetailLink() {
        return GF_URLS.ROOT + "/boards/" + boardID + "/" + topicID + "/" + messageID;
    }

    public String getBoardActionLink() {
        return GF_URLS.ROOT +
                "/boardaction?board=" + boardID + "&topic=" + topicID + "&message=" + messageID;
    }

    public String getUserDetailLink() {
        return GF_URLS.ROOT + "/community/" + username.replace(' ', '+') + "/boards";
    }

    private static AllInOneV2 aio = null;

    @Override
    public RowType getRowType() {
        return RowType.MESSAGE;
    }

    public MessageRowData(boolean isDeletedIn, String postNumIn) {
        isDeleted = isDeletedIn;
        postNum = postNumIn;
        unprocessedMessageText = "";
        messageTextForDisplay = "";
    }

    public MessageRowData(String userIn, String userTitlesIn, String avatarUrlIn, String postNumIn,
                          String postTimeIn, Element messageIn, String BID, String TID, String MID,
                          String UID, int hlColorIn, boolean cReport, boolean cDelete,
                          boolean cEdit, boolean cQuote, boolean cUpdateFlair) {

        if (aio == null || aio != AllInOneV2.get())
            aio = AllInOneV2.get();

        username = userIn;
        userTitles = userTitlesIn;
        avatarUrl = avatarUrlIn;
        postNum = postNumIn;
        postTime = postTimeIn.replace('\u00A0', ' ');
        boardID = BID;
        topicID = TID;
        messageID = MID;
        userID = UID;
        hlColor = hlColorIn;

        canReport = cReport;
        canDelete = cDelete;
        canEdit = cEdit;
        canQuote = cQuote;
        canUpdateFlair = cUpdateFlair;

        String sigHtml = "";
        Element sig = messageIn.select("div.sig_text").first();
        if (sig != null) {
            sigHtml = "<br />---<br />" + sig.html();
            messageIn.select("div.signature").remove();
        }

        if (!Session.isLoggedIn())
            messageIn.select("div.message_mpu").remove();

        if (messageIn.getElementById("poll_results") != null) {
            Element pollElem = messageIn.getElementById("poll_results");

            // Check if poll has been voted in, remove relevant html elements so
            // they don't get put in unprocessedMessageText
            if (messageIn.getElementById("poll_vote") != null) {
                messageIn.getElementById("poll_vote").remove();
                messageIn.getElementsByTag("script").last().remove();
            }

            poll = new LinearLayout(aio);
            poll.setOrientation(LinearLayout.VERTICAL);

            LinearLayout pollInnerWrapper = new LinearLayout(aio);
            pollInnerWrapper.setPadding(15, 0, 15, 15);
            pollInnerWrapper.setOrientation(LinearLayout.VERTICAL);

            Drawable s = aio.getResources().getDrawable(R.drawable.item_background);
            s.setColorFilter(Theming.colorPrimary(), PorterDuff.Mode.SRC_ATOP);
            poll.setBackgroundDrawable(s);

            HeaderRowView h = new HeaderRowView(aio);
            h.showView(new HeaderRowData(pollElem.getElementsByClass("poll_head").first().text()));
            poll.addView(h);

            poll.addView(pollInnerWrapper);

            Elements pollRows = pollElem.select("div.row");
            int rowCount = pollRows.size();

            String[] optTitles = new String[rowCount];
            String[] optPercents = new String[rowCount];
            String[] optVotes = new String[rowCount];
            int myRegisteredVote = -1;

            int x = 0;
            for (Element e : pollRows) {
                optTitles[x] = e.getElementsByClass("poll_opt").first().text();
                optPercents[x] = e.getElementsByClass("poll_pct").first().ownText();
                optVotes[x] = e.getElementsByClass("poll_votes").first().text();
                if (!e.getElementsByTag("b").isEmpty()) {
                    myRegisteredVote = x;
                }
                x++;
            }

            if (myRegisteredVote == -1) {
                // poll has NOT been voted in

                String key = pollElem.getElementsByAttributeValue("name", "key").attr("value");

                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, Theming.convertDPtoPX(1));

                for (int y = 0; y < rowCount; y++) {
                    if (y > 0) {
                        View v = new View(aio);
                        v.setLayoutParams(lp);
                        v.setBackgroundColor(Theming.colorPrimary());
                        pollInnerWrapper.addView(v);
                    }
                    Button b = new Button(aio);
                    b.setBackgroundDrawable(Theming.selectableItemBackground());
                    b.setText(optTitles[y]);
                    final HashMap<String, List<String>> data = new HashMap<>();
                    data.put("option", Collections.singletonList(String.valueOf(y + 1)));
                    data.put("board", Collections.singletonList(boardID));
                    data.put("topic", Collections.singletonList(topicID));
                    data.put("message", Collections.singletonList("0"));
                    data.put("key", Collections.singletonList(key));

                    b.setOnClickListener(v -> aio.getSession().post(
                            NetDesc.TOPIC_POLL_VOTE, "/ajax/forum_poll_vote", data));
                    pollInnerWrapper.addView(b);
                }
            } else {
                // poll has been voted in

                TextView t;
                for (int y = 0; y < rowCount; y++) {
                    t = new TextView(aio);
                    String text = optTitles[y] + " (" + optPercents[y] + ", " + optVotes[y];
                    if (optVotes[y].equals("1")) {
                        text += " vote)";
                    } else {
                        text += " votes)";
                    }
                    if (y == myRegisteredVote) {
                        SpannableStringBuilder votedFor = new SpannableStringBuilder(text);
                        votedFor.setSpan(new StyleSpan(Typeface.BOLD), 0, text.length(), 0);
                        votedFor.setSpan(new ForegroundColorSpan(Theming.colorPrimary()), 0, text.length(), 0);
                        t.setText(votedFor);
                    } else {
                        t.setText(text);
                    }

                    pollInnerWrapper.addView(t);
                }

                String foot = pollElem.getElementsByClass("poll_foot_left").text();
                if (foot.length() > 0) {
                    t = new TextView(aio);
                    t.setText(foot);
                    pollInnerWrapper.addView(t);
                }
            }

            // remove the poll results element so it doesn't get put in unprocessedMessageText
            messageIn.getElementById("poll_results").remove();
            if (messageIn.getElementById("poll_vote") != null) {
                messageIn.getElementById("poll_vote").remove();
                messageIn.getElementsByTag("script").last().remove();
            }
        }

        unprocessedMessageText = messageIn.html() + sigHtml;

        messageTextForDisplay = unprocessedMessageText
                .replace("<s>", "<gr_spoiler>")
                .replace("</s>", "</gr_spoiler>");

        // remove data elements in cite tag for spannedMessage building
        Elements cites = messageIn.select("cite[data-quote-id]");
        for (Element cite : cites) {
            cite.removeAttr("data-quote-id").removeAttr("data-user-id");
        }
    }

    public boolean isEdited() {
        return userTitles != null && userTitles.contains("(edited)");
    }

    public String getMessageForQuotingOrEditing() {
        final String br = "<br />";
        final String sigSeperator = "\n---\n";
        final String aStart = "<a ";
        final String aClose = "</a>";
        final String divVidStart = "<div class=\"vid_container\">";
        final String divEnd = "</div>";
        final String imgPreArtifact = "<img src=\"";
        final String imgPostArtifact = "\" />";
        final String tagEnd = ">";

        // Make StringBuilder for following edits
        StringBuilder stringBuilder = new StringBuilder(unprocessedMessageText);

        // Remove opening anchor tags
        while (stringBuilder.indexOf(aStart) != -1) {
            int start = stringBuilder.indexOf(aStart);
            int end = stringBuilder.indexOf(tagEnd, start) + 1;
            stringBuilder.delete(start, end);
        }

        // Remove divs surrounding embedded video URLs
        while (stringBuilder.indexOf(divVidStart) != -1) {
            int start = stringBuilder.indexOf(divVidStart);
            int end = stringBuilder.indexOf(tagEnd, start) + 1;
            stringBuilder.delete(start, end);

            start = stringBuilder.indexOf(divEnd, start);
            end = start + divEnd.length();
            stringBuilder.delete(start, end);
        }

        // Remove img tag bits from around embedded image URLs
        while (stringBuilder.indexOf(imgPreArtifact) != -1) {
            int start = stringBuilder.indexOf(imgPreArtifact);
            int end = start + imgPreArtifact.length();
            stringBuilder.delete(start, end);

            start = stringBuilder.indexOf(imgPostArtifact, start);
            end = start + imgPostArtifact.length();
            stringBuilder.delete(start, end);
        }

        // Remove closing anchor tags
        while (stringBuilder.indexOf(aClose) != -1) {
            int start = stringBuilder.indexOf(aClose);
            int end = start + aClose.length();
            stringBuilder.delete(start, end);
        }

        // Remove newline characters (these are from html layout)
        while (stringBuilder.indexOf("\n") != -1) {
            stringBuilder.deleteCharAt(stringBuilder.indexOf("\n"));
        }

        // Replace line break tags with newline characters (these are the actual breaks in the post)
        while (stringBuilder.indexOf(br) != -1) {
            int start = stringBuilder.indexOf(br);
            int end = start + br.length();
            stringBuilder.replace(start, end, "\n");
        }

        // Remove signature
        int sigStart = stringBuilder.lastIndexOf(sigSeperator);
        if (sigStart != -1) {
            stringBuilder.delete(sigStart, stringBuilder.length());
        }

        // Remove any trailing newlines
        while (stringBuilder.charAt(stringBuilder.length() - 1) == '\n')
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);

        // Return
        return StringEscapeUtils.unescapeHtml4(stringBuilder.toString());
    }

}
