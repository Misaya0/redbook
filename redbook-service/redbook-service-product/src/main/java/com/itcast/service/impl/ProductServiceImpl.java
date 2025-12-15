package com.itcast.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itcast.mapper.ProductBrowseMapper;
import com.itcast.mapper.ProductMapper;
import com.itcast.mapper.ShopMapper;
import com.itcast.model.dto.ProductDto;
import com.itcast.model.dto.ProductSearchDto;
import com.itcast.model.pojo.Product;
import com.itcast.model.pojo.ProductAttribute;
import com.itcast.model.pojo.ProductBrowse;
import com.itcast.model.pojo.Shop;
import com.itcast.model.vo.ProductVo;
import com.itcast.result.Result;
import com.itcast.service.ProductService;
import com.itcast.context.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ShopMapper shopMapper;

    @Autowired
    private ProductBrowseMapper productBrowseMapper;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public Result<List<Product>> getProductList(Integer pageNum, Integer pageSize) {
        Page<Product> page = new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 10 : pageSize);
        Page<Product> productPage = productMapper.selectPage(page, null);
        return Result.success(productPage.getRecords());
    }

    @Override
    public Result<ProductVo> getProduct(Integer productId) {
        // 1.获取商品信息
        Product product = productMapper.selectById(productId);
        if (product == null) {
            return Result.failure("商品不存在");
        }
        // 2.获取店铺信息
        Shop shop = shopMapper.selectById(product.getShopId());
        // 获取商品属性
        ProductAttribute productAttribute
                = mongoTemplate.findOne(new Query(Criteria.where("product_id").is(product.getId())), ProductAttribute.class);
        // 3.设置vo
        ProductVo productVo = new ProductVo();
        BeanUtils.copyProperties(product, productVo);
        productVo.setShop(shop);
        if (productAttribute != null) productVo.setCustomAttributes(productAttribute.getCustomAttributes());
        // 4.保存浏览记录
        try {
            ProductBrowse productBrowse = new ProductBrowse();
            productBrowse.setProductId(productId);
            productBrowse.setUserId(UserContext.getUserId());
            productBrowseMapper.insert(productBrowse);
        } catch (Exception e) {
            log.error("用户已经访问过，不需要再次插入数据库");
        }
        return Result.success(productVo);
    }

    @Override
    public Result<List<Product>> getProductByShop(Integer productId) {
        // 1.根据产品id获取店铺id
        Product product = productMapper.selectById(productId);
        Integer shopId = product.getShopId();
        // 2.根据店铺id获取该店铺所有产品
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Product::getShopId, shopId);
        List<Product> products = productMapper.selectList(queryWrapper);
        // 3.过滤该产品
        products = products.stream().filter(
                product2 -> !product2.getId().equals(productId)).collect(Collectors.toList());
        return Result.success(products);
    }

    @Override
    public Result<Void> postProduct(ProductDto productDto) {
        // 1.上传商品
        Product product = new Product();
        BeanUtils.copyProperties(productDto, product);
        productMapper.insert(product);

        // 2.上传商品属性
        ProductAttribute productAttribute = productDto.getProductAttribute();
        if (productAttribute != null) {
            productAttribute.setProduct_id(product.getId());
            mongoTemplate.insert(productAttribute);
        }
        return Result.success(null);
    }

    @Override
    public Result<Void> updateProduct(Product product) {
        productMapper.updateById(product);
        return Result.success(null);
    }

    @Override
    public Result<Void> deleteProduct(Integer productId) {
        productMapper.deleteById(productId);
        return Result.success(null);
    }

    @Override
    public Result<List<ProductVo>> searchProduct(ProductSearchDto searchDto) {
        Page<Product> page = new Page<>(searchDto.getPageNum(), searchDto.getPageSize());
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(searchDto.getKeyword())) {
            queryWrapper.like(Product::getName, searchDto.getKeyword());
        }
        if (searchDto.getMinPrice() != null) {
            queryWrapper.ge(Product::getPrice, searchDto.getMinPrice());
        }
        if (searchDto.getMaxPrice() != null) {
            queryWrapper.le(Product::getPrice, searchDto.getMaxPrice());
        }
        if (searchDto.getShopId() != null) {
            queryWrapper.eq(Product::getShopId, searchDto.getShopId());
        }

        Page<Product> productPage = productMapper.selectPage(page, queryWrapper);

        List<ProductVo> productVos = productPage.getRecords().stream().map(product -> {
            ProductVo productVo = new ProductVo();
            BeanUtils.copyProperties(product, productVo);
            // 这里可以补充更多VO需要的字段，比如店铺名称等
            return productVo;
        }).collect(Collectors.toList());
        return Result.success(productVos, productPage.getTotal());
    }
}
