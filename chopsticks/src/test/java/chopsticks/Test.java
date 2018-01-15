package chopsticks;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Test {
	public static void main(String[] args) {

		ThreadPoolExecutor ex = new ThreadPoolExecutor(0, 5, 5, TimeUnit.SECONDS, new LinkedBlockingQueue<>(5));
		ex.submit(new Runnable() {

			@Override
			public void run() {
				System.out.println(Thread.currentThread().getName());
			}
		});
		ex.submit(new Runnable() {

			@Override
			public void run() {
				System.out.println(Thread.currentThread().getName());
			}
		});
	}
}
