# Flow commands

### Issue flow
```
flow start InsuranceIssueFlow$InsuranceIssueFlowInitiator hospital: "O=Hospital,L=Mumbai,C=IN", organization: "O=Organization,L=London,C=GB", auditor: "O=Auditor,L=Dublin,C=IE", empId: "123", amount: 1000, policyNo: "111", endDate: "2033-02-15T00:00:00.00Z"
```
### Check
```
run vaultQuery contractStateType: com.template.states.InsuranceState
```
### Clam flow
```
flow start InsuranceClaimFlow$InsuranceIssueClaimInitiator hospital: "O=Hospital,L=Mumbai,C=IN", organization: "O=Organization,L=London,C=GB", auditor: "O=Auditor,L=Dublin,C=IE", policyNo: "111", claim: 10
```
