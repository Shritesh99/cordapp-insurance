package com.template.contracts;

import com.template.models.Employee;
import com.template.models.Insurance;
import com.template.models.InsuranceTests;
import com.template.states.InsuranceState;
import net.corda.core.identity.Party;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;

public class InsuranceStateTests {
    @Test
    public void hasInsuranceCompanyFieldOfCorrectType() throws NoSuchFieldException{
        InsuranceState.class.getDeclaredField("insuranceCompany");
        assert (InsuranceState.class.getDeclaredField("insuranceCompany").getType().equals(Party.class));
    }
    @Test
    public void hasCustomerFieldOfCorrectType() throws NoSuchFieldException{
        InsuranceState.class.getDeclaredField("organization");
        assert (InsuranceState.class.getDeclaredField("organization").getType().equals(Party.class));
    }
    @Test
    public void hasHospitalFieldOfCorrectType() throws NoSuchFieldException{
        InsuranceState.class.getDeclaredField("hospital");
        assert (InsuranceState.class.getDeclaredField("hospital").getType().equals(Party.class));
    }

//    @Test
//    public void hasInsuredEmployeesFieldOfCorrectType() throws NoSuchFieldException {
//        InsuranceState.class.getDeclaredField("insuredEmployees").equals();
//        assert (InsuranceState.class.getDeclaredField("insuredEmployees").equals(HashMap.class));
//    }
}
