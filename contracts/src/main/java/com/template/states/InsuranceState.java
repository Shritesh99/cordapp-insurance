package com.template.states;

import com.template.contracts.InsuranceContract;
import com.template.models.Employee;
import com.template.models.Insurance;
import com.template.models.InsuredEmployee;
import net.corda.core.contracts.*;
import net.corda.core.flows.FlowLogicRefFactory;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.serialization.ConstructorForDeserialization;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
@BelongsToContract(InsuranceContract.class)
public class InsuranceState implements SchedulableState {
    Party insuranceCompany;
    Party organization;
    Party hospital;
    Party auditor;

    private final String empId;
    private final int amount;
    private int claimed;
    private String policyNo;

    public String getEndDate() {
        return endDate;
    }
    private final String endDate;
    private final Instant policyEndInstant;

    public InsuranceState(Party insuranceCompany, Party organization, Party hospital, Party auditor, String empId, int amount, int claimed, String policyNo, String endDate, Instant policyEndInstant) {
        this.insuranceCompany = insuranceCompany;
        this.organization = organization;
        this.hospital = hospital;
        this.auditor = auditor;
        this.empId = empId;
        this.amount = amount;
        this.claimed = claimed;
        this.policyNo = policyNo;
        this.endDate = endDate;
        this.policyEndInstant = policyEndInstant;
    }
    @ConstructorForDeserialization
    public InsuranceState(Party insuranceCompany, Party organization, Party hospital, Party auditor, String empId, int amount, String policyNo, String endDate) {
        this.insuranceCompany = insuranceCompany;
        this.organization = organization;
        this.hospital = hospital;
        this.auditor = auditor;
        this.empId = empId;
        this.amount = amount;
        this.claimed = 0;
        this.policyNo = policyNo;
        this.endDate = endDate;
        this.policyEndInstant = Instant.parse(endDate);
    }

    public String getEmpId() {
        return empId;
    }

    public int getAmount() {
        return amount;
    }

    public int getClaimed() {
        return claimed;
    }

    public String getPolicyNo() {
        return policyNo;
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

    public Party getAuditor() {
        return auditor;
    }

    public InsuranceState claimAmount(int claim){
        return new InsuranceState(this.insuranceCompany,
        this.organization,
        this.hospital,
        this.auditor,
        this.empId,
        this.amount-claim,
        claim,
        this.policyNo,
        this.endDate,
        this.policyEndInstant);
    }
    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(insuranceCompany, organization, hospital, auditor);
    }

    @Nullable
    @Override
    public ScheduledActivity nextScheduledActivity(@NotNull StateRef thisStateRef, @NotNull FlowLogicRefFactory flowLogicRefFactory) {
        return new ScheduledActivity(flowLogicRefFactory.create("com.template.flows.InsuranceEndFlow", thisStateRef), policyEndInstant);
    }
}
