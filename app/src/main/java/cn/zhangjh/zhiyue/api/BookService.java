package cn.zhangjh.zhiyue.api;

import java.util.List;

import cn.zhangjh.zhiyue.model.Annotation;
import cn.zhangjh.zhiyue.model.BizListResponse;
import cn.zhangjh.zhiyue.model.BizResponse;
import cn.zhangjh.zhiyue.model.BookDetail;
import cn.zhangjh.zhiyue.model.HistoryResponse;
import cn.zhangjh.zhiyue.model.ReadingRecord;
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

    @GET("/books/recommend")
    Call<BizListResponse<BookDetail>> getRecommendBooks(
            @Query("bookIds") List<String> bookIds,
            @Query("pageIndex") Integer pageIndex
    );

	@FormUrlEncoded
	@POST("/books/download")
	Call<BizResponse<String>> downloadBook(
			@Field("bookId") String bookId,
			@Field("hashId") String hashId
	);

    @POST("/books/saveAnnotation")
    Call<BizResponse<Void>> saveAnnotation(@Body Annotation annotation);
    
    @GET("/books/getAnnotations")
    Call<BizListResponse<Annotation>> getAnnotations(@Query("userId") String userId, @Query("fileId") String fileId);

	@POST("/books/deleteAnnotation/{annotationText}")
	Call<BizResponse<Void>> deleteAnnotation(@Path("annotationText") String annotationText);

	@GET("/books/getHistory")
	Call<BizResponse<HistoryResponse>> getHistory(
			@Query("pageIndex") int pageIndex,
			@Query("pageSize") int pageSize,
			@Query("userId") String userId
	);

	@GET("/books/getRecordDetail")
	Call<BizResponse<ReadingRecord>> getRecordDetail(
			@Query("userId") String userId,
			@Query("fileId") String fileId
	);

    @POST("/parse/saveRecord")
    Call<BizResponse<Void>> saveRecord(@Body ReadingRecord readingRecord);

	@FormUrlEncoded
    @POST("/books/deleteHistory")
    Call<BizResponse<Void>> deleteHistory(
            @Field("userId") String userId,
            @Field("fileId") String fileId
    );

    @POST("/parse/updateRecord")
    Call<BizResponse<Void>> updateRecord(@Body ReadingRecord readingRecord);
}