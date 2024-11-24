package com.example.chat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.json.JSONArray;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Pattern;

public class MessageAdapter extends ArrayAdapter<String> {

    private static final int VIEW_TYPE_ME = 0;
    private static final int VIEW_TYPE_OTHER = 1;

    public MessageAdapter(Context context, List<String> messages) {
        super(context, 0, messages);
    }

    @Override
    public int getViewTypeCount() {
        return 2; // Two types of views: me and other
    }

    @Override
    public int getItemViewType(int position) {
        String message = getItem(position);
        if (message.startsWith("Me: ")) {
            return VIEW_TYPE_ME;
        } else {
            return VIEW_TYPE_OTHER;
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        int viewType = getItemViewType(position);
        if (convertView == null) {
            if (viewType == VIEW_TYPE_ME) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item, parent, false);
            } else {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_other, parent, false);
            }
        }

        TextView messageText;
        TextView date;
        TextView time;
        String message = getItem(position);
        String formattedDate = "";
        String formattedtime = "";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            LocalDate myDateObj = LocalDate.now();

             formattedDate = myDateObj.format(DateTimeFormatter.ofPattern("MMM dd"));
             formattedtime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        }
        if (viewType == VIEW_TYPE_ME) {
            messageText = convertView.findViewById(R.id.message_text);
            message = message.substring(4);
            date = convertView.findViewById(R.id.text_gchat_date_me);
            time = convertView.findViewById(R.id.text_gchat_timestamp_me);

            date.setText(formattedDate);
            time.setText(formattedtime);
        } else {
            messageText = convertView.findViewById(R.id.text_gchat_message_other);
            date = convertView.findViewById(R.id.text_gchat_date_other);
            time = convertView.findViewById(R.id.text_gchat_timestamp_other);
            date.setText(formattedDate);
            time.setText(formattedtime);
            TextView userText = convertView.findViewById(R.id.text_gchat_user_other);
            userText.setText("admin");
            if (message.contains(":")) {
                Pattern pattern = Pattern.compile(":", Pattern.LITERAL);
                String[] split = pattern.split(message, 2);
                userText.setText(split[0]);
                message = split[1];
            }
        }

        messageText.setText(message);

        return convertView;
    }
}