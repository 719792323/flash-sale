package com.actionworks.flashsale.app.util;

import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

//https://www.jianshu.com/p/9c3802080d9f
public class MultiPlaceOrderTypesCondition extends AnyNestedCondition {

    public MultiPlaceOrderTypesCondition() {
        super(ConfigurationPhase.PARSE_CONFIGURATION);
    }

    /**
     * matchIfMissing 缺少该配置时是否可以加载，如果为true，没有该配置属性时也会正常加载，反之则不会生效
     */
//    @ConditionalOnProperty(name = "place_order_type", havingValue = "normal", matchIfMissing = true)
    static class NormalCondition {

    }

//    @ConditionalOnProperty(name = "place_order_type", havingValue = "buckets")
    static class BucketsCondition {

    }
}
