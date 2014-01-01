package agu.sample.integrated;

import agu.bitmap.BitmapDecoder;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class ScaledDecodingFragment extends Fragment {
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
		TextView textJpegInfo = (TextView) getView().findViewById(R.id.text_jpeg_info);
		TextView textPngInfo = (TextView) getView().findViewById(R.id.text_png_info);
		
		int width = getResources().getDimensionPixelSize(R.dimen.scaled_decoding_width);
		int height = getResources().getDimensionPixelSize(R.dimen.scaled_decoding_height);
		
		BitmapDecoder decoder;
		Bitmap bitmap;
		int sourceWidth;
		int sourceHeight;
		
		// Jpeg
		
		decoder = BitmapDecoder.from(getResources(), R.drawable.amanda);
//		decoder = BitmapDecoder.from(getResources().openRawResource(R.drawable.amanda));
		sourceWidth = decoder.width();
		sourceHeight = decoder.height();
		
		bitmap = decoder
				.forceUseOwnDecoder()
				.scale(width, height).decode();
		imageJpeg.setImageBitmap(bitmap);
		
		textJpegInfo.setText("Source width = " + sourceWidth + ", Source height = " + sourceHeight + "\n" +
				"Bitmap width = " + bitmap.getWidth() + ", Bitmap height = " + bitmap.getHeight());

		// Png
		
//		decoder = BitmapDecoder.from(getResources(), R.drawable.amanda2);
//		sourceWidth = decoder.width();
//		sourceHeight = decoder.height();
//		
//		bitmap = decoder
//				.forceUseOwnDecoder()
//				.scale(width, height).decode();
//		imagePng.setImageBitmap(bitmap);
//		
//		textPngInfo.setText("Source width = " + sourceWidth + ", Source height = " + sourceHeight + "\n" +
//				"Bitmap width = " + bitmap.getWidth() + ", Bitmap height = " + bitmap.getHeight());
	}
}
