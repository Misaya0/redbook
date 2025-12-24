package com.itcast;

import com.itcast.mapper.SkuMapper;
import com.itcast.model.pojo.CustomAttribute;
import com.itcast.model.pojo.ProductAttribute;
import com.itcast.model.pojo.Sku;
import com.itcast.model.vo.ProductVo;
import com.itcast.result.Result;
import com.itcast.service.ProductService;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

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

    @Autowired
    private SkuMapper skuMapper; // 注入数据库操作接口

    @Autowired
    private StringRedisTemplate stringRedisTemplate; // 注入Redis操作工具
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
    private static final String STOCK_PREFIX = "product:stock:";
    @Test
    public void syncStockToRedis() {
        System.out.println("========== 开始同步库存数据到 Redis ==========");

        // 1. 从数据库查询所有 SKU 信息
        // 如果数据量特别大（比如几十万），建议分批查询，不要一次全查出来
        List<Sku> skuList = skuMapper.selectList(null);

        if (skuList == null || skuList.isEmpty()) {
            System.out.println("数据库中没有商品数据，无需同步。");
            return;
        }

        System.out.println("查询到 SKU 数量: " + skuList.size());

        // 2. 使用 Pipeline (管道) 批量写入 Redis
        // Pipeline 的好处是把几千条命令打包发给 Redis，只要一次网络往返，速度极快
        stringRedisTemplate.executePipelined(new SessionCallback<Object>() {
            @Override
            public <K, V> Object execute(RedisOperations<K, V> operations) throws DataAccessException {
                for (Sku sku : skuList) {
                    // 构造 Key：product:stock:1001
                    String key = STOCK_PREFIX + sku.getId();

                    // 获取库存，防止为 null
                    Integer stock = sku.getStock() == null ? 0 : sku.getStock();

                    // 写入 Redis (String类型)
                    // operations.opsForValue().set((K) key, (V) String.valueOf(stock));
                    // 更加稳妥的写法（直接用 stringRedisTemplate 的操作对象）
                    stringRedisTemplate.opsForValue().set(key, String.valueOf(stock));
                }
                // 返回 null 即可
                return null;
            }
        });

        System.out.println("========== 库存同步完成！已写入 Redis ==========");

        // 3. (可选) 随机抽查一个打印出来验证一下
        if (!skuList.isEmpty()) {
            Long checkId = skuList.get(0).getId();
            String redisVal = stringRedisTemplate.opsForValue().get(STOCK_PREFIX + checkId);
            System.out.println("验证数据 -> SKU ID: " + checkId + ", Redis中库存: " + redisVal);
        }
    }
}
