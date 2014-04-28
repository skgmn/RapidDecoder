package agu.sample.integrated;

import agu.bitmap.BitmapDecoder;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

public class RegionalDecodingFragment extends Fragment {
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		return inflater.inflate(R.layout.fragment_scaled_decoding, container, false);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		ImageView imageJpeg = (ImageView) getView().findViewById(R.id.image_jpeg);
		ImageView imagePng = (ImageView) getView().findViewById(R.id.image_png);
		
		// Jpeg
		
		Bitmap bitmap = BitmapDecoder.from(getResources(), R.drawable.amanda)
				.region(140, 22, 1010, 1111)
				.scaleBy(0.5f)
				.useBuiltInDecoder(MainActivity.TEST_BUILT_IN_DECODER)
				.decode();
		imageJpeg.setImageBitmap(bitmap);
		
		// Png
		
		bitmap = BitmapDecoder.from(getResources(), R.drawable.amanda2)
				.region(204, 0, 900, 773)
				.scaleBy(0.5f)
				.useBuiltInDecoder(MainActivity.TEST_BUILT_IN_DECODER)
				.decode();
		imagePng.setImageBitmap(bitmap);
	}
}
