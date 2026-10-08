package fieldservice;

/**
 * Run with one of: register, handoff, capture, inspect.
 * The same InfraiClient, base URL, and INFRAI_API_KEY cover every command.
 */
public final class FieldServiceFailureLesson {
    public static void main(String[] args) throws Exception {
        String command = args.length == 0 ? "inspect" : args[0];
        InfraiClient infrai = new InfraiClient(InfraiConfig.fromEnvironment());
        switch (command) {
            case "register" -> System.out.println(infrai.createPhotoDispatchCron("0 */10 * * *"));
            case "handoff" -> System.out.println(infrai.publishWorkOrderPhoto(
                    new WorkOrderPhoto("WO-204", "after-repair-photo-204", "DISPATCHED")));
            case "capture" -> System.out.println(infrai.captureDispatchException(
                    "Photo dispatch needs technician follow-up for WO-204"));
            case "inspect" -> inspect(infrai);
            default -> throw new IllegalArgumentException("Use register, handoff, capture, or inspect.");
        }
    }

    private static void inspect(InfraiClient infrai) throws Exception {
        String jobId = System.getenv("FIELD_SERVICE_JOB_ID");
        if (jobId == null || jobId.isBlank()) {
            throw new IllegalStateException("Set FIELD_SERVICE_JOB_ID from the register command.");
        }
        String runs = infrai.listCronRuns(jobId);
        String deadLetters = infrai.listPhotoDispatchDeadLetters("photo-dispatch");
        String errors = infrai.listCapturedErrors();
        int deadLetterCount = deadLetters.contains("message_id") ? 1 : 0;
        TechnicianFollowUp followUp = new TechnicianFollowUp("WO-204", "DISPATCHED", deadLetterCount);
        System.out.println("runs=" + runs);
        System.out.println("capturedErrors=" + errors);
        System.out.println(followUp.lesson());
    }
}
