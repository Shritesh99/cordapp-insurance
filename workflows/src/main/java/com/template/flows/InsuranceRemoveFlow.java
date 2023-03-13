package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.template.contracts.InsuranceContract;
import com.template.states.InsuranceState;
import net.corda.core.contracts.AttachmentResolutionException;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.TransactionResolutionException;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.serialization.CordaSerializable;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import org.jetbrains.annotations.NotNull;

import java.security.SignatureException;
import java.util.Arrays;
import java.util.Comparator;

public class InsuranceRemoveFlow {
    @CordaSerializable
    enum TransactionRole {ORGANIZATION, INSURANCE_COMPANY, HOSPITAL}

    @InitiatingFlow
    @StartableByRPC
    public static class InsuranceRemoveFlowInitiator extends FlowLogic<SignedTransaction> {

        private final Party insuranceCompany;
        private final Party hospital;
        private final String id;

        private final ProgressTracker.Step GENERATING_TRANSACTION = new ProgressTracker.Step("Generating transaction based on new IOU.");
        private final ProgressTracker.Step VERIFYING_TRANSACTION = new ProgressTracker.Step("Verifying contract constraints.");
        private final ProgressTracker.Step SIGNING_TRANSACTION = new ProgressTracker.Step("Signing transaction with our private key.");
        private final ProgressTracker.Step GATHERING_SIGS = new ProgressTracker.Step("Gathering the counterparty's signature.") {
            @Override
            public ProgressTracker childProgressTracker() {
                return CollectSignaturesFlow.Companion.tracker();
            }
        };
        private final ProgressTracker.Step FINALISING_TRANSACTION = new ProgressTracker.Step("Obtaining notary signature and recording transaction.") {
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
        public InsuranceRemoveFlowInitiator(Party insuranceCompany, Party hospital, String id){
            this.hospital = hospital;
            this.insuranceCompany = insuranceCompany;
            this.id = id;
        }
        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            Party organization = getOurIdentity();
            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

            progressTracker.setCurrentStep(GENERATING_TRANSACTION);
            QueryCriteria.VaultQueryCriteria criteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
            Vault.Page<InsuranceState> results = getServiceHub().getVaultService().queryBy(InsuranceState.class, criteria);

            StateAndRef<InsuranceState> latestInsuranceStateRef = results.getStates().stream()
                    .filter(insuranceStateRef -> insuranceStateRef.getState().getData().getInsuranceCompany().equals(insuranceCompany)
                            && insuranceStateRef.getState().getData().getOrganization().equals(organization)
                            && insuranceStateRef.getState().getData().getHospital().equals(hospital)
                    )
                    .max(Comparator.comparing(insuranceStateStateAndRef -> insuranceStateStateAndRef.getRef().getTxhash()))
                    .orElseThrow(() -> new FlowException("No InsuranceState found for the given insurance company and organization"));

            InsuranceState latestInsuranceState = latestInsuranceStateRef.getState().getData();
            InsuranceState outputState = latestInsuranceState.removeInsuredEmployee(id);
            final TransactionBuilder builder = new TransactionBuilder(notary);
            builder.addInputState(latestInsuranceStateRef);
            builder.addOutputState(outputState);
            builder.addCommand(new InsuranceContract.Commands.RemoveInsuredEmployee(id), Arrays.asList(this.insuranceCompany.getOwningKey(), organization.getOwningKey(), this.hospital.getOwningKey()));

            progressTracker.setCurrentStep(VERIFYING_TRANSACTION);
            builder.verify(getServiceHub());

            progressTracker.setCurrentStep(SIGNING_TRANSACTION);
            final SignedTransaction signedTx = getServiceHub().signInitialTransaction(builder);

            progressTracker.setCurrentStep(GATHERING_SIGS);
            FlowSession companySession = initiateFlow(insuranceCompany);
            companySession.send(TransactionRole.INSURANCE_COMPANY);

            FlowSession hospitalSession = initiateFlow(hospital);
            hospitalSession.send(TransactionRole.HOSPITAL);

            SignedTransaction stx = subFlow(new CollectSignaturesFlow(signedTx, Arrays.asList(companySession, hospitalSession), GATHERING_SIGS.childProgressTracker()));

            progressTracker.setCurrentStep(FINALISING_TRANSACTION);
            return subFlow(new FinalityFlow(stx, Arrays.asList(companySession, hospitalSession)));
        }
    }

    @InitiatedBy(InsuranceRemoveFlowInitiator.class)
    public static class InsuranceRemoveFlowResponder extends FlowLogic<SignedTransaction>{

        private final FlowSession counterpartySession;

        @NotNull
        private final ProgressTracker progressTracker;

        private final static ProgressTracker.Step RECEIVING_ROLE = new ProgressTracker.Step("Receiving role to impersonate.");
        private final static ProgressTracker.Step SIGNING_TRANSACTION = new ProgressTracker.Step("Signing transaction with our private key.") {
            @NotNull
            @Override
            public ProgressTracker childProgressTracker() {
                return SignTransactionFlow.Companion.tracker();
            }
        };

        private final static ProgressTracker.Step FINALISING_TRANSACTION = new ProgressTracker.Step("Obtaining notary signature and recording transaction.") {
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
        public InsuranceRemoveFlowResponder(FlowSession counterpartySession) {
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

            progressTracker.setCurrentStep(SIGNING_TRANSACTION);;
            final SignTransactionFlow signTransactionFlow = new SignTransactionFlow(counterpartySession) {
                @Suspendable
                @Override
                protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {
                    final boolean relevant;
                    try {
                        switch (myRole) {
                            case HOSPITAL: {
                                relevant = stx.toLedgerTransaction(getServiceHub(), false)
                                        .inputsOfType(InsuranceState.class)
                                        .stream()
                                        .anyMatch(it -> it.getHospital().equals(getOurIdentity()));
                            }
                            break;
                            case INSURANCE_COMPANY: {
                                relevant = stx.toLedgerTransaction(getServiceHub(), false)
                                        .inputsOfType(InsuranceState.class)
                                        .stream()
                                        .anyMatch(it -> it.getInsuranceCompany().equals(getOurIdentity()));
                            }
                            break;
                            default:
                                throw new FlowException("Unexpected value: " + myRole);
                        }
                    } catch (SignatureException | AttachmentResolutionException |
                             TransactionResolutionException ex) {
                        throw new FlowException(ex);
                    }
                    if (!relevant) throw new FlowException("Invalid Hospital");
                }
            };
            final SecureHash txId = subFlow(signTransactionFlow).getId();
            progressTracker.setCurrentStep(FINALISING_TRANSACTION);
            return subFlow(new ReceiveFinalityFlow(counterpartySession, txId));
        }
    }
}
