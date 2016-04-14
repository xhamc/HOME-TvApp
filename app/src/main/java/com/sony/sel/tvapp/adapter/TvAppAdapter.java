package com.sony.sel.tvapp.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.sony.sel.tvapp.BuildConfig;
import com.sony.sel.tvapp.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for list adapters in TV care app.
 * All lists include a title banner across the top cell.
 * List data can be in a normal, loading, empty or error state. This class provides UI for all states.
 *
 * @param <T> Data that the adapter presents.
 * @param <V> View class used for list cells. Must be derived from an Android View and implement the {@link Bindable} interface.
 */
public abstract class TvAppAdapter<T, V extends View & Bindable<T>> extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

  private List<T> data;
  private final String loadingMessage;
  private final String emptyMessage;
  private final Context appContext;
  private final int cellViewType;
  private final int cellLayoutResId;
  private Throwable error;
  private View.OnClickListener onItemClickListener;

  /**
   * Create an adapter to display specific data items.
   *
   * @param context         Android context.
   * @param cellViewType    Identifier for data cells in this view. Typically declared in an <id> resource.
   * @param cellLayoutResId Layout ID for the list cells.
   * @param emptyMessage    String to display while loading the first items.
   * @param emptyMessage    String to display when there is no data to show.
   */
  public TvAppAdapter(Context context, int cellViewType, int cellLayoutResId, String loadingMessage, String emptyMessage) {
    this.loadingMessage = loadingMessage;
    this.emptyMessage = emptyMessage;
    this.appContext = context.getApplicationContext();
    this.cellViewType = cellViewType;
    this.cellLayoutResId = cellLayoutResId;
  }

  /**
   * Put the adapter into a loading state until the next call to {@link #setData(List)} or {@link #onError(Throwable)}.
   */
  public void setLoading() {
    this.data = null;
    this.error = null;
    notifyDataSetChanged();
  }

  /**
   * Set the list data. Use an empty List to denote loading is completed, but no data was found.
   *
   * @param data List of data. Should not be null, or adapter will continue to show a loading state.
   */
  public void setData(List<T> data) {
    this.data = data;
    this.error = null;
    notifyDataSetChanged();
  }

  /**
   * Return the list data.
   */
  public List<T> getData() {
    return data;
  }

  /**
   * Is the list in a loading state?
   */
  public boolean isLoading() {
    return data == null;
  }

  /**
   * Was there an error loading data?
   *
   * @see #onError(Throwable)
   */
  public boolean isError() {
    return error != null;
  }

  /**
   * Is the data set empty (but finished loading)?
   *
   * @see #setData(List)
   */
  public boolean isEmpty() {
    return data != null ? data.size() == 0 : true;
  }

  /**
   * Notify the user an error occurred loading the data.
   *
   * @param error The error that occurred.
   */
  public void onError(Throwable error) {
    this.error = error;
    notifyDataSetChanged();
  }

  @Override
  public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    switch (viewType) {
      case R.id.loading_cell:
        return new RecyclerView.ViewHolder(LayoutInflater.from(appContext).inflate(R.layout.loading_cell, parent, false)) {
        };
      case R.id.empty_cell:
        return new RecyclerView.ViewHolder(LayoutInflater.from(appContext).inflate(R.layout.empty_cell, parent, false)) {
        };
      case R.id.error_cell:
        return new RecyclerView.ViewHolder(LayoutInflater.from(appContext).inflate(R.layout.error_cell, parent, false)) {
        };
      default:
        View v = LayoutInflater.from(appContext).inflate(cellLayoutResId, parent, false);
        v.setOnClickListener(onItemClickListener);
        return new RecyclerView.ViewHolder(v) {
        };
    }
  }

  @Override
  public int getItemCount() {
    // always at least one cell (may be loading or empty cell)
    return data != null ? data.size() : 1;
  }

  @Override
  public int getItemViewType(int position) {

    if (isError()) {
      return R.id.error_cell;
    } else if (isLoading()) {
      return R.id.loading_cell;
    } else if (isEmpty()) {
      return R.id.empty_cell;
    } else {
      return cellViewType;
    }
  }

  @Override
  public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
    switch (holder.getItemViewType()) {
      case R.id.loading_cell: {
        if (loadingMessage != null) {
          LoadingCell cell = (LoadingCell) holder.itemView;
          cell.bind(loadingMessage);
        }
        break;
      }
      case R.id.error_cell: {
        // error cell
        ErrorCell cell = (ErrorCell) holder.itemView;
        if (BuildConfig.DEBUG) {
          // debug builds show error details
          cell.bind(appContext.getString(R.string.error) + "\n\n" + error);
        } else {
          // release builds show generic error
          cell.bind(appContext.getString(R.string.genericErrorMessage));
        }
        break;
      }
      case R.id.empty_cell:
        // no items, empty cell
        EmptyCell cell = (EmptyCell) holder.itemView;
        cell.bind(emptyMessage);
        break;
      default:
        ((V) holder.itemView).bind(data.get(position));
        if (position == 0 && data.size() == 1) {
          // focus first item
          holder.itemView.requestFocus();
        }
        break;
    }
  }

  public boolean remove(T item) {
    int position = data.indexOf(item);

    if (position < 0) {
      return false;
    }

    data.remove(position);
    notifyItemRemoved(position + 1);
    return true;
  }

  public boolean contains(T item) {
    return data != null ? data.contains(item) : false;
  }

  public int indexOf(T item) {
    return data.indexOf(item);
  }

  public void add(T item) {
    if (data == null) {
      // first item
      data = new ArrayList<>();
      data.add(item);
      notifyDataSetChanged();
    } else {
      // add to existing list
      data.add(item);
      notifyItemInserted(data.size() - 1);
    }
  }

}
