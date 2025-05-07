package cn.zhangjh.zhiyue.request;

public class LoginUserRequest {

	private String product_type = "zhiyue_app";
	private String name;
	private String avatar;
	private String ext_id;
	private String ext_type = "google";
	private String email;

	public String getProduct_type() {
		return product_type;
	}

	public void setProduct_type(String product_type) {
		this.product_type = product_type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAvatar() {
		return avatar;
	}

	public void setAvatar(String avatar) {
		this.avatar = avatar;
	}

	public String getExt_id() {
		return ext_id;
	}

	public void setExt_id(String ext_id) {
		this.ext_id = ext_id;
	}

	public String getExt_type() {
		return ext_type;
	}

	public void setExt_type(String ext_type) {
		this.ext_type = ext_type;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}
}
