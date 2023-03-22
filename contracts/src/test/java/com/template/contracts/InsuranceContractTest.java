package com.template.contracts;

import com.template.models.Employee;
import com.template.models.Insurance;
import com.template.models.InsuredEmployee;
import com.template.models.Organization;
import com.template.states.InsuranceState;
import net.corda.core.identity.CordaX500Name;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockServices;
import org.junit.Before;
import org.junit.Test;
import java.util.Arrays;
import java.util.LinkedHashMap;

import static net.corda.testing.node.NodeTestUtils.ledger;

public class InsuranceContractTest {

    private final MockServices ledgerServices = new MockServices(Arrays.asList("com.template"));
    TestIdentity organization;
    TestIdentity insuranceCompany;

    String empId;
    int amount;
    int claimed;
    String policyNo;
    TestIdentity hospital;
    @Before
    public void init(){
        organization = new TestIdentity(new CordaX500Name("ABC Inc..",  "London",  "GB"));
        insuranceCompany = new TestIdentity(new CordaX500Name("XYZ Insurance Co.",  "New York",  "US"));
        hospital = new TestIdentity(new CordaX500Name("PQR Hospitals",  "Mumbai",  "IN"));

        empId = "123";
        amount = 2000;
        claimed = 111;
        policyNo = "1112";
    }

}
