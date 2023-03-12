package com.template.models;
import java.util.ArrayList;
import java.util.List;

public class Organization {
    private final String id;
    private String name;
    private List<Employee> employees;

    public Organization(String id, String name) {
        this.id = id;
        this.name = name;
        this.employees = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public List<Employee> getEmployees() {
        return employees;
    }

    public void addEmployee(Employee employee) {
        this.employees.add(employee);
    }

    public void removeEmployee(Employee employee) {
        this.employees.remove(employee);
    }
    @Override
    public String toString() {
        return "Organization{" +
                "id='" + id + '\'' +
                "name='" + name + '\'' +
        '}';
    }
}