package com.template.models;

import net.corda.core.serialization.CordaSerializable;

@CordaSerializable
public class InsuredEmployee {

    private String id;
    private Insurance insurance;
    private Employee employee;
//    private Organization organization;

    public InsuredEmployee(String id, Insurance insurance, Employee employee) {
        this.id = id;
        this.insurance = insurance;
        this.employee = employee;
//        this.organization = organization;
    }

    public String getId() {
        return id;
    }

    public Insurance getInsurance() {
        return insurance;
    }

    public Employee getEmployee() {
        return employee;
    }

//    public Organization getOrganization() {
//        return organization;
//    }
}
