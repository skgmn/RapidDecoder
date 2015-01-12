package rapid.decoder.sample;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import rapid.decoder.BitmapDecoder;
import rapid.decoder.BitmapLoader;

public class ResetFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_reset, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageView imageView1 = (ImageView) view.findViewById(R.id.image1);
        ImageView imageView2 = (ImageView) view.findViewById(R.id.image2);

        BitmapLoader loader = BitmapDecoder.from(getActivity(), "file:///android_asset/squirrel.jpg");
        Bitmap bitmap = loader.region(140, 22, 1010, 1111)
                .scale(300, 400)
                .useBuiltInDecoder(MainActivity.TEST_BUILT_IN_DECODER)
                .decode();
        imageView1.setImageBitmap(bitmap);

        bitmap = loader.reset()
                .scale(500, 500)
                .useBuiltInDecoder(MainActivity.TEST_BUILT_IN_DECODER)
                .decode();
        imageView2.setImageBitmap(bitmap);
    }
}
