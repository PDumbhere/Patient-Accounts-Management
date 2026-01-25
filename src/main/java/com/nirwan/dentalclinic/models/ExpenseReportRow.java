package com.nirwan.dentalclinic.models;

import javafx.beans.property.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ExpenseReportRow {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final ObjectProperty<LocalDate> paymentDate = new SimpleObjectProperty<>();
    private final StringProperty expenseType = new SimpleStringProperty();
    private final StringProperty product = new SimpleStringProperty();
    private final StringProperty nameTo = new SimpleStringProperty();
    private final DoubleProperty amount = new SimpleDoubleProperty();

    public Integer getId(){
        return id.get();
    }
    public void setId(Integer id){
        this.id.set(id);
    }
    public LocalDate getPaymentDate(){
        return paymentDate.get();
    }
    public void setPaymentDate(LocalDate paymentDate){
        this.paymentDate.set(paymentDate);
    }
    public String getExpenseType(){
        return expenseType.get();
    }
    public void setExpenseType(String expenseType){
        this.expenseType.set(expenseType);
    }
    public String getProduct(){
        return product.get();
    }
    public void setProduct(String product){
        this.product.set(product);
    }
    public String getNameTo(){
        return nameTo.get();
    }
    public void setNameTo(String nameTo){
        this.nameTo.set(nameTo);
    }
    public Double getAmount(){
        return amount.get();
    }
    public void setAmount(Double amount){
        this.amount.set(amount);
    }
}
