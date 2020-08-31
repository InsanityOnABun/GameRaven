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

//        Elements images = messageIn.select("a.img_container");
//        for (Element i : images) {
//            i.html(i.attr("href"));
//        }
//
//        Elements videos = messageIn.select("div.vid_container");
//        for (Element v : videos) {
//            String type = v.child(0).attr("class");
//            String data = v.child(0).attr("data-id");
//            String url;
//            switch (type) {
//                case "yt_player":
//                    url = "https://www.youtube.com/watch?v=" + data;
//                    break;
//                default:
//                    url = "GameRaven error: Video source not recognized. Source: " + type + ", Data: " + data;
//            }
//            v.html(url);
//        }

        unprocessedMessageText = messageIn.html() + sigHtml;

        messageTextForDisplay = unprocessedMessageText
                .replace("<s>", "<gr_spoiler>")
                .replace("</s>", "</gr_spoiler>");

        // remove data elements in cite tag for spannedMessage building
        Elements cites = messageIn.select("cite[data-quote-id]");
        for (Element cite : cites) {
            cite.removeAttr("data-quote-id").removeAttr("data-user-id");
        }

//        String msgForSSB = messageIn.html() + sigHtml;
//
//        AllInOneV2.wtl("creating ssb");
//        SpannableStringBuilder ssb = new SpannableStringBuilder(processContent(false, true, msgForSSB));
//
//        AllInOneV2.wtl("adding bold spans");
//        addGenericSpans(ssb, "<b>", "</b>", new StyleSpan(Typeface.BOLD));
//        AllInOneV2.wtl("adding italic spans");
//        addGenericSpans(ssb, "<i>", "</i>", new StyleSpan(Typeface.ITALIC));
//        AllInOneV2.wtl("adding code spans");
//        addGenericSpans(ssb, "<code>", "</code>", new TypefaceSpan("monospace"));
//        AllInOneV2.wtl("adding cite spans");
//        addGenericSpans(ssb, "<cite>", "</cite>", new UnderlineSpan(), new StyleSpan(Typeface.ITALIC));
//        AllInOneV2.wtl("adding quote spans");
//        addQuoteSpans(ssb);
//
//        ssb.append('\n');
//
//        AllInOneV2.wtl("linkifying");
//        MyLinkifier.addLinks(ssb, Linkify.WEB_URLS);
//
//        AllInOneV2.wtl("adding spoiler spans");
//        addSpoilerSpans(ssb);
//
//        AllInOneV2.wtl("replacing &lt; with <");
//        while (ssb.toString().contains("&lt;")) {
//            int start = ssb.toString().indexOf("&lt;");
//            ssb.replace(start, start + "&lt;".length(), "<");
//        }
//
//        AllInOneV2.wtl("replacing &gt; with >");
//        while (ssb.toString().contains("&gt;")) {
//            int start = ssb.toString().indexOf("&gt;");
//            ssb.replace(start, start + "&gt;".length(), ">");
//        }
//
//        AllInOneV2.wtl("setting spannedMessage");
//        spannedMessage = ssb;
    }

    public boolean isEdited() {
        return userTitles != null && userTitles.contains("(edited)");
    }

//    private static void addGenericSpans(SpannableStringBuilder ssb, String tag, String endTag, CharacterStyle... cs) {
//        // initialize array
//        int[] startEnd = spanStartAndEnd(ssb.toString(), tag, endTag);
//
//        // while start and end points are found...
//        while (!Arrays.equals(startEnd, noStartEndBase)) {
//            // remove the start tag
//            ssb.delete(startEnd[0], startEnd[0] + tag.length());
//
//            // adjust end point for removed start tag
//            startEnd[1] -= tag.length();
//
//            // remove end tag
//            ssb.delete(startEnd[1], startEnd[1] + endTag.length());
//
//            // apply styles
//            for (CharacterStyle c : cs)
//                ssb.setSpan(CharacterStyle.wrap(c), startEnd[0], startEnd[1], 0);
//
//            // get new start and end points
//            startEnd = spanStartAndEnd(ssb.toString(), tag, endTag);
//        }
//    }
//
//    public static final String QUOTE_START = "<blockquote>";
//    public static final String QUOTE_END = "</blockquote>";
//
//    private static void addQuoteSpans(SpannableStringBuilder ssb) {
//        // initialize array
//        int[] startEnd = spanStartAndEnd(ssb.toString(), QUOTE_START, QUOTE_END);
//
//        // while start and end points are found...
//        while (!Arrays.equals(startEnd, noStartEndBase)) {
//            // replace the start tag
//            ssb.replace(startEnd[0], startEnd[0] + QUOTE_START.length(), "\n");
//            startEnd[0]++;
//
//            // adjust end point for replaced start tag
//            startEnd[1] -= QUOTE_START.length() - 1;
//
//            // remove end tag
//            ssb.replace(startEnd[1], startEnd[1] + QUOTE_END.length(), "\n");
//
//            // apply style
//            ssb.setSpan(new GRQuoteSpan(), startEnd[0], startEnd[1], 0);
//
//            // get new start and end points
//            startEnd = spanStartAndEnd(ssb.toString(), QUOTE_START, QUOTE_END);
//        }
//    }
//
//    public static final String SPOILER_START = "<s>";
//    public static final String SPOILER_END = "</s>";
//
//    private void addSpoilerSpans(SpannableStringBuilder ssb) {
//        // initialize array
//        int[] startEnd = spanStartAndEnd(ssb.toString(), SPOILER_START, SPOILER_END);
//
//        // while start and end points are found...
//        while (!Arrays.equals(startEnd, noStartEndBase)) {
//            // remove the start tag
//            ssb.delete(startEnd[0], startEnd[0] + SPOILER_START.length());
//
//            // adjust end point for removed start tag
//            startEnd[1] -= SPOILER_START.length();
//
//            // remove end tag
//            ssb.delete(startEnd[1], startEnd[1] + SPOILER_END.length());
//
//            // apply styles
//            SpoilerBackgroundSpan spoiler = new SpoilerBackgroundSpan(Theming.colorHiddenSpoiler(), Theming.colorRevealedSpoiler());
//            SpoilerClickSpan spoilerClick = new SpoilerClickSpan(spoiler);
//            ssb.setSpan(spoiler, startEnd[0], startEnd[1], 0);
//            ssb.setSpan(spoilerClick, startEnd[0], startEnd[1], 0);
//
//            // get new start and end points
//            startEnd = spanStartAndEnd(ssb.toString(), SPOILER_START, SPOILER_END);
//        }
//    }
//
//    private static int[] noStartEndBase = {-1, -1};
//
//    private static int[] spanStartAndEnd(String text, String openTag, String closeTag) {
//        int start = -1;
//        int end = -1;
//        if (text.contains(openTag) && text.contains(closeTag)) {
//            start = text.indexOf(openTag);
//            end = text.indexOf(closeTag);
//
//            int stackCount = 1;
//            int closer;
//            int opener;
//            int innerStartPoint = start;
//            do {
//                opener = text.indexOf(openTag, innerStartPoint + 1);
//                closer = text.indexOf(closeTag, innerStartPoint + 1);
//                if (opener != -1 && opener < closer) {
//                    // found a nested tag
//                    stackCount++;
//                    innerStartPoint = opener;
//                } else {
//                    // this closer is the right one
//                    stackCount--;
//                    innerStartPoint = closer;
//                }
//            } while (stackCount > 0);
//
//            if (closer != -1)
//                end = closer;
//
//        }
//        return new int[]{start, end};
//    }

    public String getMessageForQuotingOrEditing() {
        String finalBody = unprocessedMessageText;

        while (finalBody.contains("<a ")) {
            int start = finalBody.indexOf("<a ");
            int end = finalBody.indexOf(">", start) + 1;
            finalBody = finalBody.replace(finalBody.substring(start, end),
                    "");
        }

        while (finalBody.contains("<div class=\"vid_container\">")) {
            int start = finalBody.indexOf("<div class=\"vid_container\">");
            int end = finalBody.indexOf(">", start) + 1;
            finalBody = finalBody.replace(finalBody.substring(start, end),
                    "");

            start = finalBody.indexOf("</div>", end);
            end = start + 6;
            finalBody = finalBody.replace(finalBody.substring(start, end),
                    "");
        }

        finalBody = finalBody.replace("</a>", "");

        if (finalBody.endsWith("<br />"))
            finalBody = finalBody.substring(0, finalBody.length() - 6);
        finalBody = finalBody.replace("\n", "");
        finalBody = finalBody.replace("<br />", "\n");

        int sigStart = finalBody.lastIndexOf("\n---\n");
        if (sigStart != -1)
            finalBody = finalBody.substring(0, sigStart);

        finalBody = StringEscapeUtils.unescapeHtml4(finalBody);

        return finalBody;
    }

}
