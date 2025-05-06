package cn.zhangjh.zhiyue.auth;

public class UserManager {
    private static UserManager instance;
    private String userId;
    private String email;
    private String idToken;

    private UserManager() {}

    public static synchronized UserManager getInstance() {
        if (instance == null) {
            instance = new UserManager();
        }
        return instance;
    }

    public void setUserInfo(String userId, String email, String idToken) {
        this.userId = userId;
        this.email = email;
        this.idToken = idToken;
    }

    public String getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getIdToken() {
        return idToken;
    }

    public void clearUserInfo() {
        userId = null;
        email = null;
        idToken = null;
    }
}