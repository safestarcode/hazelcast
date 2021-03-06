/*
 * Copyright (c) 2008-2015, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.map.impl.record;

import com.hazelcast.nio.serialization.Data;

/**
 * @param <V> the type of value which is in the Record
 */
public interface Record<V> {

    /**
     * If not a {@link com.hazelcast.map.impl.record.CachedDataRecord}.
     */
    Object NOT_CACHED = new Object();

    Data getKey();

    V getValue();

    void setValue(V value);

    RecordStatistics getStatistics();

    void setStatistics(RecordStatistics stats);

    void onAccess();

    void onUpdate();

    void onStore();

    long getCost();

    long getVersion();

    void setVersion(long version);

    void setEvictionCriteriaNumber(long evictionCriteriaNumber);

    long getEvictionCriteriaNumber();

    /**
     * Get current cache value or null.
     * <p/>
     * Warning: Do not use this method directly as it might expose arbitrary objects acting as a lock.
     * Use {@link Records#getCachedValue(Record)} instead.
     *
     * @return current cached value or null or cached record mutex.
     */
    Object getCachedValueUnsafe();

    /**
     * Atomically sets the cached value to the given new value
     * if the current cached value {@code ==} the expected value.
     *
     * @param expectedValue the expected cached value
     * @param newValue      the new cached value
     * @return {@code true} if successful. False return indicates that
     * the actual cached value was not equal to the expected cached value.
     */
    boolean casCachedValue(Object expectedValue, Object newValue);

    long getTtl();

    void setTtl(long ttl);

    long getLastAccessTime();

    void setLastAccessTime(long lastAccessTime);

    long getLastUpdateTime();

    void setLastUpdateTime(long lastUpdatedTime);

    long getCreationTime();

    void setCreationTime(long creationTime);

    /**
     * Only used for Hot Restart, HDRecord
     *
     * @return
     */
    long getSequence();

    /**
     * Only used for Hot Restart, HDRecord
     *
     * @return
     */
    void setSequence(long sequence);
}
