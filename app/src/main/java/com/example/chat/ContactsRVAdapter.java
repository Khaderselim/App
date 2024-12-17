package com.example.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class ContactsRVAdapter extends RecyclerView.Adapter<ContactsRVAdapter.ContactsViewHolder> {

    private ArrayList<ContactsModal> contactsModalArrayList;
    private OnItemClickListener onItemClickListener;
    private View.OnLongClickListener onLongClickListener;

    public ContactsRVAdapter(ArrayList<ContactsModal> contactsModalArrayList) {
        this.contactsModalArrayList = contactsModalArrayList;
    }

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }
    public void setOnLongClickListener(View.OnLongClickListener listener) {
        this.onLongClickListener = listener;
    }

    @NonNull
    @Override
    public ContactsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.contacts_rv_item, parent, false);
        return new ContactsViewHolder(view, onItemClickListener , onLongClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactsRVAdapter.ContactsViewHolder holder, int position) {
        ContactsModal contactsModal = contactsModalArrayList.get(position);
        holder.usernameTV.setText(contactsModal.getUsername());
        holder.statusTV.setText(contactsModal.getStatus());
    }

    @Override
    public int getItemCount() {
        return contactsModalArrayList.size();
    }

    public static class ContactsViewHolder extends RecyclerView.ViewHolder {

        TextView usernameTV, statusTV;

        public ContactsViewHolder(@NonNull View itemView, OnItemClickListener listener, View.OnLongClickListener longClickListener) {
            super(itemView);
            usernameTV = itemView.findViewById(R.id.idTVContactName);
            statusTV = itemView.findViewById(R.id.idTVStatus);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            listener.onItemClick(position);
                        }
                    }
                }
            });
            itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    return longClickListener.onLongClick(v);
                }
                return false;
            });
        }
    }
}