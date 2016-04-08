package com.sony.sel.util;

import java.io.Closeable;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * Utility class for Threads
 */
public class ThreadUtils {

  public static final String LOG_TAG = ThreadUtils.class.getSimpleName();

  private static final int MAXIMUM_THREADS = 8;
  private static final long THREAD_KEEP_ALIVE_MS = 60 * 1000;

  private static Handler sHandler = new Handler(Looper.getMainLooper());

  private static Executor sExecutor = new ThreadPoolExecutor(
      0, MAXIMUM_THREADS, THREAD_KEEP_ALIVE_MS, TimeUnit.MILLISECONDS,
      new LinkedBlockingQueue<Runnable>());

  /**
   * A Runnable that simply waits for {@link #close()}
   * to be called before returning.  Once {@link #close()} is called,
   * all calls to {@link #run()} will return immediately.
   *
   * @author glewis
   */
  public static class BlockingRunnable implements Runnable, Closeable {
    private boolean mBlocked;
    private boolean mClosed;

    @Override
    public void close() {
      synchronized (this) {
        mClosed = true;
        notifyAll();
      }
    }

    @Override
    public void run() {
      synchronized (this) {
        mBlocked = true;
        notifyAll();
        while (!mClosed) {
          try {
            wait();
          } catch (InterruptedException e) {
            Log.e(LOG_TAG, e.toString());
            break;
          }
        }
      }
    }

    /**
     * waits until {@link #run()} is called, ensuring that
     * whatever thread it is on will be completely blocked.
     */
    public void waitForBlock() {
      synchronized (this) {
        while (!mBlocked && !mClosed) {
          try {
            wait();
          } catch (InterruptedException e) {
            Log.e(LOG_TAG, e.toString());
            break;
          }
        }
      }
    }
  }

  public static void sleep(long msecs) {
    try {
      if (msecs == 0) {
        Thread.yield();
      } else {
        Thread.sleep(msecs);
      }
    } catch (InterruptedException e) {
    }
  }

  /**
   * returns true if the current thread is the UI thread.
   */
  public static boolean isOnUiThread() {
    return Thread.currentThread() == Looper.getMainLooper().getThread();
  }

  /**
   * runs the specified Runnable on the UI thread.
   * If the current thread is the UI thread, it will be executed synchronously.
   * Otherwise, it will be posted to run on the UI thread at a later time.
   */
  public static void runOnUiThread(Runnable r) {
    if (!isOnUiThread()) {
      sHandler.post(r);
    } else {
      try {
        r.run();
      } catch (Exception e) {
        Log.e(LOG_TAG, e.toString());
      }
    }
  }

  /**
   * executes the {@link Callable#call()} method on callable from the UI thread
   * and returns the result.
   * If currently on the UI thread, this is a simple passthough.
   * Otherwise, the current thread will block until the callable is executed
   * and then return the result (or throw the exception, if an exception was thrown.)
   *
   * @param callable the Callable to call
   * @return the return value of the call
   * @throws NullPointerException if callable is null.
   * @throws Exception            if the call method throws an exception.
   */
  public static <T> T callOnUiThread(final Callable<T> callable) throws Exception {
    if (isOnUiThread()) {
      return callable.call();
    }
    final Object sync = new Object();
    final AtomicReference<Boolean> done = new AtomicReference<>(false);
    final AtomicReference<T> result = new AtomicReference<T>();
    final AtomicReference<Exception> exception = new AtomicReference<Exception>();
    synchronized (sync) {
      do {
        sHandler.post(new Runnable() {
          @Override
          public void run() {
            try {
              result.set(callable.call());
            } catch (Exception e) {
              exception.set(e);
            } finally {
              synchronized (sync) {
                done.set(true);
                sync.notifyAll();
              }
            }
          }
        });
        if (!done.get()) {
          sync.wait();
        }
      } while (!done.get());
    }
    Exception e = exception.get();
    if (e != null) {
      throw e;
    } else {
      return result.get();
    }
  }

  /**
   * posts a Runnable to be run on the UI thread later.
   * It will be not be run synchronously even if the current thread
   * is the UI thread.
   */
  public static void postOnUiThread(Runnable r) {
    sHandler.post(r);
  }

  /**
   * Posts a {@link Runnable} on the UI thread, after the given amount
   * of time delay, expressed in milliseconds.
   *
   * @param r     the Runnable to post
   * @param delay the delay of time, in milliseconds.
   */
  public static void postOnUiThread(Runnable r, long delay) {
    sHandler.postDelayed(r, delay);
  }

  /**
   * Posts a {@link Runnable} on the UI thread, after the given amount
   * of time delay.  Specifying the desired {@link TimeUnit}.
   *
   * @param r     the Runnable to post
   * @param delay the delay of time expressed in the given {@link TimeUnit}.
   * @param unit  the delay of time, in milliseconds
   */
  public static void postOnUiThread(Runnable r, long delay, TimeUnit unit) {
    sHandler.postDelayed(r, unit.toMillis(delay));
  }

  public static void postOffUiThread(final Runnable r, long delay) {
    sHandler.postDelayed(new Runnable() {
      @Override
      public void run() {
        sExecutor.execute(r);
      }
    }, delay);
  }

  /**
   * runs the specified Runnable on a thread that is not the UI thread.
   * If the current thread is not UI thread, it will be executed synchronously.
   * Otherwise, it will be posted to an Executor to be run at a later time,
   * as if {@link #runElsewhere(Runnable)} were called on it.
   */
  public static void runOffUiThread(Runnable r) {
    if (isOnUiThread()) {
      sExecutor.execute(r);
    } else {
      try {
        r.run();
      } catch (Exception e) {
        Log.e(LOG_TAG, e.toString());
      }
    }
  }

  /**
   * Executes the specified Runnable using the Executor used with {@link #runOffUiThread(Runnable)}.
   */
  public static void runElsewhere(Runnable r) {
    sExecutor.execute(r);
  }

  public static void runElsewhere(Runnable r, String name) {
    sExecutor.execute(r);
  }

  public static void runElsewhere(Runnable r, int priority) {
    sExecutor.execute(r);
  }

  public static void runElsewhere(Runnable r, String name, int priority) {
    sExecutor.execute(r);
  }

  /**
   * sets the Executor to use with {@link #runElsewhere(Runnable)}
   * and {@link #runOffUiThread(Runnable)}.
   *
   * @param executor the Executor to use
   * @throws NullPointerException if executor is null.
   */
  public static void setOffUiThreadExecutor(Executor executor) {
    if (executor == null) {
      throw new NullPointerException();
    }
    sExecutor = executor;
  }

  /**
   * return the Executor used with {@link #runElsewhere(Runnable)}
   * and {@link #runOffUiThread(Runnable)}.
   */
  public static Executor getOffUiThreadExecutor() {
    return sExecutor;
  }

  /**
   * Any operations posted to the UI thread after this call returns will
   * be blocked until the returned BlockingRunner is closed.
   *
   * @param waitForBlock waits until all pending UI tasks have completed,
   *                     before returning, guaranteeing that there will be no
   *                     activity of any kind on the UI thread until the returned BlockingRunnable
   *                     is closed.
   * @return a BlockingRunnable that, when closed, will allow the UI thread
   * to run again.
   * @throws IllegalStateException if currently running on the UI thread.
   */
  public static BlockingRunnable blockUiThread(boolean waitForBlock) {
    if (isOnUiThread()) {
      throw new IllegalStateException("already on UI thread");
    }
    BlockingRunnable blocker = new BlockingRunnable();
    runOnUiThread(blocker);
    if (waitForBlock) {
      blocker.waitForBlock();
    }
    return blocker;
  }

  public static void assertUiThread(boolean onUiThread) {
    if (onUiThread != isOnUiThread()) {
      throw new IllegalStateException("method must be called " + (onUiThread ? "on" : "off") + " the main thread!!!");
    }
  }
}