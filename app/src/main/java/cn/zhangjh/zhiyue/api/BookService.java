package cn.zhangjh.zhiyue.api;

import cn.zhangjh.zhiyue.model.SearchResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface BookService {
    @GET("/books/search")
    Call<SearchResponse> searchBooks(
		    @Query("keyword") String keyword,
		    @Query("page") Integer page,
		    @Query("limit") Integer limit
    );

    // 提供一个便捷方法，使用默认参数
    default Call<SearchResponse> searchBooks(String keyword) {
        return searchBooks(keyword, null, null);
    }
} 