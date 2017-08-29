package com.example.android.newsapp;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Adapter for the list of news returned from the API
 */
class NewsListAdapter extends ArrayAdapter<News> {

    private static final String TAG = NewsListAdapter.class.getSimpleName();

    SimpleDateFormat inputDateFormat;
    SimpleDateFormat outputDateFormat;

    NewsListAdapter(@NonNull Context context, @LayoutRes int resource) {
        super(context, resource);

        inputDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
        inputDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        outputDateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;
        if (convertView != null) {
            holder = (ViewHolder) convertView.getTag();
        } else {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.search_result_item, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        }

        News currentItem = getItem(position);

        holder.newsTitle.setText(currentItem.getTitle());
        holder.newsSection.setText(currentItem.getSection());

        if (currentItem.getAuthor() != null) {
            holder.newsAuthor.setVisibility(View.VISIBLE);
            holder.newsAuthor.setText(String.format("%s %s", getContext().getResources().getString(R
                    .string.by), currentItem.getAuthor()));
        } else {
            holder.newsAuthor.setVisibility(View.GONE);
        }

        if (currentItem.getDate() != null) {
            try {
                Date newsDate = inputDateFormat.parse(currentItem.getDate());

                holder.newsDate.setVisibility(View.VISIBLE);
                holder.newsDate.setText(String.format("%s %s", getContext().getResources()
                        .getString(R.string.date), outputDateFormat.format(newsDate)));
            } catch (ParseException e) {
                Log.d(TAG, "Error parsing news publish date");
            }
        } else {
            holder.newsDate.setVisibility(View.GONE);
        }

        return convertView;
    }

    static class ViewHolder {
        @BindView(R.id.news_title)
        TextView newsTitle;

        @BindView(R.id.news_section)
        TextView newsSection;

        @BindView(R.id.news_author)
        TextView newsAuthor;

        @BindView(R.id.news_date)
        TextView newsDate;

        private ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }
}
