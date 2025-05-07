package cn.zhangjh.zhiyue.model;

// 数据库登录用户对象
public class LoginUser {

    private int id;
    private String name;
    private String avatar;
    private String ext_id;
    private String ext_type;
    private String feature;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public String getFeature() {
        return feature;
    }

    public void setFeature(String feature) {
        this.feature = feature;
    }
}
