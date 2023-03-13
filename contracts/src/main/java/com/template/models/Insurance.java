package com.template.models;

import net.corda.core.serialization.ConstructorForDeserialization;
import net.corda.core.serialization.CordaSerializable;


import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@CordaSerializable
public class Insurance {
    private final String id;
    private final String policyNumber;
    private final String startDate;
    private final String endDate;
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d/MM/yyyy");
    @ConstructorForDeserialization
    public Insurance(String id, String policyNumber, String startDate, String endDate) {
        this.id = id;
        this.policyNumber = policyNumber;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public String getId() {
        return id;
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public String getStartDate() {
        return startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    @Override
    public String toString() {
        return "Insurance{" +
                "id='" + id + '\'' +
                ", policyNumber='" + policyNumber + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                '}';
    }
}