package com.oms.inventory.mapper;

import com.oms.common.dto.InventoryDto.ProductResponse;
import com.oms.inventory.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for Product entity <-> DTO conversion.
 */
@Mapper(componentModel = "spring")
public interface ProductMapper {
    
    @Mapping(target = "id", expression = "java(product.getId().toString())")
    @Mapping(target = "availableQuantity", expression = "java(product.getAvailableQuantity())")
    ProductResponse toResponse(Product product);
}
