package com.itcast.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itcast.client.NoteClient;
import com.itcast.mapper.ProductBrowseMapper;
import com.itcast.mapper.ProductMapper;
import com.itcast.mapper.ShopMapper;
import com.itcast.mapper.SkuMapper;
import com.itcast.model.dto.ProductDto;
import com.itcast.model.dto.ProductSearchDto;
import com.itcast.model.pojo.*;
import com.itcast.model.vo.NoteSimpleVo;
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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.util.ArrayList;

import com.itcast.mapper.CategoryMapper;

@Service
@Slf4j
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private CategoryMapper categoryMapper;
    
    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private ShopMapper shopMapper;

    @Autowired
    private ProductBrowseMapper productBrowseMapper;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private NoteClient noteClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Result<List<Product>> getProductList(Integer pageNum, Integer pageSize) {
        Page<Product> page = new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 10 : pageSize);
        Page<Product> productPage = productMapper.selectPage(page, null);
        return Result.success(productPage.getRecords());
    }

    @Override
    public Result<ProductVo> getProduct(Integer productId) {
        String cacheKey = "product:detail:" + productId;
        
        // 1. 查询缓存
        try {
            String cacheValue = stringRedisTemplate.opsForValue().get(cacheKey);
            if (StringUtils.hasText(cacheValue)) {
                ProductVo productVo = objectMapper.readValue(cacheValue, ProductVo.class);
                // 异步记录浏览记录，避免影响响应时间
                recordBrowseHistory(productId);
                return Result.success(productVo);
            }
        } catch (Exception e) {
            log.error("Redis缓存读取失败: {}", e.getMessage());
        }

        // 2.获取商品信息 (SPU)
        Product product = productMapper.selectById(productId);
        if (product == null) {
            // 防止缓存穿透，可以存空值
            stringRedisTemplate.opsForValue().set(cacheKey, "", 5, java.util.concurrent.TimeUnit.MINUTES);
            return Result.failure("商品不存在");
        }
        
        // 3.获取SKU列表 (新增)
        List<com.itcast.model.pojo.Sku> skus = skuMapper.selectList(
            new LambdaQueryWrapper<com.itcast.model.pojo.Sku>().eq(com.itcast.model.pojo.Sku::getProductId, productId)
        );

        // 4.获取店铺信息
        Shop shop = shopMapper.selectById(product.getShopId());
        
        // 5.获取商品属性 (MongoDB, 兼容旧逻辑)
        ProductAttribute productAttribute
                = mongoTemplate.findOne(new Query(Criteria.where("product_id").is(product.getId())), ProductAttribute.class);
        
        // 6.设置vo
        ProductVo productVo = new ProductVo();
        BeanUtils.copyProperties(product, productVo);
        productVo.setShop(shop);
        productVo.setSkus(skus);
        if (productAttribute != null) productVo.setCustomAttributes(productAttribute.getCustomAttributes());
        
        // 6. 调用 NoteService 获取关联种草笔记 (并行优化或异常降级)
        try {
            Result<List<NoteSimpleVo>> noteResult = noteClient.getRelatedNotes(product.getId());
            if (noteResult != null && noteResult.getData() != null) {
                // 类型转换 NoteVo -> NoteSimpleVo
                // 注意：这里需要确保 NoteClient 返回的类型能被正确反序列化，或者做一层转换
                // 为简化，假设 NoteSimpleVo 兼容 NoteVo 的字段
                productVo.setRelatedNotes(noteResult.getData());
            }
        } catch (Exception e) {
            log.error("远程调用 NoteService 获取关联笔记失败", e);
            // 降级处理：不展示笔记，不影响商品详情页主流程
        }
        
        // 7. 写入缓存 (TTL 1小时 + 随机时间防止雪崩)
        try {
            long ttl = 3600 + (long)(Math.random() * 600);
            stringRedisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(productVo), ttl, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Redis缓存写入失败: {}", e.getMessage());
        }

        // 8.保存浏览记录
        recordBrowseHistory(productId);
        
        return Result.success(productVo);
    }

    private void recordBrowseHistory(Integer productId) {
        try {
            ProductBrowse productBrowse = new ProductBrowse();
            productBrowse.setProductId(productId);
            productBrowse.setUserId(UserContext.getUserId());
            productBrowseMapper.insert(productBrowse);
        } catch (Exception e) {
            // log.error("用户已经访问过，不需要再次插入数据库");
        }
    }

    @Override
    public Result<List<Product>> getProductByShop(Integer shopId) {
        // 根据店铺id获取该店铺所有产品
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Product::getShopId, shopId);
        List<Product> products = productMapper.selectList(queryWrapper);
        return Result.success(products);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public Result<Void> postProduct(ProductDto productDto) {
        // 1.上传商品 (SPU)
        Product product = new Product();
        BeanUtils.copyProperties(productDto, product);
        product.setStatus(1); // 默认上架
        product.setCreateTime(java.time.LocalDateTime.now());
        product.setUpdateTime(java.time.LocalDateTime.now());
        productMapper.insert(product);

        // 2.上传商品属性 (MongoDB, 兼容旧版)
        ProductAttribute productAttribute = productDto.getProductAttribute();
        if (productAttribute != null) {
            productAttribute.setProduct_id(product.getId());
            mongoTemplate.insert(productAttribute);
        }

        // 3. 上传 SKU (新版)
        if (productDto.getSkus() != null && !productDto.getSkus().isEmpty()) {
            for (com.itcast.model.pojo.Sku sku : productDto.getSkus()) {
                sku.setProductId(product.getId());
                // 如果没有图片，使用 SPU 主图
                if (!StringUtils.hasText(sku.getImage())) {
                    sku.setImage(product.getMainImage());
                }
                skuMapper.insert(sku);
            }
        }
        
        // 4. 发送 MQ 消息通知 Search 服务 (可选)
        
        return Result.success(null);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public Result<Void> updateProduct(ProductDto productDto) {
        // 1. 更新 SPU
        Product product = new Product();
        BeanUtils.copyProperties(productDto, product);
        product.setUpdateTime(LocalDateTime.now());
        productMapper.updateById(product);

        // 2. 更新 SKU
        if (productDto.getSkus() != null) {
            // 简单策略：获取现有SKU，对比更新/删除/新增
            // 为简化，先采用：保留ID匹配的更新，无ID的新增，数据库中有但参数中没有的删除
            
            // 查询当前数据库中的SKU
            List<Sku> dbSkus = skuMapper.selectList(
                new LambdaQueryWrapper<Sku>().eq(Sku::getProductId, product.getId())
            );
            List<Long> dbSkuIds = dbSkus.stream().map(Sku::getId).collect(Collectors.toList());
            List<Long> inputSkuIds = productDto.getSkus().stream()
                    .map(Sku::getId)
                    .filter(id -> id != null)
                    .collect(Collectors.toList());

            // 2.1 删除: db中有但input中没有
            List<Long> toDelete = dbSkuIds.stream().filter(id -> !inputSkuIds.contains(id)).collect(Collectors.toList());
            if (!toDelete.isEmpty()) {
                skuMapper.deleteBatchIds(toDelete);
            }

            // 2.2 更新或新增
            List<BigDecimal> prices = new ArrayList<>();
            for (Sku sku : productDto.getSkus()) {
                sku.setProductId(product.getId());
                if (!StringUtils.hasText(sku.getImage())) {
                    sku.setImage(product.getMainImage());
                }
                
                if (sku.getPrice() != null) {
                    prices.add(sku.getPrice());
                }

                if (sku.getId() != null && dbSkuIds.contains(sku.getId())) {
                    skuMapper.updateById(sku);
                } else {
                    sku.setId(null); // 确保是新增
                    skuMapper.insert(sku);
                }
            }
            
            // 2.3 更新商品主表价格（取SKU最低价）
            if (!prices.isEmpty()) {
                BigDecimal minPrice = prices.stream().min(BigDecimal::compareTo).orElse(null);
                if (minPrice != null) {
                    Product updatePrice = new Product();
                    updatePrice.setId(product.getId());
                    updatePrice.setPrice(minPrice.doubleValue());
                    productMapper.updateById(updatePrice);
                }
            }
        }
        
        // 3. 清除缓存
        String cacheKey = "product:detail:" + product.getId();
        stringRedisTemplate.delete(cacheKey);

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
        if (searchDto.getCategoryId() != null) {
            queryWrapper.eq(Product::getCategoryId, searchDto.getCategoryId());
        }

        Page<Product> productPage = productMapper.selectPage(page, queryWrapper);

        List<ProductVo> productVos = productPage.getRecords().stream().map(product -> {
            ProductVo productVo = new ProductVo();
            BeanUtils.copyProperties(product, productVo);
            Shop shop = shopMapper.selectById(product.getShopId());
            productVo.setShop(shop);

            // 获取SKU并聚合库存和销量
            List<com.itcast.model.pojo.Sku> skus = skuMapper.selectList(
                new LambdaQueryWrapper<Sku>().eq(Sku::getProductId, product.getId())
            );
            
            if (skus != null && !skus.isEmpty()) {
                int totalStock = skus.stream().mapToInt(sku -> sku.getStock() == null ? 0 : sku.getStock()).sum();
                productVo.setTotalStock(totalStock);
                
                // 可选：更新展示价格为最低SKU价格
                // productVo.setPrice(skus.stream().map(com.itcast.model.pojo.Sku::getPrice).min(java.math.BigDecimal::compareTo).orElse(java.math.BigDecimal.ZERO).doubleValue());
            } else {
                productVo.setTotalStock(0);
            }
            
            return productVo;
        }).collect(Collectors.toList());
        return Result.success(productVos, productPage.getTotal());
    }

    @Override
    public Result<String> uploadImage(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
             return Result.failure("文件为空");
        }
        // 1.获取文件名
        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename != null && originalFilename.contains(".") 
                ? originalFilename.substring(originalFilename.lastIndexOf(".")) 
                : ".jpg";
        String fileName = UUID.randomUUID().toString() + suffix;
        
        // 2.创建保存目录
        String uploadPath = System.getProperty("user.dir") + File.separator + "uploads";
        File dir = new File(uploadPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        // 3.保存文件
        file.transferTo(new File(dir, fileName));
        
        // 4.返回访问URL
        return Result.success("/product/uploads/" + fileName);
    }

    @Override
    public Result<List<Category>> getCategoryList(Integer parentId, Integer level) {
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        if (parentId != null) {
            queryWrapper.eq(Category::getParentId, parentId);
        }
        if (level != null) {
            queryWrapper.eq(Category::getLevel, level);
        }
        queryWrapper.orderByAsc(Category::getSort);
        return Result.success(categoryMapper.selectList(queryWrapper));
    }
}
