package cn.zhangjh.zhiyue.api;

import cn.zhangjh.zhiyue.model.Annotation;
import cn.zhangjh.zhiyue.model.BizListResponse;
import cn.zhangjh.zhiyue.model.BizResponse;
import cn.zhangjh.zhiyue.model.BookDetail;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface BookService {
    @GET("/books/search")
    Call<BizListResponse<BookDetail>> searchBooks(
		    @Query("keyword") String keyword,
			@Query("format") String format,
		    @Query("page") Integer page,
		    @Query("limit") Integer limit
    );

	@FormUrlEncoded
	@POST("/books/download")
	Call<BizResponse> downloadBook(
			@Field("bookId") String bookId,
			@Field("hashId") String hashId
	);

	//	todo: add annotation api
    @POST("/annotations")
    Call<BizResponse> saveAnnotation(@Body Annotation annotation);
    
    @GET("/annotations/{bookId}")
    Call<BizListResponse<Annotation>> getAnnotations(@Path("bookId") String bookId);
}