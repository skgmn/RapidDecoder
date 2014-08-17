package rapid.decoder.sample;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import rapid.decoder.BitmapDecoder;

public class WrapContentFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_wrap_content, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageView pic1 = (ImageView) view.findViewById(R.id.pic1);
        View pic2 = view.findViewById(R.id.pic2);
        ImageView pic3 = (ImageView) view.findViewById(R.id.pic3);
        View pic4 = view.findViewById(R.id.pic4);

        Resources res = getResources();

        BitmapDecoder.from(res, R.drawable.horizontal_reflex_848).into(pic1);
        BitmapDecoder.from(res, R.drawable.vertical_panorama02).into(pic2);
        BitmapDecoder.from(res, R.drawable.horizontal_reflex_848).into(pic3);
        BitmapDecoder.from(res, R.drawable.vertical_panorama02).into(pic4);
    }
}
