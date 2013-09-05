package com.ioabsoftware.gameraven.views.rowdata;

import com.ioabsoftware.gameraven.views.BaseRowData;
import com.ioabsoftware.gameraven.views.RowType;

import android.content.Context;

public class PMRowData extends BaseRowData {

	private String subject, sender, time, url;
	public String getSubject() {return subject;}
	public String getSender() {return sender;}
	public String getTime() {return time;}
	public String getUrl() {return url;}
	
	private boolean isOld;
	public boolean isOld() {return isOld;}
	
	public PMRowData(String subjectIn, String senderIn, String timeIn, String urlIn, boolean isOldIn) {
		subject = subjectIn;
		sender = senderIn;
		time = timeIn;
		url = urlIn;
		isOld = isOldIn;
	}
	
	@Override
	public RowType getRowType() {
		return RowType.PM;
	}

}
