package info.evelio.imgurtinder;

import android.app.Activity;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.View;
import android.widget.Toast;

import java.util.List;
import java.util.Random;

import butterknife.ButterKnife;
import butterknife.InjectView;
import info.evelio.imgurtinder.util.L;
import info.evelio.imgurtinder.widget.ImageVoteView;
import rx.Observer;
import rx.Subscription;

import static android.view.View.GONE;
import static android.widget.Toast.LENGTH_SHORT;
import static info.evelio.imgurtinder.api.Repository.IMAGE_TO_URLS;
import static info.evelio.imgurtinder.api.Repository.frontPage;
import static rx.android.app.AppObservable.bindActivity;


public class MainActivity extends Activity {
  private static final String TAG = "it:MainActivity";
  private Subscription mSubscription;
  @InjectView(R.id.images)
  ImageVoteView mImages;
  @InjectView(R.id.progress_indicator)
  View mProgressIndicator;

  Observer<List<String>> mObserver = new InternalObserver();

  Random mRandom = new Random();
  SparseArray<String[]> mCachedArrays = new SparseArray<>();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    ButterKnife.inject(this);

    mImages.setOnActionCompletedListener(new ImageVoteView.OnActionCompletedListener() {
      @Override
      public void onActionCompleted(ImageVoteView.Action action) {
        CharSequence msg = null;
        switch (action) {
          case UPVOTE:
            msg = random(R.array.upvote_msgs);
            break;
          case DOWNVOTE:
            msg = random(R.array.downvote_msgs);
            break;
          default:
            break;
        }
        if (msg != null) {
          Toast.makeText(MainActivity.this, msg, LENGTH_SHORT).show();
        }
      }
    });

    load();
  }

  @Override
  protected void onPause() {
    super.onPause();

    unsubscribe();
  }

  private void load() {
    unsubscribe();
    mSubscription = bindActivity(this, frontPage())
        .map(IMAGE_TO_URLS)
        .subscribe(mObserver);
  }

  private void unsubscribe() {
    if (mSubscription != null) {
      mSubscription.unsubscribe();
      mSubscription = null;
    }
  }

  private class InternalObserver implements Observer<List<String>> {
    @Override
    public void onCompleted() {
      mProgressIndicator.setVisibility(GONE);
    }

    @Override
    public void onError(Throwable throwable) {
      // TODO show error or something
      L.e(TAG, "onError", throwable);
    }

    @Override
    public void onNext(List<String> urls) {
      L.d(TAG, "onNext" + urls);
      mImages.setUrls(urls);
    }
  }

  private CharSequence random(int id) {
    String[] strings = mCachedArrays.get(id);
    if (strings == null) {
      strings = getResources().getStringArray(id);
      mCachedArrays.put(id, strings);
    }
    return strings[mRandom.nextInt(strings.length)];
  }
}
