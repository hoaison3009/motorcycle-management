package com.example.sqlitek24dtcn533;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.util.ArrayList;

public final class RowAdapter extends ArrayAdapter<RowAdapter.Row> {
    public static final class Row {
        public final long id;
        public final String title,detail;
        public Row(long id,String title,String detail) { this.id=id; this.title=title; this.detail=detail; }
    }
    public RowAdapter(Context context) { super(context,0,new ArrayList<>()); }
    @Override public View getView(int position,View recycled,ViewGroup parent) {
        View view = recycled == null ? LayoutInflater.from(getContext()).inflate(R.layout.row_item,parent,false) : recycled;
        Row row=getItem(position);
        if(row!=null) {
            ((TextView)view.findViewById(R.id.rowTitle)).setText(row.title);
            ((TextView)view.findViewById(R.id.rowDetail)).setText(row.detail);
        }
        return view;
    }
}
