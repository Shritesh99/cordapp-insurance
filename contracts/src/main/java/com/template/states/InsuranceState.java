package com.template.states;

import com.template.contracts.InsuranceContract;
import com.template.models.Employee;
import com.template.models.Insurance;
import com.template.models.InsuredEmployee;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
@BelongsToContract(InsuranceContract.class)
public class InsuranceState implements ContractState {
    Party insuranceCompany;
    Party organization;
    Party hospital;
    LinkedHashMap<String, InsuredEmployee> insuredEmployees; // Replaced by DB

    public InsuranceState(Party insuranceCompany, Party organization, Party hospital, LinkedHashMap<String, InsuredEmployee> insuredEmployees) {
        this.insuranceCompany = insuranceCompany;
        this.organization = organization;
        this.hospital = hospital;
        this.insuredEmployees = insuredEmployees;
    }

    public Party getInsuranceCompany() {
        return insuranceCompany;
    }

    public Party getOrganization() {
        return organization;
    }

    public Party getHospital() {
        return hospital;
    }

    public LinkedHashMap<String, InsuredEmployee> getInsuredEmployees() {
        return insuredEmployees;
    }

    public InsuranceState addInsuredEmployee(String id, InsuredEmployee emp) {
        LinkedHashMap<String, InsuredEmployee> newMap = new LinkedHashMap<>(insuredEmployees);
        newMap.put(id, emp);
        return new InsuranceState(insuranceCompany, organization, hospital, newMap);
    }

    public InsuranceState removeInsuredEmployee(String id) {
        LinkedHashMap<String, InsuredEmployee> newMap = new LinkedHashMap<>(insuredEmployees);
        newMap.remove(id);
        return new InsuranceState(insuranceCompany, organization, hospital, newMap);
    }

    public boolean checkIdExist(String id){
        return this.insuredEmployees.containsKey(id);
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(insuranceCompany, organization, hospital);
    }
}
