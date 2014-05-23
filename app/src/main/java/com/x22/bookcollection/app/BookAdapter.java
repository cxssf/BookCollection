package com.x22.bookcollection.app;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.x22.bookcollection.app.model.BookItem;

import java.util.List;

public class BookAdapter extends ArrayAdapter<BookItem> {
    private Context context;
    private List<BookItem> data;
    private int layoutResourceId;

    public BookAdapter(Context context, int layoutResourceId, List<BookItem> data) {
        super(context, layoutResourceId, data);

        this.context = context;
        this.layoutResourceId = layoutResourceId;
        this.data = data;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        BookItemHolder holder = null;

        if (row == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            row = inflater.inflate(layoutResourceId, parent, false);

            holder = new BookItemHolder();
            holder.title = (TextView) row.findViewById(R.id.title);
            holder.author = (TextView) row.findViewById(R.id.author);

            row.setTag(holder);
        } else {
            holder = (BookItemHolder) row.getTag();
        }

        BookItem bookItem = data.get(position);
        holder.title.setText(bookItem.getTitle());
        holder.author.setText(bookItem.getAuthor());

        return row;
    }

    /*public void updateBookList(List<BookItem> newlist) {
        receiptlist.clear();
        receiptlist.addAll(newlist);
        this.notifyDataSetChanged();
    }*/

    private static class BookItemHolder {
        TextView title;
        TextView author;
    }
}