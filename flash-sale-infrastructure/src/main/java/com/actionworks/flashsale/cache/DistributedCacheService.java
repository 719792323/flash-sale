package com.actionworks.flashsale.cache;

import java.util.List;
import java.util.concurrent.TimeUnit;

//远端缓存接口设计，避免依赖具体实现
public interface DistributedCacheService {
    void put(String key, String value);

    void put(String key, Object value);

    void put(String key, Object value, long timeout, TimeUnit unit);

    void put(String key, Object value, long expireTime);

    <T> T getObject(String key, Class<T> targetClass);

    String getString(String key);

    <T> List<T> getList(String key, Class<T> targetClass);

    Boolean delete(String key);

    Boolean hasKey(String key);
}
