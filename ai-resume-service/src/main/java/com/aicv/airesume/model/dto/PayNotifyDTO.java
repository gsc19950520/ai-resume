package com.aicv.airesume.model.dto;

/**
 * 支付回调DTO
 */
public class PayNotifyDTO {
    
    private String returnCode;
    private String returnMsg;
    private String appid;
    private String mchId;
    private String nonceStr;
    private String sign;
    private String resultCode;
    private String openid;
    private String isSubscribe;
    private String tradeType;
    private String bankType;
    private Integer totalFee;
    private Integer settlementTotalFee;
    private String feeType;
    private String cashFee;
    private String cashFeeType;
    private String couponFee;
    private Integer couponCount;
    private String transactionId;
    private String outTradeNo;
    private String timeEnd;
    
    public String getReturnCode() {
        return returnCode;
    }
    
    public void setReturnCode(String returnCode) {
        this.returnCode = returnCode;
    }
    
    public String getReturnMsg() {
        return returnMsg;
    }
    
    public void setReturnMsg(String returnMsg) {
        this.returnMsg = returnMsg;
    }
    
    public String getAppid() {
        return appid;
    }
    
    public void setAppid(String appid) {
        this.appid = appid;
    }
    
    public String getMchId() {
        return mchId;
    }
    
    public void setMchId(String mchId) {
        this.mchId = mchId;
    }
    
    public String getNonceStr() {
        return nonceStr;
    }
    
    public void setNonceStr(String nonceStr) {
        this.nonceStr = nonceStr;
    }
    
    public String getSign() {
        return sign;
    }
    
    public void setSign(String sign) {
        this.sign = sign;
    }
    
    public String getResultCode() {
        return resultCode;
    }
    
    public void setResultCode(String resultCode) {
        this.resultCode = resultCode;
    }
    
    public String getOpenid() {
        return openid;
    }
    
    public void setOpenid(String openid) {
        this.openid = openid;
    }
    
    public String getIsSubscribe() {
        return isSubscribe;
    }
    
    public void setIsSubscribe(String isSubscribe) {
        this.isSubscribe = isSubscribe;
    }
    
    public String getTradeType() {
        return tradeType;
    }
    
    public void setTradeType(String tradeType) {
        this.tradeType = tradeType;
    }
    
    public String getBankType() {
        return bankType;
    }
    
    public void setBankType(String bankType) {
        this.bankType = bankType;
    }
    
    public Integer getTotalFee() {
        return totalFee;
    }
    
    public void setTotalFee(Integer totalFee) {
        this.totalFee = totalFee;
    }
    
    public Integer getSettlementTotalFee() {
        return settlementTotalFee;
    }
    
    public void setSettlementTotalFee(Integer settlementTotalFee) {
        this.settlementTotalFee = settlementTotalFee;
    }
    
    public String getFeeType() {
        return feeType;
    }
    
    public void setFeeType(String feeType) {
        this.feeType = feeType;
    }
    
    public String getCashFee() {
        return cashFee;
    }
    
    public void setCashFee(String cashFee) {
        this.cashFee = cashFee;
    }
    
    public String getCashFeeType() {
        return cashFeeType;
    }
    
    public void setCashFeeType(String cashFeeType) {
        this.cashFeeType = cashFeeType;
    }
    
    public String getCouponFee() {
        return couponFee;
    }
    
    public void setCouponFee(String couponFee) {
        this.couponFee = couponFee;
    }
    
    public Integer getCouponCount() {
        return couponCount;
    }
    
    public void setCouponCount(Integer couponCount) {
        this.couponCount = couponCount;
    }
    
    public String getTransactionId() {
        return transactionId;
    }
    
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }
    
    public String getOutTradeNo() {
        return outTradeNo;
    }
    
    public void setOutTradeNo(String outTradeNo) {
        this.outTradeNo = outTradeNo;
    }
    
    public String getTimeEnd() {
        return timeEnd;
    }
    
    public void setTimeEnd(String timeEnd) {
        this.timeEnd = timeEnd;
    }
}