package com.itcast.service;

import com.itcast.model.dto.ProductDto;
import com.itcast.model.dto.ProductEsDTO;
import com.itcast.model.dto.ProductSearchDto;
import com.itcast.model.pojo.Product;
import com.itcast.model.vo.SpecGroupVo;
import com.itcast.model.vo.ProductSpecsVo;
import com.itcast.model.vo.ProductVo;
import com.itcast.result.Result;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

import com.itcast.model.pojo.Category;

public interface ProductService {
    Result<List<Product>> getProductList(Integer pageNum, Integer pageSize);

    Result<List<ProductEsDTO>> getProductEsList(Integer pageNum, Integer pageSize);

    Result<ProductVo> getProduct(Integer productId);

    Result<ProductSpecsVo> getProductSpecs(Integer productId);

    Result<List<SpecGroupVo>> getCategorySpecs(Integer categoryId);

    Result<List<Product>> getProductByShop(Integer shopId);

    Result<Void> postProduct(ProductDto productDto);

    Result<Void> updateProduct(ProductDto productDto);
    
    Result<Void> deleteProduct(Integer productId);

    Result<List<ProductVo>> searchProduct(ProductSearchDto searchDto);

    Result<String> uploadImage(MultipartFile file) throws IOException;

    Result<List<Category>> getCategoryList(Integer parentId, Integer level);
}
