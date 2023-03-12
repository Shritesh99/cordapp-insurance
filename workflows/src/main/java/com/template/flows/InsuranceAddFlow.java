package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.template.contracts.InsuranceContract;
import com.template.models.InsuredEmployee;
import com.template.states.InsuranceState;
import net.corda.core.contracts.AttachmentResolutionException;
import net.corda.core.contracts.TransactionResolutionException;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria.VaultQueryCriteria;
import net.corda.core.serialization.CordaSerializable;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.core.utilities.ProgressTracker.Step;
import org.jetbrains.annotations.NotNull;

import java.security.SignatureException;
import java.util.Arrays;
import java.util.Comparator;

public class InsuranceAddFlow {
    @CordaSerializable
    enum TransactionRole {ORGANIZATION, INSURANCE_COMPANY, HOSPITAL}

    @InitiatingFlow
    @StartableByRPC
    public static class InsuranceAddFlowInitiator extends FlowLogic<SignedTransaction> {

        private final Party insuranceCompany;
        private final Party hospital;
        private final InsuredEmployee insuredEmployee;
        private final String id;

        private final Step GENERATING_TRANSACTION = new Step("Generating transaction based on new IOU.");
        private final Step VERIFYING_TRANSACTION = new Step("Verifying contract constraints.");
        private final Step SIGNING_TRANSACTION = new Step("Signing transaction with our private key.");
        private final Step GATHERING_SIGS = new Step("Gathering the counterparty's signature.") {
            @Override
            public ProgressTracker childProgressTracker() {
                return CollectSignaturesFlow.Companion.tracker();
            }
        };
        private final Step FINALISING_TRANSACTION = new Step("Obtaining notary signature and recording transaction.") {
            @Override
            public ProgressTracker childProgressTracker() {
                return FinalityFlow.Companion.tracker();
            }
        };

        private final ProgressTracker progressTracker = new ProgressTracker(
                GENERATING_TRANSACTION,
                VERIFYING_TRANSACTION,
                SIGNING_TRANSACTION,
                GATHERING_SIGS,
                FINALISING_TRANSACTION
        );
        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }
        public InsuranceAddFlowInitiator(Party insuranceCompany, Party hospital, InsuredEmployee insuredEmployee, String id){
            this.hospital = hospital;
            this.insuranceCompany = insuranceCompany;
            this.id = id;
            this.insuredEmployee = insuredEmployee;
        }
        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            Party organization = getOurIdentity();
            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

            progressTracker.setCurrentStep(GENERATING_TRANSACTION);
            VaultQueryCriteria criteria = new VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
            Vault.Page<InsuranceState> results = getServiceHub().getVaultService().queryBy(InsuranceState.class, criteria);

            StateAndRef<InsuranceState> latestInsuranceStateRef = results.getStates().stream()
                    .filter(insuranceStateRef -> insuranceStateRef.getState().getData().getInsuranceCompany().equals(insuranceCompany)
                            && insuranceStateRef.getState().getData().getOrganization().equals(organization)
                            && insuranceStateRef.getState().getData().getHospital().equals(hospital)
                    )
                    .max(Comparator.comparing(insuranceStateStateAndRef -> insuranceStateStateAndRef.getRef().getTxhash()))
                    .orElseThrow(() -> new FlowException("No InsuranceState found for the given insurance company and organization"));

            InsuranceState latestInsuranceState = latestInsuranceStateRef.getState().getData();
            InsuranceState outputState = latestInsuranceState.addInsuredEmployee(id, insuredEmployee);
            final TransactionBuilder builder = new TransactionBuilder(notary);
            builder.addOutputState(outputState);
            builder.addCommand(new InsuranceContract.Commands.AddInsuredEmployee(id, insuredEmployee), Arrays.asList(getOurIdentity().getOwningKey(), organization.getOwningKey(), this.hospital.getOwningKey()));

            progressTracker.setCurrentStep(VERIFYING_TRANSACTION);
            builder.verify(getServiceHub());

            progressTracker.setCurrentStep(SIGNING_TRANSACTION);
            final SignedTransaction signedTx = getServiceHub().signInitialTransaction(builder);

            progressTracker.setCurrentStep(GATHERING_SIGS);
            FlowSession companySession = initiateFlow(insuranceCompany);
            companySession.send(TransactionRole.INSURANCE_COMPANY);

            FlowSession hospitalSession = initiateFlow(hospital);
            companySession.send(TransactionRole.HOSPITAL);

            SignedTransaction stx = subFlow(new CollectSignaturesFlow(signedTx, Arrays.asList(companySession, hospitalSession), GATHERING_SIGS.childProgressTracker()));

            progressTracker.setCurrentStep(FINALISING_TRANSACTION);
            return subFlow(new FinalityFlow(stx, Arrays.asList(companySession, hospitalSession)));
        }
    }

    @InitiatedBy(InsuranceAddFlowInitiator.class)
    public static class InsuranceAddFlowResponder extends FlowLogic<SignedTransaction>{

        private final FlowSession counterpartySession;

        @NotNull
        private final ProgressTracker progressTracker;

        private final static Step RECEIVING_ROLE = new Step("Receiving role to impersonate.");
        private final static Step SIGNING_TRANSACTION = new Step("Signing transaction with our private key.") {
            @NotNull
            @Override
            public ProgressTracker childProgressTracker() {
                return SignTransactionFlow.Companion.tracker();
            }
        };

        private final static Step FINALISING_TRANSACTION = new Step("Obtaining notary signature and recording transaction.") {
            @NotNull
            @Override
            public ProgressTracker childProgressTracker() {
                return FinalityFlow.Companion.tracker();
            }
        };

        @NotNull
        public static ProgressTracker tracker() {
            return new ProgressTracker(
                    RECEIVING_ROLE,
                    SIGNING_TRANSACTION,
                    FINALISING_TRANSACTION);
        }


        //Constructor
        public InsuranceAddFlowResponder(FlowSession counterpartySession) {
            this.counterpartySession = counterpartySession;
            this.progressTracker = tracker();
        }


        @NotNull
        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }
        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            progressTracker.setCurrentStep(RECEIVING_ROLE);
            final TransactionRole myRole = counterpartySession.receive(TransactionRole.class).unwrap(it -> it);

            progressTracker.setCurrentStep(SIGNING_TRANSACTION);
            final SecureHash txId;
            switch (myRole) {
                // We do not need to sign.
                case HOSPITAL:{
                    final SignTransactionFlow signTransactionFlow = new SignTransactionFlow(counterpartySession) {
                        @Override
                        protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {
                            final boolean relevant;
                            try {
                                relevant = stx.toLedgerTransaction(getServiceHub(), false)
                                        .inputsOfType(InsuranceState.class)
                                        .stream()
                                        .anyMatch(it -> it.getHospital().equals(getOurIdentity()));
                            } catch (SignatureException | AttachmentResolutionException | TransactionResolutionException ex) {
                                throw new FlowException(ex);
                            }
                            if (!relevant) throw new FlowException("Invalid Company");
                        }
                    };
                    txId = subFlow(signTransactionFlow).getId();
                }
                    break;
                case INSURANCE_COMPANY: {
                    final SignTransactionFlow signTransactionFlow = new SignTransactionFlow(counterpartySession) {
                        @Override
                        protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {
                            final boolean relevant;
                            try {
                                relevant = stx.toLedgerTransaction(getServiceHub(), false)
                                        .inputsOfType(InsuranceState.class)
                                        .stream()
                                        .anyMatch(it -> it.getInsuranceCompany().equals(getOurIdentity()));
                            } catch (SignatureException | AttachmentResolutionException | TransactionResolutionException ex) {
                                throw new FlowException(ex);
                            }
                            if (!relevant) throw new FlowException("Invalid Company");
                        }
                    };
                    txId = subFlow(signTransactionFlow).getId();
                }
                break;
                default:
                    throw new FlowException("Unexpected value: " + myRole);
            }

            progressTracker.setCurrentStep(FINALISING_TRANSACTION);
            return subFlow(new ReceiveFinalityFlow(counterpartySession, txId));
        }
    }
}
