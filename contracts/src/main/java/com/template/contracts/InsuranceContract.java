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

        if (commandData instanceof InsuranceContract.Commands.Init) {
            requireThat(require -> {
                require.using("No inputs should be consumed when sending the Init Command", tx.getInputStates().size() == 0);
                require.using("Only one should be consumed when sending the Init Command", tx.getOutputStates().size() == 1);
                return null;
            });
        }
        if(commandData instanceof InsuranceContract.Commands.AddInsuredEmployee){
            InsuranceState input = tx.inputsOfType(InsuranceState.class).get(0);
            InsuranceState output = tx.outputsOfType(InsuranceState.class).get(0);
            requireThat(require -> {
                require.using("Id already exist", !input.checkIdExist(((Commands.AddInsuredEmployee) commandData).id));
                require.using("Id does not exist", output.checkIdExist(((Commands.AddInsuredEmployee) commandData).id));
                return null;
            });
        }
        if(commandData instanceof InsuranceContract.Commands.RemoveInsuredEmployee){
            InsuranceState input = tx.inputsOfType(InsuranceState.class).get(0);
            InsuranceState output = tx.outputsOfType(InsuranceState.class).get(0);
            requireThat(require -> {
                require.using("Id does not exist", input.checkIdExist(((Commands.RemoveInsuredEmployee) commandData).id));
                require.using("Id already exist", !output.checkIdExist(((Commands.RemoveInsuredEmployee) commandData).id));
                return null;
            });
        }
    }

    public interface Commands extends CommandData {
        class Init extends TypeOnlyCommandData implements Commands {}
        class AddInsuredEmployee implements Commands {
            private String id;
            private InsuredEmployee insuredEmployee;
            @ConstructorForDeserialization
            public AddInsuredEmployee(String id, InsuredEmployee insuredEmployee) {
                this.id = id;
                this.insuredEmployee = insuredEmployee;
            }

            public AddInsuredEmployee(String id, String insuredEmpId, Employee emp, Insurance insurance){
                this.insuredEmployee = new InsuredEmployee(insuredEmpId, insurance, emp);
            }
        }
        class RemoveInsuredEmployee implements Commands {
            private String id;
            @ConstructorForDeserialization
            public RemoveInsuredEmployee(String id) {
                this.id = id;
            }
        }
    }
}
