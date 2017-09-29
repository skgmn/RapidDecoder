package rapid.decoder.sample;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import rapiddecoder.BitmapLoader;

public class ScaledDecodingFragment extends Fragment {
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		return inflater.inflate(R.layout.fragment_scaled_decoding, container, false);
	}

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageView imageJpeg = (ImageView) view.findViewById(R.id.image_jpeg);
        ImageView imagePng = (ImageView) view.findViewById(R.id.image_png);
        TextView textJpegInfo = (TextView) view.findViewById(R.id.text_jpeg_info);
        TextView textPngInfo = (TextView) view.findViewById(R.id.text_png_info);

        int width = getResources().getDimensionPixelSize(R.dimen.scaled_decoding_width);

        Bitmap bitmap;
        int sourceWidth;
        int sourceHeight;

        // Jpeg

        BitmapLoader pumpkins = BitmapLoader.fromResource(getResources(), R.drawable.pumpkins);
        sourceWidth = pumpkins.getSourceWidth();
        sourceHeight = pumpkins.getSourceHeight();

        bitmap = pumpkins
                .scaleTo(width, Math.round(width * 0.6665f))
                .loadBitmap();
        imageJpeg.setImageBitmap(bitmap);

        textJpegInfo.setText("Source width = " + sourceWidth + ", Source height = " + sourceHeight + "\n" +
                "Bitmap width = " + bitmap.getWidth() + ", Bitmap height = " + bitmap.getHeight());

        // Png

        BitmapLoader a = BitmapLoader.fromResource(getResources(), R.drawable.a);
        sourceWidth = a.getSourceWidth();
        sourceHeight = a.getSourceHeight();

        bitmap = a
                .scaleTo(width, Math.round(width * 0.575f))
                .loadBitmap();
        imagePng.setImageBitmap(bitmap);

        //noinspection ConstantConditions
        textPngInfo.setText("Source width = " + sourceWidth + ", Source height = " + sourceHeight + "\n" +
                "Bitmap width = " + bitmap.getWidth() + ", Bitmap height = " + bitmap.getHeight());
    }
}
