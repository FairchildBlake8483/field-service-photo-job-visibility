package fieldservice;

public record TechnicianFollowUp(String workOrder, String dispatchStatus, int deadLetterCount) {
    public String lesson() {
        return deadLetterCount > 0
                ? "ASSIGN_TECHNICIAN: photo dispatch needs a field check"
                : "CONTINUE_DISPATCH: photo delivery is moving";
    }
}
