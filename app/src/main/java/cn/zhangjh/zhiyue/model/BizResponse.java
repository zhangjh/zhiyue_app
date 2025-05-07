package cn.zhangjh.zhiyue.model;

public class BizResponse {

	private boolean success;

	private String data;

	private String errorMsg;

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

	public String getErrorMsg() {
		return errorMsg;
	}

	public void setErrorMsg(String errorMsg) {
		this.errorMsg = errorMsg;
	}
}
