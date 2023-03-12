package com.template.models;

import net.corda.core.serialization.CordaSerializable;

import java.time.LocalDate;

@CordaSerializable
public class Employee {
    private final String id;
    private final String empId;
    private String designation;
    private final String name;
    private final String address;
    private final LocalDate dob;
    private final String medicalId;

    public Employee(String id, String empId, String name, String address, LocalDate dob, String medicalId, String designation) {
        this.id = id;
        this.empId = empId;
        this.name = name;
        this.designation = designation;
        this.address = address;
        this.dob = dob;
        this.medicalId = medicalId;
    }

    public String getId() {
        return id;
    }

    public String getEmpId() {
        return empId;
    }

    public String getName() {
        return name;
    }

    public String getDesignation() {
        return designation;
    }

    public String getAddress() {
        return address;
    }

    public LocalDate getDob() {
        return dob;
    }

    public String getMedicalId() {
        return medicalId;
    }

    @Override
    public String toString() {
        return "Employee{" +
                "id='" + id + '\'' +
                "empId='" + empId + '\'' +
                "name='" + name + '\'' +
                "designation='" + designation + '\'' +
                ", address='" + address + '\'' +
                ", dob=" + dob +
                ", medicalId='" + medicalId + '\'' +
                '}';
    }
}