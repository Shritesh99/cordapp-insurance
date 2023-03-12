package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.template.contracts.InsuranceContract;
import com.template.states.InsuranceState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

public class InsuranceInitFlow {

    @InitiatingFlow
    @StartableByRPC
    public static class InsuranceInitFlowInitiator extends FlowLogic<SignedTransaction>{

        private final Party hospital;
        private final Party organization;
        public InsuranceInitFlowInitiator(Party hospital, Party organization){
            this.hospital = hospital;
            this.organization = organization;
        }
        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            Party insuranceCompany = getOurIdentity();
            final Party notary = getServiceHub().getNetworkMapCache().getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"));

            final InsuranceState output = new InsuranceState(insuranceCompany, organization, hospital, new LinkedHashMap<>());
            final TransactionBuilder builder = new TransactionBuilder(notary);
            builder.addOutputState(output);
            builder.addCommand(new InsuranceContract.Commands.Init(), Arrays.asList(getOurIdentity().getOwningKey(), this.organization.getOwningKey(), this.hospital.getOwningKey()));
            builder.verify(getServiceHub());

            final SignedTransaction signedTx = getServiceHub().signInitialTransaction(builder);

            List<Party> otherParties = output.getParticipants().stream().map(el -> (Party)el).collect(Collectors.toList());
            otherParties.remove(getOurIdentity());
            List<FlowSession> sessions = otherParties.stream().map(el -> initiateFlow(el)).collect(Collectors.toList());
            SignedTransaction stx = subFlow(new CollectSignaturesFlow(signedTx, sessions));

            return subFlow(new FinalityFlow(stx, sessions));
        }
    }

    @InitiatedBy(InsuranceInitFlowInitiator.class)
    public static class InsuranceInitFlowResponder extends FlowLogic<Void>{

        private final FlowSession counterpartySession;

        //Constructor
        public InsuranceInitFlowResponder(FlowSession counterpartySession) {
            this.counterpartySession = counterpartySession;
        }

        @Override
        @Suspendable
        public Void call() throws FlowException {
            SignedTransaction signedTransaction = subFlow(new SignTransactionFlow(counterpartySession) {
                @Suspendable
                @Override
                protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {

                }
            });
            subFlow(new ReceiveFinalityFlow(counterpartySession, signedTransaction.getId()));
            return null;
        }
    }
}
