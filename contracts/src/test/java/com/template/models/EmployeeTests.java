package com.template.models;

import org.junit.Test;

import java.time.LocalDate;

public class EmployeeTests {
    @Test
    public void EmployeeHasFieldOfCorrectType() throws NoSuchFieldException {
        Employee.class.getDeclaredField("id");
        assert (Employee.class.getDeclaredField("id").getType().equals(String.class));

        Employee.class.getDeclaredField("name");
        assert (Employee.class.getDeclaredField("name").getType().equals(String.class));

        Employee.class.getDeclaredField("empId");
        assert (Employee.class.getDeclaredField("empId").getType().equals(String.class));

        Employee.class.getDeclaredField("designation");
        assert (Employee.class.getDeclaredField("designation").getType().equals(String.class));

        Employee.class.getDeclaredField("address");
        assert (Employee.class.getDeclaredField("empId").getType().equals(String.class));

        Employee.class.getDeclaredField("dob");
        assert (Employee.class.getDeclaredField("empId").getType().equals(LocalDate.class));

        Employee.class.getDeclaredField("medicalId");
        assert (Employee.class.getDeclaredField("medicalId").getType().equals(String.class));
    }
}
