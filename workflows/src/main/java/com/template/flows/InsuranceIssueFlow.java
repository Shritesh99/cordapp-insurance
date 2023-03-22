package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.template.contracts.InsuranceContract;
import com.template.states.InsuranceState;
import net.corda.core.contracts.AttachmentResolutionException;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.TransactionResolutionException;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.CordaX500Name;
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
import java.util.List;
import java.util.stream.Collectors;

public class InsuranceIssueFlow {

    @CordaSerializable
    enum TransactionRole {ORGANIZATION, INSURANCE_COMPANY, HOSPITAL, AUDITOR}

    @InitiatingFlow
    @StartableByRPC
    public static class InsuranceIssueFlowInitiator extends FlowLogic<SignedTransaction>{

        private final Party hospital;
        private final Party organization;
        Party auditor;
        String empId;
        int amount;
        String policyNo;
        String endDate;

        public InsuranceIssueFlowInitiator(Party hospital, Party organization, Party auditor, String empId, int amount, String policyNo, String endDate) {
            this.hospital = hospital;
            this.organization = organization;
            this.auditor = auditor;
            this.empId = empId;
            this.amount = amount;
            this.policyNo = policyNo;
            this.endDate = endDate;
        }

        private final ProgressTracker.Step GENERATING_TRANSACTION = new ProgressTracker.Step("Generating transaction...");
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
        @Suspendable
        public SignedTransaction call() throws FlowException {
            Party insuranceCompany = getOurIdentity();
            final Party notary = getServiceHub().getNetworkMapCache().getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"));

            progressTracker.setCurrentStep(GENERATING_TRANSACTION);
            // Checking Emp ID exist
            QueryCriteria.VaultQueryCriteria criteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
            Vault.Page<InsuranceState> results = getServiceHub().getVaultService().queryBy(InsuranceState.class, criteria);

            boolean alreadyExist = results.getStates().stream()
                    .anyMatch(insuranceStateRef -> insuranceStateRef.getState().getData().getInsuranceCompany().equals(insuranceCompany)
                            && insuranceStateRef.getState().getData().getOrganization().equals(organization)
                            && insuranceStateRef.getState().getData().getHospital().equals(hospital)
                            && insuranceStateRef.getState().getData().getAuditor().equals(auditor)
                            && insuranceStateRef.getState().getData().getEmpId() == empId
                    );

            if(alreadyExist) throw new FlowException("Emp Id already exist");

            final InsuranceState output = new InsuranceState(insuranceCompany, organization, hospital,auditor, empId, amount, policyNo, endDate);
            assert notary != null;
            final TransactionBuilder builder = new TransactionBuilder(notary);
            builder.addOutputState(output);
            builder.addCommand(new InsuranceContract.Commands.Issue(), Arrays.asList(getOurIdentity().getOwningKey(), this.organization.getOwningKey(), this.hospital.getOwningKey(), this.auditor.getOwningKey()));

            progressTracker.setCurrentStep(VERIFYING_TRANSACTION);
            builder.verify(getServiceHub());

            progressTracker.setCurrentStep(SIGNING_TRANSACTION);
            final SignedTransaction signedTx = getServiceHub().signInitialTransaction(builder);

            progressTracker.setCurrentStep(GATHERING_SIGS);
            FlowSession orgSession = initiateFlow(organization);
            orgSession.send(TransactionRole.ORGANIZATION);

            FlowSession hospitalSession = initiateFlow(hospital);
            hospitalSession.send(TransactionRole.HOSPITAL);

            FlowSession auditorSession = initiateFlow(auditor);
            auditorSession.send(TransactionRole.AUDITOR);

            SignedTransaction stx = subFlow(new CollectSignaturesFlow(signedTx, Arrays.asList(hospitalSession, orgSession, auditorSession), GATHERING_SIGS.childProgressTracker()));

            progressTracker.setCurrentStep(FINALISING_TRANSACTION);
            return subFlow(new FinalityFlow(stx, Arrays.asList(orgSession, hospitalSession, auditorSession)));
        }
    }

    @InitiatedBy(InsuranceIssueFlowInitiator.class)
    public static class InsuranceIssueFlowResponder extends FlowLogic<SignedTransaction>{

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


        public InsuranceIssueFlowResponder(FlowSession counterpartySession) {
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
            final SignTransactionFlow signTransactionFlow = new SignTransactionFlow(counterpartySession) {
                @Suspendable
                @Override
                protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {
                    final boolean relevant;
                    try {
                        switch (myRole) {
                            case HOSPITAL: {
                                relevant = stx.toLedgerTransaction(getServiceHub(), false)
                                        .outputsOfType(InsuranceState.class)
                                        .stream()
                                        .anyMatch(it -> it.getHospital().equals(getOurIdentity()));
                            }
                            break;
                            case ORGANIZATION: {
                                relevant = stx.toLedgerTransaction(getServiceHub(), false)
                                        .outputsOfType(InsuranceState.class)
                                        .stream()
                                        .anyMatch(it -> it.getOrganization().equals(getOurIdentity()));
                            }
                            break;
                            case AUDITOR:{
                                // Check Attachment
                                // stx.toLedgerTransaction(getServiceHub(), false).component4().get(0).extractFile();
                                relevant = stx.toLedgerTransaction(getServiceHub(), false)
                                        .outputsOfType(InsuranceState.class)
                                        .stream()
                                        .anyMatch(it -> it.getAuditor().equals(getOurIdentity()));
                            }break;
                            default:
                                throw new FlowException("Unexpected value: " + myRole);
                        }
                    } catch (SignatureException | AttachmentResolutionException | TransactionResolutionException ex) {
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
