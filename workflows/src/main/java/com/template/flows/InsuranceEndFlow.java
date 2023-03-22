package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.template.contracts.InsuranceContract;
import com.template.states.InsuranceState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.StateRef;
import net.corda.core.flows.*;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@InitiatingFlow
@SchedulableFlow
public  class InsuranceEndFlow extends FlowLogic<Void> {
    private final StateRef stateRef;

    private final ProgressTracker progressTracker = tracker();

    private static final ProgressTracker.Step GENERATING_TRANSACTION = new ProgressTracker.Step("Generating a HeartState transaction");
    private static final ProgressTracker.Step SIGNING_TRANSACTION = new ProgressTracker.Step("Signing transaction with out private key.");
    private static final ProgressTracker.Step FINALISING_TRANSACTION = new ProgressTracker.Step("Recording transaction") {
        @Override
        public ProgressTracker childProgressTracker() {
            return FinalityFlow.tracker();
        }
    };

    private static ProgressTracker tracker() {
        return new ProgressTracker(
                GENERATING_TRANSACTION,
                SIGNING_TRANSACTION,
                FINALISING_TRANSACTION
        );
    }
    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    public InsuranceEndFlow(StateRef stateRef){
        this.stateRef = stateRef;
    }
    @Override
    @Suspendable
    public Void call() throws FlowException {
        final Party notary = getServiceHub().getNetworkMapCache().getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"));
        progressTracker.setCurrentStep(GENERATING_TRANSACTION);

        StateAndRef<InsuranceState> input = getServiceHub().toStateAndRef(stateRef);

        final TransactionBuilder builder = new TransactionBuilder(notary);
        builder.addInputState(input);
        builder.addCommand(new InsuranceContract.Commands.End(), getOurIdentity().getOwningKey());
        builder.verify(getServiceHub());

        progressTracker.setCurrentStep(SIGNING_TRANSACTION);
        final SignedTransaction signedTx = getServiceHub().signInitialTransaction(builder);

        progressTracker.setCurrentStep(FINALISING_TRANSACTION);
        subFlow(new FinalityFlow(signedTx, Collections.emptyList()));
        return null;
    }
}

