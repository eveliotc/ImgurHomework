package info.evelio.imgurtinder.widget;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver;

import com.facebook.rebound.SimpleSpringListener;
import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringSystem;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import com.squareup.picasso.Target;

import java.util.List;
import java.util.Stack;

import info.evelio.imgurtinder.R;

import static info.evelio.imgurtinder.widget.ImageVoteView.Action.DOWNVOTE;
import static info.evelio.imgurtinder.widget.ImageVoteView.Action.NONE;
import static info.evelio.imgurtinder.widget.ImageVoteView.Action.UPVOTE;
import static java.util.Collections.emptyList;

public class ImageVoteView extends View {
  private static final String TAG = "it:ImageVoteView";
  private static final float IMAGE_SIDE_PERCENTAGE = 0.9f;
  private static final float CENTER_Y_TOLERANCE_PERCENTAGE = 0.40f;
  private Stack<String> mUrls = new Stack<>();

  private Item mOnTop;
  private Item mOnDeck;

  private boolean mHit;
  private float mLastX;
  private float mLastY;

  private static final int INVALID_POINTER_ID = -1;
  private int mActivePointerId = INVALID_POINTER_ID;

  SpringSystem mSpringSystem = SpringSystem.create();

  private OnActionCompletedListener mOnActionCompletedListener;
  private OnActionCompletedListener mInternalListener = new OnActionCompletedListener() {
    @Override
    public void onActionCompleted(Action action) {
      switch (action) {
        case UPVOTE:
        case DOWNVOTE:
          removeTopAndContinue();
        default:
          break;
      }
      if (mOnActionCompletedListener != null) {
        mOnActionCompletedListener.onActionCompleted(action);
      }
    }
  };

  public ImageVoteView(Context context) {
    super(context);
    initMe();
  }

  public ImageVoteView(Context context, AttributeSet attrs) {
    super(context, attrs);
    initMe();
  }

  public ImageVoteView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    initMe();
  }

  public ImageVoteView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
    initMe();
  }

  private void initMe() {
    Resources res = getResources();
    Paint up = new Paint();
    up.setColorFilter(new LightingColorFilter(res.getColor(R.color.upvote_green), 1));
    Paint down = new Paint();
    down.setColorFilter(new LightingColorFilter(res.getColor(R.color.downvote_red), 1));
    mOnTop = new Item(up, down);
    mOnDeck = new Item(null, null);
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();

    Spring spring = mSpringSystem.createSpring();
    spring.setOvershootClampingEnabled(false);
    mOnTop.use(this, spring, mInternalListener);
    mOnDeck.use(this);
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();

    mOnTop.useNone();
    mOnDeck.useNone();
    cancelTagged();
  }

  private void cancelTagged() {
    Picasso.with(getContext()).cancelTag(TAG);
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);

    mOnDeck.drawOn(canvas);
    mOnTop.drawOn(canvas);
  }

  @Override
  public boolean onTouchEvent(MotionEvent ev) {
    // This is pretty much stolen from GestureDetector and http://android-developers.blogspot.com/2010/06/making-sense-of-multitouch.html
    final int action = ev.getAction();

    switch (action & MotionEvent.ACTION_MASK) {
      case MotionEvent.ACTION_POINTER_UP: {
        // Extract the index of the pointer that left the touch sensor
        final int pointerIndex = (action & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
        final int pointerId = ev.getPointerId(pointerIndex);
        if (pointerId == mActivePointerId) {
          // This was our active pointer going up. Choose a new
          // active pointer and adjust accordingly.
          final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
          mLastX = ev.getX(newPointerIndex);
          mLastY = ev.getY(newPointerIndex);
          mActivePointerId = ev.getPointerId(newPointerIndex);
        }
        break;
      }
      case MotionEvent.ACTION_DOWN: {
        mLastX = ev.getX();
        mLastY = ev.getY();
        mActivePointerId = ev.getPointerId(0);

        checkForHit();
        break;
      }
      case MotionEvent.ACTION_MOVE: {
        final int pointerIndex = ev.findPointerIndex(mActivePointerId);
        final float x = ev.getX(pointerIndex);
        final float y = ev.getY(pointerIndex);
        final float scrollX = mLastX - x;
        final float scrollY = mLastY - y;
        if ((Math.abs(scrollX) >= 1) || (Math.abs(scrollY) >= 1)) {
          mLastX = x;
          mLastY = y;
        }
        if (mHit) {
          mOnTop.move(mLastX, mLastY);
          postInvalidateOnAnimation();
        }
        break;
      }
      case MotionEvent.ACTION_UP:
        mOnTop.checkForActionAndReset();
        break;
      case MotionEvent.ACTION_CANCEL:
        reset(true);
        break;
    }

    return true;
  }

  private void checkForHit() {
    mHit = mOnTop.gotHit(mLastX, mLastY);
  }

  private void reset(boolean animated) {
    mHit = false;
    mActivePointerId = INVALID_POINTER_ID;
    mOnTop.reset(animated);
  }

  public void setUrls(List<String> urls) {
    if (urls == null) {
      urls = emptyList();
    }
    mUrls.removeAllElements();
    mUrls.addAll(urls);

    requestTopOnceMeasured();
  }

  private void removeTopAndContinue() {
    if (!mUrls.isEmpty()) {
      mUrls.pop();
    }
    requestTopOnceMeasured();
  }

  private void requestTopOnceMeasured() {
    int smallest = Math.min(getWidth(), getHeight());
    if (smallest < 1) {
      ViewTreeObserver vto = getViewTreeObserver();
      if (vto != null && vto.isAlive()) {
        vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
          @Override
          public boolean onPreDraw() {
            ViewTreeObserver vto = getViewTreeObserver();
            if (vto != null && vto.isAlive()) {
              vto.removeOnPreDrawListener(this);
            }
            requestTopOnceMeasured();
            return false;
          }
        });
      }
    } else {
      requestTop(smallest);
    }
  }

  private void requestTop(int smallestSide) {
    Context context = getContext();
    String topUrl = null;
    String deckUrl = null;
    String preloadUrl = null;
    int size = mUrls.size();
    if (size > 1) {
      topUrl = mUrls.peek();
      deckUrl = mUrls.get(size - 2);
      if (size > 2) {
        preloadUrl = mUrls.get(size - 3);
      }
    } else if (size == 1) {
      topUrl = mUrls.peek();
    }
    int leSide = (int) ((smallestSide * IMAGE_SIDE_PERCENTAGE) + 0.5f);
    mOnTop.load(context, topUrl, leSide);
    mOnDeck.load(context, deckUrl, leSide);
    mOnDeck.preload(context, preloadUrl, leSide);
  }

  public OnActionCompletedListener getOnActionCompletedListener() {
    return mOnActionCompletedListener;
  }

  public void setOnActionCompletedListener(OnActionCompletedListener onActionCompletedListener) {
    mOnActionCompletedListener = onActionCompletedListener;
  }

  public enum Action { UPVOTE, DOWNVOTE, NONE }

  public interface OnActionCompletedListener {
    public void onActionCompleted(Action action);
  }

  static class Item extends SimpleSpringListener implements Target {
    Bitmap mBitmap;
    RectF mRect = new RectF();
    float mHitDeltaX;
    float mHitDeltaY;
    ImageVoteView mView;

    Spring mSpring;
    boolean mAnimating;
    float mAnimatingDeltaX;
    float mAnimatingDeltaY;

    final Paint mUpPaint;
    final Paint mDownPaint;

    private Action mLastAction = NONE;
    private OnActionCompletedListener mListener;

    Item(Paint upPaint, Paint downPaint) {
      mUpPaint = upPaint;
      mDownPaint = downPaint;
    }

    public void drawOn(Canvas canvas) {
      // TODO draw optimizations can happen here with clip and reject ops
      if (mBitmap != null) {
        canvas.drawBitmap(mBitmap, null, mRect, obtainPaint());
      }
    }

    private Paint obtainPaint() {
      Action action = resolveAction();
      switch (action) {
        case UPVOTE:
          return mUpPaint;
        case DOWNVOTE:
          return mDownPaint;
        default:
          return null;
      }
    }

    private Action resolveAction() {
      ImageVoteView view = mView;
      if (view != null) {
        float tolerance = mRect.height() * CENTER_Y_TOLERANCE_PERCENTAGE;
        float vCenterY = view.getHeight() / 2.0f;
        float itemCenterY = mRect.centerY();
        float diff = itemCenterY - tolerance;
        if (diff > vCenterY) {
          return DOWNVOTE;
        } else if ((itemCenterY + tolerance) < vCenterY) {
          return UPVOTE;
        }
      }
      return NONE;
    }

    @Override
    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
      mBitmap = bitmap;
      float halfWidth = mBitmap.getWidth() / 2.0f;
      float halfHeight = mBitmap.getHeight() / 2.0f;
      float centerX = mRect.centerX();
      float centerY = mRect.centerY();
      mRect.set(centerX - halfWidth, centerY - halfHeight, centerX + halfWidth, centerY + halfHeight);
      centerInView();
    }

    private void centerInView() {
      mAnimating = false;
      ImageVoteView view = mView;
      if (view != null) {
        float x = (view.getWidth() - mRect.width()) / 2.0f;
        float y = (view.getHeight() - mRect.height()) / 2.0f;
        mRect.offsetTo(x, y);
        view.postInvalidateOnAnimation();
      }
    }

    private boolean computeAnimatingDeltas(Action action) {
      ImageVoteView view = mView;
      if (view != null) {
        int viewWidth = view.getWidth();
        int viewHeight = view.getHeight();
        float itemWidth = mRect.width();
        float itemHeight = mRect.height();
        float x = (viewWidth - itemWidth) / 2.0f;
        float y = (viewHeight - itemHeight) / 2.0f;
        mAnimatingDeltaX = x - mRect.left;
        switch (action) {
          case UPVOTE:
            mAnimatingDeltaY = -itemHeight * 1.2f;
            return true;
          case DOWNVOTE:
            mAnimatingDeltaY = viewHeight;
            return true;
          default:
            mAnimatingDeltaY = y - mRect.top;
            int scaledTouchSlop = ViewConfiguration.get(view.getContext()).getScaledPagingTouchSlop();
            return Math.abs(mAnimatingDeltaY) > scaledTouchSlop || Math.abs(mAnimatingDeltaX) > scaledTouchSlop;
        }
      }
      return false;
    }

    @Override
    public void onBitmapFailed(Drawable errorDrawable) {
      mBitmap = null;
    }

    @Override
    public void onPrepareLoad(Drawable placeHolderDrawable) {
      mBitmap = null;
    }

    public void load(Context context, String url, int leSide) {
      doLoad(context, url, leSide, true);
    }

    public void preload(Context context, String url, int leSide) {
      doLoad(context, url, leSide, false);
    }

    private void doLoad(Context context, String url, int leSide, boolean forReal) {
      if (url == null && forReal) {
        mBitmap = null;
        return;
      }
      Picasso picasso = Picasso.with(context);
      RequestCreator request = picasso.load(url)
          .resize(leSide, leSide)
          .centerCrop()
          .tag(TAG);

      if (forReal) {
        request.into(this);
      } else {
        request.fetch();
      }
    }

    public boolean gotHit(float pointerX, float pointerY) {
      mHitDeltaX = mRect.left - pointerX;
      mHitDeltaY = mRect.top - pointerY;
      return mBitmap != null && mRect.contains(pointerX, pointerY);
    }

    public void move(float pointerX, float pointerY) {
      mRect.offsetTo(pointerX + mHitDeltaX, pointerY + mHitDeltaY);
    }

    public void use(ImageVoteView view, Spring spring, OnActionCompletedListener listener) {
      mListener = listener;
      mAnimating = false;
      mView = view;

      Spring currentSpring = mSpring;
      mSpring = spring;
      if (currentSpring != null) {
        currentSpring.destroy();
      }
      if (spring != null) {
        spring.addListener(this);
      }
    }

    public void use(ImageVoteView view) {
      use(view, null, null);
    }


    public void useNone() {
      use(null, null, null);
    }

    public void reset(boolean animated) {
      reset(NONE, animated);
    }

    void reset(Action action, boolean animated) {
      mLastAction = action;
      Spring spring = mSpring;
      if (animated && spring != null) {
        mAnimating = false;
        spring.setCurrentValue(0);
        mAnimating = computeAnimatingDeltas(action);
        if (mAnimating) {
          spring.setEndValue(1);
          return;
        }
      }
      // By default center not animating
      centerInView();
      dispatchOnActionCompleted();
    }

    @Override
    public void onSpringUpdate(Spring spring) {
      if (mAnimating) {
        float value = (float) spring.getCurrentValue();
        float diffX = mAnimatingDeltaX * value;
        float diffY = mAnimatingDeltaY * value;
        mAnimatingDeltaX -= diffX;
        mAnimatingDeltaY -= diffY;
        mRect.offset(diffX, diffY);
        ImageVoteView view = mView;
        if (view != null) {
          view.postInvalidateOnAnimation();
        }
      }
    }

    @Override
    public void onSpringAtRest(Spring spring) {
      mAnimating = false;

      dispatchOnActionCompleted();
      switch (mLastAction) {
        case NONE:
          break;
        default:
          centerInView();
          break;
      }
    }

    void checkForActionAndReset() {
      Action action = resolveAction();
      reset(action, true);
    }

    private void dispatchOnActionCompleted() {
      if (mListener != null) {
        mListener.onActionCompleted(mLastAction);
      }
    }
  }

}
