package info.evelio.imgurtinder.api;

import retrofit.http.GET;
import retrofit.http.Headers;
import rx.Observable;

public interface ImgurApi {
  @GET("/gallery/hot/viral/0.json")
  @Headers("Authorization: Client-ID e761dd3d0be7725")
  public Observable<Response> getFrontPage();
}
