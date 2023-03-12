package com.template.models;

import org.junit.Test;

import java.lang.reflect.Field;
import java.time.LocalDate;

import static org.junit.Assert.assertEquals;

public class InsuranceTests {
    @Test
    public void InsuranceHasFieldOfCorrectType() throws NoSuchFieldException {
        Field idField = Insurance.class.getDeclaredField("id");
        assertEquals(String.class, idField.getType());

        Field policyNumberField = Insurance.class.getDeclaredField("policyNumber");
        assertEquals(String.class, policyNumberField.getType());

        Field customerField = Insurance.class.getDeclaredField("employee");
        assertEquals(EmployeeTests.class, customerField.getType());

        Field startDateField = Insurance.class.getDeclaredField("startDate");
        assertEquals(LocalDate.class, startDateField.getType());

        Field endDateField = Insurance.class.getDeclaredField("endDate");
        assertEquals(LocalDate.class, endDateField.getType());
    }
}
