import request from './request';

export interface Order {
    id?: number;
    productId: number;
    quantity: number;
    couponId?: number;
    finalPrice: number;
    userId: number;
    status: number;
    createTime?: string;
}

export interface OrderVo extends Order {
    productName: string;
    productImage: string;
}

export interface OrderSearchDto {
    pageNum: number;
    pageSize: number;
    status?: number;
    startTime?: string;
    endTime?: string;
    orderId?: number;
    userId?: number;
}

export const saveOrder = (data: Order) => {
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

export const updateOrderStatus = (orderId: number, status: number) => {
    return request({
        url: `/order/status/${orderId}`,
        method: 'put',
        params: { status }
    });
};
