package com.actionworks.flashsale.app.util;

import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

//https://www.jianshu.com/p/9c3802080d9f
public class MultiPlaceOrderTypesCondition extends AnyNestedCondition {

    public MultiPlaceOrderTypesCondition() {
        super(ConfigurationPhase.PARSE_CONFIGURATION);
    }

    @ConditionalOnProperty(name = "place_order_type", havingValue = "normal", matchIfMissing = true)
    static class NormalCondition {

    }

    @ConditionalOnProperty(name = "place_order_type", havingValue = "buckets", matchIfMissing = true)
    static class BucketsCondition {

    }
}
