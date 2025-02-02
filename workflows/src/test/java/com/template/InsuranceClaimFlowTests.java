package com.template;

import com.google.common.collect.ImmutableList;
import com.template.flows.InsuranceClaimFlow;
import com.template.flows.InsuranceIssueFlow;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.testing.node.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class InsuranceClaimFlowTests {
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

//    @Test
//    public void flowReturnsSignedTransaction() throws Exception {
//        InsuranceIssueFlow.InsuranceInitFlowInitiator flow = new InsuranceIssueFlow.InsuranceInitFlowInitiator(hospital, organization);
//        CordaFuture<SignedTransaction> future = c.startFlow(flow);
//        network.runNetwork();
//
//        SignedTransaction signedTx = future.get();
//        assertEquals(0, signedTx.getTx().getInputs().size());
//
//        InsuranceClaimFlow.InsuranceAddFlowInitiator flow2 = new InsuranceClaimFlow.InsuranceAddFlowInitiator(insuranceCompany, hospital, "123", "1234");
//        CordaFuture<SignedTransaction> future1 = b.startFlow(flow2);
//        network.runNetwork();
//
//        SignedTransaction signedTx1 = future1.get();
//
//        assertEquals(1, signedTx1.getTx().getOutputs().size());
//    }
}
