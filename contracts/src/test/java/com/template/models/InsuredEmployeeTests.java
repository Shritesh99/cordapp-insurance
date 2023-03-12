package com.template.models;

import org.junit.Test;

public class InsuredEmployeeTests {
    @Test
    public void InsuredEmployeeHasFieldOfCorrectType() throws NoSuchFieldException {
        InsuredEmployee.class.getDeclaredField("id");
        assert (InsuredEmployee.class.getDeclaredField("id").getType().equals(String.class));

        InsuredEmployee.class.getDeclaredField("insurance");
        assert (InsuredEmployee.class.getDeclaredField("insurance").getType().equals(Insurance.class));

        InsuredEmployee.class.getDeclaredField("employee");
        assert (InsuredEmployee.class.getDeclaredField("employee").getType().equals(Employee.class));

//        InsuredEmployee.class.getDeclaredField("organization");
//        assert (InsuredEmployee.class.getDeclaredField("organization").getType().equals(Organization.class));
    }
}
