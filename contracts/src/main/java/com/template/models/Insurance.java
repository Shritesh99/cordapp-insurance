package com.template.models;

import net.corda.core.serialization.CordaSerializable;

import java.time.LocalDate;
@CordaSerializable
public class Insurance {
    private final String id;
    private final String policyNumber;
    private final Employee employee;
    private final LocalDate startDate;
    private final LocalDate endDate;

    public Insurance(String id, String policyNumber, Employee employee, LocalDate startDate, LocalDate endDate) {
        this.id = id;
        this.policyNumber = policyNumber;
        this.employee = employee;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public String getId() {
        return id;
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public Employee getEmployee() {
        return employee;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    @Override
    public String toString() {
        return "Insurance{" +
                "id='" + id + '\'' +
                ", policyNumber='" + policyNumber + '\'' +
                ", employee=" + employee.getName() +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                '}';
    }
}