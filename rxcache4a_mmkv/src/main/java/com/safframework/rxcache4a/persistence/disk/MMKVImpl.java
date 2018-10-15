package com.safframework.rxcache4a.persistence.disk;

import com.safframework.rxcache.config.Constant;
import com.safframework.rxcache.domain.CacheHolder;
import com.safframework.rxcache.domain.Record;
import com.safframework.rxcache.domain.Source;
import com.safframework.rxcache.persistence.converter.Converter;
import com.safframework.rxcache.persistence.converter.GsonConverter;
import com.safframework.rxcache.persistence.disk.Disk;
import com.safframework.tony.common.utils.Preconditions;
import com.tencent.mmkv.MMKV;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

/**
 * @FileName: com.safframework.rxcache4a.persistence.disk.MMKVImpl
 * @author: Tony Shen
 * @date: 2018-10-06 10:37
 * @version: V1.0 <描述当前版本功能>
 */
public class MMKVImpl implements Disk {

    private MMKV kv = null;
    private Converter converter;

    public MMKVImpl() {

        this.kv = MMKV.defaultMMKV();
        this.converter = new GsonConverter();
    }

    public MMKVImpl(String mmkvID,Converter converter) {

        this.kv = MMKV.mmkvWithID(mmkvID);
        this.converter = converter;
    }

    @Override
    public int storedMB() {
        double megabytes = Math.ceil((double) kv.totalSize() / 1024 / 1024);
        return (int) megabytes;
    }

    @Override
    public <T> Record<T> retrieve(String key, Type type) {

        CacheHolder holder = converter.fromJson(kv.decodeString(key),CacheHolder.class);

        if (holder==null) return null;

        long timestamp = holder.timestamp;
        long expireTime = holder.expireTime;

        T result = null;

        if (expireTime<0) { // 缓存的数据从不过期

            String json = holder.data;

            result = converter.fromJson(json,type);
        } else {

            if (timestamp + expireTime > System.currentTimeMillis()) {  // 缓存的数据还没有过期

                String json = holder.data;

                result = converter.fromJson(json,type);
            } else {        // 缓存的数据已经过期

                evict(key);
            }
        }

        return result != null ? new Record<>(Source.PERSISTENCE, key, result, timestamp, expireTime) : null;
    }

    @Override
    public <T> void save(String key, T value) {
        save(key, value, Constant.NEVER_EXPIRE);
    }

    @Override
    public <T> void save(String key, T value, long expireTime) {

        if (Preconditions.isNotBlanks(key,value)) {

            CacheHolder holder = new CacheHolder(converter.toJson(value),System.currentTimeMillis(),expireTime);
            kv.encode(key, converter.toJson(holder));
        }
    }

    @Override
    public List<String> allKeys() {

        return Arrays.asList(kv.allKeys());
    }

    @Override
    public boolean containsKey(String key) {

        return kv.containsKey(key);
    }

    @Override
    public void evict(String key) {

        kv.removeValueForKey(key);
    }

    @Override
    public void evictAll() {

        kv.clearAll();
    }
}