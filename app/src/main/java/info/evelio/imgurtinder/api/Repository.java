package info.evelio.imgurtinder.api;

import java.util.LinkedList;
import java.util.List;

import info.evelio.imgurtinder.util.L;
import retrofit.ErrorHandler;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import rx.Observable;
import rx.functions.Func1;

import static rx.android.schedulers.AndroidSchedulers.mainThread;
import static rx.schedulers.Schedulers.io;

public class Repository {
  private static final String TAG = "it:Repository";

  private static final String ENDPOINT = "https://api.imgur.com/3";
  static final RestAdapter REST_ADAPTER = new RestAdapter.Builder()
      .setEndpoint(ENDPOINT)
      .setErrorHandler(new ErrorHandler() {
        @Override
        public Throwable handleError(RetrofitError retrofitError) {
          L.e(TAG, "handleError", retrofitError);
          return retrofitError;
        }
      })
      .setLog(new RestAdapter.Log() {
        @Override
        public void log(String s) {
          L.e(TAG, s);
        }
      })
      .build();
  static final ImgurApi API = REST_ADAPTER.create(ImgurApi.class);


  public static Observable<Response> frontPage() {
    return API.getFrontPage()
        .subscribeOn(io())
        .observeOn(mainThread());
  }

  public static final Func1<Response, List<String>> IMAGE_TO_URLS = new Func1<Response, List<String>>() {
    @Override
    public List<String> call(Response response) {
      List<String> urls = new LinkedList<>();
      if (response == null) {
        return urls;
      }
      List<Image> data = response.getData();
      if (data == null) {
        return urls;
      }

      for (Image image : data) {
        if (image != null) {
          String imageUrl = image.getImageUrl();
          if (imageUrl != null) {
            urls.add(imageUrl);
          }
        }
      }
      return urls;
    }
  };
}
