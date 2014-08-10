package rapid.decoder;

import android.net.Uri;

import java.util.Arrays;

class QueriedContentId {
    public Uri uri;
    public String columnName;
    public String selection;
    public String[] selectionArgs;
    public String sortOrder;

    private int mHashCode;

    QueriedContentId(Uri uri, String columnName, String selection, String[] selectionArgs,
                     String sortOrder) {
        this.uri = uri;
        this.columnName = columnName;
        this.selection = selection;
        this.selectionArgs = selectionArgs;
        this.sortOrder = sortOrder;
    }

    @Override
    public int hashCode() {
        if (mHashCode == 0) {
            mHashCode = uri.hashCode() ^
                    columnName.hashCode() ^
                    (selection != null ? selection.hashCode() : 0) ^
                    (selectionArgs != null ? Arrays.hashCode(selectionArgs) : 0) ^
                    (sortOrder != null ? sortOrder.hashCode() : 0);
        }
        return mHashCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QueriedContentId)) return false;

        QueriedContentId id = (QueriedContentId) o;
        return uri.equals(id.uri) &&
                columnName.equals(id.columnName) &&
                (selection != null ? selection.equals(id.selection) : id.selection == null) &&
                (selectionArgs != null ? Arrays.equals(selectionArgs, id.selectionArgs) : id
                        .selectionArgs == null) &&
                (sortOrder != null ? sortOrder.equals(id.sortOrder) : id.sortOrder == null);
    }
}
