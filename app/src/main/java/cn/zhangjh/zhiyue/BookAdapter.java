package cn.zhangjh.zhiyue;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

import cn.zhangjh.zhiyue.pojo.Book;

public class BookAdapter extends RecyclerView.Adapter<BookAdapter.BookViewHolder> {
    private List<Book> books = new ArrayList<>();
    private OnBookClickListener listener;
    private int expandedPosition = -1;

    public interface OnBookClickListener {
        void onReadButtonClick(Book book);
    }

    public void setOnBookClickListener(OnBookClickListener listener) {
        this.listener = listener;
    }

    public void setBooks(List<Book> books) {
        this.books = books;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_book, parent, false);
        return new BookViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookViewHolder holder, int position) {
        Book book = books.get(position);
        holder.bind(book, position);
    }

    @Override
    public int getItemCount() {
        return books.size();
    }

    class BookViewHolder extends RecyclerView.ViewHolder {
        private final ImageView bookCover;
        private final TextView bookTitle;
        private final TextView bookAuthor;
        private final TextView bookSize;
        private final TextView bookFormat;
        private final TextView bookDescription;
        private final LinearLayout expandableContent;
        private final MaterialButton readButton;

        public BookViewHolder(@NonNull View itemView) {
            super(itemView);
            bookCover = itemView.findViewById(R.id.bookCover);
            bookTitle = itemView.findViewById(R.id.bookTitle);
            bookAuthor = itemView.findViewById(R.id.bookAuthor);
            bookSize = itemView.findViewById(R.id.bookSize);
            bookFormat = itemView.findViewById(R.id.bookFormat);
            bookDescription = itemView.findViewById(R.id.bookDescription);
            expandableContent = itemView.findViewById(R.id.expandableContent);
            readButton = itemView.findViewById(R.id.readButton);
        }

        void bind(final Book book, final int position) {
            bookTitle.setText(book.getTitle());
            bookAuthor.setText(book.getAuthor());
            bookSize.setText(book.getSize());
            bookFormat.setText(book.getFormat());
            bookDescription.setText(book.getDescription());

            // Load book cover using Glide
            Glide.with(itemView.getContext())
                    .load(book.getCoverUrl())
//                    .placeholder(R.drawable.ic_menu_gallery)
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(bookCover);

            // Handle expansion
            boolean isExpanded = position == expandedPosition;
            expandableContent.setVisibility(isExpanded ? View.VISIBLE : View.GONE);

            itemView.setOnClickListener(v -> {
                expandedPosition = isExpanded ? -1 : position;
                notifyDataSetChanged();
            });

            readButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onReadButtonClick(book);
                }
            });
        }
    }
} 