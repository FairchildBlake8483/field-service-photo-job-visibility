package fieldservice;

public final class TechnicianFollowUpTest {
    public static void main(String[] args) {
        TechnicianFollowUp stalled = new TechnicianFollowUp("WO-204", "DISPATCHED", 1);
        if (!"ASSIGN_TECHNICIAN: photo dispatch needs a field check".equals(stalled.lesson())) {
            throw new AssertionError("A dead-lettered photo must create technician follow-up.");
        }
    }
}
