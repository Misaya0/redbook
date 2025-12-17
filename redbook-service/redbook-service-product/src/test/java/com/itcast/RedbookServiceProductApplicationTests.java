package com.itcast;

import com.itcast.model.pojo.CustomAttribute;
import com.itcast.model.pojo.ProductAttribute;
import com.itcast.model.vo.ProductVo;
import com.itcast.result.Result;
import com.itcast.service.ProductService;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@SpringBootTest
class RedbookServiceProductApplicationTests {

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private ProductService productService;

    @Test
    void contextLoads() {
    }

    private static int stock = 50;

    @Test
    void testRedission() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(100);
        for (int i = 0; i < 100; i++) {
            new Thread(() -> {
                RLock lock = redissonClient.getLock("itcast");
                try {
                    boolean res = lock.tryLock(100, 10, TimeUnit.SECONDS);
                    if (res) {
                        System.out.println(Thread.currentThread().getName() + "获取锁成功");
                        if (stock > 0) {
                            stock -= 1;
                        }
                    } else {
                        System.out.println(Thread.currentThread().getName() + "获取锁失败");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    lock.unlock();
                }
                latch.countDown();
            }).start();
        }

        latch.await();
        System.out.println("所有线程完成");
        System.out.println(stock);
    }

    @Test
    void testPostProductV2() {
        com.itcast.model.dto.ProductDto dto = new com.itcast.model.dto.ProductDto();
        dto.setName("测试商品iPhone 15");
        dto.setTitle("A17 Pro芯片 | 钛金属边框");
        dto.setMainImage("http://oss.aliyun.com/iphone15.jpg");
        dto.setShopId(1L);
        dto.setCategoryId(1001L);
        dto.setPrice(7999.00); // 起售价
        
        // 构建 SKU
        List<com.itcast.model.pojo.Sku> skus = new ArrayList<>();
        
        com.itcast.model.pojo.Sku sku1 = new com.itcast.model.pojo.Sku();
        sku1.setName("iPhone 15 Pro 黑色钛金属 256G");
        sku1.setPrice(new java.math.BigDecimal("8999.00"));
        sku1.setStock(100);
        sku1.setSpecs("{\"color\":\"黑色钛金属\", \"storage\":\"256G\"}");
        skus.add(sku1);
        
        com.itcast.model.pojo.Sku sku2 = new com.itcast.model.pojo.Sku();
        sku2.setName("iPhone 15 Pro 白色钛金属 512G");
        sku2.setPrice(new java.math.BigDecimal("10999.00"));
        sku2.setStock(50);
        sku2.setSpecs("{\"color\":\"白色钛金属\", \"storage\":\"512G\"}");
        skus.add(sku2);
        
        dto.setSkus(skus);
        
        // 构建 MongoDB 属性 (兼容)
        List<CustomAttribute> attrs = new ArrayList<>();
        CustomAttribute attr1 = new CustomAttribute();
        attr1.setLabel("网络类型");
        attr1.setValue(Arrays.asList("5G", "4G"));
        attrs.add(attr1);
        
        ProductAttribute productAttribute = new ProductAttribute();
        productAttribute.setCustomAttributes(attrs);
        dto.setProductAttribute(productAttribute);
        
        productService.postProduct(dto);
        System.out.println("发布商品成功，请检查数据库");
    }

    @Test
    void testGetProduct() {
        Result<ProductVo> product = productService.getProduct(2);
        if (product.getData() != null) {
            ProductVo productVo = product.getData();
            System.out.println("SPU名称: " + productVo.getName());
            if (productVo.getCustomAttributes() != null) {
                System.out.println("属性: " + productVo.getCustomAttributes().toString());
            }
            if (productVo.getSkus() != null) {
                System.out.println("SKU数量: " + productVo.getSkus().size());
                productVo.getSkus().forEach(sku -> System.out.println("SKU: " + sku.getName() + ", 价格: " + sku.getPrice()));
            }
        } else {
            System.out.println("商品不存在");
        }
    }
}
