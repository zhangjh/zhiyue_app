package cn.zhangjh.zhiyue;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.ArrayList;
import java.util.List;

import cn.zhangjh.zhiyue.model.ReadingHistory;

public class ReadingHistoryAdapter extends RecyclerView.Adapter<ReadingHistoryAdapter.ViewHolder> {
	private List<ReadingHistory> histories = new ArrayList<>();
	private OnHistoryItemClickListener listener;

	public interface OnHistoryItemClickListener {
		void onContinueReading(ReadingHistory history);

		void onDeleteHistory(ReadingHistory history);
	}

	public void setOnHistoryItemClickListener(OnHistoryItemClickListener listener) {
		this.listener = listener;
	}

	public void setHistories(List<ReadingHistory> histories) {
		this.histories = new ArrayList<>(histories);
		notifyDataSetChanged();
	}

	public void addHistories(List<ReadingHistory> newHistories) {
		int startPosition = this.histories.size();
		this.histories.addAll(newHistories);
		notifyItemRangeInserted(startPosition, newHistories.size());
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.item_reading_history, parent, false);
		return new ViewHolder(view);
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		ReadingHistory history = histories.get(position);
		holder.bind(history);
	}

	@Override
	public int getItemCount() {
		return histories.size();
	}

	class ViewHolder extends RecyclerView.ViewHolder {
		private final TextView bookTitle;
		private final TextView bookAuthor;
		private final CircularProgressIndicator progressBar;  // 修改类型为 CircularProgressIndicator
		private final TextView readingProgress;
		private final TextView startReadingTime;
		private final TextView lastReadingTime;
		private final MaterialButton deleteButton;
		private final MaterialButton continueReadingButton;

		public ViewHolder(@NonNull View itemView) {
			super(itemView);
			bookTitle = itemView.findViewById(R.id.bookTitle);
			bookAuthor = itemView.findViewById(R.id.bookAuthor);
			progressBar = itemView.findViewById(R.id.progressBar);
			readingProgress = itemView.findViewById(R.id.readingProgress);
			startReadingTime = itemView.findViewById(R.id.startReadingTime);
			lastReadingTime = itemView.findViewById(R.id.lastReadingTime);
			deleteButton = itemView.findViewById(R.id.deleteButton);
			continueReadingButton = itemView.findViewById(R.id.continueReadingButton);
		}

		void bind(ReadingHistory history) {
			bookTitle.setText(history.getBookTitle());
			bookAuthor.setText(history.getBookAuthor());
			readingProgress.setText(String.format("%d%%", history.getProgress()));
			startReadingTime.setText(String.format("开始阅读：%s", history.getStartTime()));
			lastReadingTime.setText(String.format("最近阅读：%s", history.getLastReadTime()));

			deleteButton.setOnClickListener(v -> {
				if (listener != null) {
					listener.onDeleteHistory(history);
				}
			});

			continueReadingButton.setOnClickListener(v -> {
				if (listener != null) {
					listener.onContinueReading(history);
				}
			});

			// 设置进度
			int progress = history.getProgress();
			progressBar.setProgress(progress);
			
			// 根据进度设置不同的颜色
			int color;
			if (progress < 30) {
				color = itemView.getContext().getColor(R.color.progress_low);
			} else if (progress < 70) {
				color = itemView.getContext().getColor(R.color.progress_medium);
			} else {
				color = itemView.getContext().getColor(R.color.progress_high);
			}
			progressBar.setIndicatorColor(color);
			readingProgress.setTextColor(color);
		}
	}
}