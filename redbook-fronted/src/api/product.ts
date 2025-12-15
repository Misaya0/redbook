import request from './request';

export interface Product {
    id?: number;
    name: string;
    price: number;
    image: string;
    time?: string;
    sales?: number;
    userId?: number;
    shopId?: number;
    stock: number;
}

export interface ProductVo extends Product {
    shop?: any;
    customAttributes?: any[];
}

export interface ProductSearchDto {
    pageNum: number;
    pageSize: number;
    keyword?: string;
    minPrice?: number;
    maxPrice?: number;
    shopId?: number;
}

export const getProductList = (pageNum: number = 1, pageSize: number = 10) => {
    return request({
        url: '/product/getProductList',
        method: 'get',
        params: { pageNum, pageSize }
    });
};

export const getProduct = (id: number) => {
    return request({
        url: `/product/getProduct/${id}`,
        method: 'get'
    });
};

export const postProduct = (data: Product) => {
    return request({
        url: '/product/postProduct',
        method: 'post',
        data
    });
};

export const updateProduct = (data: Product) => {
    return request({
        url: '/product/updateProduct',
        method: 'put',
        data
    });
};

export const deleteProduct = (productId: number) => {
    return request({
        url: `/product/delete/${productId}`,
        method: 'delete'
    });
};

export const searchProduct = (data: ProductSearchDto) => {
    return request({
        url: '/product/search',
        method: 'post',
        data
    });
};
