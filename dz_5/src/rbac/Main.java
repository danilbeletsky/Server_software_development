import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class MultiThreadSimulation {

    private static final int THREAD_COUNT = 5;
    private static final int TASK_LENGTH = 30;

    private static final Object lock = new Object();

    public static void main(String[] args) throws InterruptedException {
        List<Thread> threads = new ArrayList<>();

        System.out.print("\033[H\033[2J");
        System.out.flush();

        for (int i = 0; i < THREAD_COUNT; i++) {
            int threadNumber = i + 1;

            Thread thread = new Thread(() -> runTask(threadNumber));
            threads.add(thread);
            thread.start();
        }

        for (Thread t : threads) {
            t.join();
        }

        System.out.println("\nВсе потоки завершены.");
    }

    private static void runTask(int threadNumber) {
        long threadId = Thread.currentThread().getId();
        long startTime = System.currentTimeMillis();

        StringBuilder progress = new StringBuilder();

        for (int i = 0; i < TASK_LENGTH; i++) {
            try {
                Thread.sleep(ThreadLocalRandom.current().nextInt(50, 150));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            progress.append("#");

            printProgress(threadNumber, threadId, progress.toString());
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        printResult(threadNumber, threadId, progress.toString(), duration);
    }

    private static void printProgress(int threadNumber, long threadId, String progress) {
        synchronized (lock) {
            moveCursorUp(THREAD_COUNT);

            for (int i = 1; i <= THREAD_COUNT; i++) {
                if (i == threadNumber) {
                    System.out.printf(
                            "Поток %d (ID=%d): [%-30s]\n",
                            threadNumber,
                            threadId,
                            progress
                    );
                } else {
                    System.out.println();
                }
            }
        }
    }

    private static void printResult(int threadNumber, long threadId, String progress, long duration) {
        synchronized (lock) {
            moveCursorUp(THREAD_COUNT);

            for (int i = 1; i <= THREAD_COUNT; i++) {
                if (i == threadNumber) {
                    System.out.printf(
                            "Поток %d (ID=%d): [%-30s] Время: %d мс\n",
                            threadNumber,
                            threadId,
                            progress,
                            duration
                    );
                } else {
                    System.out.println();
                }
            }
        }
    }

    private static void moveCursorUp(int lines) {
        if (lines > 0) {
            System.out.print("\033[" + lines + "A");
        }
    }
}