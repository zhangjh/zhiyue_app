package cn.zhangjh.zhiyue.adapter;

import android.text.Html;
import android.text.Spanned;
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

import cn.zhangjh.zhiyue.R;
import cn.zhangjh.zhiyue.model.Book;

public class BookAdapter extends RecyclerView.Adapter<BookAdapter.BookViewHolder> {
    private List<Book> books = new ArrayList<>();
    private OnBookClickListener listener;
    private int expandedPosition = -1;

    public void setBooks(List<Book> books) {
        this.books = new ArrayList<>(books);
        notifyDataSetChanged();
    }

    public void addBooks(List<Book> newBooks) {
        int startPosition = this.books.size();
        this.books.addAll(newBooks);
        notifyItemRangeInserted(startPosition, newBooks.size());
    }

    public void clearBooks() {
        int size = this.books.size();
        this.books.clear();
        notifyItemRangeRemoved(0, size);
    }

    public void setOnBookClickListener(OnBookClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public BookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_book, parent, false);
        return new BookViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookViewHolder holder, int position) {
        holder.bind(books.get(position), position);
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
            
            // 处理HTML格式的描述文本
            String description = book.getDescription();
            if (description != null) {
                Spanned spannedText;
	            spannedText = Html.fromHtml(description, Html.FROM_HTML_MODE_LEGACY);
	            bookDescription.setText(spannedText);
            } else {
                bookDescription.setText("");
            }

            Glide.with(itemView.getContext())
                    .load(book.getCoverUrl())
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

    public interface OnBookClickListener {
        void onReadButtonClick(Book book);
    }
}