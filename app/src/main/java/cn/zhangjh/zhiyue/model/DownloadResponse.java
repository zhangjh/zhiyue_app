package cn.zhangjh.zhiyue.model;

public class DownloadResponse {

	private boolean success;

	private String data;

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public String getData() { return this.data; }

	public void setData(String data) {
		this.data = data;
	}
}
