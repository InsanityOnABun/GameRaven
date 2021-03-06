package com.ioabsoftware.gameraven.views.rowview;

import android.app.AlertDialog;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.ioabsoftware.gameraven.AllInOneV2;
import com.ioabsoftware.gameraven.R;
import com.ioabsoftware.gameraven.networking.NetDesc;
import com.ioabsoftware.gameraven.views.BaseRowData;
import com.ioabsoftware.gameraven.views.BaseRowView;
import com.ioabsoftware.gameraven.views.RowType;
import com.ioabsoftware.gameraven.views.rowdata.BoardRowData;

public class BoardRowView extends BaseRowView {

    private TextView desc, lastPost, tpcMsgDetails, name;

    private static float nameTextSize = 0;
    private static float descTextSize, lpTextSize, detailsTextSize;

    BoardRowData myData;

    public BoardRowView(Context context) {
        super(context);
    }

    public BoardRowView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BoardRowView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void init(Context context) {
        myType = RowType.BOARD;
        LayoutInflater.from(context).inflate(R.layout.boardview, this, true);

        desc = findViewById(R.id.bvDesc);
        lastPost = findViewById(R.id.bvLastPost);
        tpcMsgDetails = findViewById(R.id.bvTpcMsgDetails);
        name = findViewById(R.id.bvName);

        if (nameTextSize == 0) {
            nameTextSize = name.getTextSize();
            detailsTextSize = tpcMsgDetails.getTextSize();
            lpTextSize = lastPost.getTextSize();
            descTextSize = desc.getTextSize();
        }

        setOnClickListener(v -> {
            if (myData.getUrl() == null) {
                AlertDialog.Builder b = new AlertDialog.Builder(BoardRowView.this.getContext());
                b.setTitle("Cannot Access " + myData.getName());
                b.setMessage(myData.getName() +
                        " cannot be accessed, most likely due to user level requirements.");
                b.setPositiveButton("Ok", null);
                b.create().show();
            } else {
                NetDesc desc = (myData.getBoardType() == BoardRowData.BoardType.EXPLORER_LINK ?
                        NetDesc.BOARDS_EXPLORE : NetDesc.BOARD);
                AllInOneV2.get().getSession().get(desc, myData.getUrl());
            }
        });
    }

    @Override
    protected void retheme() {
        desc.setTextSize(PX, descTextSize * myScale);
        lastPost.setTextSize(PX, lpTextSize * myScale);
        tpcMsgDetails.setTextSize(PX, detailsTextSize * myScale);
        name.setTextSize(PX, nameTextSize * myScale);
    }

    @Override
    public void showView(BaseRowData data) {
        if (data.getRowType() != myType)
            throw new IllegalArgumentException("data RowType does not match myType");

        myData = (BoardRowData) data;

        name.setText(myData.getName());

        String descText = myData.getDesc();
        if (descText != null) {
            desc.setVisibility(View.VISIBLE);
            desc.setText(descText);
        } else
            desc.setVisibility(View.INVISIBLE);

        switch (myData.getBoardType()) {
            case NORMAL:
                tpcMsgDetails.setVisibility(View.VISIBLE);
                lastPost.setVisibility(View.VISIBLE);

                tpcMsgDetails.setText("Tpcs: " + myData.getTCount() + "; Msgs: " + myData.getMCount());
                lastPost.setText("Last Post: " + myData.getLastPost());
                break;
            case SPLIT:
                tpcMsgDetails.setVisibility(View.INVISIBLE);
                lastPost.setVisibility(View.VISIBLE);

                lastPost.setText("--Split List--");
                break;
            case EXPLORER_LINK:
                tpcMsgDetails.setVisibility(View.INVISIBLE);
                lastPost.setVisibility(View.INVISIBLE);

        }
    }

}
