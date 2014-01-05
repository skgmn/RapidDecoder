package agu.sample.integrated;

import agu.bitmap.BitmapDecoder;
import agu.compat.DisplayCompat;
import agu.scaling.BitmapFrameBuilder;
import agu.scaling.ScaleAlignment;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

public class FrameFragment extends Fragment {
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		return inflater.inflate(R.layout.fragment_frame, container, false);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		Display display = getActivity().getWindowManager().getDefaultDisplay();
		int width = DisplayCompat.getWidth(display);
		
		int imageWidth = width / 5;
		
		ImageView imageFitInLeft = (ImageView) getView().findViewById(R.id.image_fit_in_left);
		ImageView imageFitInCenter = (ImageView) getView().findViewById(R.id.image_fit_in_center);
		ImageView imageFitInRight = (ImageView) getView().findViewById(R.id.image_fit_in_right);
		ImageView imageCutOutTop = (ImageView) getView().findViewById(R.id.image_cut_out_top);
		ImageView imageCutOutCenter = (ImageView) getView().findViewById(R.id.image_cut_out_center);
		ImageView imageCutOutBottom = (ImageView) getView().findViewById(R.id.image_cut_out_bottom);
		
		Drawable transparentBackground = getResources().getDrawable(R.drawable.transparent_background);

		BitmapDecoder amanda = BitmapDecoder.from(getResources(), R.drawable.amanda);
		Bitmap bitmap;
		
		bitmap = new BitmapFrameBuilder(amanda, imageWidth, imageWidth)
				.background(transparentBackground)
				.scaleAlignment(ScaleAlignment.LEFT_OR_TOP)
				.fitIn();
		imageFitInLeft.setImageBitmap(bitmap);
		
		bitmap = new BitmapFrameBuilder(amanda, imageWidth, imageWidth)
				.background(transparentBackground)
				.scaleAlignment(ScaleAlignment.CENTER)
				.fitIn();
		imageFitInCenter.setImageBitmap(bitmap);

		bitmap = new BitmapFrameBuilder(amanda, imageWidth, imageWidth)
				.background(transparentBackground)
				.scaleAlignment(ScaleAlignment.RIGHT_OR_BOTTOM)
				.fitIn();
		imageFitInRight.setImageBitmap(bitmap);
		
		bitmap = new BitmapFrameBuilder(amanda, imageWidth, imageWidth)
				.scaleAlignment(ScaleAlignment.LEFT_OR_TOP).cutOut();
		imageCutOutTop.setImageBitmap(bitmap);
		
		bitmap = new BitmapFrameBuilder(amanda, imageWidth, imageWidth)
				.scaleAlignment(ScaleAlignment.CENTER).cutOut();
		imageCutOutCenter.setImageBitmap(bitmap);
		
		bitmap = new BitmapFrameBuilder(amanda, imageWidth, imageWidth)
				.scaleAlignment(ScaleAlignment.RIGHT_OR_BOTTOM).cutOut();
		imageCutOutBottom.setImageBitmap(bitmap);
	}
}
