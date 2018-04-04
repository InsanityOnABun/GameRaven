package com.ioabsoftware.gameraven.views;

public abstract class BaseRowData {
    public enum ReadStatus {
        UNREAD, READ, NEW_POST
    }

    public abstract RowType getRowType();

    @Override
    public abstract String toString();
}
