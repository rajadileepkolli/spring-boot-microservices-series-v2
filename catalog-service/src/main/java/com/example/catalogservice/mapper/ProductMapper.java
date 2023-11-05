/***
<p>
    Licensed under MIT License Copyright (c) 2021-2023 Raja Kolli.
</p>
***/

package com.example.catalogservice.mapper;

import com.example.catalogservice.entities.Product;
import com.example.catalogservice.model.response.ProductResponse;
import com.example.common.dtos.ProductDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "inStock", ignore = true)
    Product toEntity(ProductDto productDto);

    ProductResponse toProductResponse(Product product);
}
