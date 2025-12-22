// @ts-ignore
import request from './request.js';

export interface Order {
    id?: number;
    productId: number;
    skuId?: number;
    quantity: number;
    couponId?: number;
    skuPrice?: number;
    finalPrice: number;
    userId: number;
    status: number;
    createTime?: string;
    payTime?: string;
}

export interface OrderVo extends Order {
    productName: string;
    productImage: string;
    skuSpec?: string;
    storeName?: string;
    storeAvatar?: string;
    buyerName?: string;
    buyerAvatar?: string;
    buyerPhone?: string;
    merchantMemo?: string;
}

export interface OrderCreateRequest {
    skuId: number;
    quantity?: number;
    couponId?: number;
    couponPrice?: number;
    selectAttributes?: Array<{ label: string; value: string[] }>;
}

export interface OrderSearchDto {
    pageNum?: number;
    pageSize?: number;
    status?: number;
    startTime?: string;
    endTime?: string;
    orderId?: number;
    userId?: number;
    keyword?: string;
}

export const saveOrder = (data: OrderCreateRequest) => {
    return request({
        url: '/order/saveOrder',
        method: 'post',
        data
    });
};

export const buyOrder = (orderId: number) => {
    return request({
        url: `/order/buyOrder/${orderId}`,
        method: 'put'
    });
};

export const getOrderList = () => {
    return request({
        url: '/order/getOrderList',
        method: 'get'
    });
};

export const searchOrders = (data: OrderSearchDto) => {
    return request({
        url: '/order/search',
        method: 'post',
        data
    });
};

export const getOrderDetail = (orderId: number) => {
    return request({
        url: `/order/detail/${orderId}`,
        method: 'get'
    });
};

export interface OrderStatisticsVo {
    totalOrders: number;
    totalAmount: number;
    pendingPayment: number;
    paid: number;
    shipped: number;
    completed: number;
    cancelled: number;
}

export const getStatistics = () => {
    return request({
        url: '/order/statistics',
        method: 'get'
    });
};

export const getServerTime = () => {
    return request({
        url: '/order/serverTime',
        method: 'get',
        // 用于前端倒计时同步，失败时不应频繁弹窗
        skipErrorHandler: true
    });
};

export const getPaymentTimeoutSeconds = () => {
    return request({
        url: '/order/paymentTimeoutSeconds',
        method: 'get',
        // 用于前端倒计时配置，失败时允许使用默认值
        skipErrorHandler: true
    });
};

export const timeoutCancelOrder = (orderId: number) => {
    return request({
        url: `/order/timeoutCancel/${orderId}`,
        method: 'put',
        // 轮询/到期触发可能频繁调用，失败时不应频繁弹窗
        skipErrorHandler: true
    });
};

export const updateOrderStatus = (orderId: number, status: number) => {
    return request({
        url: `/order/status/${orderId}`,
        method: 'put',
        params: { status }
    });
};
