package rapid.decoder.sample;

import android.support.v4.app.Fragment;

public class FrameFragment extends Fragment {
//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container,
//                             Bundle savedInstanceState) {
//
//        return inflater.inflate(R.layout.fragment_frame, container, false);
//    }
//
//    @SuppressWarnings("SuspiciousNameCombination")
//    @Override
//    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
//        super.onViewCreated(view, savedInstanceState);
//
//        Display display = getActivity().getWindowManager().getDefaultDisplay();
//        int width = DisplayCompat.getWidth(display);
//
//        int imageWidth = width / 5;
//
//        ImageView imageMatrix = (ImageView) view.findViewById(R.id.image_matrix);
//        ImageView imageFitXY = (ImageView) view.findViewById(R.id.image_fit_xy);
//        ImageView imageFitStart = (ImageView) view.findViewById(R.id.image_fit_start);
//        ImageView imageFitCenter = (ImageView) view.findViewById(R.id.image_fit_center);
//        ImageView imageFitEnd = (ImageView) view.findViewById(R.id.image_fit_end);
//        ImageView imageCenter = (ImageView) view.findViewById(R.id.image_center);
//        ImageView imageCenterCrop = (ImageView) view.findViewById(R.id.image_center_crop);
//        ImageView imageCenterInside = (ImageView) view.findViewById(R.id.image_center_inside);
//
//        Drawable transparentBackground = getResources().getDrawable(R.drawable
//                .transparent_background);
//
//        BitmapDecoder amanda = BitmapDecoder.from(getResources(), R.drawable.amanda).scaleBy(0.5f);
//        Bitmap bitmap;
//
//        bitmap = FramedDecoder.newInstance(amanda, imageWidth, imageWidth,
//                ImageView.ScaleType.MATRIX)
//                .background(transparentBackground)
//                .decode();
//        imageMatrix.setImageBitmap(bitmap);
//
//        bitmap = FramedDecoder.newInstance(amanda, imageWidth, imageWidth,
//                ImageView.ScaleType.FIT_XY)
//                .background(transparentBackground)
//                .decode();
//        imageFitXY.setImageBitmap(bitmap);
//
//        bitmap = FramedDecoder.newInstance(amanda, imageWidth, imageWidth,
//                ImageView.ScaleType.FIT_START)
//                .background(transparentBackground)
//                .decode();
//        imageFitStart.setImageBitmap(bitmap);
//
//        bitmap = FramedDecoder.newInstance(amanda, imageWidth, imageWidth,
//                ImageView.ScaleType.FIT_CENTER)
//                .background(transparentBackground)
//                .decode();
//        imageFitCenter.setImageBitmap(bitmap);
//
//        bitmap = FramedDecoder.newInstance(amanda, imageWidth, imageWidth,
//                ImageView.ScaleType.FIT_END)
//                .background(transparentBackground)
//                .decode();
//        imageFitEnd.setImageBitmap(bitmap);
//
//        bitmap = FramedDecoder.newInstance(amanda, imageWidth, imageWidth,
//                ImageView.ScaleType.CENTER)
//                .background(transparentBackground)
//                .decode();
//        imageCenter.setImageBitmap(bitmap);
//
//        bitmap = FramedDecoder.newInstance(amanda, imageWidth, imageWidth,
//                ImageView.ScaleType.CENTER_CROP)
//                .background(transparentBackground)
//                .decode();
//        imageCenterCrop.setImageBitmap(bitmap);
//
//        bitmap = FramedDecoder.newInstance(amanda, imageWidth, imageWidth,
//                ImageView.ScaleType.CENTER_INSIDE)
//                .background(transparentBackground)
//                .decode();
//        imageCenterInside.setImageBitmap(bitmap);
//    }
}
