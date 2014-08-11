package rapid.decoder.sample;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import rapid.decoder.BitmapDecoder;
import rapid.decoder.BitmapPostProcessor;
import rapid.decoder.binder.TextViewBinder;
import rapid.decoder.binder.ViewBinder;

import static rapid.decoder.cache.ResourcePool.*;

public class ContactsFragment extends ListFragment implements LoaderManager
        .LoaderCallbacks<Cursor> {
    private CursorAdapter mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        return inflater.inflate(R.layout.fragment_contacts, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mAdapter = new ContactsAdapter(getActivity());
        getListView().setAdapter(mAdapter);
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        String[] projection;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            projection = new String[]{ContactsContract.Contacts._ID, ContactsContract.Contacts
                    .DISPLAY_NAME, ContactsContract.Contacts.PHOTO_THUMBNAIL_URI};
        } else {
            projection = new String[]{ContactsContract.Contacts._ID, ContactsContract.Contacts
                    .DISPLAY_NAME};
        }
        return new CursorLoader(getActivity(), ContactsContract.Contacts.CONTENT_URI,
                projection, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mAdapter.changeCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        if (mAdapter != null) {
            mAdapter.changeCursor(null);
        }
    }

    private static class ContactsAdapter extends CursorAdapter {
        public ContactsAdapter(Context context) {
            super(context, null, false);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return LayoutInflater.from(context).inflate(R.layout.item_contacts, parent, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            TextView textView = (TextView) view;
            int imageSize = context.getResources().getDimensionPixelSize(R.dimen
                    .contacts_profile_image_size);
            textView.setText(cursor.getString(1));

            long id = cursor.getLong(0);
            Uri uri = ContactsContract.Contacts.CONTENT_URI.buildUpon()
                    .appendPath(Long.toString(id))
                    .appendPath(ContactsContract.Contacts.Photo.CONTENT_DIRECTORY)
                    .build();
            ViewBinder<TextView> binder =
                    TextViewBinder.obtain(textView, Gravity.LEFT, imageSize, imageSize)
                            .scaleType(ImageView.ScaleType.CENTER_CROP)
                            .placeholder(R.drawable.contacts_profile_image_placeholder);
            BitmapDecoder.from(context.getContentResolver(), uri,
                    ContactsContract.CommonDataKinds.Photo.PHOTO, null, null, null)
                    .id(uri)
                    .postProcessor(new BitmapPostProcessor(context) {
                        @Override
                        public Bitmap process(Bitmap bitmap) {
                            return createRoundedImage(getContext(), bitmap);
                        }
                    })
                    .into(binder);
        }

        private static Bitmap createRoundedImage(Context context, Bitmap bitmap) {
            Drawable placeholder = context.getResources().getDrawable(R.drawable
                    .contacts_profile_image_placeholder);
            Bitmap bitmap2 = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap
                    .Config.ARGB_8888);
            Canvas canvas = CANVAS.obtain(bitmap2);
            placeholder.setBounds(0, 0, bitmap2.getWidth(), bitmap2.getHeight());
            placeholder.draw(canvas);
            Paint paint = PAINT.obtain();
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            canvas.drawBitmap(bitmap, 0, 0, paint);
            PAINT.recycle(paint);
            CANVAS.recycle(canvas);

            return bitmap2;
        }
    }
}