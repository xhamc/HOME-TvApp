package com.sony.sel.tvapp.adapter;

/**
 * Interface for a class (e.g. an Android View used as a list cell) that can be bound to data.
 */
public interface Bindable<T> {
  void bind(T data);

  T getData();
}
