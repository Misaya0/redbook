package com.itcast.service;

import com.itcast.model.dto.ProductDto;
import com.itcast.model.dto.ProductSearchDto;
import com.itcast.model.pojo.Product;
import com.itcast.model.vo.ProductVo;
import com.itcast.result.Result;

import java.util.List;

public interface ProductService {
    Result<List<Product>> getProductList(Integer pageNum, Integer pageSize);

    Result<ProductVo> getProduct(Integer productId);

    Result<List<Product>> getProductByShop(Integer productId);

    Result<Void> postProduct(ProductDto productDto);

    Result<Void> updateProduct(Product product);
    
    Result<Void> deleteProduct(Integer productId);

    Result<List<ProductVo>> searchProduct(ProductSearchDto searchDto);
}
