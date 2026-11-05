package dev.ldv.timeout;

final class ExceptionChain {

    private ExceptionChain() {
    }

    static boolean hasCauseNamed(Throwable throwable, String simpleName) {
        return throwable != null
                && (throwable.getClass().getSimpleName().equals(simpleName)
                        || hasCauseNamed(throwable.getCause(), simpleName));
    }

    static String describe(Throwable throwable) {
        StringBuilder result = new StringBuilder();
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            if (!result.isEmpty()) {
                result.append(System.lineSeparator()).append("caused by: ");
            }
            result.append(current.getClass().getName()).append(": ").append(current.getMessage());
        }
        return result.toString();
    }
}
