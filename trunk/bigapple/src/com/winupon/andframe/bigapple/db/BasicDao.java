package com.winupon.andframe.bigapple.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.winupon.andframe.bigapple.db.callback.MapRowMapper;
import com.winupon.andframe.bigapple.db.callback.MultiRowMapper;
import com.winupon.andframe.bigapple.db.callback.SingleRowMapper;
import com.winupon.andframe.bigapple.db.helper.SqlCreator;
import com.winupon.andframe.bigapple.utils.log.LogUtils;

/**
 * 对原生数据库操作做了一层轻量级封装，主要屏蔽了显式的close操作，当然也可以使用原生的API
 * 
 * @author xuan
 */
public class BasicDao {
    final static ReentrantLock lock = new ReentrantLock();// 保证多线程访问数据库的安全，性能有所损失

    private DBHelper dbHelper;
    private final Context context;

    public BasicDao(Context context) {
        this.context = context;
    }

    /**
     * 想使用安卓本身的数据操作方法可以获取这个对象，但操作完后要显示的Close掉
     * 
     * @return
     */
    public SQLiteDatabase getSQLiteDatabase() {
        dbHelper = new DBHelper(context);
        return dbHelper.getWritableDatabase();
    }

    /**
     * 使用完后请Close数据库连接，dbHelper的close其实内部就是sqliteDatabase的close
     */
    public void close() {
        // sqliteDatabase.close();
        dbHelper.close();
    }

    public Context getContext() {
        return context;
    }

    // ///////////////////////////////////////////////插入或者更新////////////////////////////////////////////////////
    /**
     * 插入或者更新
     * 
     * @param sql
     */
    protected void update(String sql) {
        lock.lock();
        try {
            getSQLiteDatabase().execSQL(sql);
        }
        catch (Exception e) {
            LogUtils.e("", e);
        }
        finally {
            close();
            lock.unlock();
        }
    }

    /**
     * 插入或者更新，带参
     * 
     * @param sql
     * @param args
     */
    protected void update(String sql, Object[] args) {
        if (null == args) {
            update(sql);
        }
        else {
            lock.lock();
            try {
                getSQLiteDatabase().execSQL(sql, args);
            }
            catch (Exception e) {
                LogUtils.e("", e);
            }
            finally {
                close();
                lock.unlock();
            }
        }
    }

    /**
     * 插入或者更新，批量
     * 
     * @param sql
     * @param argsList
     */
    protected void updateBatch(String sql, List<Object[]> argsList) {
        if (null == argsList) {
            return;
        }

        lock.lock();
        SQLiteDatabase sqliteDatabase = getSQLiteDatabase();
        try {
            sqliteDatabase.beginTransaction();
            for (Object[] args : argsList) {
                sqliteDatabase.execSQL(sql, args);
            }
            sqliteDatabase.setTransactionSuccessful();
        }
        catch (Exception e) {
            LogUtils.e("", e);
        }
        finally {
            sqliteDatabase.endTransaction();
            close();
            lock.unlock();
        }
    }

    // ///////////////////////////////////////////////查询//////////////////////////////////////////////////////////////
    /**
     * 查询，返回多条记录
     * 
     * @param sql
     * @param args
     * @param multiRowMapper
     * @return
     */
    protected <T> List<T> query(String sql, String[] args, MultiRowMapper<T> multiRowMapper) {
        List<T> ret = new ArrayList<T>();

        lock.lock();
        Cursor cursor = getSQLiteDatabase().rawQuery(sql, args);
        try {
            int i = 0;
            while (cursor.moveToNext()) {
                T t = multiRowMapper.mapRow(cursor, i);
                ret.add(t);
                i++;
            }
        }
        catch (Exception e) {
            LogUtils.e("", e);
        }
        finally {
            cursor.close();
            close();
            lock.unlock();
        }

        return ret;
    }

    /**
     * 查询，返回单条记录
     * 
     * @param sql
     * @param args
     * @param singleRowMapper
     * @return
     */
    protected <T> T query(String sql, String[] args, SingleRowMapper<T> singleRowMapper) {
        T ret = null;

        lock.lock();
        Cursor cursor = getSQLiteDatabase().rawQuery(sql, args);
        try {
            if (cursor.moveToNext()) {
                ret = singleRowMapper.mapRow(cursor);
            }
        }
        catch (Exception e) {
            LogUtils.e("", e);
        }
        finally {
            cursor.close();
            close();
            lock.unlock();
        }

        return ret;
    }

    /**
     * 查询，返回MAP集合
     * 
     * @param sql
     * @param args
     * @param singleRowMapper
     * @return
     */
    protected <K, V> Map<K, V> query(String sql, String[] args, MapRowMapper<K, V> mapRowMapper) {
        Map<K, V> ret = new HashMap<K, V>();

        lock.lock();
        Cursor cursor = getSQLiteDatabase().rawQuery(sql, args);
        try {
            int i = 0;
            while (cursor.moveToNext()) {
                K k = mapRowMapper.mapRowKey(cursor, i);
                V v = mapRowMapper.mapRowValue(cursor, i);
                ret.put(k, v);
                i++;
            }
        }
        catch (Exception e) {
            LogUtils.e("", e);
        }
        finally {
            cursor.close();
            close();
            lock.unlock();
        }

        return ret;
    }

    /**
     * IN查询，返回LIST集合
     * 
     * @param sql
     * @param args
     * @param multiRowMapper
     * @return
     */
    protected <T> List<T> queryForInSQL(String prefix, String[] prefixArgs, String[] inArgs, String postfix,
            MultiRowMapper<T> multiRowMapper) {
        if (null == prefixArgs) {
            prefixArgs = new String[0];
        }

        StringBuilder sql = new StringBuilder();
        sql.append(prefix).append(SqlCreator.getInSQL(inArgs.length));

        if (!TextUtils.isEmpty(postfix)) {
            sql.append(postfix);
        }

        String[] args = new String[inArgs.length + prefixArgs.length];

        System.arraycopy(prefixArgs, 0, args, 0, prefixArgs.length);
        System.arraycopy(inArgs, 0, args, prefixArgs.length, inArgs.length);

        return query(sql.toString(), args, multiRowMapper);
    }

    /**
     * IN查询，返回MAP集合
     * 
     * @param sql
     * @param args
     * @param multiRowMapper
     * @return
     */
    protected <K, V> Map<K, V> queryForInSQL(String prefix, String[] prefixArgs, String[] inArgs, String postfix,
            MapRowMapper<K, V> mapRowMapper) {
        if (null == prefixArgs) {
            prefixArgs = new String[0];
        }

        StringBuilder sql = new StringBuilder();
        sql.append(prefix).append(SqlCreator.getInSQL(inArgs.length));

        if (!TextUtils.isEmpty(postfix)) {
            sql.append(postfix);
        }

        String[] args = new String[inArgs.length + prefixArgs.length];

        System.arraycopy(prefixArgs, 0, args, 0, prefixArgs.length);
        System.arraycopy(inArgs, 0, args, prefixArgs.length, inArgs.length);

        return query(sql.toString(), args, mapRowMapper);
    }

}
