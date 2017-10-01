package rapid.decoder.sample;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.res.ResourcesCompat;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import rapiddecoder.BitmapLoader;
import rapiddecoder.compat.DisplayCompat;

public class FrameFragment extends Fragment {
    private static final float SCALE = 0.5f;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_frame, container, false);
    }

    @SuppressWarnings("SuspiciousNameCombination")
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Display display = getActivity().getWindowManager().getDefaultDisplay();
        int width = DisplayCompat.getWidth(display);

        int imageWidth = width / 5;

        ImageView imageMatrix = (ImageView) view.findViewById(R.id.image_matrix);
        ImageView imageFitXY = (ImageView) view.findViewById(R.id.image_fit_xy);
        ImageView imageFitStart = (ImageView) view.findViewById(R.id.image_fit_start);
        ImageView imageFitCenter = (ImageView) view.findViewById(R.id.image_fit_center);
        ImageView imageFitEnd = (ImageView) view.findViewById(R.id.image_fit_end);
        ImageView imageCenter = (ImageView) view.findViewById(R.id.image_center);
        ImageView imageCenterCrop = (ImageView) view.findViewById(R.id.image_center_crop);
        ImageView imageCenterInside = (ImageView) view.findViewById(R.id.image_center_inside);

        Drawable transparentBackground = ResourcesCompat.getDrawable(getResources(),
                R.drawable.transparent_background, null);

        BitmapLoader pumpkins = BitmapLoader.fromResource(getResources(), R.drawable.pumpkins);
        Bitmap bitmap;

        bitmap = pumpkins
                .scaleBy(SCALE)
                .frame(ImageView.ScaleType.MATRIX, imageWidth, imageWidth, transparentBackground)
                .loadBitmap();
        imageMatrix.setImageBitmap(bitmap);

        bitmap = pumpkins.frame(ImageView.ScaleType.FIT_XY, imageWidth, imageWidth, transparentBackground)
                .loadBitmap();
        imageFitXY.setImageBitmap(bitmap);

        bitmap = pumpkins
                .frame(ImageView.ScaleType.FIT_START, imageWidth, imageWidth, transparentBackground)
                .loadBitmap();
        imageFitStart.setImageBitmap(bitmap);

        bitmap = pumpkins
                .frame(ImageView.ScaleType.FIT_CENTER, imageWidth, imageWidth, transparentBackground)
                .loadBitmap();
        imageFitCenter.setImageBitmap(bitmap);

        bitmap = pumpkins
                .frame(ImageView.ScaleType.FIT_END, imageWidth, imageWidth, transparentBackground)
                .loadBitmap();
        imageFitEnd.setImageBitmap(bitmap);

        bitmap = pumpkins
                .scaleBy(SCALE)
                .frame(ImageView.ScaleType.CENTER, imageWidth, imageWidth, transparentBackground)
                .loadBitmap();
        imageCenter.setImageBitmap(bitmap);

        bitmap = pumpkins
                .frame(ImageView.ScaleType.CENTER_CROP, imageWidth, imageWidth, transparentBackground)
                .loadBitmap();
        imageCenterCrop.setImageBitmap(bitmap);

        bitmap = pumpkins
                .frame(ImageView.ScaleType.CENTER_INSIDE, imageWidth, imageWidth, transparentBackground)
                .loadBitmap();
        imageCenterInside.setImageBitmap(bitmap);
    }
}
