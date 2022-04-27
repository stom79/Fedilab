package com.smarteist.autoimageslider;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;

import java.util.LinkedList;
import java.util.Queue;


public abstract class SliderViewAdapter<VH extends SliderViewAdapter.ViewHolder> extends PagerAdapter {

    private final Queue<VH> destroyedItems = new LinkedList<>();
    private DataSetListener dataSetListener;

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        VH viewHolder = destroyedItems.poll();
        if (viewHolder == null) {
            viewHolder = onCreateViewHolder(container);
        }
        // Re-add existing view before rendering so that we can make change inside getView()
        container.addView(viewHolder.itemView);
        onBindViewHolder(viewHolder, position);

        return viewHolder;
    }

    @Override
    public final void destroyItem(ViewGroup container, int position, @NonNull Object object) {
        container.removeView(((VH) object).itemView);
        destroyedItems.add((VH) object);
    }

    @Override
    public final boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return ((VH) object).itemView == view;
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        if (this.dataSetListener != null) {
            dataSetListener.dataSetChanged();
        }
    }

    /**
     * Create a new view holder
     *
     * @param parent wrapper view
     * @return view holder
     */
    public abstract VH onCreateViewHolder(ViewGroup parent);

    /**
     * Bind data at position into viewHolder
     *
     * @param viewHolder item view holder
     * @param position   item position
     */
    public abstract void onBindViewHolder(VH viewHolder, int position);

    void dataSetChangedListener(SliderViewAdapter.DataSetListener dataSetListener) {
        this.dataSetListener = dataSetListener;
    }

    interface DataSetListener {
        void dataSetChanged();
    }

    //Default View holder class
    public static abstract class ViewHolder {
        public final View itemView;

        public ViewHolder(View itemView) {
            this.itemView = itemView;
        }
    }

}
