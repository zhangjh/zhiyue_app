package cn.zhangjh.zhiyue.api;

import cn.zhangjh.zhiyue.model.DownloadResponse;
import cn.zhangjh.zhiyue.model.SearchResponse;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface BookService {
    @GET("/books/search")
    Call<SearchResponse> searchBooks(
		    @Query("keyword") String keyword,
			@Query("format") String format,
		    @Query("page") Integer page,
		    @Query("limit") Integer limit
    );

	@FormUrlEncoded
	@POST("/books/download")
	Call<DownloadResponse> downloadBook(
			@Field("bookId") String bookId,
			@Field("hashId") String hashId
	);
} 