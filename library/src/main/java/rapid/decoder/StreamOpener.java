package rapid.decoder;

import java.io.IOException;
import java.io.InputStream;

interface StreamOpener {
	InputStream openInputStream() throws IOException;
}
