package rapid.decoder;

import android.net.Uri;

import java.util.Arrays;

class QueriedDatabaseId {
    public String tableName;
    public String columnName;
    public String selection;
    public String[] selectionArgs;
    public String groupBy;
    public String having;
    public String orderBy;

    private int mHashCode;

    QueriedDatabaseId(String tableName, String columnName, String selection,
                      String[] selectionArgs, String groupBy, String having, String orderBy) {
        this.tableName = tableName;
        this.columnName = columnName;
        this.selection = selection;
        this.selectionArgs = selectionArgs;
        this.groupBy = groupBy;
        this.having = having;
        this.orderBy = orderBy;
    }

    @Override
    public int hashCode() {
        if (mHashCode == 0) {
            mHashCode = tableName.hashCode() ^
                    columnName.hashCode() ^
                    (selection != null ? selection.hashCode() : 0) ^
                    (selectionArgs != null ? Arrays.hashCode(selectionArgs) : 0) ^
                    (groupBy != null ? groupBy.hashCode() : 0) ^
                    (having != null ? having.hashCode() : 0) ^
                    (orderBy != null ? orderBy.hashCode() : 0);
        }
        return mHashCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QueriedDatabaseId)) return false;

        QueriedDatabaseId id = (QueriedDatabaseId) o;
        return tableName.equals(id.tableName) &&
                columnName.equals(id.columnName) &&
                (selection != null ? selection.equals(id.selection) : id.selection == null) &&
                (selectionArgs != null ? Arrays.equals(selectionArgs, id.selectionArgs) : id
                        .selectionArgs == null) &&
                (groupBy != null ? groupBy.equals(id.groupBy) : id.groupBy == null) &&
                (having != null ? having.equals(id.having) : id.having == null) &&
                (orderBy != null ? orderBy.equals(id.orderBy) : id.orderBy == null);
    }
}
