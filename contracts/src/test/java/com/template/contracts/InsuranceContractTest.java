package com.template.contracts;

import com.template.models.Employee;
import com.template.models.Insurance;
import com.template.models.InsuredEmployee;
import com.template.models.Organization;
import com.template.states.InsuranceState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.CordaX500Name;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockServices;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.BeforeAll;

import java.time.LocalDate;
import java.time.Month;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;

import static net.corda.testing.node.NodeTestUtils.ledger;

public class InsuranceContractTest {

    private final MockServices ledgerServices = new MockServices(Arrays.asList("com.template"));
    TestIdentity organization;
    TestIdentity insuranceCompany;

    Organization org;
    Employee emp;
    Insurance insurance;
    InsuredEmployee insuredEmployee;

    TestIdentity hospital;
    @Before
    public void init(){
        organization = new TestIdentity(new CordaX500Name("ABC Inc..",  "London",  "GB"));
        insuranceCompany = new TestIdentity(new CordaX500Name("XYZ Insurance Co.",  "New York",  "US"));
        hospital = new TestIdentity(new CordaX500Name("PQR Hospitals",  "Mumbai",  "IN"));

        org = new Organization("2121", "ABC Inc.");
        emp = new Employee("12121","1222", "Test", "ghar", LocalDate.of(1997, Month.AUGUST, 23), "11122", "SWE");
        insurance = new Insurance("122", "2332", emp, LocalDate.now(), LocalDate.now().plusYears(10));
        org.addEmployee(emp);
        insuredEmployee = new InsuredEmployee("1234", insurance, emp);
    }
    @Test
    public void initInsuranceState() {
        InsuranceState state = new InsuranceState(insuranceCompany.getParty(), organization.getParty(), hospital.getParty(), new LinkedHashMap<>());
        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.input(InsuranceContract.ID, state);
                tx.output(InsuranceContract.ID, state);
                tx.command(organization.getPublicKey(), new InsuranceContract.Commands.Init());
                return tx.fails();
            });
            l.transaction(tx -> {
                tx.output(InsuranceContract.ID, state);
                tx.command(organization.getPublicKey(), new InsuranceContract.Commands.Init());
                return tx.verifies();
            });
            return null;
        });
    }
    @Test
    public void addInsuredEmployeeState() {
        InsuranceState state = new InsuranceState(insuranceCompany.getParty(), organization.getParty(), hospital.getParty(), new LinkedHashMap<>());
        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.output(InsuranceContract.ID, state);
                tx.command(organization.getPublicKey(), new InsuranceContract.Commands.Init());
                return tx.verifies();
            });
            l.transaction(tx -> {
                tx.input(InsuranceContract.ID, state);
                InsuranceState state2 = state.addInsuredEmployee("1212", insuredEmployee);
                tx.output(InsuranceContract.ID, state2);
                tx.command(organization.getPublicKey(), new InsuranceContract.Commands.AddInsuredEmployee("1212", insuredEmployee));
                return tx.verifies();
            });
            return null;
        });
    }

    @Test
    public void addTwoInsuredEmployeeState() {
        InsuranceState state = new InsuranceState(insuranceCompany.getParty(), organization.getParty(), hospital.getParty(), new LinkedHashMap<>());
        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.output(InsuranceContract.ID, state);
                tx.command(organization.getPublicKey(), new InsuranceContract.Commands.Init());
                return tx.verifies();
            });
            l.transaction(tx -> {
                tx.input(InsuranceContract.ID, state);
                InsuranceState state2 = state.addInsuredEmployee("1212", insuredEmployee);
                tx.output(InsuranceContract.ID, state2);
                tx.command(organization.getPublicKey(), new InsuranceContract.Commands.AddInsuredEmployee("1212", insuredEmployee));
                return tx.verifies();
            });
            l.transaction(tx -> {
                tx.input(InsuranceContract.ID, state);
                InsuranceState state2 = state.addInsuredEmployee("12123", insuredEmployee);
                tx.output(InsuranceContract.ID, state2);
                tx.command(organization.getPublicKey(), new InsuranceContract.Commands.AddInsuredEmployee("12123", insuredEmployee));
                return tx.verifies();
            });
            return null;
        });
    }

    @Test
    public void removeInsuredEmployeeState() {
        InsuranceState state = new InsuranceState(insuranceCompany.getParty(), organization.getParty(), hospital.getParty(), new LinkedHashMap<>());
        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.output(InsuranceContract.ID, state);
                tx.command(organization.getPublicKey(), new InsuranceContract.Commands.Init());
                return tx.verifies();
            });
            l.transaction(tx -> {
                tx.input(InsuranceContract.ID, state);
                InsuranceState state2 = state.addInsuredEmployee("1212", insuredEmployee);
                tx.output(InsuranceContract.ID, state2);
                tx.command(organization.getPublicKey(), new InsuranceContract.Commands.AddInsuredEmployee("1212", insuredEmployee));
                InsuranceState state3 = state2.removeInsuredEmployee("1212");
                tx.output(InsuranceContract.ID, state3);
                tx.command(organization.getPublicKey(), new InsuranceContract.Commands.RemoveInsuredEmployee("1212"));
                return tx.verifies();
            });
            return null;
        });
    }

}
