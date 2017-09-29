package rapid.decoder.sample;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import rapiddecoder.BitmapLoader;
import rapiddecoder.LoadBitmapOptions;

public class MutableDecodingFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mutable_decoding, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        View view = getView();
        assert view != null;
        ImageView imageView = (ImageView) view.findViewById(R.id.image_view);

        int imageWidth = getResources().getDimensionPixelOffset(R.dimen.scaled_decoding_width);

        Bitmap bitmap = BitmapLoader.fromResource(getResources(), R.drawable.pumpkins)
                .scaleWidth(imageWidth)
                .loadBitmap(new LoadBitmapOptions.Builder()
                        .setMutable(true)
                        .build());
        Canvas cv = new Canvas(bitmap);

        int ribbonLeft = getResources().getDimensionPixelOffset(R.dimen.ribbon_x);
        int ribbonTop = getResources().getDimensionPixelOffset(R.dimen.ribbon_y);
        int ribbonWidth = getResources().getDimensionPixelSize(R.dimen.ribbon_width);

        Bitmap ribbon = BitmapLoader.fromResource(getResources(), R.drawable.ribbon)
                .scaleWidth(ribbonWidth)
                .loadBitmap(new LoadBitmapOptions.Builder()
                        .setFinalScale(false)
                        .build());
        cv.drawBitmap(ribbon, ribbonLeft, ribbonTop, null);

        imageView.setImageBitmap(bitmap);
    }
}
