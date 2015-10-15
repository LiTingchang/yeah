package com.yeah.android.activity.camera.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.yeah.android.utils.DistanceUtil;
import com.yeah.android.utils.ImageLoaderUtils;
import com.yeah.android.R;
import com.yeah.android.model.PhotoItem;

import java.util.List;

/**
 * @author tongqian.ni
 *
 */
public class GalleryAdapter extends BaseAdapter {

    private Context             mContext;
    private List<PhotoItem>     values;
    public static GalleryHolder holder;

    /**
     * @param
     * @param values
     */
    public GalleryAdapter(Context context, List<PhotoItem> values) {
        this.mContext = context;
        this.values = values;
    }

    @Override
    public int getCount() {
        return values.size();
    }

    @Override
    public Object getItem(int position) {
        return values.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final GalleryHolder holder;
        int width = DistanceUtil.getCameraAlbumWidth();
        if (convertView == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(mContext);
            convertView = layoutInflater.inflate(R.layout.item_gallery, null);
            holder = new GalleryHolder();
            holder.sample = (ImageView) convertView.findViewById(R.id.gallery_sample_image);
            holder.sample.setLayoutParams(new AbsListView.LayoutParams(width, width));
            convertView.setTag(holder);
        } else {
            holder = (GalleryHolder) convertView.getTag();
        }
        final PhotoItem gallery = (PhotoItem) getItem(position);

        ImageLoaderUtils.displayLocalImage(gallery.getImageUri(), holder.sample,null);
        
        return convertView;
    }

    class GalleryHolder {
        ImageView sample;
    }

}
