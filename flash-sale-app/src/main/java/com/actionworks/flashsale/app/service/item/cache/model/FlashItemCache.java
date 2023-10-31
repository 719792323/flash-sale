package com.actionworks.flashsale.app.service.item.cache.model;

import com.actionworks.flashsale.domain.model.entity.FlashItem;
import lombok.Data;
import lombok.experimental.Accessors;

/*
存储到本地cache的对象类
 */
@Data
@Accessors(chain = true)
public class FlashItemCache {
    protected boolean exist;
    private FlashItem flashItem;
    private Long version;
    private boolean later;

    public FlashItemCache with(FlashItem flashActivity) {
        this.exist = true;
        this.flashItem = flashActivity;
        return this;
    }


    public FlashItemCache withVersion(Long version) {
        this.version = version;
        return this;
    }

    public FlashItemCache tryLater() {
        this.later = true;
        return this;
    }

    public FlashItemCache notExist() {
        this.exist = false;
        return this;
    }
}
