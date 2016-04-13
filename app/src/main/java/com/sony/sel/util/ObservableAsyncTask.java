package com.sony.sel.util;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;

/**
 * Async task that notifies observers at various stages of the task.
 */
public abstract class ObservableAsyncTask<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> {

  private ObserverSet<AsyncTaskObserver<Progress, Result>> observers = new ObserverSet<>((Class<AsyncTaskObserver<Progress, Result>>) (Class<?>) AsyncTaskObserver.class);

  public interface AsyncTaskObserver<Progress, Result> {
    void onStarting();

    void onProgress(Progress... values);

    void onCanceled(Result result);

    void onComplete(Result result);

    void onError(Throwable error);
  }

  public static abstract class BasicObserver<Progress, Result> implements AsyncTaskObserver<Progress, Result> {

    @Override
    public void onStarting() {

    }

    @Override
    public void onProgress(Progress... values) {

    }

    @Override
    public void onCanceled(Result result) {

    }
  }

  public ObserverSet<AsyncTaskObserver<Progress, Result>> getObservers() {
    return observers;
  }

  @Override
  protected void onPreExecute() {
    super.onPreExecute();
    observers.proxy().onStarting();
  }

  @Override
  protected void onProgressUpdate(Progress... values) {
    super.onProgressUpdate(values);
    observers.proxy().onProgress(values);
  }

  @Override
  protected void onCancelled(Result result) {
    super.onCancelled(result);
    observers.proxy().onCanceled(result);
  }

  @Override
  protected void onPostExecute(Result result) {
    super.onPostExecute(result);
    observers.proxy().onComplete(result);
  }

  protected void onError(final Throwable error) {
    new Handler(Looper.getMainLooper()).post(new Runnable() {
      @Override
      public void run() {
        observers.proxy().onError(error);
      }
    });
  }
}
