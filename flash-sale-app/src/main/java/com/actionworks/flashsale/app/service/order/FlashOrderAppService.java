package com.actionworks.flashsale.app.service.order;

import com.actionworks.flashsale.app.model.command.FlashPlaceOrderCommand;
import com.actionworks.flashsale.app.model.dto.FlashOrderDTO;
import com.actionworks.flashsale.app.model.query.FlashOrdersQuery;
import com.actionworks.flashsale.app.model.result.AppMultiResult;
import com.actionworks.flashsale.app.model.result.AppResult;
import com.actionworks.flashsale.app.model.result.AppSimpleResult;
import com.actionworks.flashsale.app.model.result.OrderTaskHandleResult;
import com.actionworks.flashsale.app.model.result.PlaceOrderResult;

public interface FlashOrderAppService {
    //下单
    AppSimpleResult<PlaceOrderResult> placeOrder(Long userId, FlashPlaceOrderCommand placeOrderCommand);
    //获取订单
    AppSimpleResult<OrderTaskHandleResult> getPlaceOrderTaskResult(Long userId, Long itemId, String placeOrderTaskId);
    //根据用户获取订单
    AppMultiResult<FlashOrderDTO> getOrdersByUser(Long userId, FlashOrdersQuery flashOrdersQuery);
    //取消订单
    AppResult cancelOrder(Long userId, Long orderId);
}
