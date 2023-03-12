package com.template;

import com.google.common.collect.ImmutableList;
import com.template.contracts.InsuranceContract;
import com.template.flows.InsuranceInitFlow;
import com.template.flows.TemplateFlow;
import com.template.states.InsuranceState;
import com.template.states.TemplateState;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.StateAndContract;
import net.corda.core.contracts.TransactionState;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.testing.node.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.concurrent.Future;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class InsuranceInitFlowTests {

    private MockNetwork network;
    private StartedMockNode a;
    private StartedMockNode b;
    private StartedMockNode c;
    private Party hospital;
    private Party organization;
    private Party insuranceCompany;

    @Before
    public void setup() {
        network = new MockNetwork(new MockNetworkParameters()
                .withCordappsForAllNodes(ImmutableList.of(
                        TestCordapp.findCordapp("com.template.contracts"),
                        TestCordapp.findCordapp("com.template.flows")
                )).withNotarySpecs(ImmutableList.of(new MockNetworkNotarySpec(CordaX500Name.parse("O=Notary,L=London,C=GB")))));
        a = network.createPartyNode(null);
        b = network.createPartyNode(null);
        c = network.createPartyNode(null);
        hospital = a.getInfo().getLegalIdentities().get(0);
        organization = b.getInfo().getLegalIdentities().get(0);
        insuranceCompany = c.getInfo().getLegalIdentities().get(0);
        network.runNetwork();
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

    @Test
    public void flowReturnsSignedTransaction() throws Exception {
        InsuranceInitFlow.InsuranceInitFlowInitiator flow = new InsuranceInitFlow.InsuranceInitFlowInitiator(hospital, organization);
        CordaFuture<SignedTransaction> future = c.startFlow(flow);
        network.runNetwork();

        SignedTransaction signedTx = future.get();

        assertEquals(0, signedTx.getTx().getInputs().size());
        assertEquals(1, signedTx.getTx().getOutputs().size());
        assertTrue(signedTx.getTx().getOutputs().get(0).getData() instanceof InsuranceState);
        TransactionState<ContractState> stateAndContract = signedTx.getTx().getOutputs().get(0);
        assertEquals("com.template.contracts.InsuranceContract", stateAndContract.getContract());
        assertTrue(stateAndContract.getData() instanceof InsuranceState);
        InsuranceState output = (InsuranceState) stateAndContract.getData();
        assertEquals(hospital, output.getHospital());
        assertEquals(organization, output.getOrganization());
        assertEquals(c.getInfo().getLegalIdentities().get(0), output.getInsuranceCompany());

        for (StartedMockNode node : ImmutableList.of(a, b)) {
            SignedTransaction recordedTx = node.getServices().getValidatedTransactions().getTransaction(signedTx.getId());
            assertNotNull(recordedTx);
            assertEquals(signedTx, recordedTx);
        }
    }
}
