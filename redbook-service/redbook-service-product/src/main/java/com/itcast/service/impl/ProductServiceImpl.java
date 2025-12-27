package com.itcast.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itcast.client.NoteClient;
import com.itcast.mapper.CategorySpecMapper;
import com.itcast.mapper.ProductBrowseMapper;
import com.itcast.mapper.ProductMapper;
import com.itcast.mapper.ShopMapper;
import com.itcast.mapper.SkuMapper;
import com.itcast.mapper.SpecMapper;
import com.itcast.mapper.SpecOptionMapper;
import com.itcast.model.dto.ProductDto;
import com.itcast.model.dto.ProductSearchDto;
import com.itcast.model.pojo.*;
import com.itcast.model.vo.NoteSimpleVo;
import com.itcast.model.vo.ProductSpecsVo;
import com.itcast.model.vo.ProductVo;
import com.itcast.model.vo.SpecGroupVo;
import com.itcast.model.vo.SpecOptionVo;
import com.itcast.model.vo.SkuSpecVo;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.math.BigDecimal;

import com.itcast.mapper.CategoryMapper;

import com.itcast.constant.MqConstant;
import com.itcast.model.dto.ProductEsDTO;
import com.itcast.model.dto.ProductSyncMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@Service
@Slf4j
public class ProductServiceImpl implements ProductService {

    private static final DateTimeFormatter PRODUCT_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private CategoryMapper categoryMapper;
    
    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private SpecMapper specMapper;

    @Autowired
    private SpecOptionMapper specOptionMapper;

    @Autowired
    private CategorySpecMapper categorySpecMapper;

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
    public Result<List<ProductEsDTO>> getProductEsList(Integer pageNum, Integer pageSize) {
        int finalPageNum = pageNum == null ? 1 : pageNum;
        int finalPageSize = pageSize == null ? 100 : pageSize;
        Page<Product> page = new Page<>(finalPageNum, finalPageSize);
        Page<Product> productPage = productMapper.selectPage(page, null);
        List<Product> products = productPage.getRecords();
        if (products == null || products.isEmpty()) {
            return Result.success(new ArrayList<>());
        }

        List<ProductEsDTO> list = products.stream().map(this::buildProductEsDTO).collect(Collectors.toList());
        return Result.success(list);
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

    // 辅助方法：发送同步消息
    private void sendProductSyncMessage(Product product, String type) {
        try {
            ProductSyncMessage message = new ProductSyncMessage();
            message.setType(type);
            message.setId(product.getId());
            
            if ("save".equals(type)) {
                Product productForBuild = product;
                if (productForBuild.getName() == null || productForBuild.getShopId() == null) {
                    Product dbProduct = productMapper.selectById(product.getId());
                    if (dbProduct != null) {
                        productForBuild = dbProduct;
                    }
                }
                ProductEsDTO dto = buildProductEsDTO(productForBuild);
                message.setData(dto);
            }
            
            rabbitTemplate.convertAndSend(MqConstant.PRODUCT_ES_EXCHANGE, MqConstant.PRODUCT_ES_SYNC_KEY, objectMapper.writeValueAsString(message));
            log.info("Sent product sync message: type={}, id={}", type, product.getId());
        } catch (Exception e) {
            log.error("Failed to send product sync message", e);
        }
    }

    private ProductEsDTO buildProductEsDTO(Product product) {
        ProductEsDTO dto = new ProductEsDTO();
        BeanUtils.copyProperties(product, dto);
        dto.setImage(product.getMainImage());

        Integer stock = 0;
        if (product.getId() != null) {
            List<Sku> skus = skuMapper.selectList(new LambdaQueryWrapper<Sku>().eq(Sku::getProductId, product.getId()));
            if (skus != null && !skus.isEmpty()) {
                stock = skus.stream().map(Sku::getStock).filter(s -> s != null).reduce(0, Integer::sum);
                List<BigDecimal> prices = skus.stream().map(Sku::getPrice).filter(p -> p != null).collect(Collectors.toList());
                if (!prices.isEmpty()) {
                    BigDecimal min = prices.stream().min(BigDecimal::compareTo).orElse(null);
                    if (min != null) {
                        dto.setPrice(min.doubleValue());
                    }
                }
            }
        }
        dto.setStock(stock);

        if (dto.getSales() == null) {
            dto.setSales(0);
        }
        LocalDateTime createTime = product.getCreateTime();
        if (createTime == null) {
            createTime = LocalDateTime.now();
        }
        dto.setCreateTime(createTime);

        return dto;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public Result<Void> postProduct(ProductDto productDto) {
        // 1.上传商品 (SPU)
        Product product = new Product();
        BeanUtils.copyProperties(productDto, product);
        product.setStatus(1); // 默认上架
        product.setSales(0); // 初始化销量为0
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
                sku.setSpecs(normalizeSpecsJson(sku.getSpecs()));
                skuMapper.insert(sku);
            }
        }
        
        // 4. 发送 MQ 消息通知 Search 服务 (可选) -> 现在实现了
        sendProductSyncMessage(product, "save");
        
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
                sku.setSpecs(normalizeSpecsJson(sku.getSpecs()));
                
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

        // 4. 同步到 ES
        sendProductSyncMessage(product, "save");

        return Result.success(null);
    }

    @Override
    public Result<List<SpecGroupVo>> getCategorySpecs(Integer categoryId) {
        if (categoryId == null) {
            return Result.failure("品类ID不能为空");
        }
        Long cid = Long.valueOf(categoryId);
        Category category = categoryMapper.selectById(cid);
        Long effectiveCategoryId = cid;
        if (category != null && category.getParentId() != null && category.getParentId() > 0) {
            effectiveCategoryId = category.getParentId();
        }

        List<SpecGroupVo> groups = buildSpecGroups(effectiveCategoryId, new ArrayList<>());
        if ((groups == null || groups.isEmpty()) && effectiveCategoryId.longValue() != cid.longValue()) {
            groups = buildSpecGroups(cid, new ArrayList<>());
        }
        return Result.success(groups);
    }

    @Override
    public Result<Void> deleteProduct(Integer productId) {
        productMapper.deleteById(productId);
        
        // 同步删除 ES
        Product product = new Product();
        product.setId(Long.valueOf(productId));
        sendProductSyncMessage(product, "delete");
        
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
    public Result<ProductSpecsVo> getProductSpecs(Integer productId) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            return Result.failure("商品不存在");
        }

        List<Sku> skus = skuMapper.selectList(
                new LambdaQueryWrapper<Sku>().eq(Sku::getProductId, product.getId())
        );

        List<SkuSpecVo> skuVos = buildSkuSpecVos(skus);
        List<SpecGroupVo> groups = buildSpecGroups(product.getCategoryId(), skuVos);

        ProductSpecsVo vo = new ProductSpecsVo();
        vo.setProductId(product.getId());
        vo.setSpecGroups(groups);
        vo.setSkus(skuVos);
        return Result.success(vo);
    }

    private List<SkuSpecVo> buildSkuSpecVos(List<Sku> skus) {
        if (skus == null || skus.isEmpty()) {
            return new ArrayList<>();
        }
        List<SkuSpecVo> list = new ArrayList<>();
        for (Sku sku : skus) {
            SkuSpecVo vo = new SkuSpecVo();
            vo.setId(sku.getId());
            vo.setName(sku.getName());
            vo.setPrice(sku.getPrice());
            vo.setStock(sku.getStock());
            vo.setImage(sku.getImage());
            vo.setSpecs(parseSpecsMap(sku.getSpecs()));
            list.add(vo);
        }
        return list;
    }

    private Map<String, String> parseSpecsMap(String specs) {
        if (!StringUtils.hasText(specs)) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(specs, new TypeReference<Map<String, String>>() {
            });
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    private String normalizeSpecsJson(String specs) {
        if (!StringUtils.hasText(specs)) {
            return "{}";
        }
        try {
            Map<String, Object> map = objectMapper.readValue(specs, new TypeReference<Map<String, Object>>() {
            });
            if (map == null || map.isEmpty()) {
                return "{}";
            }
            Map<String, String> normalized = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String k = entry.getKey();
                Object v = entry.getValue();
                if (!StringUtils.hasText(k) || v == null) {
                    continue;
                }
                String sv = String.valueOf(v);
                if (!StringUtils.hasText(sv)) {
                    continue;
                }
                normalized.put(k, sv);
            }
            if (normalized.isEmpty()) {
                return "{}";
            }
            return objectMapper.writeValueAsString(normalized);
        } catch (Exception e) {
            return "{}";
        }
    }

    private List<SpecGroupVo> buildSpecGroups(Long categoryId, List<SkuSpecVo> skuVos) {
        List<CategorySpec> relations = categorySpecMapper.selectList(
                new LambdaQueryWrapper<CategorySpec>()
                        .eq(CategorySpec::getCategoryId, categoryId)
                        .orderByAsc(CategorySpec::getSort)
        );
        if (relations == null || relations.isEmpty()) {
            Category category = categoryMapper.selectById(categoryId);
            if (category != null && category.getParentId() != null && category.getParentId() > 0) {
                relations = categorySpecMapper.selectList(
                        new LambdaQueryWrapper<CategorySpec>()
                                .eq(CategorySpec::getCategoryId, category.getParentId())
                                .orderByAsc(CategorySpec::getSort)
                );
            }
        }
        if (relations == null || relations.isEmpty()) {
            return inferSpecGroupsFromSkus(skuVos);
        }

        List<Long> specIds = relations.stream().map(CategorySpec::getSpecId).distinct().collect(Collectors.toList());
        List<Spec> specs = specMapper.selectBatchIds(specIds);
        Map<Long, Spec> specById = specs == null ? new LinkedHashMap<>() : specs.stream()
                .collect(Collectors.toMap(Spec::getId, s -> s, (a, b) -> a, LinkedHashMap::new));

        List<SpecOption> options = specOptionMapper.selectList(
                new LambdaQueryWrapper<SpecOption>()
                        .in(SpecOption::getSpecId, specIds)
                        .orderByAsc(SpecOption::getSort)
        );
        Map<Long, List<SpecOption>> optionsBySpecId = options == null ? new LinkedHashMap<>() : options.stream()
                .collect(Collectors.groupingBy(SpecOption::getSpecId, LinkedHashMap::new, Collectors.toList()));

        List<SpecGroupVo> groups = new ArrayList<>();
        for (CategorySpec relation : relations) {
            Spec spec = specById.get(relation.getSpecId());
            if (spec == null) {
                continue;
            }
            SpecGroupVo group = new SpecGroupVo();
            group.setKey(spec.getSpecCode());
            group.setTitle(spec.getSpecName());
            group.setDisplayType(spec.getDisplayType());

            List<SpecOption> optList = optionsBySpecId.getOrDefault(spec.getId(), new ArrayList<>());
            List<SpecOptionVo> optVos;
            List<SpecOptionVo> inferredOptVos = inferSpecOptionsFromSkus(spec.getSpecCode(), skuVos);
            if (inferredOptVos != null && !inferredOptVos.isEmpty()) {
                if (optList == null || optList.isEmpty()) {
                    optVos = inferredOptVos;
                } else {
                    Map<String, SpecOption> optionByValueOrLabel = new HashMap<>();
                    for (SpecOption opt : optList) {
                        if (opt == null) {
                            continue;
                        }
                        if (StringUtils.hasText(opt.getOptionValue())) {
                            optionByValueOrLabel.put(opt.getOptionValue(), opt);
                        }
                        if (StringUtils.hasText(opt.getOptionLabel())) {
                            optionByValueOrLabel.put(opt.getOptionLabel(), opt);
                        }
                    }
                    List<SpecOptionVo> merged = new ArrayList<>();
                    for (SpecOptionVo inferred : inferredOptVos) {
                        if (inferred == null || !StringUtils.hasText(inferred.getValue())) {
                            continue;
                        }
                        SpecOptionVo optVo = new SpecOptionVo();
                        optVo.setValue(inferred.getValue());
                        optVo.setLabel(inferred.getLabel());
                        SpecOption raw = optionByValueOrLabel.get(inferred.getValue());
                        if (raw != null && StringUtils.hasText(raw.getImage())) {
                            optVo.setImage(raw.getImage());
                        }
                        merged.add(optVo);
                    }
                    optVos = merged;
                }
            } else if (optList == null || optList.isEmpty()) {
                optVos = new ArrayList<>();
            } else {
                optVos = optList.stream().map(opt -> {
                    SpecOptionVo optVo = new SpecOptionVo();
                    optVo.setValue(opt.getOptionValue());
                    optVo.setLabel(opt.getOptionLabel());
                    optVo.setImage(opt.getImage());
                    return optVo;
                }).collect(Collectors.toList());
            }
            group.setOptions(optVos);
            groups.add(group);
        }
        return groups;
    }

    private List<SpecOptionVo> inferSpecOptionsFromSkus(String specCode, List<SkuSpecVo> skuVos) {
        if (!StringUtils.hasText(specCode) || skuVos == null || skuVos.isEmpty()) {
            return new ArrayList<>();
        }
        Set<String> values = new HashSet<>();
        for (SkuSpecVo sku : skuVos) {
            Map<String, String> map = sku.getSpecs();
            if (map == null) {
                continue;
            }
            String v = map.get(specCode);
            if (StringUtils.hasText(v)) {
                values.add(v);
            }
        }
        List<String> sortedValues = new ArrayList<>(values);
        sortedValues.sort(String::compareTo);
        return sortedValues.stream().map(v -> {
            SpecOptionVo opt = new SpecOptionVo();
            opt.setValue(v);
            opt.setLabel(v);
            return opt;
        }).collect(Collectors.toList());
    }

    private List<SpecGroupVo> inferSpecGroupsFromSkus(List<SkuSpecVo> skuVos) {
        Set<String> keys = new HashSet<>();
        Map<String, Set<String>> valuesByKey = new LinkedHashMap<>();
        for (SkuSpecVo sku : skuVos) {
            Map<String, String> map = sku.getSpecs();
            if (map == null) {
                continue;
            }
            for (Map.Entry<String, String> entry : map.entrySet()) {
                String k = entry.getKey();
                String v = entry.getValue();
                if (!StringUtils.hasText(k) || !StringUtils.hasText(v)) {
                    continue;
                }
                keys.add(k);
                valuesByKey.computeIfAbsent(k, kk -> new HashSet<>()).add(v);
            }
        }

        List<String> orderedKeys = new ArrayList<>(keys);
        List<String> defaultOrder = List.of("version", "color", "disk", "ram", "storage", "size", "package");
        orderedKeys.sort(Comparator.<String>comparingInt(k -> {
            int idx = defaultOrder.indexOf(k);
            return idx < 0 ? 999 : idx;
        }).thenComparing(Comparator.naturalOrder()));

        List<SpecGroupVo> groups = new ArrayList<>();
        Map<String, String> titleMap = new LinkedHashMap<>();
        titleMap.put("version", "版本");
        titleMap.put("color", "颜色分类");
        titleMap.put("disk", "硬盘容量");
        titleMap.put("ram", "内存容量");
        titleMap.put("storage", "存储容量");
        titleMap.put("size", "尺码");
        titleMap.put("package", "套餐类型");
        for (String key : orderedKeys) {
            SpecGroupVo group = new SpecGroupVo();
            group.setKey(key);
            group.setTitle(titleMap.getOrDefault(key, key));
            group.setDisplayType(key.toLowerCase().contains("color") ? "list" : "chip");
            Set<String> values = valuesByKey.getOrDefault(key, new HashSet<>());
            List<String> sortedValues = new ArrayList<>(values);
            sortedValues.sort(String::compareTo);
            List<SpecOptionVo> options = sortedValues.stream().map(v -> {
                SpecOptionVo opt = new SpecOptionVo();
                opt.setValue(v);
                opt.setLabel(v);
                return opt;
            }).collect(Collectors.toList());
            group.setOptions(options);
            groups.add(group);
        }
        return groups;
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

    @Override
    public Result<Sku> getSku(Long skuId) {
        if (skuId == null) {
            return Result.failure("skuId不能为空");
        }
        Sku sku = skuMapper.selectById(skuId);
        if (sku == null) {
            return Result.failure("SKU不存在");
        }
        return Result.success(sku);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> decreaseSkuStock(Long skuId, Integer quantity) {
        if (skuId == null) {
            return Result.failure("skuId不能为空");
        }
        if (quantity == null || quantity <= 0) {
            return Result.failure("quantity不合法");
        }

        Sku sku = skuMapper.selectById(skuId);
        if (sku == null) {
            return Result.failure("SKU不存在");
        }

        LambdaUpdateWrapper<Sku> updateWrapper = new LambdaUpdateWrapper<Sku>()
                .eq(Sku::getId, skuId)
                .ge(Sku::getStock, quantity)
                .setSql("stock = stock - " + quantity);

        int updated = skuMapper.update(null, updateWrapper);
        if (updated <= 0) {
            return Result.failure("库存不足");
        }

        String cacheKey = "product:detail:" + sku.getProductId();
        stringRedisTemplate.delete(cacheKey);

        return Result.success(null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> increaseSkuStock(Long skuId, Integer quantity) {
        if (skuId == null) {
            return Result.failure("skuId不能为空");
        }
        if (quantity == null || quantity <= 0) {
            return Result.failure("quantity不合法");
        }

        Sku sku = skuMapper.selectById(skuId);
        if (sku == null) {
            return Result.failure("SKU不存在");
        }

        LambdaUpdateWrapper<Sku> updateWrapper = new LambdaUpdateWrapper<Sku>()
                .eq(Sku::getId, skuId)
                .setSql("stock = stock + " + quantity);

        int updated = skuMapper.update(null, updateWrapper);
        if (updated <= 0) {
            return Result.failure("回补库存失败");
        }

        String cacheKey = "product:detail:" + sku.getProductId();
        stringRedisTemplate.delete(cacheKey);

        return Result.success(null);
    }
}
