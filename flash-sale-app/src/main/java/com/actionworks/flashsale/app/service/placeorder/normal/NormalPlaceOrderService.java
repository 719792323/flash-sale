package com.actionworks.flashsale.app.service.placeorder.normal;

import com.actionworks.flashsale.app.exception.BizException;
import com.actionworks.flashsale.app.model.command.FlashPlaceOrderCommand;
import com.actionworks.flashsale.app.model.dto.FlashItemDTO;
import com.actionworks.flashsale.app.model.result.AppSimpleResult;
import com.actionworks.flashsale.app.model.result.PlaceOrderResult;
import com.actionworks.flashsale.app.service.activity.FlashActivityAppService;
import com.actionworks.flashsale.app.service.item.FlashItemAppService;
import com.actionworks.flashsale.app.service.placeorder.PlaceOrderService;
import com.actionworks.flashsale.app.service.stock.ItemStockCacheService;
import com.actionworks.flashsale.app.util.MultiPlaceOrderTypesCondition;
import com.actionworks.flashsale.app.util.OrderNoGenerateContext;
import com.actionworks.flashsale.app.util.OrderNoGenerateService;
import com.actionworks.flashsale.domain.model.StockDeduction;
import com.actionworks.flashsale.domain.model.entity.FlashOrder;
import com.actionworks.flashsale.domain.service.FlashOrderDomainService;
import com.actionworks.flashsale.domain.service.StockDeductionDomainService;
import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import static com.actionworks.flashsale.app.exception.AppErrorCode.INVALID_PARAMS;
import static com.actionworks.flashsale.app.exception.AppErrorCode.ITEM_NOT_FOUND;
import static com.actionworks.flashsale.app.exception.AppErrorCode.PLACE_ORDER_FAILED;
import static com.actionworks.flashsale.app.model.builder.FlashOrderAppBuilder.toDomain;

@Service
@ConditionalOnProperty(name = "PlaceOrderService", havingValue = "normal", matchIfMissing = true)
public class NormalPlaceOrderService implements PlaceOrderService {
    private static final Logger logger = LoggerFactory.getLogger(NormalPlaceOrderService.class);
    @Resource
    private FlashOrderDomainService flashOrderDomainService;
    @Resource
    private StockDeductionDomainService stockDeductionDomainService;
    @Resource
    private FlashActivityAppService flashActivityAppService;
    @Resource
    private ItemStockCacheService itemStockCacheService;
    @Resource
    private OrderNoGenerateService orderNoGenerateService;
    @Resource
    private FlashItemAppService flashItemAppService;

    @PostConstruct
    public void init() {
        logger.info("initPlaceOrderService|默认下单服务已经初始化");
    }


    @Override
    public PlaceOrderResult doPlaceOrder(Long userId, FlashPlaceOrderCommand placeOrderCommand) {
        logger.info("placeOrder|开始下单|{},{}", userId, JSON.toJSONString(placeOrderCommand));
        //参数校验
        if (userId == null || placeOrderCommand == null || !placeOrderCommand.validateParams()) {
            throw new BizException(INVALID_PARAMS);
        }
        //活动校验
        boolean isActivityAllowPlaceOrder = flashActivityAppService.isAllowPlaceOrderOrNot(placeOrderCommand.getActivityId());
        if (!isActivityAllowPlaceOrder) {
            logger.info("placeOrder|秒杀活动下单规则校验未通过|{},{}", userId, placeOrderCommand.getActivityId());
            return PlaceOrderResult.failed(PLACE_ORDER_FAILED);
        }

        //商品检验
        boolean isItemAllowPlaceOrder = flashItemAppService.isAllowPlaceOrderOrNot(placeOrderCommand.getItemId());
        if (!isItemAllowPlaceOrder) {
            logger.info("placeOrder|秒杀品下单规则校验未通过|{},{}", userId, placeOrderCommand.getActivityId());
            return PlaceOrderResult.failed(PLACE_ORDER_FAILED);
        }
        //获取对应秒杀品
        AppSimpleResult<FlashItemDTO> flashItemResult = flashItemAppService.getFlashItem(placeOrderCommand.getItemId());
        if (!flashItemResult.isSuccess() || flashItemResult.getData() == null) {
            return PlaceOrderResult.failed(ITEM_NOT_FOUND);
        }
        FlashItemDTO flashItem = flashItemResult.getData();
        //生成订单ID
        Long orderId = orderNoGenerateService.generateOrderNo(new OrderNoGenerateContext());
        //生成订单对象
        FlashOrder flashOrderToPlace = toDomain(placeOrderCommand);
        flashOrderToPlace.setItemTitle(flashItem.getItemTitle());
        flashOrderToPlace.setFlashPrice(flashItem.getFlashPrice());
        flashOrderToPlace.setUserId(userId);
        flashOrderToPlace.setId(orderId);
        //生成库存数量变化对象
        StockDeduction stockDeduction = new StockDeduction()
                .setItemId(placeOrderCommand.getItemId())
                .setQuantity(placeOrderCommand.getQuantity())
                .setUserId(userId);

        boolean preDecreaseStockSuccess = false;
        //执行扣库存逻辑
        try {
            //预扣库存，通过redis缓存完成，没有走本地缓存（最大程度降低延迟导致的不一致），通过lua脚本保证原子执行
            //缓存预扣库存是为了减少数据库压力
            preDecreaseStockSuccess = itemStockCacheService.decreaseItemStock(stockDeduction);
            if (!preDecreaseStockSuccess) {
                logger.info("placeOrder|库存预扣减失败|{},{}", userId, JSON.toJSONString(placeOrderCommand));
                return PlaceOrderResult.failed(PLACE_ORDER_FAILED.getErrCode(), PLACE_ORDER_FAILED.getErrDesc());
            }
            //数据库扣库存（保证一致性）
            boolean decreaseStockSuccess = stockDeductionDomainService.decreaseItemStock(stockDeduction);
            if (!decreaseStockSuccess) {
                logger.info("placeOrder|库存扣减失败|{},{}", userId, JSON.toJSONString(placeOrderCommand));
                return PlaceOrderResult.failed(PLACE_ORDER_FAILED.getErrCode(), PLACE_ORDER_FAILED.getErrDesc());
            }
            //下单（保证一致性）
            boolean placeOrderSuccess = flashOrderDomainService.placeOrder(userId, flashOrderToPlace);
            if (!placeOrderSuccess) {
                throw new BizException(PLACE_ORDER_FAILED.getErrDesc());
            }
        } catch (Exception e) {
            //恢复redis库存值
            if (preDecreaseStockSuccess) {
                boolean recoverStockSuccess = itemStockCacheService.increaseItemStock(stockDeduction);
                if (!recoverStockSuccess) {
                    logger.error("placeOrder|预扣库存恢复失败|{},{}", userId, JSON.toJSONString(placeOrderCommand), e);
                }
            }
            logger.error("placeOrder|下单失败|{},{}", userId, JSON.toJSONString(placeOrderCommand), e);
            throw new BizException(PLACE_ORDER_FAILED.getErrDesc());
        }
        logger.info("placeOrder|下单成功|{},{}", userId, orderId);
        return PlaceOrderResult.ok(orderId);
    }
}
