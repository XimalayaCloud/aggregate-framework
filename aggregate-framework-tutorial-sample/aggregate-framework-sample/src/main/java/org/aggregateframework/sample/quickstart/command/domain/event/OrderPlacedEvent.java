package org.aggregateframework.sample.quickstart.command.domain.event;

import org.aggregateframework.sample.quickstart.command.domain.entity.PricedOrder;

/**
 * Created by changming.xie on 4/7/16.
 */
public class OrderPlacedEvent {

    private PricedOrder pricedOrder;

//    private String no;

    public OrderPlacedEvent(PricedOrder pricedOrder) {
        this.pricedOrder = pricedOrder;
//        this.no = pricedOrder.getMerchantOrderNo();
    }

    public PricedOrder getPricedOrder() {
        return pricedOrder;
    }

//    public String getNo() {
//        return no;
//    }
//
//    public void setNo(String no) {
//        this.no = no;
//    }
}
