package com.template.contracts;

import com.template.models.Employee;
import com.template.models.Insurance;
import com.template.models.InsuredEmployee;
import com.template.models.Organization;
import com.template.states.InsuranceState;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.TypeOnlyCommandData;
import net.corda.core.serialization.ConstructorForDeserialization;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class InsuranceContract implements Contract {

    public static final String ID = "com.template.contracts.InsuranceContract";
    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {
        final CommandData commandData = tx.getCommands().get(0).getValue();

        if (commandData instanceof InsuranceContract.Commands.Issue) {
            requireThat(require -> {
                require.using("No inputs should be consumed when sending the Issue Command", tx.getInputStates().size() == 0);
                require.using("Only one should be consumed when sending the Issue Command", tx.getOutputStates().size() == 1);
                return null;
            });
        }
        if(commandData instanceof InsuranceContract.Commands.Claim){
            InsuranceState input = tx.inputsOfType(InsuranceState.class).get(0);
            InsuranceState output = tx.outputsOfType(InsuranceState.class).get(0);
            requireThat(require -> {
                require.using("Only One should be consumed when sending the Claim Command", tx.getInputStates().size() == 1);
                require.using("Only one should be consumed when sending the Claim Command", tx.getOutputStates().size() == 1);
                require.using("Claim should be less than the total amount", output.getAmount()>=output.getClaimed());
                return null;
            });
        }
        if(commandData instanceof InsuranceContract.Commands.End){
            InsuranceState input = tx.inputsOfType(InsuranceState.class).get(0);
            InsuranceState output = tx.outputsOfType(InsuranceState.class).get(0);
            requireThat(require -> {
                require.using("Only one should be consumed when sending the End Command", tx.getInputStates().size() == 1);
                require.using("No inputs should be consumed when sending the End Command", tx.getOutputStates().size() == 0);
                return null;
            });
        }
    }

    public interface Commands extends CommandData {
        class Issue extends TypeOnlyCommandData implements Commands {}
        class Claim extends TypeOnlyCommandData implements Commands {}
        class End extends TypeOnlyCommandData implements Commands {}
    }
}
