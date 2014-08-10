package rapid.decoder.sample;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import rapid.decoder.BitmapDecoder;
import rapid.decoder.binder.ImageViewBinder;

public class GalleryFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private GalleryAdapter mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_gallery, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        GridView grid = (GridView) view.findViewById(R.id.grid);
        mAdapter = new GalleryAdapter(view.getContext());
        grid.setAdapter(mAdapter);
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(getActivity(),
                MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI,
                new String[]{
                        MediaStore.Images.Thumbnails._ID,
                        MediaStore.Images.Thumbnails.DATA,
                        MediaStore.Images.Thumbnails.IMAGE_ID
                },
                null, null, null
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mAdapter.changeCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mAdapter.changeCursor(null);
    }

    private static class GalleryAdapter extends CursorAdapter {
        public GalleryAdapter(Context context) {
            super(context, null, false);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return LayoutInflater.from(context).inflate(R.layout.item_gallery, parent, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ImageView imageView = (ImageView) view;
            imageView.setImageDrawable(new ColorDrawable(0xffcccccc));
            String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Thumbnails
                    .DATA));
            BitmapDecoder.from(path).into(ImageViewBinder.obtain(imageView).scaleType(ImageView
                    .ScaleType.CENTER_CROP));
        }
    }
}
