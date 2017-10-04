package rapid.decoder.sample;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import rapiddecoder.BitmapLoader;

public class RegionDecodingFragment extends Fragment {
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

        // Jpeg

        Bitmap bitmap = BitmapLoader.fromResource(getResources(), R.drawable.pumpkins)
                .region(140, 22, 1010, 1111)
                .scaleBy(0.5f)
                .loadBitmap();
        imageJpeg.setImageBitmap(bitmap);

        // Png

        bitmap = BitmapLoader.fromResource(getResources(), R.drawable.a)
                .region(204, 0, 900, 773)
                .scaleBy(0.5f)
                .loadBitmap();
        imagePng.setImageBitmap(bitmap);
    }
}
