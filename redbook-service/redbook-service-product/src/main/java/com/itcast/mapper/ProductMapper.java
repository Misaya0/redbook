package com.itcast.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itcast.model.pojo.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    /**
     * 原子递增商品销量
     */
    @Update("UPDATE rb_product SET sales = sales + #{num} WHERE id = #{id}")
    int increaseSales(@Param("id") Long id, @Param("num") Integer num);
}
