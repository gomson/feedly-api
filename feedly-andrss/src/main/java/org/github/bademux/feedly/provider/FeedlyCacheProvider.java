/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Contributors:
 *                Bademus
 */

package org.github.bademux.feedly.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

import org.github.bademux.feedly.api.provider.FeedlyContract;
import org.github.bademux.feedly.api.util.db.FeedlyDbUtils;

import static android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE;
import static org.github.bademux.feedly.api.provider.FeedlyContract.Categories;
import static org.github.bademux.feedly.api.provider.FeedlyContract.Entries;
import static org.github.bademux.feedly.api.provider.FeedlyContract.EntriesTags;
import static org.github.bademux.feedly.api.provider.FeedlyContract.Feeds;
import static org.github.bademux.feedly.api.provider.FeedlyContract.FeedsByCategory;
import static org.github.bademux.feedly.api.provider.FeedlyContract.FeedsCategories;
import static org.github.bademux.feedly.api.util.db.FeedlyDbUtils.merge;

public class FeedlyCacheProvider extends ContentProvider {

  private static final UriMatcher URI_MATCHER = new UriMatcher(Code.AUTHORITY);

  static {
    URI_MATCHER.addURI(FeedlyContract.AUTHORITY, Entries.TBL_NAME + "/#", Code.ENTRY);
    URI_MATCHER.addURI(FeedlyContract.AUTHORITY, Entries.TBL_NAME, Code.ENTRIES);
    URI_MATCHER.addURI(FeedlyContract.AUTHORITY, Feeds.TBL_NAME + "/#", Code.FEED);
    URI_MATCHER.addURI(FeedlyContract.AUTHORITY, Feeds.TBL_NAME, Code.FEEDS);
    URI_MATCHER.addURI(FeedlyContract.AUTHORITY,
                       FeedsByCategory.TBL_NAME + "/*", Code.FEEDS_BY_CATEGORY);
    URI_MATCHER.addURI(FeedlyContract.AUTHORITY, Categories.TBL_NAME + "/#", Code.CATEGORY);
    URI_MATCHER.addURI(FeedlyContract.AUTHORITY, Categories.TBL_NAME, Code.CATEGORIES);
    URI_MATCHER.addURI(FeedlyContract.AUTHORITY, FeedsCategories.TBL_NAME, Code.FEEDS_CATEGORIES);
    URI_MATCHER.addURI(FeedlyContract.AUTHORITY, EntriesTags.TBL_NAME, Code.ENTRIES_TAGS);
  }

  /** {@inheritDoc} */
  @Override
  public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                      String sortOrder) {
    Log.i(TAG, "Querying database");

    final SQLiteDatabase db = mHelper.getReadableDatabase();
    assert db != null;
    switch (URI_MATCHER.match(uri)) {
      case Code.ENTRY:
        return db.query(Entries.TBL_NAME, projection, selection, selectionArgs, null, null, null);
      case Code.ENTRIES:
        return db.query(Entries.TBL_NAME, merge(projection, "rowid as _id"),
                        null, null, null, null, sortOrder);
      case Code.FEED:
        return db.query(Feeds.TBL_NAME, projection, selection, selectionArgs, null, null, null);
      case Code.FEEDS:
        return db.query(Feeds.TBL_NAME, merge(projection, "rowid as _id"),
                        null, null, null, null, sortOrder);
      case Code.FEEDS_BY_CATEGORY:
        return db.query(FeedsByCategory.TBL_NAME,
                        merge(projection, "rowid as _id", FeedsByCategory.CATEGORY_ID),
                        FeedsByCategory.CATEGORY_ID + "=?",
                        new String[]{uri.getLastPathSegment()}, null, null, null);
      case Code.CATEGORY:
        return db.query(Categories.TBL_NAME, projection, selection, selectionArgs,
                        null, null, null);
      case Code.CATEGORIES:
        return db.query(Categories.TBL_NAME, merge(projection, "rowid as _id"),
                        null, null, null, null, sortOrder);
      case Code.AUTHORITY: return null;
      default:
        throw new UnsupportedOperationException("Unsupported Uri " + uri);
    }
  }

  /** {@inheritDoc} */
  @Override
  public Uri insert(final Uri uri, final ContentValues values) {
    Log.i(TAG, "Inserting into database");
    int code = URI_MATCHER.match(uri);
    if (code == -1) {
      throw new UnsupportedOperationException("Unmatched Uri: " + uri);
    }
    long rowId = insert(mHelper.getWritableDatabase(), code, values);
    return Uri.withAppendedPath(uri, String.valueOf(rowId));
  }

  protected long insert(final SQLiteDatabase db, final int uriCode, final ContentValues values) {
    switch (uriCode) {
      case Code.ENTRIES:
        return db.replace(Entries.TBL_NAME, null, values);
      case Code.FEEDS:
        return db.replace(Feeds.TBL_NAME, null, values);
      case Code.CATEGORIES:
        return db.replace(Categories.TBL_NAME, null, values);
      case Code.FEEDS_CATEGORIES:
        return db.insertWithOnConflict(FeedsCategories.TBL_NAME, null, values, CONFLICT_IGNORE);
      case Code.ENTRIES_TAGS:
        return db.insertWithOnConflict(EntriesTags.TBL_NAME, null, values, CONFLICT_IGNORE);
      default:
        throw new UnsupportedOperationException("Unsupported Uri code: " + uriCode);
    }
  }

  /** {@inheritDoc} */
  @Override
  public int delete(final Uri uri, final String selection, final String[] selectionArgs) {
    Log.i(TAG, "Deleting from database");
    final SQLiteDatabase db = mHelper.getWritableDatabase();
    assert db != null;
    switch (URI_MATCHER.match(uri)) {
      case Code.ENTRIES:
        return db.delete(Entries.TBL_NAME, selection, selectionArgs);
      case Code.FEEDS:
        return db.delete(Feeds.TBL_NAME, selection, selectionArgs);
      case Code.CATEGORIES:
        return db.delete(Categories.TBL_NAME, selection, selectionArgs);
      case Code.FEEDS_CATEGORIES:
        return db.delete(FeedsCategories.TBL_NAME, selection, selectionArgs);
      default:
        throw new UnsupportedOperationException("Unsupported Uri " + uri);
    }
  }

  /** {@inheritDoc} */
  @Override
  public int update(final Uri uri, final ContentValues values,
                    final String selection, final String[] selectionArgs) {
    Log.i(TAG, "Updating data in database");
    final SQLiteDatabase db = mHelper.getWritableDatabase();
    assert db != null;
    switch (URI_MATCHER.match(uri)) {
      case Code.ENTRIES:
        return db.update(Entries.TBL_NAME, values, selection, selectionArgs);
      case Code.FEEDS:
        return db.update(Feeds.TBL_NAME, values, selection, selectionArgs);
      case Code.CATEGORIES:
        return db.update(Categories.TBL_NAME, values, selection, selectionArgs);
      default:
        throw new UnsupportedOperationException("Unsupported Uri " + uri);
    }
  }

  /** {@inheritDoc} */
  @Override
  public String getType(final Uri uri) { return null; }

  /** {@inheritDoc} */
  @Override
  public boolean onCreate() { mHelper = new DatabaseHelper(getContext()); return true; }

  private DatabaseHelper mHelper;

  private static final String TAG = "FeedlyCacheProvider";

  private static class DatabaseHelper extends SQLiteOpenHelper {

    /**
     * Enables sql relationship
     * {@inheritDoc}
     */
    @Override
    public void onConfigure(SQLiteDatabase db) { db.setForeignKeyConstraintsEnabled(true); }

    /** {@inheritDoc} */
    @Override
    public void onCreate(SQLiteDatabase db) {
      Log.i(TAG, "Creating database");
      FeedlyDbUtils.create(db);
    }

    /** {@inheritDoc} */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
      Log.i(TAG, "Upgrading database; wiping app data");
      FeedlyDbUtils.dropAll(db);
      onCreate(db);
    }

    public DatabaseHelper(Context context) { super(context, DB_NAME, null, VERSION); }

    private static final String DB_NAME = "feedly_cache.db";

    private static final int VERSION = 13;

    static final String TAG = "DatabaseHelper";
  }

  private interface Code {

    static final int AUTHORITY = 0;
    static final int FEEDS = 100, FEED = 101, FEEDS_BY_CATEGORY = 102;
    static final int CATEGORIES = 200, CATEGORY = 201;
    static final int FEEDS_CATEGORIES = 300, ENTRIES_TAGS = 301;
    static final int ENTRIES = 400, ENTRY = 401;
  }
}