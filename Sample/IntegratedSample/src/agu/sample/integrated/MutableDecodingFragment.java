package agu.sample.integrated;

import agu.bitmap.BitmapDecoder;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

public class MutableDecodingFragment extends Fragment {
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		return inflater.inflate(R.layout.fragment_mutable_decoding, container, false);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		ImageView imageView = (ImageView) getView().findViewById(R.id.image_view);
		
		int imageWidth = getResources().getDimensionPixelOffset(R.dimen.scaled_decoding_width);
		int imageHeight = getResources().getDimensionPixelOffset(R.dimen.scaled_decoding_height);
		
		Bitmap bitmap = BitmapDecoder.from(getResources(), R.drawable.amanda)
				.scale(imageWidth, imageHeight).mutable().decode();
		Canvas cv = new Canvas(bitmap);
		
		int ribbonLeft = getResources().getDimensionPixelOffset(R.dimen.ribbon_x);
		int ribbonTop = getResources().getDimensionPixelOffset(R.dimen.ribbon_y);
		int ribbonWidth = getResources().getDimensionPixelSize(R.dimen.ribbon_width);
		int ribbonHeight = getResources().getDimensionPixelSize(R.dimen.ribbon_width);
		
		BitmapDecoder.from(getResources(), R.drawable.ribbon)
			.scale(ribbonWidth, ribbonHeight)
			.draw(cv, ribbonLeft, ribbonTop);
		
		imageView.setImageBitmap(bitmap);
	}
}
