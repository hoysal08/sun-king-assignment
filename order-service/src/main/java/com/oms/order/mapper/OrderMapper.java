package com.oms.order.mapper;

import com.oms.common.dto.OrderDto.*;
import com.oms.order.entity.Order;
import com.oms.order.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper for Order entity <-> DTO conversion.
 */
@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(target = "items", source = "items")
    OrderResponse toResponse(Order order);

    @Mapping(target = "sku", source = "productSku")
    OrderItemResponse toItemResponse(OrderItem item);

    List<OrderItemResponse> toItemResponses(List<OrderItem> items);

    @Mapping(target = "itemCount", expression = "java(order.getItems().size())")
    OrderSummaryResponse toSummaryResponse(Order order);
}
