package com.template.models;

import net.corda.core.serialization.ConstructorForDeserialization;
import net.corda.core.serialization.CordaSerializable;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@CordaSerializable
public class Employee {
    private final String id;
    private final String empId;
    final private String designation;
    private final String name;
    private final String address;
    private final String dob;
    private final String medicalId;
//    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d/MM/yyyy");
    @ConstructorForDeserialization
    public Employee(String id, String empId,  String designation, String name, String address, String dob, String medicalId) {
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

    public String getDob() {
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