package com.template.models;

import org.junit.Test;

public class OrganizationTests {
    @Test
    public void OrganizationHasFieldOfCorrectType() throws NoSuchFieldException {
        Employee.class.getDeclaredField("id");
        assert (Employee.class.getDeclaredField("id").getType().equals(String.class));

        Employee.class.getDeclaredField("name");
        assert (Employee.class.getDeclaredField("name").getType().equals(String.class));
    }
}
