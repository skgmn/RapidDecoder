package rapid.decoder;

import android.support.annotation.Nullable;

import java.io.InputStream;

interface StreamOpener {
    @Nullable
	InputStream openInputStream();
}
