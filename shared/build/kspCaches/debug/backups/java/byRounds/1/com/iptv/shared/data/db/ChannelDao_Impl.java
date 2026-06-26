package com.iptv.shared.data.db;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomDatabaseKt;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class ChannelDao_Impl implements ChannelDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ChannelEntity> __insertionAdapterOfChannelEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public ChannelDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfChannelEntity = new EntityInsertionAdapter<ChannelEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `channels` (`url`,`name`,`tvgId`,`tvgName`,`logoUrl`,`groupTitle`) VALUES (?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ChannelEntity entity) {
        statement.bindString(1, entity.getUrl());
        statement.bindString(2, entity.getName());
        if (entity.getTvgId() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getTvgId());
        }
        if (entity.getTvgName() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getTvgName());
        }
        if (entity.getLogoUrl() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getLogoUrl());
        }
        if (entity.getGroupTitle() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getGroupTitle());
        }
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM channels";
        return _query;
      }
    };
  }

  @Override
  public Object insertAll(final List<ChannelEntity> channels,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfChannelEntity.insert(channels);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object replacePlaylist(final List<ChannelEntity> channels,
      final Continuation<? super Unit> $completion) {
    return RoomDatabaseKt.withTransaction(__db, (__cont) -> ChannelDao.DefaultImpls.replacePlaylist(ChannelDao_Impl.this, channels, __cont), $completion);
  }

  @Override
  public Object deleteAll(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAll.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteAll.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<ChannelEntity>> getAllChannelsFlow() {
    final String _sql = "SELECT * FROM channels";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"channels"}, new Callable<List<ChannelEntity>>() {
      @Override
      @NonNull
      public List<ChannelEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "url");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfTvgId = CursorUtil.getColumnIndexOrThrow(_cursor, "tvgId");
          final int _cursorIndexOfTvgName = CursorUtil.getColumnIndexOrThrow(_cursor, "tvgName");
          final int _cursorIndexOfLogoUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "logoUrl");
          final int _cursorIndexOfGroupTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "groupTitle");
          final List<ChannelEntity> _result = new ArrayList<ChannelEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ChannelEntity _item;
            final String _tmpUrl;
            _tmpUrl = _cursor.getString(_cursorIndexOfUrl);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpTvgId;
            if (_cursor.isNull(_cursorIndexOfTvgId)) {
              _tmpTvgId = null;
            } else {
              _tmpTvgId = _cursor.getString(_cursorIndexOfTvgId);
            }
            final String _tmpTvgName;
            if (_cursor.isNull(_cursorIndexOfTvgName)) {
              _tmpTvgName = null;
            } else {
              _tmpTvgName = _cursor.getString(_cursorIndexOfTvgName);
            }
            final String _tmpLogoUrl;
            if (_cursor.isNull(_cursorIndexOfLogoUrl)) {
              _tmpLogoUrl = null;
            } else {
              _tmpLogoUrl = _cursor.getString(_cursorIndexOfLogoUrl);
            }
            final String _tmpGroupTitle;
            if (_cursor.isNull(_cursorIndexOfGroupTitle)) {
              _tmpGroupTitle = null;
            } else {
              _tmpGroupTitle = _cursor.getString(_cursorIndexOfGroupTitle);
            }
            _item = new ChannelEntity(_tmpUrl,_tmpName,_tmpTvgId,_tmpTvgName,_tmpLogoUrl,_tmpGroupTitle);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getAllChannels(final Continuation<? super List<ChannelEntity>> $completion) {
    final String _sql = "SELECT * FROM channels";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<ChannelEntity>>() {
      @Override
      @NonNull
      public List<ChannelEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "url");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfTvgId = CursorUtil.getColumnIndexOrThrow(_cursor, "tvgId");
          final int _cursorIndexOfTvgName = CursorUtil.getColumnIndexOrThrow(_cursor, "tvgName");
          final int _cursorIndexOfLogoUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "logoUrl");
          final int _cursorIndexOfGroupTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "groupTitle");
          final List<ChannelEntity> _result = new ArrayList<ChannelEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ChannelEntity _item;
            final String _tmpUrl;
            _tmpUrl = _cursor.getString(_cursorIndexOfUrl);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpTvgId;
            if (_cursor.isNull(_cursorIndexOfTvgId)) {
              _tmpTvgId = null;
            } else {
              _tmpTvgId = _cursor.getString(_cursorIndexOfTvgId);
            }
            final String _tmpTvgName;
            if (_cursor.isNull(_cursorIndexOfTvgName)) {
              _tmpTvgName = null;
            } else {
              _tmpTvgName = _cursor.getString(_cursorIndexOfTvgName);
            }
            final String _tmpLogoUrl;
            if (_cursor.isNull(_cursorIndexOfLogoUrl)) {
              _tmpLogoUrl = null;
            } else {
              _tmpLogoUrl = _cursor.getString(_cursorIndexOfLogoUrl);
            }
            final String _tmpGroupTitle;
            if (_cursor.isNull(_cursorIndexOfGroupTitle)) {
              _tmpGroupTitle = null;
            } else {
              _tmpGroupTitle = _cursor.getString(_cursorIndexOfGroupTitle);
            }
            _item = new ChannelEntity(_tmpUrl,_tmpName,_tmpTvgId,_tmpTvgName,_tmpLogoUrl,_tmpGroupTitle);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<ChannelEntity>> getChannelsByGroupFlow(final String groupTitle) {
    final String _sql = "SELECT * FROM channels WHERE groupTitle = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, groupTitle);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"channels"}, new Callable<List<ChannelEntity>>() {
      @Override
      @NonNull
      public List<ChannelEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "url");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfTvgId = CursorUtil.getColumnIndexOrThrow(_cursor, "tvgId");
          final int _cursorIndexOfTvgName = CursorUtil.getColumnIndexOrThrow(_cursor, "tvgName");
          final int _cursorIndexOfLogoUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "logoUrl");
          final int _cursorIndexOfGroupTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "groupTitle");
          final List<ChannelEntity> _result = new ArrayList<ChannelEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ChannelEntity _item;
            final String _tmpUrl;
            _tmpUrl = _cursor.getString(_cursorIndexOfUrl);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpTvgId;
            if (_cursor.isNull(_cursorIndexOfTvgId)) {
              _tmpTvgId = null;
            } else {
              _tmpTvgId = _cursor.getString(_cursorIndexOfTvgId);
            }
            final String _tmpTvgName;
            if (_cursor.isNull(_cursorIndexOfTvgName)) {
              _tmpTvgName = null;
            } else {
              _tmpTvgName = _cursor.getString(_cursorIndexOfTvgName);
            }
            final String _tmpLogoUrl;
            if (_cursor.isNull(_cursorIndexOfLogoUrl)) {
              _tmpLogoUrl = null;
            } else {
              _tmpLogoUrl = _cursor.getString(_cursorIndexOfLogoUrl);
            }
            final String _tmpGroupTitle;
            if (_cursor.isNull(_cursorIndexOfGroupTitle)) {
              _tmpGroupTitle = null;
            } else {
              _tmpGroupTitle = _cursor.getString(_cursorIndexOfGroupTitle);
            }
            _item = new ChannelEntity(_tmpUrl,_tmpName,_tmpTvgId,_tmpTvgName,_tmpLogoUrl,_tmpGroupTitle);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<String>> getUniqueGroupsFlow() {
    final String _sql = "SELECT DISTINCT groupTitle FROM channels WHERE groupTitle IS NOT NULL AND groupTitle != ''";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"channels"}, new Callable<List<String>>() {
      @Override
      @NonNull
      public List<String> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final List<String> _result = new ArrayList<String>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final String _item;
            _item = _cursor.getString(0);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
